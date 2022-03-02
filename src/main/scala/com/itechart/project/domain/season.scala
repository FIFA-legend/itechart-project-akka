package com.itechart.project.domain

import eu.timepit.refined.W
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.MatchesRegex

import java.sql.Date

object season {

  final case class SeasonId(value: Long)

  type SeasonName = String Refined MatchesRegex[W.`"^[0-9]{2}/[0-9]{2}$"`.T]

  final case class Season(
    id:        SeasonId,
    name:      SeasonName,
    isCurrent: Boolean,
    startDate: Date,
    endDate:   Date
  )

}
