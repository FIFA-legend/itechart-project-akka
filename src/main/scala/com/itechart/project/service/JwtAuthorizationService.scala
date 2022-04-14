package com.itechart.project.service

import akka.actor.{Actor, ActorLogging, Props}
import com.itechart.project.domain.user.{Login, Role, User}
import com.itechart.project.repository.UserRepository
import com.itechart.project.service.JwtAuthorizationService.JwtConfiguration
import io.circe.generic.JsonCodec
import pdi.jwt.algorithms.JwtHmacAlgorithm
import pdi.jwt.{JwtAlgorithm, JwtClaim, JwtSprayJson}

import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class JwtAuthorizationService(
  userRepository:   UserRepository,
  jwtConfiguration: JwtConfiguration
)(
  implicit ec: ExecutionContext
) extends Actor
    with ActorLogging {
  import JwtAuthorizationService._

  val algorithm: JwtHmacAlgorithm = JwtAlgorithm.HS256

  override def receive: Receive = {
    case LoginRequest(username, password) =>
      val senderToReturn = sender()
      val userFuture     = userRepository.findByLogin(Login(username))
      userFuture.onComplete {
        case Success(None) =>
          log.info(s"User with username = $username not found")
          senderToReturn ! LoginNotFound
        case Success(Some(user)) =>
          if (!checkPassword(user, password)) {
            log.info(s"Password mismatch with user = $username")
            senderToReturn ! PasswordMismatch
          } else {
            log.info(s"Token for user = $username successfully created")
            senderToReturn ! LoginToken(createToken(user))
          }
        case Failure(ex) =>
          log.error(s"An error occurred while creating jwt token for user = $username: $ex")
      }

    case ValidateToken(token) =>
      if (isTokenValid(token)) {
        log.info(s"Token $token is valid")
        sender() ! ValidToken
      } else {
        log.info(s"Token $token is invalid")
        sender() ! InvalidToken
      }

    case TokenExpiration(token) =>
      if (isTokenExpired(token)) {
        log.info(s"Token $token has expired")
        sender() ! TokenExpired
      } else {
        log.info(s"Token $token has not expired")
        sender() ! TokenNotExpired
      }

    case TokenRole(token) =>
      sender() ! TokenRoleResult(getRole(token))
  }

  private def checkPassword(user: User, password: String): Boolean = {
    val encryptedUserPassword  = user.passwordHash.value.getBytes
    val encryptedInputPassword = MessageDigest.getInstance("SHA-256").digest(password.getBytes)
    encryptedUserPassword == encryptedInputPassword
  }

  private def createToken(user: User, expirationPeriodInDays: Int = 1): String = {
    val claims = JwtClaim(
      content    = s"${user.login}: ${user.role}",
      expiration = Some(System.currentTimeMillis() / 1000 + TimeUnit.DAYS.toSeconds(expirationPeriodInDays)),
      issuedAt   = Some(System.currentTimeMillis() / 1000),
      issuer     = Some("football_stats.com")
    )

    JwtSprayJson.encode(claims, jwtConfiguration.secret, algorithm)
  }

  private def isTokenExpired(token: String): Boolean =
    JwtSprayJson.decode(token, jwtConfiguration.secret, Seq(algorithm)) match {
      case Success(claims) => claims.expiration.getOrElse(0L) < System.currentTimeMillis() / 1000
      case Failure(_)      => true
    }

  private def isTokenValid(token: String): Boolean =
    JwtSprayJson.isValid(token, jwtConfiguration.secret, Seq(algorithm))

  private def getRole(token: String): Role =
    JwtSprayJson.decode(token, jwtConfiguration.secret, Seq(algorithm)) match {
      case Success(claims) => Role.withName(claims.content.split(":")(1).strip())
      case Failure(_)      => Role.User
    }
}

object JwtAuthorizationService {
  def props(userRepository: UserRepository, jwtConfiguration: JwtConfiguration)(implicit ec: ExecutionContext): Props =
    Props(new JwtAuthorizationService(userRepository, jwtConfiguration))

  case class LoginRequest(username: String, password: String)
  case class LoginToken(token: String)
  case object LoginNotFound
  case object PasswordMismatch

  case class ValidateToken(token: String)
  case object ValidToken
  case object InvalidToken

  case class TokenExpiration(token: String)
  case object TokenExpired
  case object TokenNotExpired

  case class TokenRole(token: String)
  case class TokenRoleResult(role: Role)

  @JsonCodec
  case class JwtConfiguration(secret: String)
}
