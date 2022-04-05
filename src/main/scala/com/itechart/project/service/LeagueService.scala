package com.itechart.project.service

import akka.actor.{Actor, ActorLogging, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.itechart.project.domain.country.CountryId
import com.itechart.project.domain.league.{League, LeagueId}
import com.itechart.project.dto.league.LeagueApiDto
import com.itechart.project.repository.{CountryRepository, LeagueRepository}
import com.itechart.project.service.CommonServiceMessages.Requests._
import com.itechart.project.service.CommonServiceMessages.Responses._
import com.itechart.project.service.domain_errors.LeagueErrors.LeagueError
import com.itechart.project.service.domain_errors.LeagueErrors.LeagueError._
import com.itechart.project.utils.RefinedConversions.validateParameter
import eu.timepit.refined.predicates.all.NonEmpty

import java.sql.SQLIntegrityConstraintViolationException
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class LeagueService(
  leagueRepository:  LeagueRepository,
  countryRepository: CountryRepository
)(
  implicit ec: ExecutionContext,
  timeout:     Timeout
) extends Actor
    with ActorLogging {
  import LeagueService._

  override def receive: Receive = {
    case GetAllEntities =>
      val senderToReturn = sender()
      log.info("Getting all leagues from database")
      val leaguesFuture = leagueRepository.findAll
      leaguesFuture.onComplete {
        case Success(leagues) =>
          log.info(s"Got ${leagues.size} leagues out of database")
          senderToReturn ! AllFoundLeagues(leagues.map(domainLeagueToDtoLeague))
        case Failure(ex) =>
          log.error(s"An error occurred while extracting all leagues out of database: $ex")
          senderToReturn ! InternalServerError
      }

    case GetEntityByT(id: Int) =>
      val senderToReturn = sender()
      log.info(s"Getting league with id = $id")
      val leagueFuture = leagueRepository.findById(LeagueId(id))
      leagueFuture.onComplete {
        case Success(maybeLeague) =>
          log.info(s"League with id = $id ${if (maybeLeague.isEmpty) "not "}found")
          senderToReturn ! OneFoundEntity(maybeLeague.map(domainLeagueToDtoLeague))
        case Failure(ex) =>
          log.error(s"An error occurred while extracting a league with id = $id: $ex")
          senderToReturn ! InternalServerError
      }

    case GetEntityByT(name: String) =>
      val senderToReturn = sender()
      log.info(s"Getting league with name = $name")
      val validatedNameEither = validateParameter[LeagueError, String, NonEmpty](name, InvalidLeagueName(name))
      validatedNameEither match {
        case Left(error) =>
          log.info(s"Validation of name = $name failed")
          senderToReturn ! LeagueValidationErrors(List(error))
        case Right(validName) =>
          log.info(s"Extracting league with name = $name out of database")
          val leagueFuture = leagueRepository.findByName(validName)
          leagueFuture.onComplete {
            case Success(maybeLeague) =>
              log.info(s"League with name = $name ${if (maybeLeague.isEmpty) "not " else ""}found")
              senderToReturn ! OneFoundEntity(maybeLeague.map(domainLeagueToDtoLeague))
            case Failure(ex) =>
              log.error(s"An error occurred while extracting a league with name = $name: $ex")
              senderToReturn ! InternalServerError
          }
      }

    case GetLeaguesByCountry(countryId) =>
      val senderToReturn = sender()
      log.info(s"Getting leagues with countryId = $countryId")
      val leagueFuture = leagueRepository.findByCountry(CountryId(countryId))
      leagueFuture.onComplete {
        case Success(leagues) =>
          log.info(s"Got ${leagues.size} leagues with countryId = $countryId out of database")
          senderToReturn ! AllFoundLeagues(leagues.map(domainLeagueToDtoLeague))
        case Failure(ex) =>
          log.error(s"An error occurred while extracting leagues with countryId = $countryId: $ex")
          senderToReturn ! InternalServerError
      }

    case AddOneEntity(leagueDto: LeagueApiDto) =>
      val senderToReturn = sender()
      log.info(s"Adding a league = $leagueDto")
      val validatedLeague = validateLeagueDto(leagueDto)
      validatedLeague match {
        case Left(errors) =>
          log.info(s"Validation of league = $leagueDto failed because of: ${errors.mkString("[", ", ", "]")}")
          senderToReturn ! LeagueValidationErrors(errors)
        case Right(league) =>
          val leagueIdOrErrors = for {
            errors   <- validateLeagueDuplicatesOnCreate(league)
            leagueId <- if (errors.isEmpty) leagueRepository.create(league) else Future(LeagueId(0))
            result    = if (leagueId.value == 0) Left(errors) else Right(leagueId)
          } yield result
          leagueIdOrErrors.onComplete {
            case Success(Right(id)) =>
              log.info(s"League $league successfully created")
              senderToReturn ! OneEntityAdded(leagueDto.copy(id = id.value))
            case Success(Left(errors)) =>
              log.info(s"League $league doesn't created because of: ${errors.mkString("[", ", ", "]")}")
              senderToReturn ! LeagueValidationErrors(errors)
            case Failure(ex) =>
              log.error(s"An error occurred while creating a league $league: $ex")
              senderToReturn ! InternalServerError
          }
      }

    case AddAllLeagues(leagueDtoList) =>
      val senderToReturn = sender()
      log.info(s"Adding leagues $leagueDtoList")
      val addedLeagues = Future.traverse(leagueDtoList.map(self ? AddOneEntity(_)))(identity)
      addedLeagues.onComplete {
        case Success(list) =>
          val leagues: List[LeagueApiDto] = list.flatMap {
            case OneEntityAdded(league: LeagueApiDto) => List(league)
            case _ => List()
          }
          val errors: List[LeagueError] = list.flatMap {
            case LeagueValidationErrors(errors) => errors
            case _                              => List()
          }
          log.info(s"Leagues $leagues added successfully")
          log.info(s"Other leagues aren't added because of: ${errors.mkString("[", ", ", "]")}")
          senderToReturn ! AllLeaguesAdded(leagues, errors)
        case Failure(ex) =>
          log.error(s"An error occurred while creating leagues $leagueDtoList: $ex")
          senderToReturn ! InternalServerError
      }

    case UpdateEntity(leagueDto: LeagueApiDto) =>
      val senderToReturn = sender()
      log.info(s"Updating a league = $leagueDto")
      val validatedLeague = validateLeagueDto(leagueDto)
      validatedLeague match {
        case Left(errors) =>
          log.info(s"Validation of league = $leagueDto failed because of: ${errors.mkString("[", ", ", "]")}")
          senderToReturn ! LeagueValidationErrors(errors)
        case Right(league) =>
          val rowsUpdatedOrErrors = for {
            errors      <- validateLeagueDuplicatesOnUpdate(league)
            rowsUpdated <- if (errors.isEmpty) leagueRepository.update(league) else Future(-1)
            result       = if (rowsUpdated == -1) Left(errors) else Right(rowsUpdated)
          } yield result
          rowsUpdatedOrErrors.onComplete {
            case Success(Right(rowsUpdated)) =>
              log.info(s"League $league is ${if (rowsUpdated == 0) "not " else ""}updated")
              val result = if (rowsUpdated == 0) UpdateFailed else UpdateCompleted
              senderToReturn ! result
            case Success(Left(errors)) =>
              log.info(s"League $league isn't updated because of: ${errors.mkString("[", ", ", "]")}")
              senderToReturn ! LeagueValidationErrors(errors)
            case Failure(ex) =>
              log.error(s"An error occurred while updating a league $league: $ex")
              senderToReturn ! InternalServerError
          }
      }

    case RemoveEntity(id: Int) =>
      val senderToReturn = sender()
      log.info(s"Deleting league with id = $id")
      val leagueFuture = leagueRepository.delete(LeagueId(id))
      leagueFuture.onComplete {
        case Success(rowsDeleted) =>
          log.info(s"League with id = $id ${if (rowsDeleted == 0) "not " else ""}removed")
          val result = if (rowsDeleted == 0) RemoveFailed else RemoveCompleted
          senderToReturn ! result
        case Failure(_: SQLIntegrityConstraintViolationException) =>
          log.info(s"A league with id = $id can't be deleted because it's a part of foreign key")
          senderToReturn ! LeagueValidationErrors(List(LeagueForeignKey(id)))
        case Failure(ex) =>
          log.error(s"An error occurred while deleting a league with id = $id: $ex")
          senderToReturn ! InternalServerError
      }
  }

  private def validateLeagueDuplicatesOnCreate(league: League): Future[List[LeagueError]] = for {
    maybeLeagueByName <- leagueRepository.findByName(league.name)
    maybeCountry      <- countryRepository.findById(league.countryId)
    duplicatedNameError <- Future(
      if (maybeLeagueByName.isEmpty) List() else List(DuplicateLeagueName(league.name.value))
    )
    countryNotFoundError <- Future(
      if (maybeCountry.isEmpty) List(InvalidLeagueCountryId(league.countryId.value)) else List()
    )
  } yield duplicatedNameError ++ countryNotFoundError

  private def validateLeagueDuplicatesOnUpdate(league: League): Future[List[LeagueError]] = for {
    maybeLeagueByName <- leagueRepository.findByName(league.name)
    maybeCountry      <- countryRepository.findById(league.countryId)
    duplicatedNameError <- Future(
      if (maybeLeagueByName.isEmpty || maybeLeagueByName.head.id == league.id) List()
      else List(DuplicateLeagueName(league.name.value))
    )
    countryNotFoundError <- Future(
      if (maybeCountry.isEmpty) List(InvalidLeagueCountryId(league.countryId.value)) else List()
    )
  } yield duplicatedNameError ++ countryNotFoundError

  private def validateLeagueDto(leagueDto: LeagueApiDto): Either[List[LeagueError], League] = {
    val result = for {
      name <- validateParameter[LeagueError, String, NonEmpty](leagueDto.name, InvalidLeagueName(leagueDto.name))
    } yield League(LeagueId(leagueDto.id), name, CountryId(leagueDto.countryId))

    result.left.map(List(_))
  }

  private def domainLeagueToDtoLeague(league: League): LeagueApiDto =
    LeagueApiDto(league.id.value, league.countryId.value, league.name.value)
}

object LeagueService {
  def props(
    leagueRepository:  LeagueRepository,
    countryRepository: CountryRepository
  )(
    implicit ec: ExecutionContext,
    timeout:     Timeout
  ): Props = Props(new LeagueService(leagueRepository, countryRepository))

  case class GetLeaguesByCountry(countryId: Int)
  case class AddAllLeagues(leagueDtoList: List[LeagueApiDto])

  case class AllFoundLeagues(leagues: List[LeagueApiDto])
  case class LeagueValidationErrors(errors: List[LeagueError])
  case class AllLeaguesAdded(leagues: List[LeagueApiDto], errors: List[LeagueError])
}
