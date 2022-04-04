package com.itechart.project.dto

import java.sql.Date

object season {

  final case class SeasonApiDto(
    id:        Int,
    name:      String,
    isCurrent: Boolean,
    startDate: Date,
    endDate:   Date
  )

}
