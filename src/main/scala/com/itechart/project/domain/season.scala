package com.itechart.project.domain

import eu.timepit.refined.W
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.MatchesRegex

import java.time.LocalDate

object season {

  final case class SeasonId(value: Int)

  type SeasonName = String Refined MatchesRegex[W.`"^[0-9]{4}/[0-9]{4}$"`.T]

  final case class Season(
    id:        SeasonId,
    name:      SeasonName,
    isCurrent: Boolean,
    startDate: LocalDate,
    endDate:   LocalDate
  )

}
