package com.itechart.project.service

import akka.actor.{Actor, ActorLogging, Props}
import com.itechart.project.domain.country.CountryId
import com.itechart.project.domain.league.{League, LeagueId}
import com.itechart.project.dto.league_dto.LeagueApiDto
import com.itechart.project.repository.LeagueRepository
import com.itechart.project.service.domain_errors.LeagueErrors.LeagueError
import com.itechart.project.service.domain_errors.LeagueErrors.LeagueError._
import com.itechart.project.utils.RefinedConversions.validateParameter
import eu.timepit.refined.predicates.all.NonEmpty

import java.sql.SQLIntegrityConstraintViolationException
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class LeagueService(leagueRepository: LeagueRepository, implicit val ec: ExecutionContext)
  extends Actor
    with ActorLogging {
  import LeagueService._

  override def receive: Receive = {
    case GetAllLeagues =>
      val senderToReturn = sender()
      log.info("Getting all leagues from database")
      val leaguesFuture = leagueRepository.findAll
      leaguesFuture.onComplete {
        case Success(leagues) =>
          log.info(s"Got ${leagues.size} leagues out of database")
          senderToReturn ! leagues.map(domainLeagueToDtoLeague)
        case Failure(ex) =>
          log.error(s"An error occurred while extracting all leagues out of database: $ex")
          senderToReturn ! LeagueOperationFail
      }

    case GetLeagueById(id) =>
      val senderToReturn = sender()
      log.info(s"Getting league with id = $id")
      val leagueFuture = leagueRepository.findById(LeagueId(id))
      leagueFuture.onComplete {
        case Success(maybeLeague) =>
          log.info(s"League with id = $id ${if (maybeLeague.isEmpty) "not "}found")
          senderToReturn ! maybeLeague.map(domainLeagueToDtoLeague)
        case Failure(ex) =>
          log.error(s"An error occurred while extracting a league with id = $id: $ex")
          senderToReturn ! LeagueOperationFail
      }

    case GetLeagueByName(name) =>
      val senderToReturn = sender()
      log.info(s"Getting league with name = $name")
      val validatedNameEither = validateParameter[LeagueError, String, NonEmpty](name, InvalidLeagueName(name))
      validatedNameEither match {
        case Left(error) =>
          log.info(s"League with name = $name not found because of $error")
          senderToReturn ! error
        case Right(validName) =>
          log.info(s"Getting league with name = $name out of database")
          val leagueFuture = leagueRepository.findByName(validName)
          leagueFuture.onComplete {
            case Success(maybeLeague) =>
              log.info(s"League with name = $name ${if (maybeLeague.isEmpty) "not " else ""}found")
              senderToReturn ! maybeLeague.map(domainLeagueToDtoLeague)
            case Failure(ex) =>
              log.error(s"An error occurred while extracting a league with name = $name: $ex")
              senderToReturn ! LeagueOperationFail
          }
      }

    case GetLeaguesByCountry(countryId) =>
      val senderToReturn = sender()
      log.info(s"Getting league with countryId = $countryId")
      val leagueFuture = leagueRepository.findByCountry(CountryId(countryId))
      leagueFuture.onComplete {
        case Success(leagues) =>
          log.info(s"Got ${leagues.size} leagues with countryId = $countryId out of database")
          senderToReturn ! leagues.map(domainLeagueToDtoLeague)
        case Failure(ex) =>
          log.error(s"An error occurred while extracting leagues with countryId = $countryId: $ex")
          senderToReturn ! LeagueOperationFail
      }

    case AddLeague(leagueDto) =>
      val senderToReturn = sender()
      log.info(s"Trying to create league $leagueDto in database")
      val validatedLeague = validateLeagueDto(leagueDto)
      validatedLeague match {
        case Left(errors) => senderToReturn ! errors
        case Right(league) =>
          val future = leagueRepository.create(league)
          future.onComplete {
            case Success(id) =>
              log.info(s"League $league successfully created")
              senderToReturn ! leagueDto.copy(league_id = id.value)
            case Failure(ex: SQLIntegrityConstraintViolationException) =>
              log.info(s"League $league doesn't created because of $ex")
              senderToReturn ! List(DuplicateLeagueName(leagueDto.name), InvalidLeagueCountryId(leagueDto.country_id))
            case Failure(ex) =>
              log.error(s"An error occurred while creating a league $league: $ex")
              senderToReturn ! LeagueOperationFail
          }
      }

    case UpdateLeague(leagueDto) =>
      val senderToReturn  = sender()
      val validatedLeague = validateLeagueDto(leagueDto)
      validatedLeague match {
        case Left(errors) => senderToReturn ! errors
        case Right(league) =>
          val future = leagueRepository.update(league)
          future.onComplete {
            case Success(rowsUpdated) =>
              log.info(s"League $league update process finished")
              senderToReturn ! rowsUpdated
            case Failure(ex: SQLIntegrityConstraintViolationException) =>
              log.info(s"League $league doesn't updated because of $ex")
              senderToReturn ! List(DuplicateLeagueName(leagueDto.name), InvalidLeagueCountryId(leagueDto.country_id))
            case Failure(ex) =>
              log.error(s"An error occurred while updating a league $league: $ex")
              senderToReturn ! LeagueOperationFail
          }
      }

    case RemoveLeague(id) =>
      val senderToReturn = sender()
      log.info(s"Deleting league with id = $id")
      val leagueFuture = leagueRepository.delete(LeagueId(id))
      leagueFuture.onComplete {
        case Success(result) =>
          log.info(s"League with id = $id ${if (result == 0) "not " else ""}removed")
          senderToReturn ! result
        case Failure(_: SQLIntegrityConstraintViolationException) =>
          log.info(s"A league with id = $id can't be deleted because it's a part of foreign key")
          senderToReturn ! LeagueNotDeleted(id)
        case Failure(ex) =>
          log.error(s"An error occurred while deleting a league with id = $id: $ex")
          senderToReturn ! LeagueOperationFail
      }
  }

  private def validateLeagueDto(leagueDto: LeagueApiDto): Either[List[LeagueError], League] = {
    val validatedNameEither =
      validateParameter[LeagueError, String, NonEmpty](leagueDto.name, InvalidLeagueName(leagueDto.name))

    val result = for {
      name <- validatedNameEither
    } yield League(LeagueId(leagueDto.league_id), name, CountryId(leagueDto.country_id))

    result.left.map(List(_))
  }

  private def domainLeagueToDtoLeague(league: League): LeagueApiDto =
    LeagueApiDto(league.id.value, league.countryId.value, league.name.value)
}

object LeagueService {
  def apply(leagueRepository: LeagueRepository)(implicit ec: ExecutionContext): Props = Props(
    new LeagueService(leagueRepository, ec)
  )

  case object GetAllLeagues
  case class GetLeagueById(id: Int)
  case class GetLeagueByName(name: String)
  case class GetLeaguesByCountry(countryId: Int)
  case class AddLeague(leagueDto: LeagueApiDto)
  case class AddLeagues(leagueDtoList: List[LeagueApiDto])
  case class UpdateLeague(leagueDto: LeagueApiDto)
  case class RemoveLeague(id: Int)
}
