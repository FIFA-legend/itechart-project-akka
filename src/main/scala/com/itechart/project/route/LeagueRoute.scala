package com.itechart.project.route

import akka.actor.ActorRef
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import akka.pattern.ask
import com.itechart.project.dto.JsonConverters.LeagueJsonProtocol
import com.itechart.project.dto.league_dto.LeagueApiDto
import com.itechart.project.service.domain_errors.LeagueErrors.LeagueError
import com.itechart.project.service.domain_errors.LeagueErrors.LeagueError.{
  InvalidLeagueName,
  LeagueNotDeleted,
  LeagueOperationFail
}
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
            case None =>
              HttpResponse(status = StatusCodes.NotFound)
            case Some(leagueDto: LeagueApiDto) =>
              Utils.responseBadRequestWithBody(leagueDto)
            case LeagueOperationFail =>
              HttpResponse(status = StatusCodes.InternalServerError, entity = LeagueOperationFail.message)
          }
          complete(responseFuture)
        } ~
          parameter("name") { name =>
            val responseFuture = (actor ? GetLeagueByName(name)).map {
              case None =>
                HttpResponse(status = StatusCodes.NotFound)
              case Some(leagueDto: LeagueApiDto) =>
                Utils.responseOkWithBody(leagueDto)
              case error: InvalidLeagueName =>
                Utils.responseBadRequestWithBody(error.message)
              case LeagueOperationFail =>
                HttpResponse(status = StatusCodes.InternalServerError, entity = LeagueOperationFail.message)
            }
            complete(responseFuture)
          } ~
          parameter("country_id".as[Int]) { countryId =>
            val responseFuture = (actor ? GetLeaguesByCountry(countryId)).map {
              case Nil =>
                HttpResponse(status = StatusCodes.NotFound)
              case ::(head: LeagueApiDto, tail) =>
                Utils.responseOkWithBody(head +: tail.asInstanceOf[List[LeagueApiDto]])
              case LeagueOperationFail =>
                HttpResponse(status = StatusCodes.InternalServerError, entity = LeagueOperationFail.message)
            }
            complete(responseFuture)
          } ~
          pathEndOrSingleSlash {
            val responseFuture = (actor ? GetAllLeagues).map {
              case ::(head: LeagueApiDto, tail) =>
                Utils.responseOkWithBody(head +: tail.asInstanceOf[List[LeagueApiDto]])
              case LeagueOperationFail =>
                HttpResponse(status = StatusCodes.InternalServerError, entity = LeagueOperationFail.message)
            }
            complete(responseFuture)
          }
      } ~
        post {
          path("all") {
            entity(as[List[LeagueApiDto]]) { leagueDtoList =>
              val responseFuture = (actor ? AddLeagues(leagueDtoList)).map {
                case ::(Left(head: LeagueError), tail) =>
                  val result = (Left(head) +: tail.asInstanceOf[List[Either[LeagueError, LeagueApiDto]]]).map {
                    case Right(country) => country.toJson.prettyPrint
                    case Left(error)    => error.message
                  }
                  Utils.responseOkWithBody(result)
                case ::(Right(head: LeagueApiDto), tail) =>
                  val result = (Right(head) +: tail.asInstanceOf[List[Either[LeagueError, LeagueApiDto]]]).map {
                    case Right(country) => country.toJson.prettyPrint
                    case Left(error)    => error.message
                  }
                  Utils.responseOkWithBody(result)
                case LeagueOperationFail =>
                  HttpResponse(status = StatusCodes.InternalServerError, entity = LeagueOperationFail.message)
              }
              complete(responseFuture)
            }
          } ~
            pathEndOrSingleSlash {
              entity(as[LeagueApiDto]) { leagueDto =>
                val responseFuture = (actor ? AddLeague(leagueDto)).map {
                  case league: LeagueApiDto =>
                    HttpResponse(status = StatusCodes.Created, entity = league.toJson.prettyPrint)
                  case List(error: LeagueError) =>
                    Utils.responseBadRequestWithBody(error.message)
                  case ::(head: LeagueError, tail) =>
                    val messages = (head +: tail.asInstanceOf[List[LeagueError]]).map(_.message)
                    Utils.responseBadRequestWithBody(messages)
                  case LeagueOperationFail =>
                    HttpResponse(status = StatusCodes.InternalServerError, entity = LeagueOperationFail.message)
                }
                complete(responseFuture)
              }
            }
        } ~
        put {
          entity(as[LeagueApiDto]) { leagueDto =>
            val responseFuture = (actor ? UpdateLeague(leagueDto)).map {
              case 0 =>
                HttpResponse(status = StatusCodes.NotFound)
              case _: Int =>
                Utils.responseOk()
              case ::(head: LeagueError, tail) =>
                val messages = (head +: tail.asInstanceOf[List[LeagueError]]).map(_.message)
                Utils.responseBadRequestWithBody(messages)
              case LeagueOperationFail =>
                HttpResponse(status = StatusCodes.InternalServerError, entity = LeagueOperationFail.message)
            }
            complete(responseFuture)
          }
        } ~
        delete {
          path(IntNumber) { id =>
            val responseFuture = (actor ? RemoveLeague(id)).map {
              case error @ LeagueNotDeleted(_) =>
                Utils.responseBadRequestWithBody(error.message)
              case 0 =>
                Utils.responseBadRequest()
              case _: Int =>
                Utils.responseOk()
              case LeagueOperationFail =>
                HttpResponse(status = StatusCodes.InternalServerError, entity = LeagueOperationFail.message)
            }
            complete(responseFuture)
          }
        }
    }
  }

}
