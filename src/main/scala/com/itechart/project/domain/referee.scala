package com.itechart.project.domain

import com.itechart.project.domain.country.CountryId
import eu.timepit.refined.W
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.MatchesRegex
import eu.timepit.refined.types.string.NonEmptyString

object referee {

  final case class RefereeId(value: Long)

  type FirstName = NonEmptyString

  type LastName = NonEmptyString

  type RefereeImage = String Refined MatchesRegex[W.`"^[0-9]+.[png|jpg|jpeg]$"`.T]

  final case class Referee(
    id:        RefereeId,
    firstName: FirstName,
    lastName:  LastName,
    image:     Option[RefereeImage],
    countryId: CountryId
  )

}
