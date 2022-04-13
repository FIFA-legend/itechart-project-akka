package com.itechart.project

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.util.Timeout
import com.itechart.project.configuration.ConfigurationTypes.DatabaseConfiguration
import com.itechart.project.configuration.DatabaseSettings
import com.itechart.project.repository._
import com.itechart.project.route._
import com.itechart.project.service._
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
      migrator.migrate()

      val db = DatabaseSettings.connection(databaseConfiguration)

      val countryRepository    = CountryRepository.of(db)
      val formationRepository  = FormationRepository.of(db)
      val leagueRepository     = LeagueRepository.of(db)
      val matchRepository      = MatchRepository.of(db)
      val matchStatsRepository = MatchStatsRepository.of(db)
      val playerRepository     = PlayerRepository.of(db)
      val refereeRepository    = RefereeRepository.of(db)
      val seasonRepository     = SeasonRepository.of(db)
      val stageRepository      = StageRepository.of(db)
      val teamRepository       = TeamRepository.of(db)
      val venueRepository      = VenueRepository.of(db)

      val countryService   = system.actorOf(CountryService.props(countryRepository), "countryService")
      val formationService = system.actorOf(FormationService.props(formationRepository), "formationService")
      val leagueService    = system.actorOf(LeagueService.props(leagueRepository, countryRepository), "leagueService")
      val matchService = system.actorOf(
        MatchService.props(
          matchRepository,
          seasonRepository,
          leagueRepository,
          stageRepository,
          teamRepository,
          refereeRepository,
          venueRepository,
          formationRepository
        ),
        "matchService"
      )
      val matchStatsService = system.actorOf(MatchStatsService.props(matchStatsRepository), "matchStatsService")
      val playerService     = system.actorOf(PlayerService.props(playerRepository, countryRepository), "playerService")
      val refereeService    = system.actorOf(RefereeService.props(refereeRepository, countryRepository), "refereeService")
      val seasonService     = system.actorOf(SeasonService.props(seasonRepository), "seasonService")
      val stageService      = system.actorOf(StageService.props(stageRepository), "stageService")
      val teamService       = system.actorOf(TeamService.props(teamRepository, countryRepository), "teamService")
      val venueService      = system.actorOf(VenueService.props(venueRepository, countryRepository), "venueRepository")

      val countryRouter    = new CountryRouter(countryService)
      val formationRouter  = new FormationRouter(formationService)
      val leagueRouter     = new LeagueRouter(leagueService)
      val matchRouter      = new MatchRouter(matchService)
      val matchStatsRouter = new MatchStatsRouter(matchStatsService)
      val playerRouter     = new PlayerRouter(playerService)
      val refereeRouter    = new RefereeRouter(refereeService)
      val seasonRouter     = new SeasonRouter(seasonService)
      val stageRouter      = new StageRouter(stageService)
      val teamRouter       = new TeamRouter(teamService)
      val venueRouter      = new VenueRouter(venueService)

      Http()
        .newServerAt("localhost", 8080)
        .bind(
          countryRouter.countryRoutes ~ formationRouter.formationRoutes ~ leagueRouter.leagueRoutes ~ matchRouter.matchRoutes ~
            matchStatsRouter.matchStatsRoutes ~ playerRouter.playerRoutes ~ refereeRouter.refereeRoutes ~
            seasonRouter.seasonRoutes ~ stageRouter.stageRoutes ~ teamRouter.teamRoutes ~ venueRouter.venueRoutes
        )
  }

}
