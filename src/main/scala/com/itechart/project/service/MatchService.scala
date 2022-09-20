package com.itechart.project.service

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.util.Timeout
import com.itechart.project.domain.football_match.{Match, MatchId, Status}
import com.itechart.project.domain.formation.FormationId
import com.itechart.project.domain.league.LeagueId
import com.itechart.project.domain.mail.Mail
import com.itechart.project.domain.match_stats.MatchStatsId
import com.itechart.project.domain.referee.RefereeId
import com.itechart.project.domain.season.SeasonId
import com.itechart.project.domain.stage.StageId
import com.itechart.project.domain.team.{Team, TeamId}
import com.itechart.project.domain.user.User
import com.itechart.project.domain.venue.VenueId
import com.itechart.project.dto.football_match.MatchApiDto
import com.itechart.project.repository._
import com.itechart.project.service.CommonServiceMessages.ErrorWrapper
import com.itechart.project.service.CommonServiceMessages.Requests._
import com.itechart.project.service.CommonServiceMessages.Responses._
import com.itechart.project.service.domain_errors.MatchErrors.MatchError
import com.itechart.project.service.domain_errors.MatchErrors.MatchError._
import eu.timepit.refined.auto._

import java.sql.SQLIntegrityConstraintViolationException
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class MatchService(
  matchRepository:             MatchRepository,
  seasonRepository:            SeasonRepository,
  leagueRepository:            LeagueRepository,
  stageRepository:             StageRepository,
  teamRepository:              TeamRepository,
  refereeRepository:           RefereeRepository,
  venueRepository:             VenueRepository,
  formationRepository:         FormationRepository,
  userSubscriptionsRepository: UserSubscriptionsRepository,
  userRepository:              UserRepository,
  mailerService:               ActorRef
)(
  implicit ec: ExecutionContext,
  timeout:     Timeout
) extends Actor
    with ActorLogging {
  import MatchService._

  override def receive: Receive = {
    case GetEntityByT(id: Long) =>
      val senderToReturn = sender()
      log.info(s"Getting match with id = $id")
      val matchFuture = matchRepository.findById(MatchId(id))
      matchFuture.onComplete {
        case Success(maybeMatch) =>
          log.info(s"Match with id = $id ${if (maybeMatch.isEmpty) "not " else ""}found")
          senderToReturn ! OneFoundEntity(maybeMatch.map(domainMatchToDtoMatch))
        case Failure(ex) =>
          log.error(s"An error occurred while extracting a match with id = $id: $ex")
          senderToReturn ! InternalServerError
      }

    case GetEntityByT(date: LocalDate) =>
      val senderToReturn = sender()
      log.info(s"Getting matches with date = $date")
      val matchFuture = matchRepository.findByDate(date)
      matchFuture.onComplete {
        case Success(matches) =>
          log.info(s"Got ${matches.size} matches with date = $date out of database")
          senderToReturn ! AllFoundMatches(matches.map(domainMatchToDtoMatch))
        case Failure(ex) =>
          log.error(s"An error occurred while extracting matches with date = $date: $ex")
          senderToReturn ! InternalServerError
      }

    case AddOneEntity(matchDto: MatchApiDto) =>
      val senderToReturn = sender()
      log.info(s"Adding a match = $matchDto")
      val validatedMatch = validateMatchDto(matchDto)
      validatedMatch match {
        case Left(errors) =>
          logErrorsAndSend(senderToReturn, matchDto, errors)
        case Right(footballMatch) =>
          val matchIdOrErrors = for {
            errors  <- validateMatchDuplicates(footballMatch)
            matchId <- if (errors.isEmpty) matchRepository.create(footballMatch) else Future(MatchId(0))
            result   = if (matchId.value == 0) Left(errors) else Right(matchId)
          } yield result
          matchIdOrErrors.onComplete {
            case Success(Right(id)) =>
              log.info(s"Match $footballMatch successfully created")
              senderToReturn ! OneEntityAdded(matchDto.copy(id = id.value))
            case Success(Left(errors)) =>
              log.info(s"Match $footballMatch doesn't created because of: ${errors.mkString("[", ", ", "]")}")
              senderToReturn ! ValidationErrors(MatchErrorWrapper(errors))
            case Failure(ex) =>
              log.error(s"An error occurred while creating a match $footballMatch: $ex")
              senderToReturn ! InternalServerError
          }
      }

    case UpdateEntity(matchDto: MatchApiDto) =>
      val senderToReturn = sender()
      log.info(s"Updating a match = $matchDto")
      val validatedMatch = validateMatchDto(matchDto)
      validatedMatch match {
        case Left(errors) =>
          logErrorsAndSend(senderToReturn, matchDto, errors)
        case Right(footballMatch) =>
          val rowsUpdatedOrErrors = for {
            errors        <- validateMatchDuplicates(footballMatch)
            previousMatch <- matchRepository.findById(footballMatch.id)
            rowsUpdated   <- if (errors.isEmpty) matchRepository.update(footballMatch) else Future(-1)
            _ = if (rowsUpdated != -1) {
              previousMatch.map(sendMail(_, footballMatch))
            }
            result = if (rowsUpdated == -1) Left(errors) else Right(rowsUpdated)
          } yield result
          rowsUpdatedOrErrors.onComplete {
            case Success(Right(rowsUpdated)) =>
              log.info(s"Match $footballMatch is ${if (rowsUpdated == 0) "not " else ""}updated")
              val result = if (rowsUpdated == 0) UpdateFailed else UpdateCompleted
              senderToReturn ! result
            case Success(Left(errors)) =>
              log.info(s"Match $footballMatch isn't updated because of: ${errors.mkString("[", ", ", "]")}")
              senderToReturn ! ValidationErrors(MatchErrorWrapper(errors))
            case Failure(ex) =>
              log.error(s"An error occurred while updating a match $footballMatch: $ex")
              senderToReturn ! InternalServerError
          }
      }

    case RemoveEntity(id: Int) =>
      val senderToReturn = sender()
      log.info(s"Deleting match with id = $id")
      val matchFuture = matchRepository.delete(MatchId(id))
      matchFuture.onComplete {
        case Success(rowsDeleted) =>
          log.info(s"Match with id = $id ${if (rowsDeleted == 0) "not " else ""}removed")
          val result = if (rowsDeleted == 0) RemoveFailed else RemoveCompleted
          senderToReturn ! result
        case Failure(_: SQLIntegrityConstraintViolationException) =>
          log.info(s"A match with id = $id can't be deleted because it's a part of foreign key")
          senderToReturn ! ValidationErrors(MatchErrorWrapper(List(MatchForeignKey(id))))
        case Failure(ex) =>
          log.error(s"An error occurred while deleting a match with id = $id: $ex")
          senderToReturn ! InternalServerError
      }
  }

  private def sendMail(previousMatch: Match, currentMatch: Match) = {
    if (previousMatch.status != Status.InPlay && currentMatch.status == Status.InPlay) {
      for {
        (team1, team2) <- findTeams(currentMatch.homeTeamId, currentMatch.awayTeamId)
        emails         <- findEmails(team1, team2)
        _ = emails.foreach { user =>
          mailerService ! Mail(
            user.email,
            "Match Started",
            s"Match between team ${team1.name} and ${team2.name} started",
            currentMatch.id
          )
        }
      } yield ()
    } else if (previousMatch.status == Status.InPlay && currentMatch.status == Status.Ended) {
      for {
        (team1, team2) <- findTeams(currentMatch.homeTeamId, currentMatch.awayTeamId)
        emails         <- findEmails(team1, team2)
        _ = emails.foreach { user =>
          mailerService ! Mail(
            user.email,
            "Match Ended",
            s"Match between team ${team1.name} and ${team2.name} ended",
            currentMatch.id
          )
        }
      } yield ()
    }
  }

  private def findTeams(teamId1: TeamId, teamId2: TeamId): Future[(Team, Team)] = for {
    team1 <- teamRepository.findById(teamId1).map(_.head)
    team2 <- teamRepository.findById(teamId2).map(_.head)
  } yield (team1, team2)

  private def findEmails(team1: Team, team2: Team): Future[Set[User]] = for {
    subscription1 <- userSubscriptionsRepository.findTeamSubscriptionsByTeam(team1)
    subscription2 <- userSubscriptionsRepository.findTeamSubscriptionsByTeam(team2)
    subscriptions  = subscription1.map(_.userId).toSet ++ subscription2.map(_.userId).toSet
    users         <- Future.sequence(subscriptions.map(id => userRepository.findById(id).map(_.head)))
  } yield users

  private def logErrorsAndSend(sender: ActorRef, matchDto: MatchApiDto, errors: List[MatchError]): Unit = {
    log.info(s"Validation of match = $matchDto failed because of: ${errors.mkString("[", ", ", "]")}")
    sender ! ValidationErrors(MatchErrorWrapper(errors))
  }

  private def validateMatchDuplicates(footballMatch: Match): Future[List[MatchError]] = for {
    maybeSeason            <- seasonRepository.findById(footballMatch.seasonId)
    maybeLeague            <- leagueRepository.findById(footballMatch.leagueId)
    maybeStage             <- stageRepository.findById(footballMatch.stageId)
    maybeHomeTeam          <- teamRepository.findById(footballMatch.homeTeamId)
    maybeAwayTeam          <- teamRepository.findById(footballMatch.awayTeamId)
    maybeVenue             <- venueRepository.findById(footballMatch.venueId)
    maybeReferee           <- refereeRepository.findById(footballMatch.refereeId)
    maybeHomeTeamFormation <- formationRepository.findById(footballMatch.homeTeamFormationId)
    maybeAwayTeamFormation <- formationRepository.findById(footballMatch.awayTeamFormationId)
    seasonNotFoundError <- Future(
      if (maybeSeason.isEmpty) List(InvalidMatchSeasonId(footballMatch.seasonId.value)) else List()
    )
    leagueNotFoundError <- Future(
      if (maybeLeague.isEmpty) List(InvalidMatchLeagueId(footballMatch.leagueId.value)) else List()
    )
    stageNotFoundError <- Future(
      if (maybeStage.isEmpty) List(InvalidMatchStageId(footballMatch.stageId.value)) else List()
    )
    homeTeamNotFoundError <- Future(
      if (maybeHomeTeam.isEmpty) List(InvalidMatchHomeTeamId(footballMatch.homeTeamId.value)) else List()
    )
    awayTeamNotFoundError <- Future(
      if (maybeAwayTeam.isEmpty) List(InvalidMatchAwayTeamId(footballMatch.awayTeamId.value)) else List()
    )
    venueNotFoundError <- Future(
      if (maybeVenue.isEmpty) List(InvalidMatchVenueId(footballMatch.venueId.value)) else List()
    )
    refereeNotFoundError <- Future(
      if (maybeReferee.isEmpty) List(InvalidMatchRefereeId(footballMatch.refereeId.value)) else List()
    )
    formationHomeTeamNotFoundError <- Future(
      if (maybeHomeTeamFormation.isEmpty) List(InvalidMatchHomeTeamFormationId(footballMatch.homeTeamFormationId.value))
      else List()
    )
    formationAwayTeamNotFoundError <- Future(
      if (maybeAwayTeamFormation.isEmpty) List(InvalidMatchAwayTeamFormationId(footballMatch.awayTeamFormationId.value))
      else List()
    )
  } yield seasonNotFoundError ++ leagueNotFoundError ++ stageNotFoundError ++ homeTeamNotFoundError ++
    awayTeamNotFoundError ++ venueNotFoundError ++ refereeNotFoundError ++ formationHomeTeamNotFoundError ++ formationAwayTeamNotFoundError

  private def validateMatchDto(matchDto: MatchApiDto): Either[List[MatchError], Match] = {
    val validatedStatusEither = Status.withNameEither(matchDto.status)
    val validatedDateEither =
      if (LocalDate.now().plus(6, ChronoUnit.MONTHS).isAfter(matchDto.startDate)) Right(matchDto.startDate)
      else Left(InvalidMatchStartDate(matchDto.startDate))

    val statusErrorList = if (validatedStatusEither.isLeft) List(InvalidMatchStatus(matchDto.status)) else List()
    val dateErrorList   = if (validatedDateEither.isLeft) List(InvalidMatchStartDate(matchDto.startDate)) else List()
    val errorsList: List[MatchError] = statusErrorList ++ dateErrorList

    val result = for {
      status <- validatedStatusEither
      date   <- validatedDateEither
    } yield Match(
      MatchId(matchDto.id),
      SeasonId(matchDto.seasonId),
      LeagueId(matchDto.leagueId),
      StageId(matchDto.stageId),
      status,
      date,
      matchDto.startTime,
      TeamId(matchDto.homeTeamId),
      TeamId(matchDto.awayTeamId),
      VenueId(matchDto.venueId),
      RefereeId(matchDto.refereeId),
      MatchStatsId(matchDto.matchStatsId),
      FormationId(matchDto.homeTeamFormationId),
      FormationId(matchDto.awayTeamFormationId)
    )

    result.left.map(_ => errorsList)
  }

  private def domainMatchToDtoMatch(footballMatch: Match): MatchApiDto =
    MatchApiDto(
      footballMatch.id.value,
      footballMatch.seasonId.value,
      footballMatch.leagueId.value,
      footballMatch.stageId.value,
      footballMatch.status.toString,
      footballMatch.startDate,
      footballMatch.startTime,
      footballMatch.homeTeamId.value,
      footballMatch.awayTeamId.value,
      footballMatch.venueId.value,
      footballMatch.refereeId.value,
      footballMatch.matchStatsId.value,
      footballMatch.homeTeamFormationId.value,
      footballMatch.awayTeamFormationId.value
    )
}

object MatchService {
  def props(
    matchRepository:             MatchRepository,
    seasonRepository:            SeasonRepository,
    leagueRepository:            LeagueRepository,
    stageRepository:             StageRepository,
    teamRepository:              TeamRepository,
    refereeRepository:           RefereeRepository,
    venueRepository:             VenueRepository,
    formationRepository:         FormationRepository,
    userSubscriptionsRepository: UserSubscriptionsRepository,
    userRepository:              UserRepository,
    mailerService:               ActorRef
  )(
    implicit ec: ExecutionContext,
    timeout:     Timeout
  ): Props = Props(
    new MatchService(
      matchRepository,
      seasonRepository,
      leagueRepository,
      stageRepository,
      teamRepository,
      refereeRepository,
      venueRepository,
      formationRepository,
      userSubscriptionsRepository,
      userRepository,
      mailerService
    )
  )

  case class AllFoundMatches(matches: List[MatchApiDto])
  case class MatchErrorWrapper(override val errors: List[MatchError]) extends ErrorWrapper
}
