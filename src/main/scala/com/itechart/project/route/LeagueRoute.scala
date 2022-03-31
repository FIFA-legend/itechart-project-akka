package com.itechart.project.route

import akka.actor.ActorRef
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import akka.pattern.ask
import com.itechart.project.dto.JsonConverters.LeagueJsonProtocol
import com.itechart.project.dto.league.LeagueApiDto
import com.itechart.project.service.domain_errors.LeagueErrors.LeagueError
import com.itechart.project.service.domain_errors.LeagueErrors.LeagueError._
import spray.json._

import scala.concurrent.ExecutionContext

class LeagueRoute(actor: ActorRef, implicit val timeout: Timeout, implicit val ec: ExecutionContext)
  extends LeagueJsonProtocol
    with SprayJsonSupport {

  import com.itechart.project.service.LeagueService._

  val leagueRoutes: Route = {
    pathPrefix("api" / "leagues") {
      get {
        (path(IntNumber) | parameter("id".as[Int])) { id =>
          val responseFuture = (actor ? GetLeagueById(id)).map {
            case FoundLeague(None) =>
              HttpResponse(status = StatusCodes.NotFound)
            case FoundLeague(Some(league)) =>
              Utils.responseBadRequestWithBody(league)
            case LeagueInternalServerError =>
              HttpResponse(status = StatusCodes.InternalServerError)
          }
          complete(responseFuture)
        } ~
          parameter("name") { name =>
            val responseFuture = (actor ? GetLeagueByName(name)).map {
              case FoundLeague(None) =>
                HttpResponse(status = StatusCodes.NotFound)
              case FoundLeague(Some(league)) =>
                Utils.responseOkWithBody(league)
              case LeagueValidationErrors(errors) =>
                Utils.responseBadRequestWithBody(errors.map(_.message))
              case LeagueInternalServerError =>
                HttpResponse(status = StatusCodes.InternalServerError)
            }
            complete(responseFuture)
          } ~
          parameter("country_id".as[Int]) { countryId =>
            val responseFuture = (actor ? GetLeaguesByCountry(countryId)).map {
              case FoundLeagues(leagues) =>
                Utils.responseOkWithBody(leagues)
              case LeagueInternalServerError =>
                HttpResponse(status = StatusCodes.InternalServerError)
            }
            complete(responseFuture)
          } ~
          pathEndOrSingleSlash {
            val responseFuture = (actor ? GetAllLeagues).map {
              case FoundLeagues(leagues) =>
                Utils.responseOkWithBody(leagues)
              case LeagueInternalServerError =>
                HttpResponse(status = StatusCodes.InternalServerError)
            }
            complete(responseFuture)
          }
      } ~
        post {
          path("all") {
            entity(as[List[LeagueApiDto]]) { leagueDtoList =>
              val responseFuture = (actor ? AddLeagues(leagueDtoList)).map {
                case LeaguesAdded(leagues, errors) =>
                  val map = Map(
                    "leagues" -> leagues.toJson.prettyPrint,
                    "errors"  -> errors.map(_.message).mkString("[", ", ", "]")
                  )
                  Utils.responseOkWithBody(map)
                case LeagueInternalServerError =>
                  HttpResponse(status = StatusCodes.InternalServerError)
              }
              complete(responseFuture)
            }
          } ~
            pathEndOrSingleSlash {
              entity(as[LeagueApiDto]) { leagueDto =>
                val responseFuture = (actor ? AddLeague(leagueDto)).map {
                  case LeagueAdded(league) =>
                    HttpResponse(status = StatusCodes.Created, entity = league.toJson.prettyPrint)
                  case LeagueValidationErrors(errors) =>
                    Utils.responseBadRequestWithBody(errors.map(_.message))
                  case LeagueInternalServerError =>
                    HttpResponse(status = StatusCodes.InternalServerError)
                }
                complete(responseFuture)
              }
            }
        } ~
        put {
          entity(as[LeagueApiDto]) { leagueDto =>
            val responseFuture = (actor ? UpdateLeague(leagueDto)).map {
              case LeagueNotUpdated =>
                HttpResponse(status = StatusCodes.NotFound)
              case LeagueUpdated =>
                Utils.responseOk()
              case LeagueValidationErrors(errors) =>
                Utils.responseBadRequestWithBody(errors.map(_.message))
              case LeagueInternalServerError =>
                HttpResponse(status = StatusCodes.InternalServerError)
            }
            complete(responseFuture)
          }
        } ~
        delete {
          path(IntNumber) { id =>
            val responseFuture = (actor ? RemoveLeague(id)).map {
              case LeagueNotDeleted =>
                Utils.responseBadRequest()
              case LeagueDeleted =>
                Utils.responseOk()
              case LeagueValidationErrors(errors) =>
                Utils.responseBadRequestWithBody(errors.map(_.message))
              case LeagueValidationErrors =>
                HttpResponse(status = StatusCodes.InternalServerError)
            }
            complete(responseFuture)
          }
        }
    }
  }

}
