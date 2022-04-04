package com.itechart.project.service

import akka.actor.{Actor, ActorLogging, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.itechart.project.domain.season.{Season, SeasonId}
import com.itechart.project.dto.season.SeasonApiDto
import com.itechart.project.repository.SeasonRepository
import com.itechart.project.service.domain_errors.SeasonErrors.SeasonError
import com.itechart.project.service.domain_errors.SeasonErrors.SeasonError._
import com.itechart.project.utils.RefinedConversions.validateParameter
import eu.timepit.refined.W
import eu.timepit.refined.string.MatchesRegex

import java.sql.SQLIntegrityConstraintViolationException
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class SeasonService(seasonRepository: SeasonRepository)(implicit ec: ExecutionContext, timeout: Timeout)
  extends Actor
    with ActorLogging {
  import SeasonService._

  override def receive: Receive = {
    case GetAllSeasons =>
      val senderToReturn = sender()
      log.info("Getting all seasons from database")
      val seasonsFuture = seasonRepository.findAll
      seasonsFuture.onComplete {
        case Success(seasons) =>
          log.info(s"Got ${seasons.size} seasons out of database")
          senderToReturn ! FoundSeasons(seasons.map(domainSeasonToDtoSeason))
        case Failure(ex) =>
          log.error(s"An error occurred while extracting all seasons out of database: $ex")
          senderToReturn ! SeasonInternalServerError
      }

    case GetSeasonById(id) =>
      val senderToReturn = sender()
      log.info(s"Getting season with id = $id")
      val seasonFuture = seasonRepository.findById(SeasonId(id))
      seasonFuture.onComplete {
        case Success(maybeSeason) =>
          log.info(s"Season with id = $id ${if (maybeSeason.isEmpty) "not "}found")
          senderToReturn ! FoundSeason(maybeSeason.map(domainSeasonToDtoSeason))
        case Failure(ex) =>
          log.error(s"An error occurred while extracting a league with id = $id: $ex")
          senderToReturn ! SeasonInternalServerError
      }

    case GetSeasonByName(name) =>
      val senderToReturn = sender()
      log.info(s"Getting season with name = $name")
      val validatedNameEither =
        validateParameter[SeasonError, String, MatchesRegex[W.`"^[0-9]{4}/[0-9]{4}$"`.T]](name, InvalidSeasonName(name))
      validatedNameEither match {
        case Left(error) =>
          log.info(s"Validation of name = $name failed")
          senderToReturn ! SeasonValidationErrors(List(error))
        case Right(validName) =>
          log.info(s"Extracting season with name = $name out of database")
          val seasonFuture = seasonRepository.findByName(validName)
          seasonFuture.onComplete {
            case Success(maybeSeason) =>
              log.info(s"Season with name = $name ${if (maybeSeason.isEmpty) "not " else ""}found")
              senderToReturn ! FoundSeason(maybeSeason.map(domainSeasonToDtoSeason))
            case Failure(ex) =>
              log.error(s"An error occurred while extracting a season with name = $name: $ex")
              senderToReturn ! SeasonInternalServerError
          }
      }

    case AddSeason(seasonDto) =>
      val senderToReturn = sender()
      log.info(s"Adding a season = $seasonDto")
      val validatedSeason = validateSeasonDto(seasonDto)
      validatedSeason match {
        case Left(errors) =>
          log.info(s"Validation of season = $seasonDto failed because of: ${errors.mkString("[", ", ", "]")}")
          senderToReturn ! SeasonValidationErrors(errors)
        case Right(season) =>
          val seasonIdOrErrors = for {
            errors   <- validateSeasonDuplicatesOnCreate(season)
            seasonId <- if (errors.isEmpty) seasonRepository.create(season) else Future(SeasonId(0))
            result    = if (seasonId.value == 0) Left(errors) else Right(seasonId)
          } yield result
          seasonIdOrErrors.onComplete {
            case Success(Right(id)) =>
              log.info(s"Season $season successfully created")
              senderToReturn ! SeasonAdded(seasonDto.copy(id = id.value))
            case Success(Left(errors)) =>
              log.info(s"Season $season doesn't created because of: ${errors.mkString("[", ", ", "]")}")
              senderToReturn ! SeasonValidationErrors(errors)
            case Failure(ex) =>
              log.error(s"An error occurred while creating a season $season: $ex")
              senderToReturn ! SeasonInternalServerError
          }
      }

    case AddSeasons(seasonDtoList) =>
      val senderToReturn = sender()
      log.info(s"Adding seasons $seasonDtoList")
      val addedSeasons = Future.traverse(seasonDtoList.map(self ? AddSeason(_)))(identity)
      addedSeasons.onComplete {
        case Success(list) =>
          val seasons: List[SeasonApiDto] = list.flatMap {
            case SeasonAdded(season) => List(season)
            case _                   => List()
          }
          val errors: List[SeasonError] = list.flatMap {
            case SeasonValidationErrors(errors) => errors
            case _                              => List()
          }
          log.info(s"Seasons $seasons added successfully")
          log.info(s"Other seasons aren't added because of: ${errors.mkString("[", ", ", "]")}")
          senderToReturn ! SeasonsAdded(seasons, errors)
        case Failure(ex) =>
          log.error(s"An error occurred while creating seasons $seasonDtoList: $ex")
          senderToReturn ! SeasonInternalServerError
      }

    case UpdateSeason(seasonDto) =>
      val senderToReturn = sender()
      log.info(s"Updating a season = $seasonDto")
      val validatedSeason = validateSeasonDto(seasonDto)
      validatedSeason match {
        case Left(errors) =>
          log.info(s"Validation of season = $seasonDto failed because of: ${errors.mkString("[", ", ", "]")}")
          senderToReturn ! SeasonValidationErrors(errors)
        case Right(season) =>
          val rowsUpdatedOrErrors = for {
            errors      <- validateSeasonDuplicatesOnUpdate(season)
            rowsUpdated <- if (errors.isEmpty) seasonRepository.update(season) else Future(-1)
            result       = if (rowsUpdated == -1) Left(errors) else Right(rowsUpdated)
          } yield result
          rowsUpdatedOrErrors.onComplete {
            case Success(Right(rowsUpdated)) =>
              log.info(s"Season $season is ${if (rowsUpdated == 0) "not " else ""}updated")
              val result = if (rowsUpdated == 0) SeasonNotUpdated else SeasonUpdated
              senderToReturn ! result
            case Success(Left(errors)) =>
              log.info(s"Season $season isn't updated because of: ${errors.mkString("[", ", ", "]")}")
              senderToReturn ! SeasonValidationErrors(errors)
            case Failure(ex) =>
              log.error(s"An error occurred while updating a season $season: $ex")
              senderToReturn ! SeasonInternalServerError
          }
      }

    case RemoveSeason(id) =>
      val senderToReturn = sender()
      log.info(s"Deleting season with id = $id")
      val seasonFuture = seasonRepository.delete(SeasonId(id))
      seasonFuture.onComplete {
        case Success(rowsDeleted) =>
          log.info(s"Season with id = $id ${if (rowsDeleted == 0) "not " else ""}removed")
          val result = if (rowsDeleted == 0) SeasonNotDeleted else SeasonDeleted
          senderToReturn ! result
        case Failure(_: SQLIntegrityConstraintViolationException) =>
          log.info(s"A season with id = $id can't be deleted because it's a part of foreign key")
          senderToReturn ! SeasonValidationErrors(List(SeasonForeignKey(id)))
        case Failure(ex) =>
          log.error(s"An error occurred while deleting a season with id = $id: $ex")
          senderToReturn ! SeasonInternalServerError
      }
  }

  private def validateSeasonDuplicatesOnCreate(season: Season): Future[List[SeasonError]] = for {
    maybeSeasonByName <- seasonRepository.findByName(season.name)
    duplicatedNameError <- Future(
      if (maybeSeasonByName.isEmpty) List() else List(DuplicateSeasonName(season.name.value))
    )
  } yield duplicatedNameError

  private def validateSeasonDuplicatesOnUpdate(season: Season): Future[List[SeasonError]] = for {
    maybeSeasonByName <- seasonRepository.findByName(season.name)
    duplicatedNameError <- Future(
      if (maybeSeasonByName.isEmpty || maybeSeasonByName.head.id == season.id) List()
      else List(DuplicateSeasonName(season.name.value))
    )
  } yield duplicatedNameError

  private def validateSeasonDto(seasonDto: SeasonApiDto): Either[List[SeasonError], Season] = {
    val validatedNameEither =
      validateParameter[SeasonError, String, MatchesRegex[W.`"^[0-9]{4}/[0-9]{4}$"`.T]](
        seasonDto.name,
        InvalidSeasonName(seasonDto.name)
      )
    val start   = seasonDto.startDate
    val end     = seasonDto.endDate
    val current = LocalDate.now()
    val validatedStartDateEither =
      if (start.getYear >= 1900 && start.isBefore(current.plus(1, ChronoUnit.YEARS))) Right(start)
      else Left(InvalidSeasonStartDate(start))
    val validatedEndDateEither =
      if (end.minus(1, ChronoUnit.YEARS).isBefore(start)) Right(end)
      else Left(InvalidSeasonEndDate(end))

    val nameErrorList = if (validatedNameEither.isLeft) List(InvalidSeasonName(seasonDto.name)) else List()
    val startDateErrorList =
      if (validatedStartDateEither.isLeft) List(InvalidSeasonStartDate(start)) else List()
    val endDateErrorList =
      if (validatedEndDateEither.isLeft) List(InvalidSeasonEndDate(end)) else List()
    val errorsList: List[SeasonError] = nameErrorList ++ startDateErrorList ++ endDateErrorList

    val result = for {
      name      <- validatedNameEither
      startDate <- validatedStartDateEither
      endDate   <- validatedEndDateEither
      isCurrent  = current.isAfter(start) && current.isBefore(end)
    } yield Season(SeasonId(seasonDto.id), name, isCurrent, startDate, endDate)

    result.left.map(_ => errorsList)
  }

  private def domainSeasonToDtoSeason(season: Season): SeasonApiDto =
    SeasonApiDto(season.id.value, season.name.value, season.isCurrent, season.startDate, season.endDate)
}

object SeasonService {
  def props(seasonRepository: SeasonRepository)(implicit ec: ExecutionContext, timeout: Timeout): Props =
    Props(new SeasonService(seasonRepository))

  case object GetAllSeasons
  case class GetSeasonById(id: Int)
  case class GetSeasonByName(name: String)
  case class AddSeason(seasonDto: SeasonApiDto)
  case class AddSeasons(seasonDtoList: List[SeasonApiDto])
  case class UpdateSeason(seasonDto: SeasonApiDto)
  case class RemoveSeason(id: Int)

  case class FoundSeasons(seasons: List[SeasonApiDto])
  case class FoundSeason(maybeSeason: Option[SeasonApiDto])
  case class SeasonValidationErrors(errors: List[SeasonError])
  case class SeasonAdded(season: SeasonApiDto)
  case class SeasonsAdded(seasons: List[SeasonApiDto], errors: List[SeasonError])
  case object SeasonUpdated
  case object SeasonNotUpdated
  case object SeasonDeleted
  case object SeasonNotDeleted
  case object SeasonInternalServerError
}
