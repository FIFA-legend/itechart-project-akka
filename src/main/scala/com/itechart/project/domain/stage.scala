package com.itechart.project.domain

import eu.timepit.refined.types.string.NonEmptyString

object stage {

  final case class StageId(value: Int)

  type StageName = NonEmptyString

  final case class Stage(
    id:   StageId,
    name: StageName
  )

}
