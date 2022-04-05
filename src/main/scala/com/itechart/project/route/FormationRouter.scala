package com.itechart.project.route

import akka.actor.ActorRef
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import com.itechart.project.dto.JsonConverters.FormationJsonProtocol
import com.itechart.project.dto.formation.FormationApiDto
import com.itechart.project.service.CommonServiceMessages.Requests._
import com.itechart.project.service.CommonServiceMessages.Responses._

import scala.concurrent.ExecutionContext

class FormationRouter(formationService: ActorRef)(implicit timeout: Timeout, ec: ExecutionContext)
  extends FormationJsonProtocol
    with SprayJsonSupport {

  import com.itechart.project.service.FormationService._

  val formationRoutes: Route = {
    pathPrefix("api" / "formations") {
      get {
        (path(IntNumber) | parameter("id".as[Int])) { id =>
          val responseFuture = (formationService ? GetEntityByT(id)).map {
            case OneFoundEntity(None) =>
              HttpResponse(status = StatusCodes.NotFound)
            case OneFoundEntity(Some(formation: FormationApiDto)) =>
              Utils.responseBadRequestWithBody(formation)
            case InternalServerError =>
              HttpResponse(status = StatusCodes.InternalServerError)
          }
          complete(responseFuture)
        } ~
          parameter("name") { name =>
            val responseFuture = (formationService ? GetEntityByT(name)).map {
              case OneFoundEntity(None) =>
                HttpResponse(status = StatusCodes.NotFound)
              case OneFoundEntity(Some(formation: FormationApiDto)) =>
                Utils.responseOkWithBody(formation)
              case FormationValidationErrors(errors) =>
                Utils.responseBadRequestWithBody(errors.map(_.message))
              case InternalServerError =>
                HttpResponse(status = StatusCodes.InternalServerError)
            }
            complete(responseFuture)
          } ~
          pathEndOrSingleSlash {
            val responseFuture = (formationService ? GetAllEntities).map {
              case AllFoundFormations(formations) =>
                Utils.responseOkWithBody(formations)
              case InternalServerError =>
                HttpResponse(status = StatusCodes.InternalServerError)
            }
            complete(responseFuture)
          }
      }
    }
  }

}
