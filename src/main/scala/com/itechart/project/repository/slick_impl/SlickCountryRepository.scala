package com.itechart.project.repository.slick_impl

import com.itechart.project.domain.country._
import com.itechart.project.repository.CountryRepository
import com.itechart.project.utils.RefinedConversions.convertParameter
import eu.timepit.refined.auto._
import slick.ast.BaseTypedType
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.{JdbcType, MySQLProfile}

import scala.concurrent.{ExecutionContext, Future}

class SlickCountryRepository(db: MySQLProfile.backend.Database)(implicit ec: ExecutionContext)
  extends CountryRepository {

  implicit private val countryIdTypeMapper: JdbcType[CountryId] with BaseTypedType[CountryId] =
    MappedColumnType.base[CountryId, Long](_.value, CountryId)
  implicit private val countryNameTypeMapper: JdbcType[CountryName] with BaseTypedType[CountryName] =
    MappedColumnType.base[CountryName, String](_.value, convertParameter(_, "Belarus"))
  implicit private val countryCodeTypeMapper: JdbcType[CountryCode] with BaseTypedType[CountryCode] =
    MappedColumnType.base[CountryCode, String](_.value, convertParameter(_, "by"))
  implicit private val continentTypeMapper: JdbcType[Continent] with BaseTypedType[Continent] =
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

  class CountryTable(tag: Tag) extends Table[Country](tag, None, "countries") {
    override def * = (id, name, countryCode, continent) <> (Country.tupled, Country.unapply)
    val id:          Rep[CountryId]   = column[CountryId]("id", O.AutoInc, O.PrimaryKey)
    val name:        Rep[CountryName] = column[CountryName]("name")
    val countryCode: Rep[CountryCode] = column[CountryCode]("country_code")
    val continent:   Rep[Continent]   = column[Continent]("continent")
  }

  private val countryTable = TableQuery[CountryTable]

  override def findAll: Future[List[Country]] = {
    val allCountriesQuery = countryTable.result
    db.run[Seq[Country]](allCountriesQuery).map(_.toList)
  }

  override def findById(id: CountryId): Future[Option[Country]] = {
    val countryQuery = countryTable.filter(_.id === id)
    db.run[Seq[Country]](countryQuery.result).map(_.toList).map(_.headOption)
  }

  override def create(country: Country): Future[CountryId] = {
    val insertCountryQuery = (countryTable returning countryTable.map(_.id)) += country
    db.run(insertCountryQuery)
  }

  override def createAll(countries: List[Country]): Future[List[CountryId]] = {
    val insertCountriesQuery = (countryTable returning countryTable.map(_.id)) ++= countries
    db.run(insertCountriesQuery).map(_.toList)
  }

  override def update(country: Country): Future[Unit] = {
    val id                   = country.id
    val updateNameQuery      = countryTable.filter(_.id === id).map(_.name).update(country.name)
    val updateCodeQuery      = countryTable.filter(_.id === id).map(_.countryCode).update(country.countryCode)
    val updateContinentQuery = countryTable.filter(_.id === id).map(_.continent).update(country.continent)

    val updateCountryQuery = DBIO.seq(updateNameQuery, updateCodeQuery, updateContinentQuery)
    db.run(updateCountryQuery.transactionally)
  }

  override def delete(id: CountryId): Future[Int] = {
    val deleteCountryQuery = countryTable.filter(_.id === id).delete
    db.run(deleteCountryQuery)
  }
}
