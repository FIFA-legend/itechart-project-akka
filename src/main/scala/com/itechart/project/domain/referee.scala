package com.itechart.project.domain

import com.itechart.project.domain.country.CountryId
import eu.timepit.refined.W
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.MatchesRegex
import eu.timepit.refined.types.string.NonEmptyString

object referee {

  final case class RefereeId(value: Long)

  type RefereeFirstName = NonEmptyString

  type RefereeLastName = NonEmptyString

  type RefereeImage = String Refined MatchesRegex[W.`"^[0-9]+.(png|jpg|jpeg)$"`.T]

  final case class Referee(
    id:        RefereeId,
    firstName: RefereeFirstName,
    lastName:  RefereeLastName,
    image:     Option[RefereeImage],
    countryId: CountryId
  )

}
