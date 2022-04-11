package com.itechart.project.repository.slick_impl

import com.itechart.project.domain.country.{Continent, CountryCode, CountryId, CountryName}
import com.itechart.project.domain.football_match.{MatchId, Status}
import com.itechart.project.domain.formation.{FormationId, FormationName}
import com.itechart.project.domain.league.LeagueId
import com.itechart.project.domain.match_stats.MatchStatsId
import com.itechart.project.domain.player.PlayerId
import com.itechart.project.domain.player_stats.{Minute, PlayerStatsId, Position, ShirtNumber}
import com.itechart.project.domain.referee.RefereeId
import com.itechart.project.domain.season.{SeasonId, SeasonName}
import com.itechart.project.domain.stage.StageId
import com.itechart.project.domain.team.{TeamId, TeamShortName}
import com.itechart.project.domain.user.{Email, Login, PasswordHash, Role, UserId}
import com.itechart.project.domain.venue.VenueId
import com.itechart.project.utils.FormationNameConversion
import com.itechart.project.utils.RefinedConversions.convertParameter
import eu.timepit.refined.W
import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto._
import eu.timepit.refined.numeric.{GreaterEqual, NonNegative}
import eu.timepit.refined.string.MatchesRegex
import eu.timepit.refined.types.string.NonEmptyString
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import slick.jdbc.MySQLProfile.api._

object Implicits {

  type Image = String Refined MatchesRegex[W.`"^[0-9]+.(png|jpg|jpeg)$"`.T]

  implicit val nonEmptyStringTypeMapper: JdbcType[NonEmptyString] with BaseTypedType[NonEmptyString] =
    MappedColumnType.base[NonEmptyString, String](_.value, convertParameter(_, "Default Non-empty String"))
  implicit val nonNegativeTypeMapper: JdbcType[Int Refined NonNegative] with BaseTypedType[Int Refined NonNegative] =
    MappedColumnType.base[Int Refined NonNegative, Int](_.value, convertParameter(_, 0))
  implicit val imageTypeMapper: JdbcType[Image] with BaseTypedType[Image] =
    MappedColumnType.base[Image, String](_.value, convertParameter(_, "0.png"))

  implicit val countryIdTypeMapper: JdbcType[CountryId] with BaseTypedType[CountryId] =
    MappedColumnType.base[CountryId, Int](_.value, CountryId)
  implicit val countryNameTypeMapper: JdbcType[CountryName] with BaseTypedType[CountryName] =
    MappedColumnType.base[CountryName, String](_.value, convertParameter(_, "Belarus"))
  implicit val countryCodeTypeMapper: JdbcType[CountryCode] with BaseTypedType[CountryCode] =
    MappedColumnType.base[CountryCode, String](_.value, convertParameter(_, "by"))
  implicit val continentTypeMapper: JdbcType[Continent] with BaseTypedType[Continent] =
    MappedColumnType.base[Continent, String](
      _.toString,
      {
        case "Africa"       => Continent.Africa
        case "Asia"         => Continent.Asia
        case "Europe"       => Continent.Europe
        case "Oceania"      => Continent.Oceania
        case "NorthAmerica" => Continent.NorthAmerica
        case "SouthAmerica" => Continent.SouthAmerica
      }
    )

  implicit val formationIdTypeMapper: JdbcType[FormationId] with BaseTypedType[FormationId] =
    MappedColumnType.base[FormationId, Int](_.value, FormationId)
  implicit val formationNameTypeMapper: JdbcType[FormationName] with BaseTypedType[FormationName] =
    MappedColumnType.base[FormationName, String](
      FormationNameConversion.formationNameToPrettyString,
      FormationNameConversion.prettyStringToFormationName
    )

  implicit val leagueIdTypeMapper: JdbcType[LeagueId] with BaseTypedType[LeagueId] =
    MappedColumnType.base[LeagueId, Int](_.value, LeagueId)

  implicit val seasonIdTypeMapper: JdbcType[SeasonId] with BaseTypedType[SeasonId] =
    MappedColumnType.base[SeasonId, Int](_.value, SeasonId)
  implicit val seasonNameTypeMapper: JdbcType[SeasonName] with BaseTypedType[SeasonName] =
    MappedColumnType.base[SeasonName, String](_.value, convertParameter(_, "2021/2022"))

  implicit val teamIdTypeMapper: JdbcType[TeamId] with BaseTypedType[TeamId] =
    MappedColumnType.base[TeamId, Int](_.value, TeamId)
  implicit val shortCodeTypeMapper: JdbcType[TeamShortName] with BaseTypedType[TeamShortName] =
    MappedColumnType.base[TeamShortName, String](_.value, convertParameter(_, "XXX"))

