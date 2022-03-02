package com.itechart.project.domain

import eu.timepit.refined.W
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.NonNegative
import eu.timepit.refined.string.MatchesRegex

object stats {

  final case class StatsId(value: Long)

  type Score = String Refined MatchesRegex[W.`"^[0-9]{1,2}-[0-9]{1,2}$"`.T]

  type Attendance = NonNegative

  final case class Stats(
    id:         StatsId,
    htScore:    Option[Score],
    ftScore:    Option[Score],
    etScore:    Option[Score],
    psScore:    Option[Score],
    attendance: Option[Attendance]
  )

}
