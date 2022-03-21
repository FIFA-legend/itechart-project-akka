package com.itechart.project.service

import akka.actor.{Actor, ActorLogging, Props}
import com.itechart.project.domain.country.{Continent, Country, CountryId}
import com.itechart.project.dto.country_dto.CountryApiDto
import com.itechart.project.repository.CountryRepository
import com.itechart.project.service.domain_errors.CountryErrors.CountryError
import com.itechart.project.service.domain_errors.CountryErrors.CountryError._
import com.itechart.project.utils.RefinedConversions.validateParameter
import eu.timepit.refined.W
import eu.timepit.refined.string.MatchesRegex

import java.sql.SQLIntegrityConstraintViolationException
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class CountryService(countryRepository: CountryRepository, implicit val ec: ExecutionContext)
  extends Actor
    with ActorLogging {
  import CountryService._

  override def receive: Receive = {
    case GetAllCountries =>
      val senderToReturn = sender()
      log.info("Getting all countries from database")
      val countriesFuture = countryRepository.findAll
      countriesFuture.onComplete {
        case Success(countries) =>
          log.info(s"Got ${countries.size} countries out of database")
          senderToReturn ! countries.map(domainCountryToDtoCountry)
        case Failure(ex) =>
          log.error(s"An error occurred while extracting all countries out of database: $ex")
          senderToReturn ! CountryOperationFail()
      }

    case GetCountryById(id) =>
      val senderToReturn = sender()
      log.info(s"Getting country with id = $id")
      val countryFuture = countryRepository.findById(CountryId(id))
      countryFuture.onComplete {
        case Success(maybeCountry) =>
          log.info(s"Country with id = $id ${if (maybeCountry.isEmpty) "not "}found")
          senderToReturn ! maybeCountry.map(domainCountryToDtoCountry)
        case Failure(ex) =>
          log.error(s"An error occurred while extracting a country with id = $id: $ex")
          senderToReturn ! CountryOperationFail()
      }

    case GetCountryByName(name) =>
      val senderToReturn = sender()
      val validatedNameEither =
        validateParameter[CountryError, String, MatchesRegex[W.`"^[A-Z][A-Za-z]+$"`.T]](name, InvalidCountryName(name))
      validatedNameEither match {
        case Left(error) => senderToReturn ! error
        case Right(validName) =>
          log.info(s"Getting country with name = $name")
          val countryFuture = countryRepository.findByName(validName)
          countryFuture.onComplete {
            case Success(maybeCountry) =>
              log.info(s"Country with name = $name ${if (maybeCountry.isEmpty) "not " else ""}found")
              senderToReturn ! maybeCountry.map(domainCountryToDtoCountry)
            case Failure(ex) =>
              log.error(s"An error occurred while extracting a country with name = $name: $ex")
              senderToReturn ! CountryOperationFail()
          }
      }

    case GetCountryByCode(code) =>
      val senderToReturn = sender()
      val validatedCodeEither =
        validateParameter[CountryError, String, MatchesRegex[W.`"^[a-z]{2}$"`.T]](code, InvalidCountryCode(code))
      validatedCodeEither match {
        case Left(error) => senderToReturn ! error
        case Right(validCode) =>
          log.info(s"Getting country with code = $code")
          val countryFuture = countryRepository.findByCode(validCode)
          countryFuture.onComplete {
            case Success(maybeCountry) =>
              log.info(s"Country with code = $code ${if (maybeCountry.isEmpty) "not " else ""}found")
              senderToReturn ! maybeCountry.map(domainCountryToDtoCountry)
            case Failure(ex) =>
              log.error(s"An error occurred while extracting a country with code = $code: $ex")
              senderToReturn ! CountryOperationFail
          }
      }

    case AddCountry(countryDto) =>
      val senderToReturn   = sender()
      val validatedCountry = validateCountryDto(countryDto)
      validatedCountry match {
        case Left(errors) => senderToReturn ! errors
        case Right(country) =>
          val future = countryRepository.create(country)
          future.onComplete {
            case Success(id) =>
              log.info(s"Country $country successfully created")
              senderToReturn ! countryDto.copy(id = id.value)
            case Failure(ex: SQLIntegrityConstraintViolationException) =>
              log.info(s"Country $country doesn't created because of $ex")
              senderToReturn ! List(
                DuplicateCountryName(countryDto.name),
                DuplicateCountryCode(countryDto.country_code)
              )
            case Failure(ex) =>
              log.error(s"An error occurred while creating a country $country: $ex")
              senderToReturn ! CountryOperationFail
          }
      }

    case UpdateCountry(countryDto: CountryApiDto) =>
      val senderToReturn   = sender()
      val validatedCountry = validateCountryDto(countryDto)
      validatedCountry match {
        case Left(errors) => senderToReturn ! errors
        case Right(country) =>
          val future = countryRepository.update(country)
          future.onComplete {
            case Success(rowsUpdated) =>
              log.info(s"Country $country update process finished")
              senderToReturn ! rowsUpdated
            case Failure(ex: SQLIntegrityConstraintViolationException) =>
              log.info(s"Country $country doesn't updated because of $ex")
              senderToReturn ! List(
                DuplicateCountryName(countryDto.name),
                DuplicateCountryCode(countryDto.country_code)
              )
            case Failure(ex) =>
              log.error(s"An error occurred while updating a country $country: $ex")
              senderToReturn ! CountryOperationFail
          }
      }

    case RemoveCountry(id) =>
      val senderToReturn = sender()
      log.info(s"Deleting country with id = $id")
      val countryFuture = countryRepository.delete(CountryId(id))
      countryFuture.onComplete {
        case Success(result) =>
          log.info(s"Country with id = $id ${if (result == 0) "not " else ""}removed")
          senderToReturn ! result
        case Failure(ex) =>
          log.error(s"An error occurred while deleting a country with id = $id: $ex")
          senderToReturn ! CountryOperationFail
      }

  }

  private def validateCountryDto(countryDto: CountryApiDto): Either[List[CountryError], Country] = {
    val validatedNameEither =
      validateParameter[CountryError, String, MatchesRegex[W.`"^[A-Z][A-Za-z]+$"`.T]](
        countryDto.name,
        InvalidCountryName(countryDto.name)
      )
    val validatedCodeEither =
      validateParameter[CountryError, String, MatchesRegex[W.`"^[a-z]{2}$"`.T]](
        countryDto.country_code,
        InvalidCountryCode(countryDto.country_code)
      )
    val validatedContinentEither: Either[CountryError, Continent] = countryDto.continent match {
      case "Africa"        => Right(Continent.Africa)
      case "Asia"          => Right(Continent.Asia)
      case "Europe"        => Right(Continent.Europe)
      case "Oceania"       => Right(Continent.Oceania)
      case "North America" => Right(Continent.NorthAmerica)
      case "South America" => Right(Continent.SouthAmerica)
      case _               => Left(InvalidCountryContinent(countryDto.continent))
    }

    val nameErrorList = if (validatedNameEither.isLeft) List(InvalidCountryName(countryDto.name)) else List()
    val codeErrorList = if (validatedCodeEither.isLeft) List(InvalidCountryCode(countryDto.country_code)) else List()
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
  def apply(countryRepository: CountryRepository)(implicit ec: ExecutionContext): Props = Props(
    new CountryService(countryRepository, ec)
  )

  case object GetAllCountries
  case class GetCountryById(id: Int)
  case class GetCountryByName(name: String)
  case class GetCountryByCode(code: String)
  case class AddCountry(countryDto: CountryApiDto)
  case class AddCountries(countryDtoList: List[CountryApiDto])
  case class UpdateCountry(countryDto: CountryApiDto)
  case class RemoveCountry(id: Int)
}
