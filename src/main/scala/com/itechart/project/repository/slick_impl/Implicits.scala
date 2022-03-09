package com.itechart.project.repository.slick_impl

import com.itechart.project.domain.country.{Continent, CountryCode, CountryId, CountryName}
import com.itechart.project.domain.formation.{FormationId, FormationName}
import com.itechart.project.domain.league.{LeagueId, LeagueName}
import com.itechart.project.domain.season.{SeasonId, SeasonName}
import com.itechart.project.utils.RefinedConversions.convertParameter
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import slick.jdbc.MySQLProfile.api._
import eu.timepit.refined.auto._

object Implicits {

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
  implicit val leagueNameTypeMapper: JdbcType[LeagueName] with BaseTypedType[LeagueName] =
    MappedColumnType.base[LeagueName, String](_.value, convertParameter(_, "Bundesliga"))

  implicit val seasonIdTypeMapper: JdbcType[SeasonId] with BaseTypedType[SeasonId] =
    MappedColumnType.base[SeasonId, Int](_.value, SeasonId)
  implicit val seasonNameTypeMapper: JdbcType[SeasonName] with BaseTypedType[SeasonName] =
    MappedColumnType.base[SeasonName, String](_.value, convertParameter(_, "2021/2022"))

}
