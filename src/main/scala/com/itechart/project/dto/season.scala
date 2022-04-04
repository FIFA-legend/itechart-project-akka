package com.itechart.project.dto

import java.time.LocalDate

object season {

  final case class SeasonApiDto(
    id:        Int,
    name:      String,
    isCurrent: Boolean,
    startDate: LocalDate,
    endDate:   LocalDate
  )

}
