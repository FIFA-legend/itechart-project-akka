package com.itechart.project.route

import akka.actor.ActorRef
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import com.itechart.project.dto.JsonConverters.LeagueJsonProtocol
import com.itechart.project.dto.league.LeagueApiDto
import com.itechart.project.service.CommonServiceMessages.Requests._
import com.itechart.project.service.CommonServiceMessages.Responses._
import spray.json._

import scala.concurrent.ExecutionContext

class LeagueRouter(leagueService: ActorRef)(implicit timeout: Timeout, ec: ExecutionContext)
  extends LeagueJsonProtocol
    with SprayJsonSupport {

  import com.itechart.project.service.LeagueService._

  val leagueRoutes: Route = {
    pathPrefix("api" / "leagues") {
      get {
        (path(IntNumber) | parameter("id".as[Int])) { id =>
          val responseFuture = (leagueService ? GetEntityByT(id)).map {
            case OneFoundEntity(None) =>
              HttpResponse(status = StatusCodes.NotFound)
            case OneFoundEntity(Some(league: LeagueApiDto)) =>
              Utils.responseBadRequestWithBody(league)
            case InternalServerError =>
              HttpResponse(status = StatusCodes.InternalServerError)
          }
          complete(responseFuture)
        } ~
          parameter("name") { name =>
            val responseFuture = (leagueService ? GetEntityByT(name)).map {
              case OneFoundEntity(None) =>
                HttpResponse(status = StatusCodes.NotFound)
              case OneFoundEntity(Some(league: LeagueApiDto)) =>
                Utils.responseOkWithBody(league)
              case LeagueValidationErrors(errors) =>
                Utils.responseBadRequestWithBody(errors.map(_.message))
              case InternalServerError =>
                HttpResponse(status = StatusCodes.InternalServerError)
            }
            complete(responseFuture)
          } ~
          parameter("country_id".as[Int]) { countryId =>
            val responseFuture = (leagueService ? GetLeaguesByCountry(countryId)).map {
              case AllFoundLeagues(leagues) =>
                Utils.responseOkWithBody(leagues)
              case InternalServerError =>
                HttpResponse(status = StatusCodes.InternalServerError)
            }
            complete(responseFuture)
          } ~
          pathEndOrSingleSlash {
            val responseFuture = (leagueService ? GetAllEntities).map {
              case AllFoundLeagues(leagues) =>
                Utils.responseOkWithBody(leagues)
              case InternalServerError =>
                HttpResponse(status = StatusCodes.InternalServerError)
            }
            complete(responseFuture)
          }
      } ~
        post {
          path("all") {
            entity(as[List[LeagueApiDto]]) { leagueDtoList =>
              val responseFuture = (leagueService ? AddAllLeagues(leagueDtoList)).map {
                case AllLeaguesAdded(leagues, errors) =>
                  val map = Map(
                    "leagues" -> leagues.toJson.prettyPrint,
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
              entity(as[LeagueApiDto]) { leagueDto =>
                val responseFuture = (leagueService ? AddOneEntity(leagueDto)).map {
                  case OneEntityAdded(league: LeagueApiDto) =>
                    HttpResponse(status = StatusCodes.Created, entity = league.toJson.prettyPrint)
                  case LeagueValidationErrors(errors) =>
                    Utils.responseBadRequestWithBody(errors.map(_.message))
                  case InternalServerError =>
                    HttpResponse(status = StatusCodes.InternalServerError)
                }
                complete(responseFuture)
              }
            }
        } ~
        put {
          entity(as[LeagueApiDto]) { leagueDto =>
            val responseFuture = (leagueService ? UpdateEntity(leagueDto)).map {
              case UpdateFailed =>
                HttpResponse(status = StatusCodes.NotFound)
              case UpdateCompleted =>
                Utils.responseOk()
              case LeagueValidationErrors(errors) =>
                Utils.responseBadRequestWithBody(errors.map(_.message))
              case InternalServerError =>
                HttpResponse(status = StatusCodes.InternalServerError)
            }
            complete(responseFuture)
          }
        } ~
        delete {
          path(IntNumber) { id =>
            val responseFuture = (leagueService ? RemoveEntity(id)).map {
              case RemoveFailed =>
                Utils.responseBadRequest()
              case RemoveCompleted =>
                Utils.responseOk()
              case LeagueValidationErrors(errors) =>
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
