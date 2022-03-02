package com.itechart.project.domain

import com.itechart.project.domain.country.CountryId
import eu.timepit.refined.W
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.GreaterEqual
import eu.timepit.refined.string.MatchesRegex
import eu.timepit.refined.types.string.NonEmptyString

import java.sql.Date

object player {

  final case class PlayerId(value: Long)

  type FirstName = NonEmptyString

  type LastName = NonEmptyString

  type Age = Int Refined GreaterEqual[16]

  type Weight = Int Refined GreaterEqual[40]

  type Height = Int Refined GreaterEqual[140]

  type PlayerImage = String Refined MatchesRegex[W.`"^[0-9]+.[png|jpg|jpeg]$"`.T]

  final case class Player(
    id:        PlayerId,
    firstName: FirstName,
    lastName:  LastName,
    birthday:  Date,
    age:       Age,
    weight:    Option[Weight],
    height:    Option[Height],
    image:     Option[PlayerImage],
    countryId: CountryId
  )

}
