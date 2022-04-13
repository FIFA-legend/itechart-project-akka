package com.itechart.project.route

import akka.actor.ActorRef
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import akka.pattern.ask
import com.itechart.project.dto.JsonConverters.MatchJsonProtocol
import com.itechart.project.dto.football_match.MatchApiDto
import com.itechart.project.service.CommonServiceMessages.Requests._
import com.itechart.project.service.CommonServiceMessages.Responses._
import spray.json._

import java.time.LocalDate
import scala.concurrent.ExecutionContext

class MatchRouter(matchService: ActorRef)(implicit timeout: Timeout, ec: ExecutionContext)
  extends MatchJsonProtocol
    with SprayJsonSupport {

  import com.itechart.project.service.MatchService._

  val matchRoutes: Route = {
    pathPrefix("api" / "matches") {
      get {
        (path(LongNumber) | parameter("id".as[Long])) { id =>
          val responseFuture = (matchService ? GetEntityByT(id)).map {
            case OneFoundEntity(None) =>
              HttpResponse(status = StatusCodes.NotFound)
            case OneFoundEntity(Some(footballMatch: MatchApiDto)) =>
              Utils.responseBadRequestWithBody(footballMatch)
            case InternalServerError =>
              HttpResponse(status = StatusCodes.InternalServerError)
          }
          complete(responseFuture)
        } ~
          parameter("date".as[String]) { date =>
            val localDate = LocalDate.parse(date)
            val responseFuture = (matchService ? GetEntityByT(localDate)).map {
              case AllFoundMatches(leagues) =>
                Utils.responseOkWithBody(leagues)
              case InternalServerError =>
                HttpResponse(status = StatusCodes.InternalServerError)
            }
            complete(responseFuture)
          }
      } ~
        post {
          pathEndOrSingleSlash {
            entity(as[MatchApiDto]) { matchApi =>
              val responseFuture = (matchService ? AddOneEntity(matchApi)).map {
                case OneEntityAdded(footballMatch: MatchApiDto) =>
                  HttpResponse(status = StatusCodes.Created, entity = footballMatch.toJson.prettyPrint)
                case ValidationErrors(MatchErrorWrapper(errors)) =>
                  Utils.responseBadRequestWithBody(errors.map(_.message))
                case InternalServerError =>
                  HttpResponse(status = StatusCodes.InternalServerError)
              }
              complete(responseFuture)
            }
          }
        } ~
        put {
          entity(as[MatchApiDto]) { matchDto =>
            complete(Utils.responseOnEntityUpdate(matchService, matchDto))
          }
        } ~
        delete {
          path(LongNumber) { id =>
            complete(Utils.responseOnEntityDelete(matchService, id))
          }
        }
    }
  }

}
