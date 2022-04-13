package com.itechart.project.service

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.util.Timeout
import akka.pattern.ask
import com.itechart.project.domain.stage.{Stage, StageId}
import com.itechart.project.dto.stage.StageApiDto
import com.itechart.project.repository.StageRepository
import com.itechart.project.service.CommonServiceMessages.ErrorWrapper
import com.itechart.project.service.CommonServiceMessages.Requests._
import com.itechart.project.service.CommonServiceMessages.Responses._
import com.itechart.project.service.domain_errors.StageErrors.StageError
import com.itechart.project.service.domain_errors.StageErrors.StageError._
import com.itechart.project.utils.RefinedConversions.validateParameter
import eu.timepit.refined.predicates.all.NonEmpty

import java.sql.SQLIntegrityConstraintViolationException
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class StageService(stageRepository: StageRepository)(implicit ec: ExecutionContext, timeout: Timeout)
  extends Actor
    with ActorLogging {
  import StageService._

  override def receive: Receive = {
    case GetAllEntities =>
      val senderToReturn = sender()
      log.info("Getting all stages from database")
      val stagesFuture = stageRepository.findAll
      stagesFuture.onComplete {
        case Success(stages) =>
          log.info(s"Got ${stages.size} stages out of database")
          senderToReturn ! AllFoundStages(stages.map(domainStageToDtoStage))
        case Failure(ex) =>
          log.error(s"An error occurred while extracting all stages out of database: $ex")
          senderToReturn ! InternalServerError
      }

    case GetEntityByT(id: Int) =>
      val senderToReturn = sender()
      log.info(s"Getting stage with id = $id")
      val stageFuture = stageRepository.findById(StageId(id))
      stageFuture.onComplete {
        case Success(maybeStage) =>
          log.info(s"Stage with id = $id ${if (maybeStage.isEmpty) "not "}found")
          senderToReturn ! OneFoundEntity(maybeStage.map(domainStageToDtoStage))
        case Failure(ex) =>
          log.error(s"An error occurred while extracting a stage with id = $id: $ex")
          senderToReturn ! InternalServerError
      }

    case GetEntityByT(name: String) =>
      val senderToReturn = sender()
      log.info(s"Getting stage with name = $name")
      val validatedNameEither = validateParameter[StageError, String, NonEmpty](name, InvalidStageName(name))
      validatedNameEither match {
        case Left(error) =>
          log.info(s"Validation of name = $name failed")
          senderToReturn ! ValidationErrors(StageErrorWrapper(List(error)))
        case Right(validName) =>
          log.info(s"Extracting stage with name = $name out of database")
          val stageFuture = stageRepository.findByName(validName)
          stageFuture.onComplete {
            case Success(maybeStage) =>
              log.info(s"Stage with name = $name ${if (maybeStage.isEmpty) "not " else ""}found")
              senderToReturn ! OneFoundEntity(maybeStage.map(domainStageToDtoStage))
            case Failure(ex) =>
              log.error(s"An error occurred while extracting a stage with name = $name: $ex")
              senderToReturn ! InternalServerError
          }
      }

    case AddOneEntity(stageDto: StageApiDto) =>
      val senderToReturn = sender()
      log.info(s"Adding a stage = $stageDto")
      val validatedStage = validateStageDto(stageDto)
      validatedStage match {
        case Left(errors) =>
          logErrorsAndSend(senderToReturn, stageDto, errors)
        case Right(stage) =>
          val stageIdOrErrors = for {
            errors  <- validateStageDuplicates(stage)
            stageId <- if (errors.isEmpty) stageRepository.create(stage) else Future(StageId(0))
            result   = if (stageId.value == 0) Left(errors) else Right(stageId)
          } yield result
          stageIdOrErrors.onComplete {
            case Success(Right(id)) =>
              log.info(s"Stage $stage successfully created")
              senderToReturn ! OneEntityAdded(stageDto.copy(id = id.value))
            case Success(Left(errors)) =>
              log.info(s"Stage $stage doesn't created because of: ${errors.mkString("[", ", ", "]")}")
              senderToReturn ! ValidationErrors(StageErrorWrapper(errors))
            case Failure(ex) =>
              log.error(s"An error occurred while creating a stage $stage: $ex")
              senderToReturn ! InternalServerError
          }
      }

    case AddAllStages(stageDtoList) =>
      val senderToReturn = sender()
      log.info(s"Adding stages $stageDtoList")
      val addedStages = Future.traverse(stageDtoList.map(self ? AddOneEntity(_)))(identity)
      addedStages.onComplete {
        case Success(list) =>
          val stages: List[StageApiDto] = list.flatMap {
            case OneEntityAdded(stage: StageApiDto) => List(stage)
            case _ => List()
          }
          val errors: List[StageError] = list.flatMap {
            case ValidationErrors(StageErrorWrapper(errors)) => errors
            case _                                           => List()
          }
          log.info(s"Stages $stages added successfully")
          log.info(s"Other stages aren't added because of: ${errors.mkString("[", ", ", "]")}")
          senderToReturn ! AllStagesAdded(stages, errors)
        case Failure(ex) =>
          log.error(s"An error occurred while creating stages $stageDtoList: $ex")
          senderToReturn ! InternalServerError
      }

    case UpdateEntity(stageDto: StageApiDto) =>
      val senderToReturn = sender()
      log.info(s"Updating a stage = $stageDto")
      val validatedStage = validateStageDto(stageDto)
      validatedStage match {
        case Left(errors) =>
          logErrorsAndSend(senderToReturn, stageDto, errors)
        case Right(stage) =>
          val rowsUpdatedOrErrors = for {
            errors      <- validateStageDuplicates(stage)
            rowsUpdated <- if (errors.isEmpty) stageRepository.update(stage) else Future(-1)
            result       = if (rowsUpdated == -1) Left(errors) else Right(rowsUpdated)
          } yield result
          rowsUpdatedOrErrors.onComplete {
            case Success(Right(rowsUpdated)) =>
              log.info(s"Stage $stage is ${if (rowsUpdated == 0) "not " else ""}updated")
              val result = if (rowsUpdated == 0) UpdateFailed else UpdateCompleted
              senderToReturn ! result
            case Success(Left(errors)) =>
              log.info(s"Stage $stage isn't updated because of: ${errors.mkString("[", ", ", "]")}")
              senderToReturn ! ValidationErrors(StageErrorWrapper(errors))
            case Failure(ex) =>
              log.error(s"An error occurred while updating a stage $stage: $ex")
              senderToReturn ! InternalServerError
          }
      }

    case RemoveEntity(id: Int) =>
      val senderToReturn = sender()
      log.info(s"Deleting stage with id = $id")
      val stageFuture = stageRepository.delete(StageId(id))
      stageFuture.onComplete {
        case Success(rowsDeleted) =>
          log.info(s"Stage with id = $id ${if (rowsDeleted == 0) "not " else ""}removed")
          val result = if (rowsDeleted == 0) RemoveFailed else RemoveCompleted
          senderToReturn ! result
        case Failure(_: SQLIntegrityConstraintViolationException) =>
          log.info(s"A stage with id = $id can't be deleted because it's a part of foreign key")
          senderToReturn ! ValidationErrors(StageErrorWrapper(List(StageForeignKey(id))))
        case Failure(ex) =>
          log.error(s"An error occurred while deleting a stage with id = $id: $ex")
          senderToReturn ! InternalServerError
      }
  }

  private def logErrorsAndSend(sender: ActorRef, stageDto: StageApiDto, errors: List[StageError]): Unit = {
    log.info(s"Validation of stage = $stageDto failed because of: ${errors.mkString("[", ", ", "]")}")
    sender ! ValidationErrors(StageErrorWrapper(errors))
  }

  private def validateStageDuplicates(stage: Stage): Future[List[StageError]] = for {
    maybeStageByName <- stageRepository.findByName(stage.name)
    duplicatedNameError <- Future(
      if (maybeStageByName.isEmpty) List() else List(DuplicateStageName(stage.name.value))
    )
  } yield duplicatedNameError

  private def validateStageDto(stageDto: StageApiDto): Either[List[StageError], Stage] = {
    val result = for {
      name <- validateParameter[StageError, String, NonEmpty](stageDto.name, InvalidStageName(stageDto.name))
    } yield Stage(StageId(stageDto.id), name)

    result.left.map(List(_))
  }

  private def domainStageToDtoStage(stage: Stage): StageApiDto =
    StageApiDto(stage.id.value, stage.name.value)
}

object StageService {
  def props(stageRepository: StageRepository)(implicit ec: ExecutionContext, timeout: Timeout): Props =
    Props(new StageService(stageRepository))

  case class AddAllStages(stageDtoList: List[StageApiDto])

  case class AllFoundStages(stages: List[StageApiDto])
  case class StageErrorWrapper(override val errors: List[StageError]) extends ErrorWrapper
  case class AllStagesAdded(stages: List[StageApiDto], errors: List[StageError])
}
