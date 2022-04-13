package com.itechart.project.route

import akka.actor.ActorRef
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.pattern.ask
import akka.util.Timeout
import com.itechart.project.service.CommonServiceMessages.Requests._
import com.itechart.project.service.CommonServiceMessages.Responses._
import spray.json._

import scala.concurrent.{ExecutionContext, Future}

object Utils extends DefaultJsonProtocol {

  def responseOk(): HttpResponse = HttpResponse(status = StatusCodes.OK)

  def responseOkWithBody[T: JsonWriter](body: T): HttpResponse =
    HttpResponse(status = StatusCodes.OK, entity = body.toJson.prettyPrint)

  def responseBadRequest(): HttpResponse = HttpResponse(status = StatusCodes.BadRequest)

  def responseBadRequestWithBody[T: JsonWriter](body: T): HttpResponse =
    HttpResponse(status = StatusCodes.BadRequest, entity = body.toJson.prettyPrint)

  def responseOnEntityUpdate[D](
    service: ActorRef,
    dto:     D
  )(
    implicit ec: ExecutionContext,
    timeout:     Timeout
  ): Future[HttpResponse] =
    (service ? UpdateEntity(dto)).map {
      case UpdateFailed              => HttpResponse(status = StatusCodes.NotFound)
      case UpdateCompleted           => responseOk()
      case ValidationErrors(wrapper) => responseBadRequestWithBody(wrapper.errors.map(_.message))
      case InternalServerError       => HttpResponse(status = StatusCodes.InternalServerError)
    }

  def responseOnEntityDelete[I](
    service: ActorRef,
    id:      I
  )(
    implicit ec: ExecutionContext,
    timeout:     Timeout
  ): Future[HttpResponse] =
    (service ? RemoveEntity(id)).map {
      case RemoveFailed              => responseBadRequest()
      case RemoveCompleted           => responseOk()
      case ValidationErrors(wrapper) => responseBadRequestWithBody(wrapper.errors.map(_.message))
      case InternalServerError       => HttpResponse(status = StatusCodes.InternalServerError)
    }
}
