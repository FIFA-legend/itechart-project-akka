package com.itechart.project.service

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.util.Timeout
import akka.pattern.ask
import com.itechart.project.domain.country.CountryId
import com.itechart.project.domain.referee.{Referee, RefereeId, RefereeImage}
import com.itechart.project.dto.referee.RefereeApiDto
import com.itechart.project.repository.{CountryRepository, RefereeRepository}
import com.itechart.project.service.CommonServiceMessages.Requests._
import com.itechart.project.service.CommonServiceMessages.Responses._
import com.itechart.project.service.domain_errors.RefereeErrors.RefereeError
import com.itechart.project.service.domain_errors.RefereeErrors.RefereeError._
import com.itechart.project.utils.RefinedConversions.validateParameter
import eu.timepit.refined.W
import eu.timepit.refined.predicates.all.NonEmpty
import eu.timepit.refined.string.MatchesRegex

import java.sql.SQLIntegrityConstraintViolationException
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class RefereeService(
  refereeRepository: RefereeRepository,
  countryRepository: CountryRepository
)(
  implicit ec: ExecutionContext,
  timeout:     Timeout
) extends Actor
    with ActorLogging {
  import RefereeService._

  override def receive: Receive = {
    case GetAllEntities =>
      val senderToReturn = sender()
      log.info("Getting all referees from database")
      val refereesFuture = refereeRepository.findAll
      refereesFuture.onComplete {
        case Success(referees) =>
          log.info(s"Got ${referees.size} referees out of database")
          senderToReturn ! AllFoundReferees(referees.map(domainRefereeToDtoReferee))
        case Failure(ex) =>
          log.error(s"An error occurred while extracting all referees out of database: $ex")
          senderToReturn ! InternalServerError
      }

    case GetEntityByT(id: Int) =>
      val senderToReturn = sender()
      log.info(s"Getting referee with id = $id")
      val refereeFuture = refereeRepository.findById(RefereeId(id))
      refereeFuture.onComplete {
        case Success(maybeReferee) =>
          log.info(s"Referee with id = $id ${if (maybeReferee.isEmpty) "not "}found")
          senderToReturn ! OneFoundEntity(maybeReferee.map(domainRefereeToDtoReferee))
        case Failure(ex) =>
          log.error(s"An error occurred while extracting a referee with id = $id: $ex")
          senderToReturn ! InternalServerError
      }

    case GetRefereesByCountry(countryId) =>
      val senderToReturn = sender()
      log.info(s"Getting referees with countryId = $countryId")
      val refereesFuture = refereeRepository.findByCountry(CountryId(countryId))
      refereesFuture.onComplete {
        case Success(referees) =>
          log.info(s"Got ${referees.size} referees with countryId = $countryId out of database")
          senderToReturn ! AllFoundReferees(referees.map(domainRefereeToDtoReferee))
        case Failure(ex) =>
          log.error(s"An error occurred while extracting referees with countryId = $countryId: $ex")
          senderToReturn ! InternalServerError
      }

    case AddOneEntity(refereeDto: RefereeApiDto) =>
      val senderToReturn = sender()
      log.info(s"Adding a referee = $refereeDto")
      val validatedReferee = validateRefereeDto(refereeDto)
      validatedReferee match {
        case Left(errors) =>
          logErrorsAndSend(senderToReturn, refereeDto, errors)
        case Right(referee) =>
          val refereeIdOrErrors = for {
            errors    <- validateRefereeDuplicates(referee)
            refereeId <- if (errors.isEmpty) refereeRepository.create(referee) else Future(RefereeId(0))
            result     = if (refereeId.value == 0) Left(errors) else Right(refereeId)
          } yield result
          refereeIdOrErrors.onComplete {
            case Success(Right(id)) =>
              log.info(s"Referee $referee successfully created")
              senderToReturn ! OneEntityAdded(refereeDto.copy(id = id.value))
            case Success(Left(errors)) =>
              log.info(s"Referee $referee doesn't created because of: ${errors.mkString("[", ", ", "]")}")
              senderToReturn ! RefereeValidationErrors(errors)
            case Failure(ex) =>
              log.error(s"An error occurred while creating a referee $referee: $ex")
              senderToReturn ! InternalServerError
          }
      }

    case AddAllReferees(refereeDtoList) =>
      val senderToReturn = sender()
      log.info(s"Adding referees $refereeDtoList")
      val addedReferees = Future.traverse(refereeDtoList.map(self ? AddOneEntity(_)))(identity)
      addedReferees.onComplete {
        case Success(list) =>
          val referees: List[RefereeApiDto] = list.flatMap {
            case OneEntityAdded(referee: RefereeApiDto) => List(referee)
            case _ => List()
          }
          val errors: List[RefereeError] = list.flatMap {
            case RefereeValidationErrors(errors) => errors
            case _                               => List()
          }
          log.info(s"Referees $referees added successfully")
          log.info(s"Other referees aren't added because of: ${errors.mkString("[", ", ", "]")}")
          senderToReturn ! AllRefereesAdded(referees, errors)
        case Failure(ex) =>
          log.error(s"An error occurred while creating referees $refereeDtoList: $ex")
          senderToReturn ! InternalServerError
      }

    case UpdateEntity(refereeDto: RefereeApiDto) =>
      val senderToReturn = sender()
      log.info(s"Updating a referee = $refereeDto")
      val validatedReferee = validateRefereeDto(refereeDto)
      validatedReferee match {
        case Left(errors) =>
          logErrorsAndSend(senderToReturn, refereeDto, errors)
        case Right(referee) =>
          val rowsUpdatedOrErrors = for {
            errors      <- validateRefereeDuplicates(referee)
            rowsUpdated <- if (errors.isEmpty) refereeRepository.update(referee) else Future(-1)
            result       = if (rowsUpdated == -1) Left(errors) else Right(rowsUpdated)
          } yield result
          rowsUpdatedOrErrors.onComplete {
            case Success(Right(rowsUpdated)) =>
              log.info(s"Referee $referee is ${if (rowsUpdated == 0) "not " else ""}updated")
              val result = if (rowsUpdated == 0) UpdateFailed else UpdateCompleted
              senderToReturn ! result
            case Success(Left(errors)) =>
              log.info(s"Referee $referee isn't updated because of: ${errors.mkString("[", ", ", "]")}")
              senderToReturn ! RefereeValidationErrors(errors)
            case Failure(ex) =>
              log.error(s"An error occurred while updating a referee $referee: $ex")
              senderToReturn ! InternalServerError
          }
      }

    case RemoveEntity(id: Int) =>
      val senderToReturn = sender()
      log.info(s"Deleting referee with id = $id")
      val refereeFuture = refereeRepository.delete(RefereeId(id))
      refereeFuture.onComplete {
        case Success(rowsDeleted) =>
          log.info(s"Referee with id = $id ${if (rowsDeleted == 0) "not " else ""}removed")
          val result = if (rowsDeleted == 0) RemoveFailed else RemoveCompleted
          senderToReturn ! result
        case Failure(_: SQLIntegrityConstraintViolationException) =>
          log.info(s"A referee with id = $id can't be deleted because it's a part of foreign key")
          senderToReturn ! RefereeValidationErrors(List(RefereeForeignKey(id)))
        case Failure(ex) =>
          log.error(s"An error occurred while deleting a referee with id = $id: $ex")
          senderToReturn ! InternalServerError
      }
  }

  private def logErrorsAndSend(sender: ActorRef, refereeDto: RefereeApiDto, errors: List[RefereeError]): Unit = {
    log.info(s"Validation of referee = $refereeDto failed because of: ${errors.mkString("[", ", ", "]")}")
    sender ! RefereeValidationErrors(errors)
  }

  private def validateRefereeDuplicates(referee: Referee): Future[List[RefereeError]] = for {
    maybeCountry <- countryRepository.findById(referee.countryId)
    countryNotFoundError <- Future(
      if (maybeCountry.isEmpty) List(InvalidRefereeCountryId(referee.countryId.value)) else List()
    )
  } yield countryNotFoundError

  private def validateRefereeDto(refereeDto: RefereeApiDto): Either[List[RefereeError], Referee] = {
    val validatedFirstNameEither =
      validateParameter[RefereeError, String, NonEmpty](
        refereeDto.firstName,
        InvalidRefereeFirstName(refereeDto.firstName)
      )
    val validatedLastNameEither =
      validateParameter[RefereeError, String, NonEmpty](
        refereeDto.lastName,
        InvalidRefereeLastName(refereeDto.lastName)
      )
    val validatedImageEither = validateImage(refereeDto.image)

    val firstNameErrorList =
      if (validatedFirstNameEither.isLeft) List(InvalidRefereeFirstName(refereeDto.firstName)) else List()
    val lastNameErrorList =
      if (validatedLastNameEither.isLeft) List(InvalidRefereeLastName(refereeDto.lastName)) else List()
    val imageErrorList =
      if (validatedImageEither.isLeft) List(InvalidRefereeImage(refereeDto.image.head)) else List()
    val errorsList: List[RefereeError] = firstNameErrorList ++ lastNameErrorList ++ imageErrorList

    val result = for {
      fullName  <- validatedFirstNameEither
      shortName <- validatedLastNameEither
      logo      <- validatedImageEither
    } yield Referee(RefereeId(refereeDto.id), fullName, shortName, logo, CountryId(refereeDto.countryId))

    result.left.map(_ => errorsList)
  }

  private def validateImage(image: Option[String]): Either[RefereeError, Option[RefereeImage]] = {
    if (image.isEmpty) Right(None)
    else
      validateParameter[RefereeError, String, MatchesRegex[W.`"^[0-9]+.(png|jpg|jpeg)$"`.T]](
        image.head,
        InvalidRefereeImage(image.head)
      )
        .map(Option(_))
  }

  private def domainRefereeToDtoReferee(referee: Referee): RefereeApiDto =
    RefereeApiDto(
      referee.id.value,
      referee.firstName.value,
      referee.lastName.value,
      referee.image.map(_.value),
      referee.countryId.value
    )
}

object RefereeService {
  def props(
    refereeRepository: RefereeRepository,
    countryRepository: CountryRepository
  )(
    implicit ec: ExecutionContext,
    timeout:     Timeout
  ): Props =
    Props(new RefereeService(refereeRepository, countryRepository))

  case class GetRefereesByCountry(countryId: Int)
  case class AddAllReferees(refereeDtoList: List[RefereeApiDto])

  case class AllFoundReferees(referees: List[RefereeApiDto])
  case class RefereeValidationErrors(errors: List[RefereeError])
  case class AllRefereesAdded(referees: List[RefereeApiDto], errors: List[RefereeError])
}
