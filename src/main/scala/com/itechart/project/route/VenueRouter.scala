package com.itechart.project.route

import akka.actor.ActorRef
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import akka.pattern.ask
import com.itechart.project.dto.JsonConverters.VenueJsonProtocol
import com.itechart.project.dto.venue.VenueApiDto
import com.itechart.project.service.CommonServiceMessages.Requests._
import com.itechart.project.service.CommonServiceMessages.Responses._
import spray.json._

import scala.concurrent.ExecutionContext

class VenueRouter(venueService: ActorRef)(implicit timeout: Timeout, ec: ExecutionContext)
  extends VenueJsonProtocol
    with SprayJsonSupport {

  import com.itechart.project.service.VenueService._

  val venueRoutes: Route = {
    pathPrefix("api" / "venues") {
      get {
        (path(IntNumber) | parameter("id".as[Int])) { id =>
          val responseFuture = (venueService ? GetEntityByT(id)).map {
            case OneFoundEntity(None) =>
              HttpResponse(status = StatusCodes.NotFound)
            case OneFoundEntity(Some(venue: VenueApiDto)) =>
              Utils.responseBadRequestWithBody(venue)
            case InternalServerError =>
              HttpResponse(status = StatusCodes.InternalServerError)
          }
          complete(responseFuture)
        } ~
          parameter("name") { name =>
            val responseFuture = (venueService ? GetEntityByT(name)).map {
              case OneFoundEntity(None) =>
                HttpResponse(status = StatusCodes.NotFound)
              case OneFoundEntity(Some(venue: VenueApiDto)) =>
                Utils.responseOkWithBody(venue)
              case VenueValidationErrors(errors) =>
                Utils.responseBadRequestWithBody(errors.map(_.message))
              case InternalServerError =>
                HttpResponse(status = StatusCodes.InternalServerError)
            }
            complete(responseFuture)
          } ~
          parameter("city".as[String]) { city =>
            val responseFuture = (venueService ? GetVenuesByCity(city)).map {
              case VenueValidationErrors(errors) =>
                Utils.responseBadRequestWithBody(errors.map(_.message))
              case AllFoundVenues(venues) =>
                Utils.responseOkWithBody(venues)
              case InternalServerError =>
                HttpResponse(status = StatusCodes.InternalServerError)
            }
            complete(responseFuture)
          } ~
          parameter("country_id".as[Int]) { countryId =>
            val responseFuture = (venueService ? GetVenuesByCountry(countryId)).map {
              case AllFoundVenues(venues) =>
                Utils.responseOkWithBody(venues)
              case InternalServerError =>
                HttpResponse(status = StatusCodes.InternalServerError)
            }
            complete(responseFuture)
          } ~
          pathEndOrSingleSlash {
            val responseFuture = (venueService ? GetAllEntities).map {
              case AllFoundVenues(venues) =>
                Utils.responseOkWithBody(venues)
              case InternalServerError =>
                HttpResponse(status = StatusCodes.InternalServerError)
            }
            complete(responseFuture)
          }
      } ~
        post {
          path("all") {
            entity(as[List[VenueApiDto]]) { venueDtoList =>
              val responseFuture = (venueService ? AddAllVenues(venueDtoList)).map {
                case AllVenuesAdded(venues, errors) =>
                  val map = Map(
                    "venues" -> venues.toJson.prettyPrint,
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
              entity(as[VenueApiDto]) { venueDto =>
                val responseFuture = (venueService ? AddOneEntity(venueDto)).map {
                  case OneEntityAdded(venue: VenueApiDto) =>
                    HttpResponse(status = StatusCodes.Created, entity = venue.toJson.prettyPrint)
                  case VenueValidationErrors(errors) =>
                    Utils.responseBadRequestWithBody(errors.map(_.message))
                  case InternalServerError =>
                    HttpResponse(status = StatusCodes.InternalServerError)
                }
                complete(responseFuture)
              }
            }
        } ~
        put {
          entity(as[VenueApiDto]) { venueDto =>
            val responseFuture = (venueService ? UpdateEntity(venueDto)).map {
              case UpdateFailed =>
                HttpResponse(status = StatusCodes.NotFound)
              case UpdateCompleted =>
                Utils.responseOk()
              case VenueValidationErrors(errors) =>
                Utils.responseBadRequestWithBody(errors.map(_.message))
              case InternalServerError =>
                HttpResponse(status = StatusCodes.InternalServerError)
            }
            complete(responseFuture)
          }
        } ~
        delete {
          path(IntNumber) { id =>
            val responseFuture = (venueService ? RemoveEntity(id)).map {
              case RemoveFailed =>
                Utils.responseBadRequest()
              case RemoveCompleted =>
                Utils.responseOk()
              case VenueValidationErrors(errors) =>
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
