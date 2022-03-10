package com.itechart.project.domain

import com.itechart.project.domain.country.CountryId
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.NonNegative
import eu.timepit.refined.types.string.NonEmptyString

object venue {

  final case class VenueId(value: Int)

  type VenueName = NonEmptyString

  type Capacity = Int Refined NonNegative

  type VenueCity = NonEmptyString

  final case class Venue(
    id:        VenueId,
    name:      VenueName,
    capacity:  Capacity,
    city:      VenueCity,
    countryId: CountryId
  )

}
