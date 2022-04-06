package com.itechart.project.dto

object team {

  final case class TeamApiDto(
    id:        Int,
    name:      String,
    shortCode: String,
    logo:      Option[String],
    countryId: Int
  )

}
