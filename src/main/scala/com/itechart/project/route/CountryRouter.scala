package com.itechart.project.route

import akka.actor.ActorRef
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import com.itechart.project.dto.JsonConverters.CountryJsonProtocol
import com.itechart.project.dto.country.CountryApiDto
import spray.json._

import scala.concurrent.ExecutionContext

class CountryRouter(countryService: ActorRef)(implicit timeout: Timeout, ec: ExecutionContext)
  extends CountryJsonProtocol
    with SprayJsonSupport {

  import com.itechart.project.service.CountryService._

  val countryRoutes: Route = {
    pathPrefix("api" / "countries") {
      get {
        (path(IntNumber) | parameter("id".as[Int])) { id =>
          val responseFuture = (countryService ? ReadCountryById(id)).map {
            case CountryNotFound =>
              HttpResponse(status = StatusCodes.NotFound)
            case ReadCountry(country) =>
              Utils.responseOkWithBody(country)
          }
          complete(responseFuture)
        } ~
          parameter("name") { name =>
            val responseFuture = (countryService ? ReadCountryByName(name)).map {
              case CountryNotFound =>
                HttpResponse(status = StatusCodes.NotFound)
              case ReadCountry(country) =>
                Utils.responseOkWithBody(country)
              case ReadCountryError(error) =>
                Utils.responseBadRequestWithBody(error.message)
            }
            complete(responseFuture)
          } ~
          parameter("code") { code =>
            val responseFuture = (countryService ? ReadCountryByCode(code)).map {
              case CountryNotFound =>
                HttpResponse(status = StatusCodes.NotFound)
              case ReadCountry(country) =>
                Utils.responseOkWithBody(country)
              case ReadCountryError(error) =>
                Utils.responseBadRequestWithBody(error.message)
            }
            complete(responseFuture)
          } ~
          pathEndOrSingleSlash {
            complete(
              (countryService ? ReadCountries).mapTo[ReadCountries].map(_.dtoCountries)
            )
          }
      } ~
        post {
          pathEndOrSingleSlash {
            entity(as[CountryApiDto]) { countryDto =>
              val responseFuture = (countryService ? CreateCountry(countryDto)).map {
                case ReadCountry(country) =>
                  HttpResponse(status = StatusCodes.Created, entity = country.toJson.prettyPrint)
                case ReadCountryErrors(errors) =>
                  Utils.responseBadRequestWithBody(errors.map(_.message))
              }
              complete(responseFuture)
            }
          }
        } ~
        put {
          entity(as[CountryApiDto]) { countryDto =>
            val responseFuture = (countryService ? UpdateCountry(countryDto)).map {
              case ReadCountry(country) =>
                HttpResponse(entity = country.toJson.prettyPrint)
              case ReadCountryErrors(errors) =>
                Utils.responseBadRequestWithBody(errors.map(_.message))
            }
            complete(responseFuture)
          }
        } ~
        delete {
          path(IntNumber) { id =>
            val responseFuture = (countryService ? DeleteCountry(id)).map {
              case CountryDeleteFailed =>
                HttpResponse(status = StatusCodes.NotFound)
              case CountryDeleteCompleted => HttpResponse()
              case ReadCountryError(error) =>
                Utils.responseBadRequestWithBody(error.message)
            }
            complete(responseFuture)
          }
        }
    }

  }
}
