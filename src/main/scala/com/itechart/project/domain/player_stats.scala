package com.itechart.project.domain

import enumeratum.{Enum, EnumEntry}
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Interval.Closed
import eu.timepit.refined.numeric.NonNegative

object player_stats {

  final case class PlayerStatsId(value: Long)

  type ShirtNumber = Int Refined Closed[1, 100]

  type Minute = Int Refined Closed[1, 120]

  type Goals = Int Refined NonNegative

  type Assists = Int Refined NonNegative

  type Tackles = Int Refined NonNegative

  type Passes = Int Refined NonNegative

  type Dribbling = Int Refined NonNegative

  sealed trait Position extends EnumEntry

  object Position extends Enum[Position] {
    case object GK extends Position
    case object LWB extends Position
    case object LB extends Position
    case object CB extends Position
    case object RB extends Position
    case object RWB extends Position
    case object LM extends Position
    case object CM extends Position
    case object CDM extends Position
    case object CAM extends Position
    case object RM extends Position
    case object LW extends Position
    case object LF extends Position
    case object ST extends Position
    case object CF extends Position
    case object RF extends Position
    case object RW extends Position

    override def values: IndexedSeq[Position] = findValues
  }

  final case class PlayerStats(
    id:                  PlayerStatsId,
    shirtNumber:         ShirtNumber,
    position:            Position,
    startMinute:         Minute,
    playedMinutes:       Minute,
    goals:               Goals,
    assists:             Assists,
    successfulTackles:   Tackles,
    totalTackles:        Tackles,
    successfulPasses:    Passes,
    totalPasses:         Passes,
    successfulDribbling: Dribbling,
    totalDribbling:      Dribbling
  )

}
