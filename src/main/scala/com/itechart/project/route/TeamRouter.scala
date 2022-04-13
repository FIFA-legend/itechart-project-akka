package com.itechart.project.route

import akka.actor.ActorRef
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import akka.pattern.ask
import com.itechart.project.dto.JsonConverters.TeamJsonProtocol
import com.itechart.project.dto.team.TeamApiDto
import com.itechart.project.service.CommonServiceMessages.Requests._
import com.itechart.project.service.CommonServiceMessages.Responses._
import spray.json._

import scala.concurrent.ExecutionContext

class TeamRouter(teamService: ActorRef)(implicit timeout: Timeout, ec: ExecutionContext)
  extends TeamJsonProtocol
    with SprayJsonSupport {

  import com.itechart.project.service.TeamService._

  val teamRoutes: Route = {
    pathPrefix("api" / "teams") {
      get {
        (path(IntNumber) | parameter("id".as[Int])) { id =>
          val responseFuture = (teamService ? GetEntityByT(id)).map {
            case OneFoundEntity(None) =>
              HttpResponse(status = StatusCodes.NotFound)
            case OneFoundEntity(Some(team: TeamApiDto)) =>
              Utils.responseBadRequestWithBody(team)
            case InternalServerError =>
              HttpResponse(status = StatusCodes.InternalServerError)
          }
          complete(responseFuture)
        } ~
          parameter("name") { name =>
            val responseFuture = (teamService ? GetEntityByT(name)).map {
              case AllFoundTeams(teams) =>
                Utils.responseOkWithBody(teams)
              case ValidationErrors(TeamErrorWrapper(errors)) =>
                Utils.responseBadRequestWithBody(errors.map(_.message))
              case InternalServerError =>
                HttpResponse(status = StatusCodes.InternalServerError)
            }
            complete(responseFuture)
          } ~
          parameter("country_id".as[Int]) { countryId =>
            val responseFuture = (teamService ? GetTeamsByCountry(countryId)).map {
              case AllFoundTeams(teams) =>
                Utils.responseOkWithBody(teams)
              case InternalServerError =>
                HttpResponse(status = StatusCodes.InternalServerError)
            }
            complete(responseFuture)
          } ~
          pathEndOrSingleSlash {
            val responseFuture = (teamService ? GetAllEntities).map {
              case AllFoundTeams(leagues) =>
                Utils.responseOkWithBody(leagues)
              case InternalServerError =>
                HttpResponse(status = StatusCodes.InternalServerError)
            }
            complete(responseFuture)
          }
      } ~
        post {
          path("all") {
            entity(as[List[TeamApiDto]]) { teamDtoList =>
              val responseFuture = (teamService ? AddAllTeams(teamDtoList)).map {
                case AllTeamsAdded(teams, errors) =>
                  val map = Map(
                    "teams"  -> teams.toJson.prettyPrint,
                    "errors" -> errors.map(_.message).mkString("[", ", ", "]")
                  )
                  Utils.responseOkWithBody(map)
                case InternalServerError =>
                  HttpResponse(status = StatusCodes.InternalServerError)
              }
              complete(responseFuture)
            }
          } ~
            pathEndOrSingleSlash {
              entity(as[TeamApiDto]) { teamDto =>
                val responseFuture = (teamService ? AddOneEntity(teamDto)).map {
                  case OneEntityAdded(team: TeamApiDto) =>
                    HttpResponse(status = StatusCodes.Created, entity = team.toJson.prettyPrint)
                  case ValidationErrors(TeamErrorWrapper(errors)) =>
                    Utils.responseBadRequestWithBody(errors.map(_.message))
                  case InternalServerError =>
                    HttpResponse(status = StatusCodes.InternalServerError)
                }
                complete(responseFuture)
              }
            }
        } ~
        put {
          entity(as[TeamApiDto]) { teamDto =>
            val responseFuture = (teamService ? UpdateEntity(teamDto)).map {
              case UpdateFailed =>
                HttpResponse(status = StatusCodes.NotFound)
              case UpdateCompleted =>
                Utils.responseOk()
              case ValidationErrors(TeamErrorWrapper(errors)) =>
                Utils.responseBadRequestWithBody(errors.map(_.message))
              case InternalServerError =>
                HttpResponse(status = StatusCodes.InternalServerError)
            }
            complete(responseFuture)
          }
        } ~
        delete {
          path(IntNumber) { id =>
            val responseFuture = (teamService ? RemoveEntity(id)).map {
              case RemoveFailed =>
                Utils.responseBadRequest()
              case RemoveCompleted =>
                Utils.responseOk()
              case ValidationErrors(TeamErrorWrapper(errors)) =>
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
