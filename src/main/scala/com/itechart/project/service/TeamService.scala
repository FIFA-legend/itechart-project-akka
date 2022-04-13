package com.itechart.project.service

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.util.Timeout
import akka.pattern.ask
import com.itechart.project.domain.country.CountryId
import com.itechart.project.domain.team.{Team, TeamId, TeamLogo}
import com.itechart.project.dto.team.TeamApiDto
import com.itechart.project.repository.{CountryRepository, TeamRepository}
import com.itechart.project.service.CommonServiceMessages.ErrorWrapper
import com.itechart.project.service.CommonServiceMessages.Requests._
import com.itechart.project.service.CommonServiceMessages.Responses._
import com.itechart.project.service.domain_errors.TeamErrors.TeamError
import com.itechart.project.service.domain_errors.TeamErrors.TeamError._
import com.itechart.project.utils.RefinedConversions.validateParameter
import eu.timepit.refined.W
import eu.timepit.refined.predicates.all.NonEmpty
import eu.timepit.refined.string.MatchesRegex

import java.sql.SQLIntegrityConstraintViolationException
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class TeamService(
  teamRepository:    TeamRepository,
  countryRepository: CountryRepository
)(
  implicit ec: ExecutionContext,
  timeout:     Timeout
) extends Actor
    with ActorLogging {
  import TeamService._

  override def receive: Receive = {
    case GetAllEntities =>
      val senderToReturn = sender()
      log.info("Getting all teams from database")
      val teamsFuture = teamRepository.findAll
      teamsFuture.onComplete {
        case Success(teams) =>
          log.info(s"Got ${teams.size} teams out of database")
          senderToReturn ! AllFoundTeams(teams.map(domainTeamToDtoTeam))
        case Failure(ex) =>
          log.error(s"An error occurred while extracting all teams out of database: $ex")
          senderToReturn ! InternalServerError
      }

    case GetEntityByT(id: Int) =>
      val senderToReturn = sender()
      log.info(s"Getting team with id = $id")
      val teamFuture = teamRepository.findById(TeamId(id))
      teamFuture.onComplete {
        case Success(maybeTeam) =>
          log.info(s"Team with id = $id ${if (maybeTeam.isEmpty) "not "}found")
          senderToReturn ! OneFoundEntity(maybeTeam.map(domainTeamToDtoTeam))
        case Failure(ex) =>
          log.error(s"An error occurred while extracting a team with id = $id: $ex")
          senderToReturn ! InternalServerError
      }

    case GetEntityByT(name: String) =>
      val senderToReturn = sender()
      log.info(s"Getting teams with name = $name")
      val validatedNameEither = validateParameter[TeamError, String, NonEmpty](name, InvalidTeamFullName(name))
      validatedNameEither match {
        case Left(error) =>
          log.info(s"Validation of name = $name failed")
          senderToReturn ! ValidationErrors(TeamErrorWrapper(List(error)))
        case Right(validName) =>
          log.info(s"Extracting teams with name = $name out of database")
          val teamsFuture = teamRepository.findByName(validName)
          teamsFuture.onComplete {
            case Success(teams) =>
              log.info(s"Got ${teams.size} teams with name = $name out of database")
              senderToReturn ! AllFoundTeams(teams.map(domainTeamToDtoTeam))
            case Failure(ex) =>
              log.error(s"An error occurred while extracting a team with name = $name: $ex")
              senderToReturn ! InternalServerError
          }
      }

    case GetTeamsByCountry(countryId) =>
      val senderToReturn = sender()
      log.info(s"Getting teams with countryId = $countryId")
      val teamFuture = teamRepository.findByCountry(CountryId(countryId))
      teamFuture.onComplete {
        case Success(teams) =>
          log.info(s"Got ${teams.size} teams with countryId = $countryId out of database")
          senderToReturn ! AllFoundTeams(teams.map(domainTeamToDtoTeam))
        case Failure(ex) =>
          log.error(s"An error occurred while extracting teams with countryId = $countryId: $ex")
          senderToReturn ! InternalServerError
      }

    case AddOneEntity(teamDto: TeamApiDto) =>
      val senderToReturn = sender()
      log.info(s"Adding a team = $teamDto")
      val validatedTeam = validateTeamDto(teamDto)
      validatedTeam match {
        case Left(errors) =>
          logErrorsAndSend(senderToReturn, teamDto, errors)
        case Right(team) =>
          val teamIdOrErrors = for {
            errors <- validateTeamDuplicates(team)
            teamId <- if (errors.isEmpty) teamRepository.create(team) else Future(TeamId(0))
            result  = if (teamId.value == 0) Left(errors) else Right(teamId)
          } yield result
          teamIdOrErrors.onComplete {
            case Success(Right(id)) =>
              log.info(s"Team $team successfully created")
              senderToReturn ! OneEntityAdded(teamDto.copy(id = id.value))
            case Success(Left(errors)) =>
              log.info(s"Team $team doesn't created because of: ${errors.mkString("[", ", ", "]")}")
              senderToReturn ! ValidationErrors(TeamErrorWrapper(errors))
            case Failure(ex) =>
              log.error(s"An error occurred while creating a team $team: $ex")
              senderToReturn ! InternalServerError
          }
      }

    case AddAllTeams(teamDtoList) =>
      val senderToReturn = sender()
      log.info(s"Adding teams $teamDtoList")
      val addedTeams = Future.traverse(teamDtoList.map(self ? AddOneEntity(_)))(identity)
      addedTeams.onComplete {
        case Success(list) =>
          val teams: List[TeamApiDto] = list.flatMap {
            case OneEntityAdded(team: TeamApiDto) => List(team)
            case _ => List()
          }
          val errors: List[TeamError] = list.flatMap {
            case ValidationErrors(TeamErrorWrapper(errors)) => errors
            case _                                          => List()
          }
          log.info(s"Teams $teams added successfully")
          log.info(s"Other teams aren't added because of: ${errors.mkString("[", ", ", "]")}")
          senderToReturn ! AllTeamsAdded(teams, errors)
        case Failure(ex) =>
          log.error(s"An error occurred while creating teams $teamDtoList: $ex")
          senderToReturn ! InternalServerError
      }

    case UpdateEntity(teamDto: TeamApiDto) =>
      val senderToReturn = sender()
      log.info(s"Updating a team = $teamDto")
      val validatedTeam = validateTeamDto(teamDto)
      validatedTeam match {
        case Left(errors) =>
          logErrorsAndSend(senderToReturn, teamDto, errors)
        case Right(team) =>
          val rowsUpdatedOrErrors = for {
            errors      <- validateTeamDuplicates(team)
            rowsUpdated <- if (errors.isEmpty) teamRepository.update(team) else Future(-1)
            result       = if (rowsUpdated == -1) Left(errors) else Right(rowsUpdated)
          } yield result
          rowsUpdatedOrErrors.onComplete {
            case Success(Right(rowsUpdated)) =>
              log.info(s"Team $team is ${if (rowsUpdated == 0) "not " else ""}updated")
              val result = if (rowsUpdated == 0) UpdateFailed else UpdateCompleted
              senderToReturn ! result
            case Success(Left(errors)) =>
              log.info(s"Team $team isn't updated because of: ${errors.mkString("[", ", ", "]")}")
              senderToReturn ! ValidationErrors(TeamErrorWrapper(errors))
            case Failure(ex) =>
              log.error(s"An error occurred while updating a team $team: $ex")
              senderToReturn ! InternalServerError
          }
      }

    case RemoveEntity(id: Int) =>
      val senderToReturn = sender()
      log.info(s"Deleting team with id = $id")
      val teamFuture = teamRepository.delete(TeamId(id))
      teamFuture.onComplete {
        case Success(rowsDeleted) =>
          log.info(s"Team with id = $id ${if (rowsDeleted == 0) "not " else ""}removed")
          val result = if (rowsDeleted == 0) RemoveFailed else RemoveCompleted
          senderToReturn ! result
        case Failure(_: SQLIntegrityConstraintViolationException) =>
          log.info(s"A team with id = $id can't be deleted because it's a part of foreign key")
          senderToReturn ! ValidationErrors(TeamErrorWrapper(List(TeamForeignKey(id))))
        case Failure(ex) =>
          log.error(s"An error occurred while deleting a team with id = $id: $ex")
          senderToReturn ! InternalServerError
      }
  }

  private def logErrorsAndSend(sender: ActorRef, teamDto: TeamApiDto, errors: List[TeamError]): Unit = {
    log.info(s"Validation of team = $teamDto failed because of: ${errors.mkString("[", ", ", "]")}")
    sender ! ValidationErrors(TeamErrorWrapper(errors))
  }

  private def validateTeamDuplicates(team: Team): Future[List[TeamError]] = for {
    maybeCountry <- countryRepository.findById(team.countryId)
    countryNotFoundError <- Future(
      if (maybeCountry.isEmpty) List(InvalidTeamCountryId(team.countryId.value)) else List()
    )
  } yield countryNotFoundError

  private def validateTeamDto(teamDto: TeamApiDto): Either[List[TeamError], Team] = {
    val validatedNameEither =
      validateParameter[TeamError, String, NonEmpty](teamDto.name, InvalidTeamFullName(teamDto.name))
    val validatedShortNameEither =
      validateParameter[TeamError, String, MatchesRegex[W.`"^[A-Z]{3}$"`.T]](
        teamDto.shortCode,
        InvalidTeamShortName(teamDto.shortCode)
      )
    val validatedLogoEither = validateLogo(teamDto.logo)

    val fullNameErrorList = if (validatedNameEither.isLeft) List(InvalidTeamFullName(teamDto.name)) else List()
    val shortNameErrorList =
      if (validatedShortNameEither.isLeft) List(InvalidTeamShortName(teamDto.shortCode)) else List()
    val logoErrorList =
      if (validatedLogoEither.isLeft) List(InvalidTeamLogo(teamDto.logo.head)) else List()
    val errorsList: List[TeamError] = fullNameErrorList ++ shortNameErrorList ++ logoErrorList

    val result = for {
      fullName  <- validatedNameEither
      shortName <- validatedShortNameEither
      logo      <- validatedLogoEither
    } yield Team(TeamId(teamDto.id), fullName, shortName, logo, CountryId(teamDto.countryId))

    result.left.map(_ => errorsList)
  }

  private def validateLogo(logo: Option[String]): Either[TeamError, Option[TeamLogo]] = {
    if (logo.isEmpty) Right(None)
    else
      validateParameter[TeamError, String, MatchesRegex[W.`"^[0-9]+.(png|jpg|jpeg)$"`.T]](
        logo.head,
        InvalidTeamLogo(logo.head)
      )
        .map(Option(_))
  }

  private def domainTeamToDtoTeam(team: Team): TeamApiDto =
    TeamApiDto(team.id.value, team.name.value, team.shortCode.value, team.logo.map(_.value), team.countryId.value)
}

object TeamService {
  def props(
    teamRepository:    TeamRepository,
    countryRepository: CountryRepository
  )(
    implicit ec: ExecutionContext,
    timeout:     Timeout
  ): Props =
    Props(new TeamService(teamRepository, countryRepository))

  case class GetTeamsByCountry(countryId: Int)
  case class AddAllTeams(teamDtoList: List[TeamApiDto])

  case class AllFoundTeams(teams: List[TeamApiDto])
  case class TeamErrorWrapper(override val errors: List[TeamError]) extends ErrorWrapper
  case class AllTeamsAdded(teams: List[TeamApiDto], errors: List[TeamError])
}
