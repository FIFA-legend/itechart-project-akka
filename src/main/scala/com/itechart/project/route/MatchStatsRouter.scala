package com.itechart.project.route

import akka.actor.ActorRef
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import akka.pattern.ask
import com.itechart.project.dto.JsonConverters.MatchStatsJsonProtocol
import com.itechart.project.dto.match_stats.MatchStatsApiDto
import com.itechart.project.service.CommonServiceMessages.Requests._
import com.itechart.project.service.CommonServiceMessages.Responses._
import spray.json._

import scala.concurrent.ExecutionContext

class MatchStatsRouter(matchStatsService: ActorRef)(implicit timeout: Timeout, ec: ExecutionContext)
  extends MatchStatsJsonProtocol
    with SprayJsonSupport {

  import com.itechart.project.service.MatchStatsService._

  val matchStatsRoutes: Route = {
    pathPrefix("api" / "match" / "stats") {
      get {
        (path(LongNumber) | parameter("id".as[Long])) { id =>
          val responseFuture = (matchStatsService ? GetEntityByT(id)).map {
            case OneFoundEntity(None) =>
              HttpResponse(status = StatusCodes.NotFound)
            case OneFoundEntity(Some(matchStats: MatchStatsApiDto)) =>
              Utils.responseBadRequestWithBody(matchStats)
            case InternalServerError =>
              HttpResponse(status = StatusCodes.InternalServerError)
          }
          complete(responseFuture)
        }
      } ~
        post {
          pathEndOrSingleSlash {
            entity(as[MatchStatsApiDto]) { matchStatsDto =>
              val responseFuture = (matchStatsService ? AddOneEntity(matchStatsDto)).map {
                case OneEntityAdded(matchStats: MatchStatsApiDto) =>
                  HttpResponse(status = StatusCodes.Created, entity = matchStats.toJson.prettyPrint)
                case MatchStatsValidationErrors(errors) =>
                  Utils.responseBadRequestWithBody(errors.map(_.message))
                case InternalServerError =>
                  HttpResponse(status = StatusCodes.InternalServerError)
              }
              complete(responseFuture)
            }
          }
        } ~
        put {
          entity(as[MatchStatsApiDto]) { matchStatsDto =>
            val responseFuture = (matchStatsService ? UpdateEntity(matchStatsDto)).map {
              case UpdateFailed =>
                HttpResponse(status = StatusCodes.NotFound)
              case UpdateCompleted =>
                Utils.responseOk()
              case MatchStatsValidationErrors(errors) =>
                Utils.responseBadRequestWithBody(errors.map(_.message))
              case InternalServerError =>
                HttpResponse(status = StatusCodes.InternalServerError)
            }
            complete(responseFuture)
          }
        } ~
        delete {
          path(IntNumber) { id =>
            val responseFuture = (matchStatsService ? RemoveEntity(id)).map {
              case RemoveFailed =>
                Utils.responseBadRequest()
              case RemoveCompleted =>
                Utils.responseOk()
              case MatchStatsValidationErrors(errors) =>
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
