package com.itechart.project.repository.slick_impl

import com.itechart.project.domain.country.{Continent, CountryCode, CountryId, CountryName}
import com.itechart.project.domain.formation.{FormationId, FormationName}
import com.itechart.project.domain.league.LeagueId
import com.itechart.project.domain.player.PlayerId
import com.itechart.project.domain.referee.RefereeId
import com.itechart.project.domain.season.{SeasonId, SeasonName}
import com.itechart.project.domain.stage.StageId
import com.itechart.project.domain.team.{TeamId, TeamShortName}
import com.itechart.project.domain.user.{Email, Login, PasswordHash, Role, UserId}
import com.itechart.project.domain.venue.VenueId
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
      {
        case FormationName.FOUR_THREE_THREE      => "4-3-3"
        case FormationName.FOUR_FOUR_TWO         => "4-4-2"
        case FormationName.FOUR_FOUR_ONE_ONE     => "4-4-1-1"
        case FormationName.FOUR_TWO_THREE_ONE    => "4-2-3-1"
        case FormationName.FOUR_THREE_ONE_TWO    => "4-3-1-2"
        case FormationName.FOUR_THREE_TWO_ONE    => "4-3-2-1"
        case FormationName.FIVE_THREE_TWO        => "5-3-2"
        case FormationName.THREE_FIVE_TWO        => "3-5-2"
        case FormationName.THREE_THREE_THREE_ONE => "3-3-3-1"
        case FormationName.FIVE_FOUR_ONE         => "5-4-1"
        case FormationName.THREE_FOUR_THREE      => "3-4-3"
        case FormationName.FOUR_TWO_TWO_TWO      => "4-2-2-2"
        case FormationName.FOUR_ONE_TWO_ONE_TWO  => "4-1-2-1-2"
        case FormationName.FOUR_TWO_ONE_THREE    => "4-2-1-3"
      },
      {
        case "4-3-3"     => FormationName.FOUR_THREE_THREE
        case "4-4-2"     => FormationName.FOUR_FOUR_TWO
        case "4-4-1-1"   => FormationName.FOUR_FOUR_ONE_ONE
        case "4-2-3-1"   => FormationName.FOUR_TWO_THREE_ONE
        case "4-3-1-2"   => FormationName.FOUR_THREE_ONE_TWO
        case "4-3-2-1"   => FormationName.FOUR_THREE_TWO_ONE
        case "5-3-2"     => FormationName.FIVE_THREE_TWO
        case "3-5-2"     => FormationName.THREE_FIVE_TWO
        case "3-3-3-1"   => FormationName.THREE_THREE_THREE_ONE
        case "5-4-1"     => FormationName.FIVE_FOUR_ONE
        case "3-4-3"     => FormationName.THREE_FOUR_THREE
        case "4-2-2-2"   => FormationName.FOUR_TWO_TWO_TWO
        case "4-1-2-1-2" => FormationName.FOUR_ONE_TWO_ONE_TWO
        case "4-2-1-3"   => FormationName.FOUR_TWO_ONE_THREE
      }
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

}
