package com.itechart.project.route

import akka.actor.ActorRef
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import akka.pattern.ask
import com.itechart.project.dto.JsonConverters.RefereeJsonProtocol
import com.itechart.project.dto.referee.RefereeApiDto
import com.itechart.project.service.CommonServiceMessages.Requests._
import com.itechart.project.service.CommonServiceMessages.Responses._
import spray.json._

import scala.concurrent.ExecutionContext

class RefereeRouter(refereeService: ActorRef)(implicit timeout: Timeout, ec: ExecutionContext)
  extends RefereeJsonProtocol
    with SprayJsonSupport {

  import com.itechart.project.service.RefereeService._

  val refereeRoutes: Route = {
    pathPrefix("api" / "referees") {
      get {
        (path(IntNumber) | parameter("id".as[Int])) { id =>
          val responseFuture = (refereeService ? GetEntityByT(id)).map {
            case OneFoundEntity(None) =>
              HttpResponse(status = StatusCodes.NotFound)
            case OneFoundEntity(Some(referee: RefereeApiDto)) =>
              Utils.responseBadRequestWithBody(referee)
            case InternalServerError =>
              HttpResponse(status = StatusCodes.InternalServerError)
          }
          complete(responseFuture)
        } ~
          parameter("country_id".as[Int]) { countryId =>
            val responseFuture = (refereeService ? GetRefereesByCountry(countryId)).map {
              case AllFoundReferees(referees) =>
                Utils.responseOkWithBody(referees)
              case InternalServerError =>
                HttpResponse(status = StatusCodes.InternalServerError)
            }
            complete(responseFuture)
          } ~
          pathEndOrSingleSlash {
            val responseFuture = (refereeService ? GetAllEntities).map {
              case AllFoundReferees(referees) =>
                Utils.responseOkWithBody(referees)
              case InternalServerError =>
                HttpResponse(status = StatusCodes.InternalServerError)
            }
            complete(responseFuture)
          }
      } ~
        post {
          path("all") {
            entity(as[List[RefereeApiDto]]) { refereeDtoList =>
              val responseFuture = (refereeService ? AddAllReferees(refereeDtoList)).map {
                case AllRefereesAdded(referees, errors) =>
                  val map = Map(
                    "referees" -> referees.toJson.prettyPrint,
                    "errors"   -> errors.map(_.message).mkString("[", ", ", "]")
                  )
                  Utils.responseOkWithBody(map)
                case InternalServerError =>
                  HttpResponse(status = StatusCodes.InternalServerError)
              }
              complete(responseFuture)
            }
          } ~
            pathEndOrSingleSlash {
              entity(as[RefereeApiDto]) { refereeDto =>
                val responseFuture = (refereeService ? AddOneEntity(refereeDto)).map {
                  case OneEntityAdded(referee: RefereeApiDto) =>
                    HttpResponse(status = StatusCodes.Created, entity = referee.toJson.prettyPrint)
                  case ValidationErrors(RefereeErrorWrapper(errors)) =>
                    Utils.responseBadRequestWithBody(errors.map(_.message))
                  case InternalServerError =>
                    HttpResponse(status = StatusCodes.InternalServerError)
                }
                complete(responseFuture)
              }
            }
        } ~
        put {
          entity(as[RefereeApiDto]) { refereeDto =>
            val responseFuture = (refereeService ? UpdateEntity(refereeDto)).map {
              case UpdateFailed =>
                HttpResponse(status = StatusCodes.NotFound)
              case UpdateCompleted =>
                Utils.responseOk()
              case ValidationErrors(RefereeErrorWrapper(errors)) =>
                Utils.responseBadRequestWithBody(errors.map(_.message))
              case InternalServerError =>
                HttpResponse(status = StatusCodes.InternalServerError)
            }
            complete(responseFuture)
          }
        } ~
        delete {
          path(IntNumber) { id =>
            val responseFuture = (refereeService ? RemoveEntity(id)).map {
              case RemoveFailed =>
                Utils.responseBadRequest()
              case RemoveCompleted =>
                Utils.responseOk()
              case ValidationErrors(RefereeErrorWrapper(errors)) =>
                Utils.responseBadRequestWithBody(errors.map(_.message))
              case InternalServerError =>
                HttpResponse(status = StatusCodes.InternalServerError)
            }
            complete(responseFuture)
          }
        }
    }
  }

}
