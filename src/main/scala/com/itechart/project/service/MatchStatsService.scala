package com.itechart.project.service

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.util.Timeout
import com.itechart.project.domain.match_stats.{Attendance, MatchScore, MatchStats, MatchStatsId}
import com.itechart.project.dto.match_stats.MatchStatsApiDto
import com.itechart.project.repository.MatchStatsRepository
import com.itechart.project.service.CommonServiceMessages.Requests._
import com.itechart.project.service.CommonServiceMessages.Responses._
import com.itechart.project.service.domain_errors.MatchStatsErrors.MatchStatsError
import com.itechart.project.service.domain_errors.MatchStatsErrors.MatchStatsError._
import com.itechart.project.utils.RefinedConversions.validateParameter
import eu.timepit.refined.numeric.NonNegative

import java.sql.SQLIntegrityConstraintViolationException
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class MatchStatsService(matchStatsRepository: MatchStatsRepository)(implicit ec: ExecutionContext, timeout: Timeout)
  extends Actor
    with ActorLogging {
  import MatchStatsService._

  override def receive: Receive = {
    case GetEntityByT(id: Long) =>
      val senderToReturn = sender()
      log.info(s"Getting match stats with id = $id")
      val matchStatsFuture = matchStatsRepository.findById(MatchStatsId(id))
      matchStatsFuture.onComplete {
        case Success(maybeMatchStats) =>
          log.info(s"Match stats with id = $id ${if (maybeMatchStats.isEmpty) "not "}found")
          senderToReturn ! OneFoundEntity(maybeMatchStats.map(domainMatchStatsToDtoMatchStats))
        case Failure(ex) =>
          log.error(s"An error occurred while extracting match stats with id = $id: $ex")
          senderToReturn ! InternalServerError
      }

    case AddOneEntity(matchStatsDto: MatchStatsApiDto) =>
      val senderToReturn = sender()
      log.info(s"Adding match stats = $matchStatsDto")
      val validatedMatchStats = validateMatchStatsDto(matchStatsDto)
      validatedMatchStats match {
        case Left(errors) =>
          logErrorsAndSend(senderToReturn, matchStatsDto, errors)
        case Right(matchStats) =>
          val matchStatsId = matchStatsRepository.create(matchStats)
          matchStatsId.onComplete {
            case Success(id) =>
              log.info(s"Match stats $matchStats successfully created")
              senderToReturn ! OneEntityAdded(matchStatsDto.copy(id = id.value))
            case Failure(ex) =>
              log.error(s"An error occurred while creating match stats $matchStats: $ex")
              senderToReturn ! InternalServerError
          }
      }

    case UpdateEntity(matchStatsDto: MatchStatsApiDto) =>
      val senderToReturn = sender()
      log.info(s"Updating match stats = $matchStatsDto")
      val validatedMatchStats = validateMatchStatsDto(matchStatsDto)
      validatedMatchStats match {
        case Left(errors) =>
          logErrorsAndSend(senderToReturn, matchStatsDto, errors)
        case Right(matchStats) =>
          val rowsUpdated = matchStatsRepository.update(matchStats)
          rowsUpdated.onComplete {
            case Success(rows) =>
              log.info(s"Match stats $matchStats is ${if (rows == 0) "not " else ""}updated")
              val result = if (rows == 0) UpdateFailed else UpdateCompleted
              senderToReturn ! result
            case Failure(ex) =>
              log.error(s"An error occurred while updating match stats $matchStats: $ex")
              senderToReturn ! InternalServerError
          }
      }

    case RemoveEntity(id: Long) =>
      val senderToReturn = sender()
      log.info(s"Deleting match stats with id = $id")
      val matchStatsFuture = matchStatsRepository.delete(MatchStatsId(id))
      matchStatsFuture.onComplete {
        case Success(rowsDeleted) =>
          log.info(s"Match stats with id = $id ${if (rowsDeleted == 0) "not " else ""}removed")
          val result = if (rowsDeleted == 0) RemoveFailed else RemoveCompleted
          senderToReturn ! result
        case Failure(_: SQLIntegrityConstraintViolationException) =>
          log.info(s"Match stats with id = $id can't be deleted because it's a part of foreign key")
          senderToReturn ! MatchStatsValidationErrors(List(MatchStatsForeignKey(id)))
        case Failure(ex) =>
          log.error(s"An error occurred while deleting match stats with id = $id: $ex")
          senderToReturn ! InternalServerError
      }
  }

  private def logErrorsAndSend(
    sender:        ActorRef,
    matchStatsDto: MatchStatsApiDto,
    errors:        List[MatchStatsError]
  ): Unit = {
    log.info(s"Validation of match stats = $matchStatsDto failed because of: ${errors.mkString("[", ", ", "]")}")
    sender ! MatchStatsValidationErrors(errors)
  }

  private def validateMatchStatsDto(matchStatsDto: MatchStatsApiDto): Either[List[MatchStatsError], MatchStats] = {
    val validatedHtHomeTeamScoreEither = validateScore(matchStatsDto.htHomeTeamScore, "half")
    val validatedHtAwayTeamScoreEither = validateScore(matchStatsDto.htAwayTeamScore, "half")
    val validatedFtHomeTeamScoreEither = validateScore(matchStatsDto.ftHomeTeamScore, "full")
    val validatedFtAwayTeamScoreEither = validateScore(matchStatsDto.ftAwayTeamScore, "full")
    val validatedEtHomeTeamScoreEither = validateScore(matchStatsDto.etHomeTeamScore, "extra")
    val validatedEtAwayTeamScoreEither = validateScore(matchStatsDto.etAwayTeamScore, "extra")
    val validatedPHomeTeamScoreEither  = validateScore(matchStatsDto.pHomeTeamScore, "penalty")
    val validatedPAwayTeamScoreEither  = validateScore(matchStatsDto.pAwayTeamScore, "penalty")
    val validatedAttendanceEither      = validateAttendance(matchStatsDto.attendance)

    val htHomeTeamScoreErrorList =
      if (validatedHtHomeTeamScoreEither.isLeft) List(InvalidMatchScore(matchStatsDto.htHomeTeamScore.head, "half"))
      else List()
    val htAwayTeamScoreErrorList =
      if (validatedHtAwayTeamScoreEither.isLeft) List(InvalidMatchScore(matchStatsDto.htAwayTeamScore.head, "half"))
      else List()
    val ftHomeTeamScoreErrorList =
      if (validatedFtHomeTeamScoreEither.isLeft) List(InvalidMatchScore(matchStatsDto.ftHomeTeamScore.head, "full"))
      else List()
    val ftAwayTeamScoreErrorList =
      if (validatedFtAwayTeamScoreEither.isLeft) List(InvalidMatchScore(matchStatsDto.ftAwayTeamScore.head, "full"))
      else List()
    val etHomeTeamScoreErrorList =
      if (validatedEtHomeTeamScoreEither.isLeft) List(InvalidMatchScore(matchStatsDto.etHomeTeamScore.head, "extra"))
      else List()
    val etAwayTeamScoreErrorList =
      if (validatedEtAwayTeamScoreEither.isLeft) List(InvalidMatchScore(matchStatsDto.etAwayTeamScore.head, "extra"))
      else List()
    val pHomeTeamScoreErrorList =
      if (validatedPHomeTeamScoreEither.isLeft) List(InvalidMatchScore(matchStatsDto.pHomeTeamScore.head, "penalty"))
      else List()
    val pAwayTeamScoreErrorList =
      if (validatedPAwayTeamScoreEither.isLeft) List(InvalidMatchScore(matchStatsDto.pAwayTeamScore.head, "penalty"))
      else List()
    val attendanceErrorList =
      if (validatedAttendanceEither.isLeft) List(InvalidMatchAttendance(matchStatsDto.attendance.head))
      else List()
    val errorsList: List[MatchStatsError] =
      htHomeTeamScoreErrorList ++ htAwayTeamScoreErrorList ++ ftHomeTeamScoreErrorList ++
        ftAwayTeamScoreErrorList ++ etHomeTeamScoreErrorList ++ etAwayTeamScoreErrorList ++
        pHomeTeamScoreErrorList ++ pAwayTeamScoreErrorList ++ attendanceErrorList ++ validateEmptyScores(matchStatsDto)

    val result = for {
      htHomeTeamScore <- validatedHtHomeTeamScoreEither
      htAwayTeamScore <- validatedHtAwayTeamScoreEither
      ftHomeTeamScore <- validatedFtHomeTeamScoreEither
      ftAwayTeamScore <- validatedFtAwayTeamScoreEither
      etHomeTeamScore <- validatedEtHomeTeamScoreEither
      etAwayTeamScore <- validatedEtAwayTeamScoreEither
      pHomeTeamScore  <- validatedPHomeTeamScoreEither
      pAwayTeamScore  <- validatedPAwayTeamScoreEither
      attendance      <- validatedAttendanceEither
      _               <- if (validateEmptyScores(matchStatsDto).isEmpty) Right(()) else Left(())
    } yield MatchStats(
      MatchStatsId(matchStatsDto.id),
      htHomeTeamScore,
      htAwayTeamScore,
      ftHomeTeamScore,
      ftAwayTeamScore,
      etHomeTeamScore,
      etAwayTeamScore,
      pHomeTeamScore,
      pAwayTeamScore,
      attendance
    )

    result.left.map(_ => errorsList)
  }

  private def validateEmptyScores(matchStatsDto: MatchStatsApiDto): List[MatchStatsError] = {
    if (matchStatsDto.pHomeTeamScore.isDefined || matchStatsDto.pAwayTeamScore.isDefined) {
      val penaltyScoreError =
        if (matchStatsDto.pHomeTeamScore.isEmpty || matchStatsDto.pAwayTeamScore.isEmpty)
          List(EmptyMatchScore("penalty"))
        else List()
      val extraTimeScoreError =
        if (matchStatsDto.etHomeTeamScore.isEmpty || matchStatsDto.etAwayTeamScore.isEmpty)
          List(EmptyMatchScore("extra"))
        else List()
      penaltyScoreError ++ extraTimeScoreError
    } else if (matchStatsDto.etHomeTeamScore.isDefined || matchStatsDto.etAwayTeamScore.isDefined) {
      val extraTimeScoreError =
        if (matchStatsDto.etHomeTeamScore.isEmpty || matchStatsDto.etAwayTeamScore.isEmpty)
          List(EmptyMatchScore("extra"))
        else List()
      val fullTimeScoreError =
        if (matchStatsDto.ftHomeTeamScore.isEmpty || matchStatsDto.ftAwayTeamScore.isEmpty)
          List(EmptyMatchScore("full"))
        else List()
      extraTimeScoreError ++ fullTimeScoreError
    } else if (matchStatsDto.ftHomeTeamScore.isDefined || matchStatsDto.ftAwayTeamScore.isDefined) {
      val fullTimeScoreError =
        if (matchStatsDto.ftHomeTeamScore.isEmpty || matchStatsDto.ftAwayTeamScore.isEmpty)
          List(EmptyMatchScore("full"))
        else List()
      val halfTimeScoreError =
        if (matchStatsDto.htHomeTeamScore.isEmpty || matchStatsDto.htAwayTeamScore.isEmpty)
          List(EmptyMatchScore("half"))
        else List()
      fullTimeScoreError ++ halfTimeScoreError
    } else {
      List()
    }
  }

  private def validateScore(score: Option[Int], time: String): Either[MatchStatsError, Option[MatchScore]] = {
    if (score.isEmpty) Right(None)
    else
      validateParameter[MatchStatsError, Int, NonNegative](score.head, InvalidMatchScore(score.head, time))
        .map(Option(_))
  }

  private def validateAttendance(attendance: Option[Int]): Either[MatchStatsError, Option[Attendance]] = {
    if (attendance.isEmpty) Right(None)
    else
      validateParameter[MatchStatsError, Int, NonNegative](attendance.head, InvalidMatchAttendance(attendance.head))
        .map(Option(_))
  }

  private def domainMatchStatsToDtoMatchStats(matchStats: MatchStats): MatchStatsApiDto =
    MatchStatsApiDto(
      matchStats.id.value,
      matchStats.htHomeTeamScore.map(_.value),
      matchStats.htAwayTeamScore.map(_.value),
      matchStats.ftHomeTeamScore.map(_.value),
      matchStats.ftAwayTeamScore.map(_.value),
      matchStats.etHomeTeamScore.map(_.value),
      matchStats.etAwayTeamScore.map(_.value),
      matchStats.pHomeTeamScore.map(_.value),
      matchStats.pAwayTeamScore.map(_.value),
      matchStats.attendance.map(_.value)
    )
}

object MatchStatsService {
  def props(matchStatsRepository: MatchStatsRepository)(implicit ec: ExecutionContext, timeout: Timeout): Props =
    Props(new MatchStatsService(matchStatsRepository))

  case class MatchStatsValidationErrors(errors: List[MatchStatsError])
}