  implicit val refereeIdTypeMapper: JdbcType[RefereeId] with BaseTypedType[RefereeId] =
    MappedColumnType.base[RefereeId, Int](_.value, RefereeId)

  implicit val venueIdTypeMapper: JdbcType[VenueId] with BaseTypedType[VenueId] =
    MappedColumnType.base[VenueId, Int](_.value, VenueId)

  implicit val stageIdTypeMapper: JdbcType[StageId] with BaseTypedType[StageId] =
    MappedColumnType.base[StageId, Int](_.value, StageId)

  implicit val playerIdTypeMapper: JdbcType[PlayerId] with BaseTypedType[PlayerId] =
    MappedColumnType.base[PlayerId, Long](_.value, PlayerId)
  implicit val ageTypeMapper: JdbcType[Int Refined GreaterEqual[16]] with BaseTypedType[Int Refined GreaterEqual[16]] =
    MappedColumnType.base[Int Refined GreaterEqual[16], Int](_.value, convertParameter(_, 16))
  implicit val weightTypeMapper
    : JdbcType[Int Refined GreaterEqual[40]] with BaseTypedType[Int Refined GreaterEqual[40]] =
    MappedColumnType.base[Int Refined GreaterEqual[40], Int](_.value, convertParameter(_, 40))
  implicit val heightTypeMapper
    : JdbcType[Int Refined GreaterEqual[100]] with BaseTypedType[Int Refined GreaterEqual[100]] =
    MappedColumnType.base[Int Refined GreaterEqual[100], Int](_.value, convertParameter(_, 100))

  implicit val userIdTypeMapper: JdbcType[UserId] with BaseTypedType[UserId] =
    MappedColumnType.base[UserId, Long](_.value, UserId)
  implicit val loginTypeMapper: JdbcType[Login] with BaseTypedType[Login] =
    MappedColumnType.base[Login, String](_.value, Login)
  implicit val passwordHashTypeMapper: JdbcType[PasswordHash] with BaseTypedType[PasswordHash] =
    MappedColumnType.base[PasswordHash, String](_.value, PasswordHash)
  implicit val emailTypeMapper: JdbcType[Email] with BaseTypedType[Email] =
    MappedColumnType.base[Email, String](_.value, convertParameter(_, "default_email@gmail.com"))
  implicit val roleTypeMapper: JdbcType[Role] with BaseTypedType[Role] =
    MappedColumnType.base[Role, String](
      _.toString,
      {
        case "Admin" => Role.Admin
        case "User"  => Role.User
      }
    )

  implicit val playerStatsIdTypeMapper: JdbcType[PlayerStatsId] with BaseTypedType[PlayerStatsId] =
    MappedColumnType.base[PlayerStatsId, Long](_.value, PlayerStatsId)
  implicit val shirtNumberTypeMapper: JdbcType[ShirtNumber] with BaseTypedType[ShirtNumber] =
    MappedColumnType.base[ShirtNumber, Int](_.value, convertParameter(_, 1))
  implicit val positionNumberTypeMapper: JdbcType[Position] with BaseTypedType[Position] =
    MappedColumnType.base[Position, String](
      _.toString,
      {
        case "GK"  => Position.GK
        case "LWB" => Position.LWB
        case "LB"  => Position.LB
        case "CB"  => Position.CB
        case "RB"  => Position.RB
        case "RWB" => Position.RWB
        case "LM"  => Position.LM
        case "CM"  => Position.CM
        case "CDM" => Position.CDM
        case "CAM" => Position.CAM
        case "RM"  => Position.RM
        case "LW"  => Position.LW
        case "LF"  => Position.LF
        case "ST"  => Position.ST
        case "CF"  => Position.CF
        case "RF"  => Position.RF
        case "RW"  => Position.RW
      }
    )
  implicit val minuteTypeMapper: JdbcType[Minute] with BaseTypedType[Minute] =
    MappedColumnType.base[Minute, Int](_.value, convertParameter(_, 1))

  implicit val matchStatsIdTypeMapper: JdbcType[MatchStatsId] with BaseTypedType[MatchStatsId] =
    MappedColumnType.base[MatchStatsId, Long](_.value, MatchStatsId)

  implicit val matchIdTypeMapper: JdbcType[MatchId] with BaseTypedType[MatchId] =
    MappedColumnType.base[MatchId, Long](_.value, MatchId)
  implicit val matchStatusMapper: JdbcType[Status] with BaseTypedType[Status] =
    MappedColumnType.base[Status, String](_.toString, Status.withName)

}
