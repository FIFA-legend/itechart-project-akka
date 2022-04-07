package com.itechart.project.dto

object venue {

  final case class VenueApiDto(
    id:        Int,
    name:      String,
    capacity:  Int,
    city:      String,
    countryId: Int
  )

}
