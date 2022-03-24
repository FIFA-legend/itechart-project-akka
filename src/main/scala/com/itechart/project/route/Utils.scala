package com.itechart.project.route

import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import spray.json._

object Utils {

  def responseOk(): HttpResponse = HttpResponse(status = StatusCodes.OK)

  def responseOkWithBody[T: JsonWriter](body: T): HttpResponse =
    HttpResponse(status = StatusCodes.OK, entity = body.toJson.prettyPrint)

  def responseBadRequest(): HttpResponse = HttpResponse(status = StatusCodes.BadRequest)

  def responseBadRequestWithBody[T: JsonWriter](body: T): HttpResponse =
    HttpResponse(status = StatusCodes.BadRequest, entity = body.toJson.prettyPrint)
}
