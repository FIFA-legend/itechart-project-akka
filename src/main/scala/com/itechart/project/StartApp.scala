package com.itechart.project

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.routing.RoundRobinPool
import akka.util.Timeout
import com.itechart.project.configuration.ConfigurationTypes.DatabaseConfiguration
import com.itechart.project.configuration.DatabaseSettings
import com.itechart.project.https.HttpsContext
import com.itechart.project.https.HttpsContext.HttpsConfiguration
import com.itechart.project.repository._
import com.itechart.project.route._
import com.itechart.project.service.JwtAuthorizationService.JwtConfiguration
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
  val httpsSettingsEither: Either[Throwable, HttpsConfiguration] =
    parser.decodePath[HttpsConfiguration]("https-settings")
  val jwtSettingsEither: Either[Throwable, JwtConfiguration] =
    parser.decodePath[JwtConfiguration]("jwt-settings")

  (databaseSettingsEither, httpsSettingsEither, jwtSettingsEither) match {
    case (Right(databaseConfiguration), Right(httpsConfiguration), Right(jwtConfiguration)) =>
      val httpsConnectionContext = HttpsContext.httpsContext(httpsConfiguration)
      val migrator               = DatabaseSettings.migrator(databaseConfiguration)
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
      val userRepository       = UserRepository.of(db)
      val venueRepository      = VenueRepository.of(db)

      val numberOfActors = 5
      val jwtAuthorizationService = system.actorOf(
        RoundRobinPool(numberOfActors).props(JwtAuthorizationService.props(userRepository, jwtConfiguration)),
        "authorizationService"
      )

      val countryService =
        system.actorOf(
          RoundRobinPool(numberOfActors).props(CountryService.props(countryRepository)),
          "countryService"
        )
      val formationService = system.actorOf(
        RoundRobinPool(numberOfActors).props(FormationService.props(formationRepository)),
        "formationService"
      )
      val leagueService = system.actorOf(
        RoundRobinPool(numberOfActors).props(LeagueService.props(leagueRepository, countryRepository)),
        "leagueService"
      )
      val matchService = system.actorOf(
        RoundRobinPool(numberOfActors * 2).props(
          MatchService.props(
            matchRepository,
            seasonRepository,
            leagueRepository,
            stageRepository,
            teamRepository,
            refereeRepository,
            venueRepository,
            formationRepository
          )
        ),
        "matchService"
      )
      val matchStatsService = system.actorOf(
        RoundRobinPool(numberOfActors * 2).props(MatchStatsService.props(matchStatsRepository)),
        "matchStatsService"
      )
      val playerService = system.actorOf(
        RoundRobinPool(numberOfActors).props(PlayerService.props(playerRepository, countryRepository)),
        "playerService"
      )
      val refereeService = system.actorOf(
        RoundRobinPool(numberOfActors).props(RefereeService.props(refereeRepository, countryRepository)),
        "refereeService"
      )
      val seasonService = system.actorOf(
        RoundRobinPool(numberOfActors).props(SeasonService.props(seasonRepository)),
        "seasonService"
      )
      val stageService = system.actorOf(
        RoundRobinPool(numberOfActors).props(StageService.props(stageRepository)),
        "stageService"
      )
      val teamService = system.actorOf(
        RoundRobinPool(numberOfActors).props(TeamService.props(teamRepository, countryRepository)),
        "teamService"
      )
      val userService = system.actorOf(
        RoundRobinPool(numberOfActors).props(UserService.props(userRepository)),
        "userService"
      )
      val venueService = system.actorOf(
        RoundRobinPool(numberOfActors).props(VenueService.props(venueRepository, countryRepository)),
        "venueRepository"
      )

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
      val userRouter       = new UserRouter(userService, jwtAuthorizationService)
      val venueRouter      = new VenueRouter(venueService)

      val routes = countryRouter.countryRoutes ~ formationRouter.formationRoutes ~ leagueRouter.leagueRoutes ~
        matchRouter.matchRoutes ~ matchStatsRouter.matchStatsRoutes ~ playerRouter.playerRoutes ~
        refereeRouter.refereeRoutes ~ seasonRouter.seasonRoutes ~ stageRouter.stageRoutes ~
        teamRouter.teamRoutes ~ userRouter.userRoutes ~ venueRouter.venueRoutes

      Http()
        .newServerAt("localhost", 8080)
        .enableHttps(httpsConnectionContext)
        .bind(routes)

    case (_, _, _) =>
      println("Some errors in configuration")
  }

}
