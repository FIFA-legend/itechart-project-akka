package com.itechart.project.route

import akka.actor.ActorRef
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import com.itechart.project.dto.JsonConverters.CountryJsonProtocol
import com.itechart.project.dto.country_dto.CountryApiDto
import com.itechart.project.service.domain_errors.CountryErrors.CountryError
import com.itechart.project.service.domain_errors.CountryErrors.CountryError._
import spray.json._

import scala.concurrent.ExecutionContext

class CountryRoute(actor: ActorRef, implicit val timeout: Timeout, implicit val ec: ExecutionContext)
  extends CountryJsonProtocol
    with SprayJsonSupport {

  import com.itechart.project.service.CountryService._

  val countryRoutes: Route = {
    pathPrefix("api" / "countries") {
      get {
        (path(IntNumber) | parameter("id".as[Int])) { id =>
          val responseFuture = (actor ? GetCountryById(id)).map {
            case None =>
              HttpResponse(status = StatusCodes.NotFound)
            case Some(country: CountryApiDto) =>
              Utils.responseOkWithBody(country)
            case CountryOperationFail =>
              HttpResponse(status = StatusCodes.InternalServerError, entity = CountryOperationFail.message)
          }
          complete(responseFuture)
        } ~
          parameter("name") { name =>
            val responseFuture = (actor ? GetCountryByName(name)).map {
              case None =>
                HttpResponse(status = StatusCodes.NotFound)
              case Some(country: CountryApiDto) =>
                Utils.responseOkWithBody(country)
              case error: InvalidCountryName =>
                Utils.responseBadRequestWithBody(error.message)
              case CountryOperationFail =>
                HttpResponse(status = StatusCodes.InternalServerError, entity = CountryOperationFail.message)
            }
            complete(responseFuture)
          } ~
          parameter("code") { code =>
            val responseFuture = (actor ? GetCountryByCode(code)).map {
              case None =>
                HttpResponse(status = StatusCodes.NotFound)
              case Some(country: CountryApiDto) =>
                Utils.responseOkWithBody(country)
              case error: InvalidCountryCode =>
                Utils.responseBadRequestWithBody(error.message)
              case CountryOperationFail =>
                HttpResponse(status = StatusCodes.InternalServerError, entity = CountryOperationFail.message)
            }
            complete(responseFuture)
          } ~
          pathEndOrSingleSlash {
            val responseFuture = (actor ? GetAllCountries).map {
              case ::(head: CountryApiDto, tail) =>
                Utils.responseOkWithBody(head +: tail.asInstanceOf[List[CountryApiDto]])
              case CountryOperationFail =>
                HttpResponse(status = StatusCodes.InternalServerError, entity = CountryOperationFail.message)
            }
            complete(responseFuture)
          }
      } ~
        post {
          entity(as[CountryApiDto]) { countryDto =>
            val responseFuture = (actor ? AddCountry(countryDto)).map {
              case country: CountryApiDto =>
                HttpResponse(status = StatusCodes.Created, entity = country.toJson.prettyPrint)
              case ::(head: CountryError, tail) =>
                val errorMessages = (head +: tail.asInstanceOf[List[CountryError]]).map(_.message)
                Utils.responseBadRequestWithBody(errorMessages)
              case CountryOperationFail =>
                HttpResponse(status = StatusCodes.InternalServerError, entity = CountryOperationFail.message)
            }
            complete(responseFuture)
          }
        } ~
        put {
          entity(as[CountryApiDto]) { countryDto =>
            val responseFuture = (actor ? UpdateCountry(countryDto)).map {
              case 0 =>
                HttpResponse(status = StatusCodes.NotFound)
              case _: Int =>
                Utils.responseOk()
              case ::(head: CountryError, tail) =>
                val errorMessages = (head +: tail.asInstanceOf[List[CountryError]]).map(_.message)
                Utils.responseBadRequestWithBody(errorMessages)
              case CountryOperationFail =>
                HttpResponse(status = StatusCodes.InternalServerError, entity = CountryOperationFail.message)
            }
            complete(responseFuture)
          }
        } ~
        delete {
          path(IntNumber) { id =>
            val responseFuture = (actor ? RemoveCountry(id)).map {
              case error @ CountryNotDeleted(_) =>
                Utils.responseBadRequestWithBody(error.message)
              case 0 =>
                Utils.responseBadRequest()
              case _: Int =>
                Utils.responseOk()
              case CountryOperationFail =>
                HttpResponse(status = StatusCodes.InternalServerError, entity = CountryOperationFail.message)
            }
            complete(responseFuture)
          }
        }
    }
  }

}
