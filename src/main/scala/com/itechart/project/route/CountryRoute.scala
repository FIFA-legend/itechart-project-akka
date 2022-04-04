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

class CountryRoute(countryService: ActorRef, implicit val timeout: Timeout, implicit val ec: ExecutionContext)
  extends CountryJsonProtocol
    with SprayJsonSupport {

  import com.itechart.project.service.CountryService._

  val countryRoutes: Route = {
    pathPrefix("api" / "countries") {
      get {
        (path(IntNumber) | parameter("id".as[Int])) { id =>
          val responseFuture = (countryService ? GetCountryById(id)).map {
            case FoundCountry(None) =>
              HttpResponse(status = StatusCodes.NotFound)
            case FoundCountry(Some(country)) =>
              Utils.responseOkWithBody(country)
            case CountryInternalServerError =>
              HttpResponse(status = StatusCodes.InternalServerError)
          }
          complete(responseFuture)
        } ~
          parameter("name") { name =>
            val responseFuture = (countryService ? GetCountryByName(name)).map {
              case FoundCountry(None) =>
                HttpResponse(status = StatusCodes.NotFound)
              case FoundCountry(Some(country)) =>
                Utils.responseOkWithBody(country)
              case CountryValidationErrors(errors) =>
                Utils.responseBadRequestWithBody(errors.map(_.message))
              case CountryInternalServerError =>
                HttpResponse(status = StatusCodes.InternalServerError)
            }
            complete(responseFuture)
          } ~
          parameter("code") { code =>
            val responseFuture = (countryService ? GetCountryByCode(code)).map {
              case FoundCountry(None) =>
                HttpResponse(status = StatusCodes.NotFound)
              case FoundCountry(Some(country)) =>
                Utils.responseOkWithBody(country)
              case CountryValidationErrors(errors) =>
                Utils.responseBadRequestWithBody(errors.map(_.message))
              case CountryInternalServerError =>
                HttpResponse(status = StatusCodes.InternalServerError)
            }
            complete(responseFuture)
          } ~
          pathEndOrSingleSlash {
            val responseFuture = (countryService ? GetAllCountries).map {
              case FoundCountries(countries) =>
                Utils.responseOkWithBody(countries)
              case CountryInternalServerError =>
                HttpResponse(status = StatusCodes.InternalServerError)
            }
            complete(responseFuture)
          }
      } ~
        post {
          path("all") {
            entity(as[List[CountryApiDto]]) { countryDtoList =>
              val responseFuture = (countryService ? AddCountries(countryDtoList)).map {
                case CountriesAdded(countries, errors) =>
                  val map = Map(
                    "countries" -> countries.toJson.prettyPrint,
                    "errors"    -> errors.map(_.message).mkString("[", ", ", "]")
                  )
                  Utils.responseOkWithBody(map)
                case CountryInternalServerError =>
                  HttpResponse(status = StatusCodes.InternalServerError)
              }
              complete(responseFuture)
            }
          } ~
            pathEndOrSingleSlash {
              entity(as[CountryApiDto]) { countryDto =>
                val responseFuture = (countryService ? AddCountry(countryDto)).map {
                  case CountryAdded(country) =>
                    HttpResponse(status = StatusCodes.Created, entity = country.toJson.prettyPrint)
                  case CountryValidationErrors(errors) =>
                    Utils.responseBadRequestWithBody(errors.map(_.message))
                  case CountryInternalServerError =>
                    HttpResponse(status = StatusCodes.InternalServerError)
                }
                complete(responseFuture)
              }
            }
        } ~
        put {
          entity(as[CountryApiDto]) { countryDto =>
            val responseFuture = (countryService ? UpdateCountry(countryDto)).map {
              case CountryNotUpdated =>
                HttpResponse(status = StatusCodes.NotFound)
              case CountryUpdated =>
                Utils.responseOk()
              case CountryValidationErrors(errors) =>
                Utils.responseBadRequestWithBody(errors.map(_.message))
              case CountryInternalServerError =>
                HttpResponse(status = StatusCodes.InternalServerError)
            }
            complete(responseFuture)
          }
        } ~
        delete {
          path(IntNumber) { id =>
            val responseFuture = (countryService ? RemoveCountry(id)).map {
              case CountryNotDeleted =>
                Utils.responseBadRequest()
              case CountryDeleted =>
                Utils.responseOk()
              case CountryValidationErrors(errors) =>
                Utils.responseBadRequestWithBody(errors.map(_.message))
              case CountryInternalServerError =>
                HttpResponse(status = StatusCodes.InternalServerError)
            }
            complete(responseFuture)
          }
        }
    }
  }

}
