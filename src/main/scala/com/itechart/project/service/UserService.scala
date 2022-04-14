package com.itechart.project.service

import akka.actor.{Actor, ActorLogging, Props}
import akka.util.Timeout
import com.itechart.project.domain.user.{Email, Login, PasswordHash, Role, User, UserId}
import com.itechart.project.dto.user.{CreateUserApiDto, UpdateUserApiDto, UserApiDto}
import com.itechart.project.repository.UserRepository
import com.itechart.project.service.CommonServiceMessages.ErrorWrapper
import com.itechart.project.service.CommonServiceMessages.Requests._
import com.itechart.project.service.CommonServiceMessages.Responses._
import com.itechart.project.service.domain_errors.UserErrors.UserError
import com.itechart.project.service.domain_errors.UserErrors.UserError._
import com.itechart.project.utils.RefinedConversions.validateParameter
import eu.timepit.refined.W
import eu.timepit.refined.string.MatchesRegex

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.sql.SQLIntegrityConstraintViolationException
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class UserService(userRepository: UserRepository)(implicit ec: ExecutionContext, timeout: Timeout)
  extends Actor
    with ActorLogging {
  import UserService._

  override def receive: Receive = {
    case GetAllEntities =>
      val senderToReturn = sender()
      log.info("Getting all users from database")
      val usersFuture = userRepository.findAll
      usersFuture.onComplete {
        case Success(users) =>
          log.info(s"Got ${users.size} users out of database")
          senderToReturn ! AllFoundUsers(users.map(domainUserToDtoUser))
        case Failure(ex) =>
          log.error(s"An error occurred while extracting all users out of database: $ex")
          senderToReturn ! InternalServerError
      }

    case GetEntityByT(id: Long) =>
      val senderToReturn = sender()
      log.info(s"Getting user with id = $id")
      val userFuture = userRepository.findById(UserId(id))
      userFuture.onComplete {
        case Success(maybeUser) =>
          log.info(s"User with id = $id ${if (maybeUser.isEmpty) "not " else ""}found")
          senderToReturn ! OneFoundEntity(maybeUser.map(domainUserToDtoUser))
        case Failure(ex) =>
          log.error(s"An error occurred while extracting an user with id = $id: $ex")
          senderToReturn ! InternalServerError
      }

    case GetEntityByT(login: String) =>
      val senderToReturn = sender()
      log.info(s"Getting user with login = $login")
      val userFuture = userRepository.findByLogin(Login(login))
      userFuture.onComplete {
        case Success(maybeUser) =>
          log.info(s"User with login = $login ${if (maybeUser.isEmpty) "not " else ""}found")
          senderToReturn ! OneFoundEntity(maybeUser.map(domainUserToDtoUser))
        case Failure(ex) =>
          log.error(s"An error occurred while extracting an user with login = $login: $ex")
          senderToReturn ! InternalServerError
      }

    case AddOneEntity(dto: CreateUserApiDto) =>
      val senderToReturn = sender()
      log.info(s"Adding an user = $dto")
      val validatedUser = validateUserDtoOnCreate(dto)
      validatedUser match {
        case Left(errors) =>
          log.info(s"Validation of user = $dto on create failed because of: ${errors.mkString("[", ", ", "]")}")
          senderToReturn ! ValidationErrors(UserErrorWrapper(errors))
        case Right(user) =>
          val userIdOrErrors = for {
            errors <- validateUserDuplicatesOnCreate(user)
            userId <- if (errors.isEmpty) userRepository.create(user) else Future(UserId(0))
            result  = if (userId.value == 0) Left(errors) else Right(userId)
          } yield result
          userIdOrErrors.onComplete {
            case Success(Right(id)) =>
              log.info(s"User $user successfully created")
              senderToReturn ! OneEntityAdded(
                UserApiDto(id.value, user.login.value, user.email.value, user.role.toString)
              )
            case Success(Left(errors)) =>
              log.info(s"User $user doesn't created because of: ${errors.mkString("[", ", ", "]")}")
              senderToReturn ! ValidationErrors(UserErrorWrapper(errors))
            case Failure(ex) =>
              log.error(s"An error occurred while creating an user $user: $ex")
              senderToReturn ! InternalServerError
          }
      }

    case UpdateEntity((username: String, dto: UpdateUserApiDto)) =>
      val senderToReturn = sender()
      val result = for {
        updatingUser <- userRepository.findByLogin(Login(username))
        user         <- userRepository.findById(UserId(dto.id))
      } yield (updatingUser, user)
      result.onComplete {
        case Failure(ex) =>
          log.error(s"An error occurred while updating an user with id = ${dto.id}: $ex")
          senderToReturn ! InternalServerError
        case Success((_, None)) | Success((None, _)) =>
          log.info(s"User with login = $username and user = $dto are not found so update is stopped")
          senderToReturn ! NotEnoughRights
        case Success((Some(updatingUser), Some(realUser))) =>
          if (updatingUser.role != Role.Admin && updatingUser.id.value != dto.id) {
            log.info(s"User with login = $username has no rights to update other user")
            senderToReturn ! NotEnoughRights
          } else {
            val validatedLeague = validateUserDtoOnUpdate(realUser.login, dto)
            validatedLeague match {
              case Left(errors) =>
                log.info(s"Validation of user = $dto on update failed because of: ${errors.mkString("[", ", ", "]")}")
                sender() ! ValidationErrors(UserErrorWrapper(errors))
              case Right(user) =>
                val rowsUpdated = userRepository.update(user)
                rowsUpdated.onComplete {
                  case Success(rowsUpdated) =>
                    log.info(s"User $user is ${if (rowsUpdated == 0) "not " else ""}updated")
                    val result = if (rowsUpdated == 0) UpdateFailed else UpdateCompleted
                    senderToReturn ! result
                  case Failure(ex) =>
                    log.error(s"An error occurred while updating a user $user: $ex")
                    senderToReturn ! InternalServerError
                }
            }
          }
      }

    case RemoveEntity((username: String, id: Long)) =>
      val senderToReturn = sender()
      val userFuture     = userRepository.findByLogin(Login(username))
      userFuture.onComplete {
        case Failure(ex) =>
          log.error(s"An error occurred while deleting an user with id = $id: $ex")
          senderToReturn ! InternalServerError
        case Success(None) =>
          log.info(s"User with login = $username not found so deletion is stopped")
          senderToReturn ! NotEnoughRights
        case Success(Some(user)) =>
          if (user.role != Role.Admin && user.id.value != id) {
            log.info(s"User with login = $username has no rights to delete other user")
            senderToReturn ! NotEnoughRights
          } else {
            log.info(s"Deleting user with id = $id")
            val deleteFuture = userRepository.delete(UserId(id))
            deleteFuture.onComplete {
              case Success(rowsDeleted) =>
                log.info(s"User with id = $id ${if (rowsDeleted == 0) "not " else ""}removed")
                val result = if (rowsDeleted == 0) RemoveFailed else RemoveCompleted
                senderToReturn ! result
              case Failure(_: SQLIntegrityConstraintViolationException) =>
                log.info(s"An user with id = $id can't be deleted because it's a part of foreign key")
                senderToReturn ! ValidationErrors(UserErrorWrapper(List(UserForeignKey(id))))
              case Failure(ex) =>
                log.error(s"An error occurred while deleting an user with id = $id: $ex")
                senderToReturn ! InternalServerError
            }
          }
      }
  }

  private def validateUserDuplicatesOnCreate(user: User): Future[List[UserError]] = for {
    maybeUserByLogin <- userRepository.findByLogin(user.login)
    duplicatedNameError <- Future(
      if (maybeUserByLogin.isEmpty) List() else List(DuplicateUserLogin(user.login.value))
    )
  } yield duplicatedNameError

  private def validateUserDtoOnCreate(userDto: CreateUserApiDto): Either[List[UserError], User] = {
    val validatedLoginEither =
      if (userDto.login.length >= 6 && userDto.login.length <= 32) Right(Login(userDto.login))
      else Left(InvalidUserLogin(userDto.login))
    val validatedPasswordEither = validatePassword(userDto.password)
    val validatedEmailEither    = validateEmail(userDto.email)

    val loginErrorList    = if (validatedLoginEither.isLeft) List(InvalidUserLogin(userDto.login)) else List()
    val passwordErrorList = if (validatedPasswordEither.isLeft) List(InvalidUserPassword(userDto.password)) else List()
    val emailErrorList    = if (validatedEmailEither.isLeft) List(InvalidUserEmail(userDto.email)) else List()
    val errorsList: List[UserError] = loginErrorList ++ passwordErrorList ++ emailErrorList

    val result = for {
      login    <- validatedLoginEither
      password <- validatedPasswordEither
      email    <- validatedEmailEither
    } yield User(UserId(0L), login, PasswordHash(password), email, Role.User)

    result.left.map(_ => errorsList)
  }

  private def validateUserDtoOnUpdate(login: Login, userDto: UpdateUserApiDto): Either[List[UserError], User] = {
    val validatedPasswordEither = validatePassword(userDto.password)
    val validatedEmailEither    = validateEmail(userDto.email)
    val validatedRoleEither     = Role.withNameEither(userDto.role)

    val emailErrorList    = if (validatedEmailEither.isLeft) List(InvalidUserEmail(userDto.email)) else List()
    val passwordErrorList = if (validatedPasswordEither.isLeft) List(InvalidUserPassword(userDto.password)) else List()
    val roleErrorList     = if (validatedRoleEither.isLeft) List(InvalidUserRole(userDto.role)) else List()
    val errorsList: List[UserError] = roleErrorList ++ passwordErrorList ++ emailErrorList

    val result = for {
      password <- validatedPasswordEither
      email    <- validatedEmailEither
      role     <- validatedRoleEither
    } yield User(UserId(userDto.id), login, PasswordHash(password), email, role)

    result.left.map(_ => errorsList)
  }

  private def validatePassword(password: String): Either[UserError, String] =
    if (password.length > 8)
      Right(
        new String(
          MessageDigest.getInstance("SHA-256").digest(password.getBytes(StandardCharsets.UTF_8)),
          StandardCharsets.UTF_8
        )
      )
    else Left(InvalidUserPassword(password))

  private def validateEmail(email: String): Either[UserError, Email] =
    validateParameter[UserError, String, MatchesRegex[W.`"^[A-Za-z0-9_]+@[A-Za-z0-9]+.[A-Za-z0-9]+$"`.T]](
      email,
      InvalidUserEmail(email)
    )

  private def domainUserToDtoUser(user: User): UserApiDto =
    UserApiDto(user.id.value, user.login.value, user.email.value, user.role.toString)
}

object UserService {
  def props(userRepository: UserRepository)(implicit ec: ExecutionContext, timeout: Timeout): Props =
    Props(new UserService(userRepository))

  case class AllFoundUsers(users: List[UserApiDto])
  case class UserErrorWrapper(override val errors: List[UserError]) extends ErrorWrapper
}
