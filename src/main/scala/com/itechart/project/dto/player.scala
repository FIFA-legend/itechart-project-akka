package com.itechart.project.dto

import java.time.LocalDate

object player {

  final case class PlayerApiDto(
    id:        Long,
    firstName: String,
    lastName:  String,
    birthday:  LocalDate,
    age:       Int,
    weight:    Option[Int],
    height:    Option[Int],
    image:     Option[String],
    countryId: Int
  )

}
