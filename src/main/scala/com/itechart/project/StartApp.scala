package com.itechart.project

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.util.Timeout
import com.itechart.project.configuration.ConfigurationTypes.DatabaseConfiguration
import com.itechart.project.configuration.DatabaseSettings
import com.itechart.project.repository.{CountryRepository, FormationRepository, LeagueRepository, SeasonRepository}
import com.itechart.project.route.{CountryRouter, FormationRouter, LeagueRouter, SeasonRouter}
import com.itechart.project.service.{CountryService, FormationService, LeagueService, SeasonService}
import io.circe.config.parser

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object StartApp extends App {

  implicit val system: ActorSystem = ActorSystem("FootballStats")

  implicit val timeout: Timeout          = Timeout(3.seconds)
  implicit val ec:      ExecutionContext = system.dispatchers.lookup("dispatcher")

  val databaseSettingsEither: Either[Throwable, DatabaseConfiguration] =
    parser.decodePath[DatabaseConfiguration]("database-settings")

  databaseSettingsEither match {
    case Left(ex) => throw ex
    case Right(databaseConfiguration) =>
      val migrator = DatabaseSettings.migrator(databaseConfiguration)
      // migrator.migrate()

      val db = DatabaseSettings.connection(databaseConfiguration)

      val countryRepository   = CountryRepository.of(db)
      val formationRepository = FormationRepository.of(db)
      val leagueRepository    = LeagueRepository.of(db)
      val seasonRepository    = SeasonRepository.of(db)

      val countryService   = system.actorOf(CountryService.props(countryRepository), "countryService")
      val formationService = system.actorOf(FormationService.props(formationRepository), "formationService")
      val leagueService    = system.actorOf(LeagueService.props(leagueRepository, countryRepository), "leagueService")
      val seasonService    = system.actorOf(SeasonService.props(seasonRepository), "seasonService")

      val countryRouter   = new CountryRouter(countryService)
      val formationRouter = new FormationRouter(formationService)
      val leagueRouter    = new LeagueRouter(leagueService)
      val seasonRouter    = new SeasonRouter(seasonService)

      Http()
        .newServerAt("localhost", 8080)
        .bind(
          countryRouter.countryRoutes ~ formationRouter.formationRoutes ~
            leagueRouter.leagueRoutes ~ seasonRouter.seasonRoutes
        )
  }

}
