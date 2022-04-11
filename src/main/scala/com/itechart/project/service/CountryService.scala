package com.itechart.project.service

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.itechart.project.domain.country.{Continent, Country, CountryId}
import com.itechart.project.dto.country.CountryApiDto
import com.itechart.project.repository.CountryRepository
import com.itechart.project.service.CommonServiceMessages.Requests._
import com.itechart.project.service.CommonServiceMessages.Responses._
import com.itechart.project.service.domain_errors.CountryErrors.CountryError
import com.itechart.project.service.domain_errors.CountryErrors.CountryError._
import com.itechart.project.utils.RefinedConversions.validateParameter
import eu.timepit.refined.W
import eu.timepit.refined.string.MatchesRegex

import java.sql.SQLIntegrityConstraintViolationException
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class CountryService(countryRepository: CountryRepository)(implicit ec: ExecutionContext, timeout: Timeout)
  extends Actor
    with ActorLogging {
  import CountryService._

  override def receive: Receive = {
    case GetAllEntities =>
      val senderToReturn = sender()
      log.info("Getting all countries from database")
      val countriesFuture = countryRepository.findAll
      countriesFuture.onComplete {
        case Success(countries) =>
          log.info(s"Got ${countries.size} countries out of database")
          senderToReturn ! AllFoundCountries(countries.map(domainCountryToDtoCountry))
        case Failure(ex) =>
          log.error(s"An error occurred while extracting all countries out of database: $ex")
          senderToReturn ! InternalServerError
      }

    case GetEntityByT(id: Int) =>
      val senderToReturn = sender()
      log.info(s"Getting country with id = $id")
      val countryFuture = countryRepository.findById(CountryId(id))
      countryFuture.onComplete {
        case Success(maybeCountry) =>
          log.info(s"Country with id = $id ${if (maybeCountry.isEmpty) "not "}found")
          senderToReturn ! OneFoundEntity(maybeCountry.map(domainCountryToDtoCountry))
        case Failure(ex) =>
          log.error(s"An error occurred while extracting a country with id = $id: $ex")
          senderToReturn ! InternalServerError
      }

    case GetEntityByT(name: String) =>
      val senderToReturn = sender()
      log.info(s"Getting country with name = $name")
      val validatedNameEither =
        validateParameter[CountryError, String, MatchesRegex[W.`"^[A-Z][A-Za-z]+$"`.T]](name, InvalidCountryName(name))
      validatedNameEither match {
        case Left(error) =>
          log.info(s"Validation of name = $name failed")
          senderToReturn ! CountryValidationErrors(List(error))
        case Right(validName) =>
          log.info(s"Extracting country with name = $name out of database")
          val countryFuture = countryRepository.findByName(validName)
          countryFuture.onComplete {
            case Success(maybeCountry) =>
              log.info(s"Country with name = $name ${if (maybeCountry.isEmpty) "not " else ""}found")
              senderToReturn ! OneFoundEntity(maybeCountry.map(domainCountryToDtoCountry))
            case Failure(ex) =>
              log.error(s"An error occurred while extracting a country with name = $name: $ex")
              senderToReturn ! InternalServerError
          }
      }

    case GetCountryByCode(code) =>
      val senderToReturn = sender()
      log.info(s"Getting country with code = $code")
      val validatedCodeEither =
        validateParameter[CountryError, String, MatchesRegex[W.`"^[a-z]{2}$"`.T]](code, InvalidCountryCode(code))
      validatedCodeEither match {
        case Left(error) =>
          log.info(s"Validation of code = $code failed")
          senderToReturn ! CountryValidationErrors(List(error))
        case Right(validCode) =>
          log.info(s"Extracting country with code = $code out of database")
          val countryFuture = countryRepository.findByCode(validCode)
          countryFuture.onComplete {
            case Success(maybeCountry) =>
              log.info(s"Country with code = $code ${if (maybeCountry.isEmpty) "not " else ""}found")
              senderToReturn ! OneFoundEntity(maybeCountry.map(domainCountryToDtoCountry))
            case Failure(ex) =>
              log.error(s"An error occurred while extracting a country with code = $code: $ex")
              senderToReturn ! InternalServerError
          }
      }

    case AddOneEntity(countryDto: CountryApiDto) =>
      val senderToReturn = sender()
      log.info(s"Adding a country = $countryDto")
      val validatedCountry = validateCountryDto(countryDto)
      validatedCountry match {
        case Left(errors) =>
          logErrorsAndSend(senderToReturn, countryDto, errors)
        case Right(country) =>
          val countryIdOrErrors = for {
            errors    <- validateCountryDuplicatesOnCreate(country)
            countryId <- if (errors.isEmpty) countryRepository.create(country) else Future(CountryId(0))
            result     = if (countryId.value == 0) Left(errors) else Right(countryId)
          } yield result
          countryIdOrErrors.onComplete {
            case Success(Right(id)) =>
              log.info(s"Country $country successfully created")
              senderToReturn ! OneEntityAdded(countryDto.copy(id = id.value))
            case Success(Left(errors)) =>
              log.info(s"Country $country doesn't created because of: ${errors.mkString("[", ", ", "]")}")
              senderToReturn ! CountryValidationErrors(errors)
            case Failure(ex) =>
              log.error(s"An error occurred while creating a country $country: $ex")
              senderToReturn ! InternalServerError
          }
      }

    case AddAllCountries(countryDtoList) =>
      val senderToReturn = sender()
      log.info(s"Adding countries $countryDtoList")
      val addedCountries = Future.traverse(countryDtoList.map(self ? AddOneEntity(_)))(identity)
      addedCountries.onComplete {
        case Success(list) =>
          val countries: List[CountryApiDto] = list.flatMap {
            case OneEntityAdded(country: CountryApiDto) => List(country)
            case _ => List()
          }
          val errors: List[CountryError] = list.flatMap {
            case CountryValidationErrors(errors) => errors
            case _                               => List()
          }
          log.info(s"Countries $countries added successfully")
          log.info(s"Other countries aren't added because of: ${errors.mkString("[", ", ", "]")}")
          senderToReturn ! AllCountriesAdded(countries, errors)
        case Failure(ex) =>
          log.error(s"An error occurred while creating countries $countryDtoList: $ex")
          senderToReturn ! InternalServerError
      }

    case UpdateEntity(countryDto: CountryApiDto) =>
      val senderToReturn = sender()
      log.info(s"Updating a country = $countryDto")
      val validatedCountry = validateCountryDto(countryDto)
      validatedCountry match {
        case Left(errors) =>
          logErrorsAndSend(senderToReturn, countryDto, errors)
        case Right(country) =>
          val rowsUpdatedOrErrors = for {
            errors      <- validateCountryDuplicatesOnUpdate(country)
            rowsUpdated <- if (errors.isEmpty) countryRepository.update(country) else Future(-1)
            result       = if (rowsUpdated == -1) Left(errors) else Right(rowsUpdated)
          } yield result
          rowsUpdatedOrErrors.onComplete {
            case Success(Right(rowsUpdated)) =>
              log.info(s"Country $country is ${if (rowsUpdated == 0) "not " else ""}updated")
              val result = if (rowsUpdated == 0) UpdateFailed else UpdateCompleted
              senderToReturn ! result
            case Success(Left(errors)) =>
              log.info(s"Country $country isn't updated because of: ${errors.mkString("[", ", ", "]")}")
              senderToReturn ! CountryValidationErrors(errors)
            case Failure(ex) =>
              log.error(s"An error occurred while updating a country $country: $ex")
              senderToReturn ! InternalServerError
          }
      }

    case RemoveEntity(id: Int) =>
      val senderToReturn = sender()
      log.info(s"Deleting country with id = $id")
      val countryFuture = countryRepository.delete(CountryId(id))
      countryFuture.onComplete {
        case Success(rowsDeleted) =>
          log.info(s"Country with id = $id ${if (rowsDeleted == 0) "not " else ""}deleted")
          val result = if (rowsDeleted == 0) RemoveFailed else RemoveCompleted
          senderToReturn ! result
        case Failure(_: SQLIntegrityConstraintViolationException) =>
          log.info(s"A country with id = $id can't be deleted because it's a part of foreign key")
          senderToReturn ! CountryValidationErrors(List(CountryForeignKey(id)))
        case Failure(ex) =>
          log.error(s"An error occurred while deleting a country with id = $id: $ex")
          senderToReturn ! InternalServerError
      }
  }

  private def logErrorsAndSend(sender: ActorRef, countryDto: CountryApiDto, errors: List[CountryError]): Unit = {
    log.info(s"Validation of country = $countryDto failed because of: ${errors.mkString("[", ", ", "]")}")
    sender ! CountryValidationErrors(errors)
  }

  private def validateCountryDuplicatesOnCreate(country: Country): Future[List[CountryError]] = for {
    maybeCountryByName <- countryRepository.findByName(country.name)
    maybeCountryByCode <- countryRepository.findByCode(country.countryCode)
    duplicatedNameError <- Future(
      if (maybeCountryByName.isEmpty) List() else List(DuplicateCountryName(country.name.value))
    )
    duplicatedCodeError <- Future(
      if (maybeCountryByCode.isEmpty) List() else List(DuplicateCountryCode(country.countryCode.value))
    )
  } yield duplicatedNameError ++ duplicatedCodeError

  private def validateCountryDuplicatesOnUpdate(country: Country): Future[List[CountryError]] = for {
    maybeCountryByName <- countryRepository.findByName(country.name)
    maybeCountryByCode <- countryRepository.findByCode(country.countryCode)
    duplicatedNameError <- Future(
      if (maybeCountryByName.isEmpty || maybeCountryByName.head.id == country.id) List()
      else List(DuplicateCountryName(country.name.value))
    )
    duplicatedCodeError <- Future(
      if (maybeCountryByCode.isEmpty || maybeCountryByCode.head.id == country.id) List()
      else List(DuplicateCountryCode(country.countryCode.value))
    )
  } yield duplicatedNameError ++ duplicatedCodeError

  private def validateCountryDto(countryDto: CountryApiDto): Either[List[CountryError], Country] = {
    val validatedNameEither =
      validateParameter[CountryError, String, MatchesRegex[W.`"^[A-Z][A-Za-z]+$"`.T]](
        countryDto.name,
        InvalidCountryName(countryDto.name)
      )
    val validatedCodeEither =
      validateParameter[CountryError, String, MatchesRegex[W.`"^[a-z]{2}$"`.T]](
        countryDto.countryCode,
        InvalidCountryCode(countryDto.countryCode)
      )
    val validatedContinentEither: Either[CountryError, Continent] = Continent
      .withNameEither(countryDto.continent)
      .left
      .map(_ => InvalidCountryContinent(countryDto.continent))

    val nameErrorList = if (validatedNameEither.isLeft) List(InvalidCountryName(countryDto.name)) else List()
    val codeErrorList = if (validatedCodeEither.isLeft) List(InvalidCountryCode(countryDto.countryCode)) else List()
    val continentErrorList =
      if (validatedContinentEither.isLeft) List(InvalidCountryContinent(countryDto.continent)) else List()
    val errorsList: List[CountryError] = nameErrorList ++ codeErrorList ++ continentErrorList

    val result = for {
      name      <- validatedNameEither
      code      <- validatedCodeEither
      continent <- validatedContinentEither
    } yield Country(CountryId(countryDto.id), name, code, continent)

    result.left.map(_ => errorsList)
  }

  private def domainCountryToDtoCountry(country: Country): CountryApiDto =
    CountryApiDto(country.id.value, country.name.value, country.countryCode.value, country.continent.toString)
}

object CountryService {
  def props(countryRepository: CountryRepository)(implicit ec: ExecutionContext, timeout: Timeout): Props =
    Props(new CountryService(countryRepository))

  case class GetCountryByCode(code: String)
  case class AddAllCountries(countryDtoList: List[CountryApiDto])

  case class AllFoundCountries(countries: List[CountryApiDto])
  case class CountryValidationErrors(errors: List[CountryError])
  case class AllCountriesAdded(countries: List[CountryApiDto], errors: List[CountryError])
}
