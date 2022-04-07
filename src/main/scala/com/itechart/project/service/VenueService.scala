package com.itechart.project.service

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.util.Timeout
import akka.pattern.ask
import com.itechart.project.domain.country.CountryId
import com.itechart.project.domain.venue.{Venue, VenueId}
import com.itechart.project.dto.venue.VenueApiDto
import com.itechart.project.repository.{CountryRepository, VenueRepository}
import com.itechart.project.service.CommonServiceMessages.Requests._
import com.itechart.project.service.CommonServiceMessages.Responses._
import com.itechart.project.service.domain_errors.VenueErrors.VenueError
import com.itechart.project.service.domain_errors.VenueErrors.VenueError._
import com.itechart.project.utils.RefinedConversions.validateParameter
import eu.timepit.refined.numeric.NonNegative
import eu.timepit.refined.predicates.all.NonEmpty

import java.sql.SQLIntegrityConstraintViolationException
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class VenueService(
  venueRepository:   VenueRepository,
  countryRepository: CountryRepository
)(
  implicit ec: ExecutionContext,
  timeout:     Timeout
) extends Actor
    with ActorLogging {
  import VenueService._

  override def receive: Receive = {
    case GetAllEntities =>
      val senderToReturn = sender()
      log.info("Getting all venues from database")
      val venuesFuture = venueRepository.findAll
      venuesFuture.onComplete {
        case Success(venues) =>
          log.info(s"Got ${venues.size} venues out of database")
          senderToReturn ! AllFoundVenues(venues.map(domainVenueToDtoVenue))
        case Failure(ex) =>
          log.error(s"An error occurred while extracting all venues out of database: $ex")
          senderToReturn ! InternalServerError
      }

    case GetEntityByT(id: Int) =>
      val senderToReturn = sender()
      log.info(s"Getting venue with id = $id")
      val venueFuture = venueRepository.findById(VenueId(id))
      venueFuture.onComplete {
        case Success(maybeVenue) =>
          log.info(s"Venue with id = $id ${if (maybeVenue.isEmpty) "not "}found")
          senderToReturn ! OneFoundEntity(maybeVenue.map(domainVenueToDtoVenue))
        case Failure(ex) =>
          log.error(s"An error occurred while extracting a venue with id = $id: $ex")
          senderToReturn ! InternalServerError
      }

    case GetEntityByT(name: String) =>
      val senderToReturn = sender()
      log.info(s"Getting venue with name = $name")
      val validatedNameEither = validateParameter[VenueError, String, NonEmpty](name, InvalidVenueName(name))
      validatedNameEither match {
        case Left(error) =>
          log.info(s"Validation of name = $name failed")
          senderToReturn ! VenueValidationErrors(List(error))
        case Right(validName) =>
          log.info(s"Extracting venue with name = $name out of database")
          val venueFuture = venueRepository.findByName(validName)
          venueFuture.onComplete {
            case Success(maybeVenue) =>
              log.info(s"Venue with name = $name ${if (maybeVenue.isEmpty) "not " else ""}found")
              senderToReturn ! OneFoundEntity(maybeVenue.map(domainVenueToDtoVenue))
            case Failure(ex) =>
              log.error(s"An error occurred while extracting a venue with name = $name: $ex")
              senderToReturn ! InternalServerError
          }
      }

    case GetVenuesByCity(city) =>
      val senderToReturn = sender()
      log.info(s"Getting venues with city = $city")
      val validatedCityEither = validateParameter[VenueError, String, NonEmpty](city, InvalidVenueCity(city))
      validatedCityEither match {
        case Left(error) =>
          log.info(s"Validation of city = $city failed")
          senderToReturn ! VenueValidationErrors(List(error))
        case Right(validCity) =>
          log.info(s"Extracting venues with city = $city out of database")
          val venuesFuture = venueRepository.findByCity(validCity)
          venuesFuture.onComplete {
            case Success(venues) =>
              log.info(s"Got ${venues.size} venues with city = $city out of database")
              senderToReturn ! AllFoundVenues(venues.map(domainVenueToDtoVenue))
            case Failure(ex) =>
              log.error(s"An error occurred while extracting venues with city = $city: $ex")
              senderToReturn ! InternalServerError
          }
      }

    case GetVenuesByCountry(countryId) =>
      val senderToReturn = sender()
      log.info(s"Getting venues with countryId = $countryId")
      val venuesFuture = venueRepository.findByCountry(CountryId(countryId))
      venuesFuture.onComplete {
        case Success(venues) =>
          log.info(s"Got ${venues.size} venues with countryId = $countryId out of database")
          senderToReturn ! AllFoundVenues(venues.map(domainVenueToDtoVenue))
        case Failure(ex) =>
          log.error(s"An error occurred while extracting venues with countryId = $countryId: $ex")
          senderToReturn ! InternalServerError
      }

    case AddOneEntity(venueDto: VenueApiDto) =>
      val senderToReturn = sender()
      log.info(s"Adding a venue = $venueDto")
      val validatedVenue = validateVenueDto(venueDto)
      validatedVenue match {
        case Left(errors) =>
          logErrorsAndSend(senderToReturn, venueDto, errors)
        case Right(venue) =>
          val venueIdOrErrors = for {
            errors  <- validateVenueDuplicatesOnCreate(venue)
            venueId <- if (errors.isEmpty) venueRepository.create(venue) else Future(VenueId(0))
            result   = if (venueId.value == 0) Left(errors) else Right(venueId)
          } yield result
          venueIdOrErrors.onComplete {
            case Success(Right(id)) =>
              log.info(s"Venue $venue successfully created")
              senderToReturn ! OneEntityAdded(venueDto.copy(id = id.value))
            case Success(Left(errors)) =>
              log.info(s"Venue $venue doesn't created because of: ${errors.mkString("[", ", ", "]")}")
              senderToReturn ! VenueValidationErrors(errors)
            case Failure(ex) =>
              log.error(s"An error occurred while creating a venue $venue: $ex")
              senderToReturn ! InternalServerError
          }
      }

    case AddAllVenues(venueDtoList) =>
      val senderToReturn = sender()
      log.info(s"Adding venues $venueDtoList")
      val addedVenues = Future.traverse(venueDtoList.map(self ? AddOneEntity(_)))(identity)
      addedVenues.onComplete {
        case Success(list) =>
          val venues: List[VenueApiDto] = list.flatMap {
            case OneEntityAdded(venue: VenueApiDto) => List(venue)
            case _ => List()
          }
          val errors: List[VenueError] = list.flatMap {
            case VenueValidationErrors(errors) => errors
            case _                             => List()
          }
          log.info(s"Venues $venues added successfully")
          log.info(s"Other venues aren't added because of: ${errors.mkString("[", ", ", "]")}")
          senderToReturn ! AllVenuesAdded(venues, errors)
        case Failure(ex) =>
          log.error(s"An error occurred while creating venues $venueDtoList: $ex")
          senderToReturn ! InternalServerError
      }

    case UpdateEntity(venueDto: VenueApiDto) =>
      val senderToReturn = sender()
      log.info(s"Updating a venue = $venueDto")
      val validatedVenue = validateVenueDto(venueDto)
      validatedVenue match {
        case Left(errors) =>
          logErrorsAndSend(senderToReturn, venueDto, errors)
        case Right(venue) =>
          val rowsUpdatedOrErrors = for {
            errors      <- validateVenueDuplicatesOnUpdate(venue)
            rowsUpdated <- if (errors.isEmpty) venueRepository.update(venue) else Future(-1)
            result       = if (rowsUpdated == -1) Left(errors) else Right(rowsUpdated)
          } yield result
          rowsUpdatedOrErrors.onComplete {
            case Success(Right(rowsUpdated)) =>
              log.info(s"Venue $venue is ${if (rowsUpdated == 0) "not " else ""}updated")
              val result = if (rowsUpdated == 0) UpdateFailed else UpdateCompleted
              senderToReturn ! result
            case Success(Left(errors)) =>
              log.info(s"Venue $venue isn't updated because of: ${errors.mkString("[", ", ", "]")}")
              senderToReturn ! VenueValidationErrors(errors)
            case Failure(ex) =>
              log.error(s"An error occurred while updating a venue $venue: $ex")
              senderToReturn ! InternalServerError
          }
      }

    case RemoveEntity(id: Int) =>
      val senderToReturn = sender()
      log.info(s"Deleting venue with id = $id")
      val venueFuture = venueRepository.delete(VenueId(id))
      venueFuture.onComplete {
        case Success(rowsDeleted) =>
          log.info(s"Venue with id = $id ${if (rowsDeleted == 0) "not " else ""}removed")
          val result = if (rowsDeleted == 0) RemoveFailed else RemoveCompleted
          senderToReturn ! result
        case Failure(_: SQLIntegrityConstraintViolationException) =>
          log.info(s"A venue with id = $id can't be deleted because it's a part of foreign key")
          senderToReturn ! VenueValidationErrors(List(VenueForeignKey(id)))
        case Failure(ex) =>
          log.error(s"An error occurred while deleting a venue with id = $id: $ex")
          senderToReturn ! InternalServerError
      }
  }

  private def logErrorsAndSend(sender: ActorRef, venueDto: VenueApiDto, errors: List[VenueError]): Unit = {
    log.info(s"Validation of venue = $venueDto failed because of: ${errors.mkString("[", ", ", "]")}")
    sender ! VenueValidationErrors(errors)
  }

  private def validateVenueDuplicatesOnCreate(venue: Venue): Future[List[VenueError]] = for {
    maybeVenueByName <- venueRepository.findByName(venue.name)
    maybeCountry     <- countryRepository.findById(venue.countryId)
    duplicatedNameError <- Future(
      if (maybeVenueByName.isEmpty) List() else List(DuplicateVenueName(venue.name.value))
    )
    countryNotFoundError <- Future(
      if (maybeCountry.isEmpty) List(InvalidVenueCountryId(venue.countryId.value)) else List()
    )
  } yield duplicatedNameError ++ countryNotFoundError

  private def validateVenueDuplicatesOnUpdate(venue: Venue): Future[List[VenueError]] = for {
    maybeVenueByName <- venueRepository.findByName(venue.name)
    maybeCountry     <- countryRepository.findById(venue.countryId)
    duplicatedNameError <- Future(
      if (maybeVenueByName.isEmpty || maybeVenueByName.head.id == venue.id) List()
      else List(DuplicateVenueName(venue.name.value))
    )
    countryNotFoundError <- Future(
      if (maybeCountry.isEmpty) List(InvalidVenueCountryId(venue.countryId.value)) else List()
    )
  } yield duplicatedNameError ++ countryNotFoundError

  private def validateVenueDto(venueDto: VenueApiDto): Either[List[VenueError], Venue] = {
    val validatedNameEither =
      validateParameter[VenueError, String, NonEmpty](venueDto.name, InvalidVenueName(venueDto.name))
    val validatedCapacityEither =
      validateParameter[VenueError, Int, NonNegative](venueDto.capacity, InvalidVenueCapacity(venueDto.capacity))
    val validatedCityEither =
      validateParameter[VenueError, String, NonEmpty](venueDto.city, InvalidVenueCity(venueDto.city))

    val nameErrorList = if (validatedNameEither.isLeft) List(InvalidVenueName(venueDto.name)) else List()
    val capacityErrorList =
      if (validatedCapacityEither.isLeft) List(InvalidVenueCapacity(venueDto.capacity)) else List()
    val cityErrorList = if (validatedCityEither.isLeft) List(InvalidVenueCity(venueDto.city)) else List()
    val errorsList: List[VenueError] = nameErrorList ++ capacityErrorList ++ cityErrorList

    val result = for {
      name     <- validatedNameEither
      capacity <- validatedCapacityEither
      city     <- validatedCityEither
    } yield Venue(VenueId(venueDto.id), name, capacity, city, CountryId(venueDto.countryId))

    result.left.map(_ => errorsList)
  }

  private def domainVenueToDtoVenue(venue: Venue): VenueApiDto =
    VenueApiDto(venue.id.value, venue.name.value, venue.capacity.value, venue.city.value, venue.countryId.value)
}

object VenueService {
  def props(
    venueRepository:   VenueRepository,
    countryRepository: CountryRepository
  )(
    implicit ec: ExecutionContext,
    timeout:     Timeout
  ): Props =
    Props(new VenueService(venueRepository, countryRepository))

  case class GetVenuesByCity(city: String)
  case class GetVenuesByCountry(countryId: Int)
  case class AddAllVenues(venueDtoList: List[VenueApiDto])

  case class AllFoundVenues(venues: List[VenueApiDto])
  case class VenueValidationErrors(errors: List[VenueError])
  case class AllVenuesAdded(venues: List[VenueApiDto], errors: List[VenueError])
}
