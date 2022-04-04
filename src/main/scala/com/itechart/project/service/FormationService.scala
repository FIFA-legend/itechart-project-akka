package com.itechart.project.service

import akka.actor.{Actor, ActorLogging, Props}
import com.itechart.project.domain.formation.{Formation, FormationId}
import com.itechart.project.dto.formation.FormationApiDto
import com.itechart.project.repository.FormationRepository
import com.itechart.project.service.domain_errors.FormationErrors.FormationError
import com.itechart.project.service.domain_errors.FormationErrors.FormationError.InvalidFormationName
import com.itechart.project.utils.FormationNameConversion

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

class FormationService(formationRepository: FormationRepository, implicit val ex: ExecutionContext)
  extends Actor
    with ActorLogging {
  import FormationService._

  override def receive: Receive = {
    case GetAllFormations =>
      val senderToReturn = sender()
      log.info("Getting all formations from database")
      val formationsFuture = formationRepository.findAll
      formationsFuture.onComplete {
        case Success(formations) =>
          log.info(s"Got ${formations.size} formations out of database")
          senderToReturn ! FoundFormations(formations.map(domainFormationToDtoFormation))
        case Failure(ex) =>
          log.error(s"An error occurred while extracting all formations out of database: $ex")
          senderToReturn ! FormationInternalServerError
      }

    case GetFormationById(id) =>
      val senderToReturn = sender()
      log.info(s"Getting formation with id = $id")
      val formationFuture = formationRepository.findById(FormationId(id))
      formationFuture.onComplete {
        case Success(maybeFormation) =>
          log.info(s"Formation with id = $id ${if (maybeFormation.isEmpty) "not "}found")
          senderToReturn ! FoundFormation(maybeFormation.map(domainFormationToDtoFormation))
        case Failure(ex) =>
          log.error(s"An error occurred while extracting a formation with id = $id: $ex")
          senderToReturn ! FormationInternalServerError
      }

    case GetFormationByName(name) =>
      val senderToReturn = sender()
      log.info(s"Getting formation with name = $name")
      val validatedNameEither = Try(FormationNameConversion.prettyStringToFormationName(name)).toEither.left
        .map(_ => InvalidFormationName(name))
      validatedNameEither match {
        case Left(error) =>
          log.info(s"Validation of name = $name failed")
          senderToReturn ! FormationValidationErrors(List(error))
        case Right(validName) =>
          log.info(s"Extracting formation with name = $name out of database")
          val formationFuture = formationRepository.findByName(validName)
          formationFuture.onComplete {
            case Success(maybeFormation) =>
              log.info(s"Formation with name = $name ${if (maybeFormation.isEmpty) "not " else ""}found")
              senderToReturn ! FoundFormation(maybeFormation.map(domainFormationToDtoFormation))
            case Failure(ex) =>
              log.error(s"An error occurred while extracting a formation with name = $name: $ex")
              senderToReturn ! FormationInternalServerError
          }
      }
  }

  private def domainFormationToDtoFormation(formation: Formation): FormationApiDto =
    FormationApiDto(formation.id.value, FormationNameConversion.formationNameToPrettyString(formation.name))
}

object FormationService {
  def props(formationRepository: FormationRepository)(implicit ec: ExecutionContext): Props = Props(
    new FormationService(formationRepository, ec)
  )

  case object GetAllFormations
  case class GetFormationById(id: Int)
  case class GetFormationByName(name: String)

  case class FoundFormations(formations: List[FormationApiDto])
  case class FoundFormation(maybeFormation: Option[FormationApiDto])
  case class FormationValidationErrors(errors: List[FormationError])
  case object FormationInternalServerError
}
