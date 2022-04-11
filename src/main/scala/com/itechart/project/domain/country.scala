package com.itechart.project.domain

import enumeratum.{Enum, EnumEntry}
import eu.timepit.refined.W
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.MatchesRegex

object country {

  final case class CountryId(value: Int)

  type CountryName = String Refined MatchesRegex[W.`"^[A-Z][A-Za-z]+$"`.T]

  type CountryCode = String Refined MatchesRegex[W.`"^[a-z]{2}$"`.T]

  sealed trait Continent extends EnumEntry

  object Continent extends Enum[Continent] {
    final case object Africa extends Continent
    final case object Asia extends Continent
    final case object Europe extends Continent
    final case object Oceania extends Continent
    final case object NorthAmerica extends Continent
    final case object SouthAmerica extends Continent

    override def values: IndexedSeq[Continent] = findValues
  }

  final case class Country(
    id:          CountryId,
    name:        CountryName,
    countryCode: CountryCode,
    continent:   Continent
  )

}
