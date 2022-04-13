package com.itechart.project.route

import akka.actor.ActorRef
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import com.itechart.project.dto.JsonConverters.StageJsonProtocol
import com.itechart.project.dto.stage.StageApiDto
import com.itechart.project.service.CommonServiceMessages.Requests._
import com.itechart.project.service.CommonServiceMessages.Responses._
import com.itechart.project.service.StageService.{AddAllStages, AllFoundStages, AllStagesAdded, StageErrorWrapper}
import spray.json._

import scala.concurrent.ExecutionContext

class StageRouter(stageService: ActorRef)(implicit timeout: Timeout, ec: ExecutionContext)
  extends StageJsonProtocol
    with SprayJsonSupport {

  val stageRoutes: Route = {
    pathPrefix("api" / "stages") {
      get {
        (path(IntNumber) | parameter("id".as[Int])) { id =>
          val responseFuture = (stageService ? GetEntityByT(id)).map {
            case OneFoundEntity(None) =>
              HttpResponse(status = StatusCodes.NotFound)
            case OneFoundEntity(Some(stage: StageApiDto)) =>
              Utils.responseBadRequestWithBody(stage)
            case InternalServerError =>
              HttpResponse(status = StatusCodes.InternalServerError)
          }
          complete(responseFuture)
        } ~
          parameter("name") { name =>
            val responseFuture = (stageService ? GetEntityByT(name)).map {
              case OneFoundEntity(None) =>
                HttpResponse(status = StatusCodes.NotFound)
              case OneFoundEntity(Some(stage: StageApiDto)) =>
                Utils.responseOkWithBody(stage)
              case ValidationErrors(StageErrorWrapper(errors)) =>
                Utils.responseBadRequestWithBody(errors.map(_.message))
              case InternalServerError =>
                HttpResponse(status = StatusCodes.InternalServerError)
            }
            complete(responseFuture)
          } ~
          pathEndOrSingleSlash {
            val responseFuture = (stageService ? GetAllEntities).map {
              case AllFoundStages(leagues) =>
                Utils.responseOkWithBody(leagues)
              case InternalServerError =>
                HttpResponse(status = StatusCodes.InternalServerError)
            }
            complete(responseFuture)
          }
      } ~
        post {
          path("all") {
            entity(as[List[StageApiDto]]) { stageDtoList =>
              val responseFuture = (stageService ? AddAllStages(stageDtoList)).map {
                case AllStagesAdded(stages, errors) =>
                  val map = Map(
                    "stages" -> stages.toJson.prettyPrint,
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
              entity(as[StageApiDto]) { stageDto =>
                val responseFuture = (stageService ? AddOneEntity(stageDto)).map {
                  case OneEntityAdded(stage: StageApiDto) =>
                    HttpResponse(status = StatusCodes.Created, entity = stage.toJson.prettyPrint)
                  case ValidationErrors(StageErrorWrapper(errors)) =>
                    Utils.responseBadRequestWithBody(errors.map(_.message))
                  case InternalServerError =>
                    HttpResponse(status = StatusCodes.InternalServerError)
                }
                complete(responseFuture)
              }
            }
        } ~
        put {
          entity(as[StageApiDto]) { stageDto =>
            val responseFuture = (stageService ? UpdateEntity(stageDto)).map {
              case UpdateFailed =>
                HttpResponse(status = StatusCodes.NotFound)
              case UpdateCompleted =>
                Utils.responseOk()
              case ValidationErrors(StageErrorWrapper(errors)) =>
                Utils.responseBadRequestWithBody(errors.map(_.message))
              case InternalServerError =>
                HttpResponse(status = StatusCodes.InternalServerError)
            }
            complete(responseFuture)
          }
        } ~
        delete {
          path(IntNumber) { id =>
            val responseFuture = (stageService ? RemoveEntity(id)).map {
              case RemoveFailed =>
                Utils.responseBadRequest()
              case RemoveCompleted =>
                Utils.responseOk()
              case ValidationErrors(StageErrorWrapper(errors)) =>
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
