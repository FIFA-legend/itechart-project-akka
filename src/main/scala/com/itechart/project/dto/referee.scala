package com.itechart.project.dto

object referee {

  final case class RefereeApiDto(
    id:        Int,
    firstName: String,
    lastName:  String,
    image:     Option[String],
    countryId: Int
  )

}
