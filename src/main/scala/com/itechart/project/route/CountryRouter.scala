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
import com.itechart.project.service.CommonServiceMessages.Requests._
import com.itechart.project.service.CommonServiceMessages.Responses._
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
          val responseFuture = (countryService ? GetEntityByT(id)).map {
            case OneFoundEntity(None) =>
              HttpResponse(status = StatusCodes.NotFound)
            case OneFoundEntity(Some(country: CountryApiDto)) =>
              Utils.responseOkWithBody(country)
            case InternalServerError =>
              HttpResponse(status = StatusCodes.InternalServerError)
          }
          complete(responseFuture)
        } ~
          parameter("name") { name =>
            val responseFuture = (countryService ? GetEntityByT(name)).map {
              case OneFoundEntity(None) =>
                HttpResponse(status = StatusCodes.NotFound)
              case OneFoundEntity(Some(country: CountryApiDto)) =>
                Utils.responseOkWithBody(country)
              case ValidationErrors(CountryErrorWrapper(errors)) =>
                Utils.responseBadRequestWithBody(errors.map(_.message))
              case InternalServerError =>
                HttpResponse(status = StatusCodes.InternalServerError)
            }
            complete(responseFuture)
          } ~
          parameter("code") { code =>
            val responseFuture = (countryService ? GetCountryByCode(code)).map {
              case OneFoundEntity(None) =>
                HttpResponse(status = StatusCodes.NotFound)
              case OneFoundEntity(Some(country: CountryApiDto)) =>
                Utils.responseOkWithBody(country)
              case ValidationErrors(CountryErrorWrapper(errors)) =>
                Utils.responseBadRequestWithBody(errors.map(_.message))
              case InternalServerError =>
                HttpResponse(status = StatusCodes.InternalServerError)
            }
            complete(responseFuture)
          } ~
          pathEndOrSingleSlash {
            val responseFuture = (countryService ? GetAllEntities).map {
              case AllFoundCountries(countries) =>
                Utils.responseOkWithBody(countries)
              case InternalServerError =>
                HttpResponse(status = StatusCodes.InternalServerError)
            }
            complete(responseFuture)
          }
      } ~
        post {
          path("all") {
            entity(as[List[CountryApiDto]]) { countryDtoList =>
              val responseFuture = (countryService ? AddAllCountries(countryDtoList)).map {
                case AllCountriesAdded(countries, errors) =>
                  val map = Map(
                    "countries" -> countries.toJson.prettyPrint,
                    "errors"    -> errors.map(_.message).mkString("[", ", ", "]")
                  )
                  Utils.responseOkWithBody(map)
                case InternalServerError =>
                  HttpResponse(status = StatusCodes.InternalServerError)
              }
              complete(responseFuture)
            }
          } ~
            pathEndOrSingleSlash {
              entity(as[CountryApiDto]) { countryDto =>
                val responseFuture = (countryService ? AddOneEntity(countryDto)).map {
                  case OneEntityAdded(country: CountryApiDto) =>
                    HttpResponse(status = StatusCodes.Created, entity = country.toJson.prettyPrint)
                  case ValidationErrors(CountryErrorWrapper(errors)) =>
                    Utils.responseBadRequestWithBody(errors.map(_.message))
                  case InternalServerError =>
                    HttpResponse(status = StatusCodes.InternalServerError)
                }
                complete(responseFuture)
              }
            }
        } ~
        put {
          entity(as[CountryApiDto]) { countryDto =>
            complete(Utils.responseOnEntityUpdate(countryService, countryDto))
          }
        } ~
        delete {
          path(IntNumber) { id =>
            complete(Utils.responseOnEntityDelete(countryService, id))
          }
        }
    }
  }

}
