package com.itechart.project.service

import akka.actor.{Actor, ActorLogging, Props}
import akka.pattern.pipe
import akka.util.Timeout
import com.itechart.project.domain.country.{Continent, Country, CountryId}
import com.itechart.project.dto.country.CountryApiDto
import com.itechart.project.repository.CountryRepository
import com.itechart.project.service.domain_errors.CountryErrors.CountryError
import com.itechart.project.service.domain_errors.CountryErrors.CountryError._
import com.itechart.project.utils.RefinedConversions.validateParameter
import eu.timepit.refined.W
import eu.timepit.refined.string.MatchesRegex

import java.sql.SQLIntegrityConstraintViolationException
import scala.concurrent.{ExecutionContext, Future}

class CountryService(countryRepository: CountryRepository)(implicit ec: ExecutionContext, timeout: Timeout)
  extends Actor
    with ActorLogging {
  import CountryService._

  override def receive: Receive = {
    case ReadCountries =>
      log.info("Getting all countries from database")
      val countriesFuture = countryRepository.findAll.mapTo[List[Country]]
      countriesFuture
        .map { countries =>
          log.info(s"Got ${countries.size} countries out of database")
          ReadCountries(countries.map(domainCountryToDtoCountry))
        }
        .pipeTo(sender())

    case ReadCountryById(id) =>
      log.info(s"Getting country with id = $id")
      val countryFuture = countryRepository.findById(CountryId(id)).mapTo[Option[Country]]
      countryFuture
        .map {
          case None =>
            log.info(s"Country with id = $id not found")
            CountryNotFound
          case Some(country) =>
            log.info(s"Country with id = $id is found: $country")
            ReadCountry(domainCountryToDtoCountry(country))
        }
        .pipeTo(sender())

    case ReadCountryByName(name) =>
      log.info(s"Getting country with name = $name")
      val nameEither =
        validateParameter[CountryError, String, MatchesRegex[W.`"^[A-Z][A-Za-z]+$"`.T]](name, InvalidCountryName(name))
      nameEither match {
        case Left(countryError) =>
          log.info(s"Validation of name = $name failed due to error: ${countryError.message}")
          sender() ! ReadCountryError(countryError)
        case Right(validName) =>
          log.info(s"Extracting country with name = $name out of database")
          val countryFuture = countryRepository.findByName(validName).mapTo[Option[Country]]
          countryFuture
            .map {
              case None =>
                log.info(s"Country with name = $name not found")
                CountryNotFound
              case Some(country) =>
                log.info(s"Country with name = $name is found: $country")
                ReadCountry(domainCountryToDtoCountry(country))
            }
            .pipeTo(sender())
      }

    case ReadCountryByCode(code) =>
      log.info(s"Getting country with code = $code")
      val codeEither =
        validateParameter[CountryError, String, MatchesRegex[W.`"^[a-z]{2}$"`.T]](code, InvalidCountryCode(code))
      codeEither match {
        case Left(countryError) =>
          log.info(s"Validation of code = $code failed due to error: ${countryError.message}")
          sender() ! ReadCountryError(countryError)
        case Right(validCode) =>
          log.info(s"Extracting country with code = $code out of database")
          val countryFuture = countryRepository.findByCode(validCode).mapTo[Option[Country]]
          countryFuture
            .map {
              case None =>
                log.info(s"Country with code = $code not found")
                CountryNotFound
              case Some(country) =>
                log.info(s"Country with code = $code is found: $country")
                ReadCountry(domainCountryToDtoCountry(country))
            }
            .pipeTo(sender())
      }

    case CreateCountry(dtoCountry) =>
      log.info(s"Creating a new country = $dtoCountry")
      val validatedCountry = validateCountryDto(dtoCountry)
      validatedCountry match {
        case Left(errors) =>
          log.info(s"Validation of country = $dtoCountry failed on creating due to: ${errors.mkString("[", ", ", "]")}")
          sender() ! ReadCountryErrors(errors)
        case Right(country) =>
          val countryOrErrors = for {
            errors    <- validateCountryDuplicatesOnCreate(country)
            countryId <- if (errors.isEmpty) countryRepository.create(country) else Future(CountryId(0))
            result =
              if (countryId.value != 0) {
                log.info(s"Country $country is successfully created")
                ReadCountry(dtoCountry.copy(id = countryId.value))
              } else {
                log.info(s"Country $country isn't created due to: ${errors.mkString("[", ", ", "]")}")
                ReadCountryErrors(errors)
              }
          } yield result
          countryOrErrors.pipeTo(sender())
      }

    case UpdateCountry(dtoCountry) =>
      log.info(s"Updating a country = $dtoCountry")
      val validatedCountry = validateCountryDto(dtoCountry)
      validatedCountry match {
        case Left(errors) =>
          log.info(s"Validation of country = $dtoCountry failed on updating due to: ${errors.mkString("[", ", ", "]")}")
          sender() ! ReadCountryErrors(errors)
        case Right(country) =>
          val countryOrErrors = for {
            errors      <- validateCountryDuplicatesOnUpdate(country)
            rowsUpdated <- if (errors.isEmpty) countryRepository.update(country) else Future(-1)
            result =
              if (rowsUpdated != -1) {
                log.info(s"Country $country is successfully updated")
                ReadCountry(dtoCountry)
              } else {
                log.info(s"Country $country isn't updated due to: ${errors.mkString("[", ", ", "]")}")
                ReadCountryErrors(errors)
              }
          } yield result
          countryOrErrors.pipeTo(sender())
      }

    case DeleteCountry(id) =>
      log.info(s"Deleting country with id = $id")
      val countryFuture = countryRepository.delete(CountryId(id)).mapTo[Int]
      countryFuture
        .map {
          case 0 =>
            log.info(s"Country with id = $id is not deleted")
            CountryDeleteFailed
          case i if i > 0 =>
            log.info(s"Country with id = $id is deleted")
            CountryDeleteCompleted
        }
        .recover { case _: SQLIntegrityConstraintViolationException =>
          log.info(s"A country with id = $id isn't deleted because it's a part of foreign key")
          ReadCountryError(CountryForeignKey(id))
        }
        .pipeTo(sender())
  }

  private def validateCountryDuplicatesOnCreate(country: Country): Future[List[CountryError]] = {
    for {
      duplicatedNameError <- countryRepository
        .findByName(country.name)
        .map {
          case None    => List()
          case Some(_) => List(DuplicateCountryName(country.name.value))
        }
      duplicatedCodeError <- countryRepository
        .findByCode(country.countryCode)
        .map {
          case None    => List()
          case Some(_) => List(DuplicateCountryCode(country.countryCode.value))
        }
    } yield duplicatedNameError ++ duplicatedCodeError
  }

  private def validateCountryDuplicatesOnUpdate(country: Country): Future[List[CountryError]] = for {
    duplicatedNameError <- countryRepository
      .findByName(country.name)
      .map {
        case None    => List()
        case Some(c) => if (c.id == country.id) List() else List(DuplicateCountryName(country.name.value))
      }
    duplicatedCodeError <- countryRepository
      .findByCode(country.countryCode)
      .map {
        case None    => List()
        case Some(c) => if (c.id == country.id) List() else List(DuplicateCountryCode(country.countryCode.value))
      }
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

  case object ReadCountries
  case class ReadCountryById(id: Int)
  case class ReadCountryByName(name: String)
  case class ReadCountryByCode(code: String)
  case class CreateCountry(dtoCountry: CountryApiDto)
  case class CreateCountries(dtoCountries: List[CountryApiDto])
  case class UpdateCountry(dtoCountry: CountryApiDto)
  case class DeleteCountry(id: Int)

  case class ReadCountry(dtoCountry: CountryApiDto)
  case class ReadCountries(dtoCountries: List[CountryApiDto])
  case object CountryDeleteCompleted
  case object CountryDeleteFailed

  case object CountryNotFound
  case class ReadCountryError(error: CountryError)
  case class ReadCountryErrors(errors: List[CountryError])
}
