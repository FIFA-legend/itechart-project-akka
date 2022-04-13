package com.itechart.project.route

import akka.actor.ActorRef
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import akka.pattern.ask
import com.itechart.project.dto.JsonConverters.SeasonJsonProtocol
import com.itechart.project.dto.season.SeasonApiDto
import com.itechart.project.service.CommonServiceMessages.Requests._
import com.itechart.project.service.CommonServiceMessages.Responses._
import spray.json._

import scala.concurrent.ExecutionContext

class SeasonRouter(seasonService: ActorRef)(implicit timeout: Timeout, ec: ExecutionContext)
  extends SeasonJsonProtocol
    with SprayJsonSupport {

  import com.itechart.project.service.SeasonService._

  val seasonRoutes: Route = {
    pathPrefix("api" / "seasons") {
      get {
        (path(IntNumber) | parameter("id".as[Int])) { id =>
          val responseFuture = (seasonService ? GetEntityByT(id)).map {
            case OneFoundEntity(None) =>
              HttpResponse(status = StatusCodes.NotFound)
            case OneFoundEntity(Some(season: SeasonApiDto)) =>
              Utils.responseBadRequestWithBody(season)
            case InternalServerError =>
              HttpResponse(status = StatusCodes.InternalServerError)
          }
          complete(responseFuture)
        } ~
          parameter("name") { name =>
            val responseFuture = (seasonService ? GetEntityByT(name)).map {
              case OneFoundEntity(None) =>
                HttpResponse(status = StatusCodes.NotFound)
              case OneFoundEntity(Some(season: SeasonApiDto)) =>
                Utils.responseOkWithBody(season)
              case ValidationErrors(SeasonErrorWrapper(errors)) =>
                Utils.responseBadRequestWithBody(errors.map(_.message))
              case InternalServerError =>
                HttpResponse(status = StatusCodes.InternalServerError)
            }
            complete(responseFuture)
          } ~
          pathEndOrSingleSlash {
            val responseFuture = (seasonService ? GetAllEntities).map {
              case AllFoundSeasons(leagues) =>
                Utils.responseOkWithBody(leagues)
              case InternalServerError =>
                HttpResponse(status = StatusCodes.InternalServerError)
            }
            complete(responseFuture)
          }
      } ~
        post {
          path("all") {
            entity(as[List[SeasonApiDto]]) { seasonDtoList =>
              val responseFuture = (seasonService ? AddAllSeasons(seasonDtoList)).map {
                case AllSeasonsAdded(seasons, errors) =>
                  val map = Map(
                    "seasons" -> seasons.toJson.prettyPrint,
                    "errors"  -> errors.map(_.message).mkString("[", ", ", "]")
                  )
                  Utils.responseOkWithBody(map)
                case InternalServerError =>
                  HttpResponse(status = StatusCodes.InternalServerError)
              }
              complete(responseFuture)
            }
          } ~
            pathEndOrSingleSlash {
              entity(as[SeasonApiDto]) { seasonDto =>
                val responseFuture = (seasonService ? AddOneEntity(seasonDto)).map {
                  case OneEntityAdded(season: SeasonApiDto) =>
                    HttpResponse(status = StatusCodes.Created, entity = season.toJson.prettyPrint)
                  case ValidationErrors(SeasonErrorWrapper(errors)) =>
                    Utils.responseBadRequestWithBody(errors.map(_.message))
                  case InternalServerError =>
                    HttpResponse(status = StatusCodes.InternalServerError)
                }
                complete(responseFuture)
              }
            }
        } ~
        put {
          entity(as[SeasonApiDto]) { seasonDto =>
            complete(Utils.responseOnEntityUpdate(seasonService, seasonDto))
          }
        } ~
        delete {
          path(IntNumber) { id =>
            complete(Utils.responseOnEntityDelete(seasonService, id))
          }
        }
    }
  }

}
