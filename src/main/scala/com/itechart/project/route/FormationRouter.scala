package com.itechart.project.route

import akka.actor.ActorRef
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import com.itechart.project.dto.JsonConverters.FormationJsonProtocol

import scala.concurrent.ExecutionContext

class FormationRouter(formationService: ActorRef)(implicit timeout: Timeout, ec: ExecutionContext)
  extends FormationJsonProtocol
    with SprayJsonSupport {

  import com.itechart.project.service.FormationService._

  val formationRoutes: Route = {
    pathPrefix("api" / "formations") {
      get {
        (path(IntNumber) | parameter("id".as[Int])) { id =>
          val responseFuture = (formationService ? GetFormationById(id)).map {
            case FoundFormation(None) =>
              HttpResponse(status = StatusCodes.NotFound)
            case FoundFormation(Some(formation)) =>
              Utils.responseBadRequestWithBody(formation)
            case FormationInternalServerError =>
              HttpResponse(status = StatusCodes.InternalServerError)
          }
          complete(responseFuture)
        } ~
          parameter("name") { name =>
            val responseFuture = (formationService ? GetFormationByName(name)).map {
              case FoundFormation(None) =>
                HttpResponse(status = StatusCodes.NotFound)
              case FoundFormation(Some(formation)) =>
                Utils.responseOkWithBody(formation)
              case FormationValidationErrors(errors) =>
                Utils.responseBadRequestWithBody(errors.map(_.message))
              case FormationInternalServerError =>
                HttpResponse(status = StatusCodes.InternalServerError)
            }
            complete(responseFuture)
          } ~
          pathEndOrSingleSlash {
            val responseFuture = (formationService ? GetAllFormations).map {
              case FoundFormations(formations) =>
                Utils.responseOkWithBody(formations)
              case FormationInternalServerError =>
                HttpResponse(status = StatusCodes.InternalServerError)
            }
            complete(responseFuture)
          }
      }
    }
  }

}
