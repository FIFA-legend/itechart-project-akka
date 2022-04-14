package com.itechart.project.route

import akka.actor.ActorRef
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import com.itechart.project.dto.JsonConverters.UserJsonProtocol
import com.itechart.project.dto.user.{CreateUserApiDto, UpdateUserApiDto, UserApiDto}
import com.itechart.project.service.CommonServiceMessages.Requests._
import com.itechart.project.service.CommonServiceMessages.Responses._
import com.itechart.project.service.JwtAuthorizationService._
import spray.json._

import scala.concurrent.{ExecutionContext, Future}

class UserRouter(
  userService:             ActorRef,
  jwtAuthorizationService: ActorRef
)(
  implicit timeout: Timeout,
  ec:               ExecutionContext
) extends UserJsonProtocol
    with SprayJsonSupport {

  import com.itechart.project.service.UserService._

  val userRoutes: Route = {
    pathPrefix("api" / "users") {
      get {
        (path(LongNumber) | parameter("id".as[Long])) { id =>
          complete(findUserByParameter(id))
        } ~
          parameter("login".as[String]) { login =>
            complete(findUserByParameter(login))
          } ~
          pathEndOrSingleSlash {
            val responseFuture = (userService ? GetAllEntities).map {
              case AllFoundUsers(users) =>
                Utils.responseOkWithBody(users)
              case InternalServerError =>
                HttpResponse(status = StatusCodes.InternalServerError)
            }
            complete(responseFuture)
          }
      } ~
        post {
          pathEndOrSingleSlash {
            entity(as[CreateUserApiDto]) { userDto =>
              val httpResponse = (userService ? AddOneEntity(userDto)).map {
                case OneEntityAdded(user: UserApiDto) =>
                  HttpResponse(status = StatusCodes.Created, entity = user.toJson.prettyPrint)
                case ValidationErrors(UserErrorWrapper(errors)) =>
                  Utils.responseBadRequestWithBody(errors.map(_.message))
                case InternalServerError =>
                  HttpResponse(status = StatusCodes.InternalServerError)
              }
              complete(httpResponse)
            }
          }
        } ~
        put {
          (optionalHeaderValueByName("Authorization") & entity(as[UpdateUserApiDto])) {
            case (None, _) => complete(HttpResponse(status = StatusCodes.Unauthorized, entity = "No token provided!"))
            case (Some(token), userDto) =>
              val httpResponse = for {
                tokenValidationStatus <- (jwtAuthorizationService ? ValidateToken(token)).mapTo[TokenValidationStatus]
                tokenExpirationStatus <- (jwtAuthorizationService ? TokenExpiration(token))
                  .mapTo[TokenExpirationStatus]
                result <-
                  if (tokenExpirationStatus == TokenNotExpired || tokenValidationStatus == InvalidToken) {
                    Future(HttpResponse(status = StatusCodes.Unauthorized, entity = "Invalid or expired token!"))
                  } else {
                    (userService ? UpdateEntity(userDto)).map {
                      case NotEnoughRights           => HttpResponse(status = StatusCodes.Forbidden)
                      case UpdateFailed              => HttpResponse(status = StatusCodes.NotFound)
                      case UpdateCompleted           => Utils.responseOk()
                      case ValidationErrors(wrapper) => Utils.responseBadRequestWithBody(wrapper.errors.map(_.message))
                      case InternalServerError       => HttpResponse(status = StatusCodes.InternalServerError)
                    }
                  }
              } yield result
              complete(httpResponse)
          }
        } ~
        delete {
          (optionalHeaderValueByName("Authorization") & path(LongNumber)) {
            case (None, _) => complete(HttpResponse(status = StatusCodes.Unauthorized, entity = "No token provided!"))
            case (Some(token), id) =>
              val httpResponse = for {
                tokenValidationStatus <- (jwtAuthorizationService ? ValidateToken(token)).mapTo[TokenValidationStatus]
                tokenExpirationStatus <- (jwtAuthorizationService ? TokenExpiration(token))
                  .mapTo[TokenExpirationStatus]
                result <-
                  if (tokenExpirationStatus == TokenNotExpired || tokenValidationStatus == InvalidToken) {
                    Future(HttpResponse(status = StatusCodes.Unauthorized, entity = "Invalid or expired token!"))
                  } else {
                    (userService ? RemoveEntity(id)).map {
                      case NotEnoughRights           => HttpResponse(status = StatusCodes.Forbidden)
                      case RemoveFailed              => Utils.responseBadRequest()
                      case RemoveCompleted           => Utils.responseOk()
                      case ValidationErrors(wrapper) => Utils.responseBadRequestWithBody(wrapper.errors.map(_.message))
                      case InternalServerError       => HttpResponse(status = StatusCodes.InternalServerError)
                    }
                  }
              } yield result
              complete(httpResponse)
          }
        }
    }
  }

  private def findUserByParameter[T](parameter: T): Future[HttpResponse] =
    (userService ? GetEntityByT(parameter)).map {
      case OneFoundEntity(None) =>
        HttpResponse(status = StatusCodes.NotFound)
      case OneFoundEntity(Some(user: UserApiDto)) =>
        Utils.responseBadRequestWithBody(user)
      case InternalServerError =>
        HttpResponse(status = StatusCodes.InternalServerError)
    }

}
