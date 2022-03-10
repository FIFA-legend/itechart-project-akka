package com.itechart.project.domain

import com.itechart.project.domain.country.CountryId
import eu.timepit.refined.W
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.MatchesRegex
import eu.timepit.refined.types.string.NonEmptyString

object team {

  final case class TeamId(value: Int)

  type TeamFullName = NonEmptyString

  type TeamShortName = String Refined MatchesRegex[W.`"^[A-Z]{3}$"`.T]

  type TeamLogo = String Refined MatchesRegex[W.`"^[0-9]+.(png|jpg|jpeg)$"`.T]

  final case class Team(
    id:        TeamId,
    name:      TeamFullName,
    shortCode: TeamShortName,
    logo:      Option[TeamLogo],
    countryId: CountryId
  )

}
