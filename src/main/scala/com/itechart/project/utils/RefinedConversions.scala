package com.itechart.project.utils

import eu.timepit.refined.api.{Refined, Validate}
import eu.timepit.refined.refineV

object RefinedConversions {

  def convertParameter[T, P](
    parameter: T,
    default:   Refined[T, P]
  )(
    implicit v: Validate[T, P]
  ): Refined[T, P] = {
    refineV(parameter).getOrElse(default)
  }

}
