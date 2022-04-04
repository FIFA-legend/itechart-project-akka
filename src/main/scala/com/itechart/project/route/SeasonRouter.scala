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
          val responseFuture = (seasonService ? GetSeasonById(id)).map {
            case FoundSeason(None) =>
              HttpResponse(status = StatusCodes.NotFound)
            case FoundSeason(Some(season)) =>
              Utils.responseBadRequestWithBody(season)
            case SeasonInternalServerError =>
              HttpResponse(status = StatusCodes.InternalServerError)
          }
          complete(responseFuture)
        } ~
          parameter("name") { name =>
            val responseFuture = (seasonService ? GetSeasonByName(name)).map {
              case FoundSeason(None) =>
                HttpResponse(status = StatusCodes.NotFound)
              case FoundSeason(Some(season)) =>
                Utils.responseOkWithBody(season)
              case SeasonValidationErrors(errors) =>
                Utils.responseBadRequestWithBody(errors.map(_.message))
              case SeasonInternalServerError =>
                HttpResponse(status = StatusCodes.InternalServerError)
            }
            complete(responseFuture)
          } ~
          pathEndOrSingleSlash {
            val responseFuture = (seasonService ? GetAllSeasons).map {
              case FoundSeasons(leagues) =>
                Utils.responseOkWithBody(leagues)
              case SeasonInternalServerError =>
                HttpResponse(status = StatusCodes.InternalServerError)
            }
            complete(responseFuture)
          }
      } ~
        post {
          path("all") {
            entity(as[List[SeasonApiDto]]) { seasonDtoList =>
              val responseFuture = (seasonService ? AddSeasons(seasonDtoList)).map {
                case SeasonsAdded(seasons, errors) =>
                  val map = Map(
                    "seasons" -> seasons.toJson.prettyPrint,
                    "errors"  -> errors.map(_.message).mkString("[", ", ", "]")
                  )
                  Utils.responseOkWithBody(map)
                case SeasonInternalServerError =>
                  HttpResponse(status = StatusCodes.InternalServerError)
              }
              complete(responseFuture)
            }
          } ~
            pathEndOrSingleSlash {
              entity(as[SeasonApiDto]) { seasonDto =>
                val responseFuture = (seasonService ? AddSeason(seasonDto)).map {
                  case SeasonAdded(season) =>
                    HttpResponse(status = StatusCodes.Created, entity = season.toJson.prettyPrint)
                  case SeasonValidationErrors(errors) =>
                    Utils.responseBadRequestWithBody(errors.map(_.message))
                  case SeasonInternalServerError =>
                    HttpResponse(status = StatusCodes.InternalServerError)
                }
                complete(responseFuture)
              }
            }
        } ~
        put {
          entity(as[SeasonApiDto]) { seasonDto =>
            val responseFuture = (seasonService ? UpdateSeason(seasonDto)).map {
              case SeasonNotUpdated =>
                HttpResponse(status = StatusCodes.NotFound)
              case SeasonUpdated =>
                Utils.responseOk()
              case SeasonValidationErrors(errors) =>
                Utils.responseBadRequestWithBody(errors.map(_.message))
              case SeasonInternalServerError =>
                HttpResponse(status = StatusCodes.InternalServerError)
            }
            complete(responseFuture)
          }
        } ~
        delete {
          path(IntNumber) { id =>
            val responseFuture = (seasonService ? RemoveSeason(id)).map {
              case SeasonNotDeleted =>
                Utils.responseBadRequest()
              case SeasonDeleted =>
                Utils.responseOk()
              case SeasonValidationErrors(errors) =>
                Utils.responseBadRequestWithBody(errors.map(_.message))
              case SeasonInternalServerError =>
                HttpResponse(status = StatusCodes.InternalServerError)
            }
            complete(responseFuture)
          }
        }
    }
  }

}
