package com.itechart.project.domain

import com.itechart.project.domain.country.CountryId
import eu.timepit.refined.types.string.NonEmptyString

object league {

  final case class LeagueId(value: Int)

  type LeagueName = NonEmptyString

  final case class League(
    id:        LeagueId,
    name:      LeagueName,
    countryId: CountryId
  )

}
