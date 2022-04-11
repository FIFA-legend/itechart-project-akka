package com.itechart.project.route

import akka.actor.ActorRef
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import akka.pattern.ask
import com.itechart.project.dto.JsonConverters.PlayerJsonProtocol
import com.itechart.project.dto.player.PlayerApiDto
import com.itechart.project.service.CommonServiceMessages.Requests._
import com.itechart.project.service.CommonServiceMessages.Responses._
import spray.json._

import scala.concurrent.ExecutionContext

class PlayerRouter(playerService: ActorRef)(implicit timeout: Timeout, ec: ExecutionContext)
  extends PlayerJsonProtocol
    with SprayJsonSupport {

  import com.itechart.project.service.PlayerService._

  val playerRoutes: Route = {
    pathPrefix("api" / "players") {
      get {
        (path(LongNumber) | parameter("id".as[Long])) { id =>
          val responseFuture = (playerService ? GetEntityByT(id)).map {
            case OneFoundEntity(None) =>
              HttpResponse(status = StatusCodes.NotFound)
            case OneFoundEntity(Some(player: PlayerApiDto)) =>
              Utils.responseBadRequestWithBody(player)
            case InternalServerError =>
              HttpResponse(status = StatusCodes.InternalServerError)
          }
          complete(responseFuture)
        } ~
          parameter("last_name") { name =>
            val responseFuture = (playerService ? GetEntityByT(name)).map {
              case AllFoundPlayers(players) =>
                Utils.responseOkWithBody(players)
              case PlayerValidationErrors(errors) =>
                Utils.responseBadRequestWithBody(errors.map(_.message))
              case InternalServerError =>
                HttpResponse(status = StatusCodes.InternalServerError)
            }
            complete(responseFuture)
          } ~
          parameter("country_id".as[Int]) { countryId =>
            val responseFuture = (playerService ? GetPlayersByCountry(countryId)).map {
              case AllFoundPlayers(players) =>
                Utils.responseOkWithBody(players)
              case InternalServerError =>
                HttpResponse(status = StatusCodes.InternalServerError)
            }
            complete(responseFuture)
          } ~
          pathEndOrSingleSlash {
            val responseFuture = (playerService ? GetAllEntities).map {
              case AllFoundPlayers(players) =>
                Utils.responseOkWithBody(players)
              case InternalServerError =>
                HttpResponse(status = StatusCodes.InternalServerError)
            }
            complete(responseFuture)
          }
      } ~
        post {
          path("all") {
            entity(as[List[PlayerApiDto]]) { playerDtoList =>
              val responseFuture = (playerService ? AddAllPlayers(playerDtoList)).map {
                case AllPlayersAdded(players, errors) =>
                  val map = Map(
                    "players" -> players.toJson.prettyPrint,
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
              entity(as[PlayerApiDto]) { playerDto =>
                val responseFuture = (playerService ? AddOneEntity(playerDto)).map {
                  case OneEntityAdded(player: PlayerApiDto) =>
                    HttpResponse(status = StatusCodes.Created, entity = player.toJson.prettyPrint)
                  case PlayerValidationErrors(errors) =>
                    Utils.responseBadRequestWithBody(errors.map(_.message))
                  case InternalServerError =>
                    HttpResponse(status = StatusCodes.InternalServerError)
                }
                complete(responseFuture)
              }
            }
        } ~
        put {
          entity(as[PlayerApiDto]) { playerDto =>
            val responseFuture = (playerService ? UpdateEntity(playerDto)).map {
              case UpdateFailed =>
                HttpResponse(status = StatusCodes.NotFound)
              case UpdateCompleted =>
                Utils.responseOk()
              case PlayerValidationErrors(errors) =>
                Utils.responseBadRequestWithBody(errors.map(_.message))
              case InternalServerError =>
                HttpResponse(status = StatusCodes.InternalServerError)
            }
            complete(responseFuture)
          }
        } ~
        delete {
          path(IntNumber) { id =>
            val responseFuture = (playerService ? RemoveEntity(id)).map {
              case RemoveFailed =>
                Utils.responseBadRequest()
              case RemoveCompleted =>
                Utils.responseOk()
              case PlayerValidationErrors(errors) =>
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
