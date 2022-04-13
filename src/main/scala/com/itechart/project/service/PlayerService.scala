package com.itechart.project.service

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.util.Timeout
import akka.pattern.ask
import com.itechart.project.domain.country.CountryId
import com.itechart.project.domain.player.{Height, Player, PlayerId, PlayerImage, Weight}
import com.itechart.project.dto.player.PlayerApiDto
import com.itechart.project.repository.{CountryRepository, PlayerRepository}
import com.itechart.project.service.CommonServiceMessages.ErrorWrapper
import com.itechart.project.service.CommonServiceMessages.Requests._
import com.itechart.project.service.CommonServiceMessages.Responses._
import com.itechart.project.service.domain_errors.PlayerErrors.PlayerError
import com.itechart.project.service.domain_errors.PlayerErrors.PlayerError._
import com.itechart.project.utils.RefinedConversions.validateParameter
import eu.timepit.refined.W
import eu.timepit.refined.numeric.GreaterEqual
import eu.timepit.refined.predicates.all.NonEmpty
import eu.timepit.refined.string.MatchesRegex

import java.sql.SQLIntegrityConstraintViolationException
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class PlayerService(
  playerRepository:  PlayerRepository,
  countryRepository: CountryRepository
)(
  implicit ec: ExecutionContext,
  timeout:     Timeout
) extends Actor
    with ActorLogging {
  import PlayerService._

  override def receive: Receive = {
    case GetAllEntities =>
      val senderToReturn = sender()
      log.info("Getting all players from database")
      val playersFuture = playerRepository.findAll
      playersFuture.onComplete {
        case Success(players) =>
          log.info(s"Got ${players.size} players out of database")
          senderToReturn ! AllFoundPlayers(players.map(domainPlayerToDtoPlayer))
        case Failure(ex) =>
          log.error(s"An error occurred while extracting all players out of database: $ex")
          senderToReturn ! InternalServerError
      }

    case GetEntityByT(id: Long) =>
      val senderToReturn = sender()
      log.info(s"Getting player with id = $id")
      val playerFuture = playerRepository.findById(PlayerId(id))
      playerFuture.onComplete {
        case Success(maybePlayer) =>
          log.info(s"Player with id = $id ${if (maybePlayer.isEmpty) "not "}found")
          senderToReturn ! OneFoundEntity(maybePlayer.map(domainPlayerToDtoPlayer))
        case Failure(ex) =>
          log.error(s"An error occurred while extracting a player with id = $id: $ex")
          senderToReturn ! InternalServerError
      }

    case GetEntityByT(lastName: String) =>
      val senderToReturn = sender()
      log.info(s"Getting players with last name = $lastName")
      val validatedNameEither =
        validateParameter[PlayerError, String, NonEmpty](lastName, InvalidPlayerLastName(lastName))
      validatedNameEither match {
        case Left(error) =>
          log.info(s"Validation of last name = $lastName failed")
          senderToReturn ! ValidationErrors(PlayerErrorWrapper(List(error)))
        case Right(validName) =>
          log.info(s"Extracting players with last name = $lastName out of database")
          val playersFuture = playerRepository.findByLastName(validName)
          playersFuture.onComplete {
            case Success(players) =>
              log.info(s"Got ${players.size} players with last name = $lastName out of database")
              senderToReturn ! AllFoundPlayers(players.map(domainPlayerToDtoPlayer))
            case Failure(ex) =>
              log.error(s"An error occurred while extracting players with last name = $lastName: $ex")
              senderToReturn ! InternalServerError
          }
      }

    case GetPlayersByCountry(countryId) =>
      val senderToReturn = sender()
      log.info(s"Getting players with countryId = $countryId")
      val playersFuture = playerRepository.findByCountry(CountryId(countryId))
      playersFuture.onComplete {
        case Success(players) =>
          log.info(s"Got ${players.size} players with countryId = $countryId out of database")
          senderToReturn ! AllFoundPlayers(players.map(domainPlayerToDtoPlayer))
        case Failure(ex) =>
          log.error(s"An error occurred while extracting players with countryId = $countryId: $ex")
          senderToReturn ! InternalServerError
      }

    case AddOneEntity(playerDto: PlayerApiDto) =>
      val senderToReturn = sender()
      log.info(s"Adding a player = $playerDto")
      val validatedPlayer = validatePlayerDto(playerDto)
      validatedPlayer match {
        case Left(errors) =>
          logErrorsAndSend(senderToReturn, playerDto, errors)
        case Right(player) =>
          val playerIdOrErrors = for {
            errors   <- validatePlayerDuplicates(player)
            playerId <- if (errors.isEmpty) playerRepository.create(player) else Future(PlayerId(0))
            result    = if (playerId.value == 0) Left(errors) else Right(playerId)
          } yield result
          playerIdOrErrors.onComplete {
            case Success(Right(id)) =>
              log.info(s"Player $player successfully created")
              senderToReturn ! OneEntityAdded(playerDto.copy(id = id.value, age = player.age.value))
            case Success(Left(errors)) =>
              log.info(s"Player $player doesn't created because of: ${errors.mkString("[", ", ", "]")}")
              senderToReturn ! ValidationErrors(PlayerErrorWrapper(errors))
            case Failure(ex) =>
              log.error(s"An error occurred while creating a player $player: $ex")
              senderToReturn ! InternalServerError
          }
      }

    case AddAllPlayers(playersListDto) =>
      val senderToReturn = sender()
      log.info(s"Adding players $playersListDto")
      val addedPlayers = Future.traverse(playersListDto.map(self ? AddOneEntity(_)))(identity)
      addedPlayers.onComplete {
        case Success(list) =>
          val players: List[PlayerApiDto] = list.flatMap {
            case OneEntityAdded(player: PlayerApiDto) => List(player)
            case _ => List()
          }
          val errors: List[PlayerError] = list.flatMap {
            case ValidationErrors(PlayerErrorWrapper(errors)) => errors
            case _                                            => List()
          }
          log.info(s"Players $players added successfully")
          log.info(s"Other players aren't added because of: ${errors.mkString("[", ", ", "]")}")
          senderToReturn ! AllPlayersAdded(players, errors)
        case Failure(ex) =>
          log.error(s"An error occurred while creating players $playersListDto: $ex")
          senderToReturn ! InternalServerError
      }

    case UpdateEntity(playerDto: PlayerApiDto) =>
      val senderToReturn = sender()
      log.info(s"Updating a player = $playerDto")
      val validatedPlayer = validatePlayerDto(playerDto)
      validatedPlayer match {
        case Left(errors) =>
          logErrorsAndSend(senderToReturn, playerDto, errors)
        case Right(player) =>
          val rowsUpdatedOrErrors = for {
            errors      <- validatePlayerDuplicates(player)
            rowsUpdated <- if (errors.isEmpty) playerRepository.update(player) else Future(-1)
            result       = if (rowsUpdated == -1) Left(errors) else Right(rowsUpdated)
          } yield result
          rowsUpdatedOrErrors.onComplete {
            case Success(Right(rowsUpdated)) =>
              log.info(s"Player $player is ${if (rowsUpdated == 0) "not " else ""}updated")
              val result = if (rowsUpdated == 0) UpdateFailed else UpdateCompleted
              senderToReturn ! result
            case Success(Left(errors)) =>
              log.info(s"Player $player isn't updated because of: ${errors.mkString("[", ", ", "]")}")
              senderToReturn ! ValidationErrors(PlayerErrorWrapper(errors))
            case Failure(ex) =>
              log.error(s"An error occurred while updating a player $player: $ex")
              senderToReturn ! InternalServerError
          }
      }

    case RemoveEntity(id: Long) =>
      val senderToReturn = sender()
      log.info(s"Deleting player with id = $id")
      val playerFuture = playerRepository.delete(PlayerId(id))
      playerFuture.onComplete {
        case Success(rowsDeleted) =>
          log.info(s"Player with id = $id ${if (rowsDeleted == 0) "not " else ""}removed")
          val result = if (rowsDeleted == 0) RemoveFailed else RemoveCompleted
          senderToReturn ! result
        case Failure(_: SQLIntegrityConstraintViolationException) =>
          log.info(s"A player with id = $id can't be deleted because it's a part of foreign key")
          senderToReturn ! ValidationErrors(PlayerErrorWrapper(List(PlayerForeignKey(id))))
        case Failure(ex) =>
          log.error(s"An error occurred while deleting a player with id = $id: $ex")
          senderToReturn ! InternalServerError
      }
  }

  private def logErrorsAndSend(sender: ActorRef, playerDto: PlayerApiDto, errors: List[PlayerError]): Unit = {
    log.info(s"Validation of player = $playerDto failed because of: ${errors.mkString("[", ", ", "]")}")
    sender ! ValidationErrors(PlayerErrorWrapper(errors))
  }

  private def validatePlayerDuplicates(player: Player): Future[List[PlayerError]] = for {
    maybeCountry <- countryRepository.findById(player.countryId)
    countryNotFoundError <- Future(
      if (maybeCountry.isEmpty) List(InvalidPlayerCountryId(player.countryId.value)) else List()
    )
  } yield countryNotFoundError

  private def validatePlayerDto(playerDto: PlayerApiDto): Either[List[PlayerError], Player] = {
    val validatedFirstNameEither =
      validateParameter[PlayerError, String, NonEmpty](playerDto.firstName, InvalidPlayerFirstName(playerDto.firstName))
    val validatedLastNameEither =
      validateParameter[PlayerError, String, NonEmpty](playerDto.lastName, InvalidPlayerLastName(playerDto.lastName))
    val age                   = ChronoUnit.YEARS.between(playerDto.birthday, LocalDate.now()).toInt
    val validatedAgeEither    = validateParameter[PlayerError, Int, GreaterEqual[16]](age, InvalidPlayerAge(age))
    val validatedWeightEither = validatePlayerWeight(playerDto.weight)
    val validatedHeightEither = validatePlayerHeight(playerDto.height)
    val validatedImageEither  = validatePlayerImage(playerDto.image)

    val firstNameErrorList =
      if (validatedFirstNameEither.isLeft) List(InvalidPlayerFirstName(playerDto.firstName)) else List()
    val lastNameErrorList =
      if (validatedLastNameEither.isLeft) List(InvalidPlayerLastName(playerDto.lastName)) else List()
    val ageErrorList    = if (validatedAgeEither.isLeft) List(InvalidPlayerAge(age)) else List()
    val weightErrorList = if (validatedWeightEither.isLeft) List(InvalidPlayerWeight(playerDto.weight.head)) else List()
    val heightErrorList = if (validatedHeightEither.isLeft) List(InvalidPlayerHeight(playerDto.height.head)) else List()
    val imageErrorList  = if (validatedImageEither.isLeft) List(InvalidPlayerImage(playerDto.image.head)) else List()
    val errorsList: List[PlayerError] =
      firstNameErrorList ++ lastNameErrorList ++ ageErrorList ++ weightErrorList ++ heightErrorList ++ imageErrorList

    val result = for {
      firstName <- validatedFirstNameEither
      lastName  <- validatedLastNameEither
      age       <- validatedAgeEither
      weight    <- validatedWeightEither
      height    <- validatedHeightEither
      image     <- validatedImageEither
    } yield Player(
      PlayerId(playerDto.id),
      firstName,
      lastName,
      playerDto.birthday,
      age,
      weight,
      height,
      image,
      CountryId(playerDto.countryId)
    )

    result.left.map(_ => errorsList)
  }

  private def validatePlayerWeight(weight: Option[Int]): Either[PlayerError, Option[Weight]] = {
    if (weight.isEmpty) Right(None)
    else
      validateParameter[PlayerError, Int, GreaterEqual[40]](weight.head, InvalidPlayerWeight(weight.head))
        .map(Option(_))
  }

  private def validatePlayerHeight(height: Option[Int]): Either[PlayerError, Option[Height]] = {
    if (height.isEmpty) Right(None)
    else
      validateParameter[PlayerError, Int, GreaterEqual[100]](height.head, InvalidPlayerHeight(height.head))
        .map(Option(_))
  }

  private def validatePlayerImage(image: Option[String]): Either[PlayerError, Option[PlayerImage]] = {
    if (image.isEmpty) Right(None)
    else
      validateParameter[PlayerError, String, MatchesRegex[W.`"^[0-9]+.(png|jpg|jpeg)$"`.T]](
        image.head,
        InvalidPlayerImage(image.head)
      )
        .map(Option(_))
  }

  private def domainPlayerToDtoPlayer(player: Player): PlayerApiDto =
    PlayerApiDto(
      player.id.value,
      player.firstName.value,
      player.lastName.value,
      player.birthday,
      player.age.value,
      player.weight.map(_.value),
      player.height.map(_.value),
      player.image.map(_.value),
      player.countryId.value
    )
}

object PlayerService {
  def props(
    playerRepository:  PlayerRepository,
    countryRepository: CountryRepository
  )(
    implicit ec: ExecutionContext,
    timeout:     Timeout
  ): Props =
    Props(new PlayerService(playerRepository, countryRepository))

  case class GetPlayersByCountry(countryId: Int)
  case class AddAllPlayers(playersListDto: List[PlayerApiDto])

  case class AllFoundPlayers(players: List[PlayerApiDto])
  case class PlayerErrorWrapper(override val errors: List[PlayerError]) extends ErrorWrapper
  case class AllPlayersAdded(players: List[PlayerApiDto], errors: List[PlayerError])
}
