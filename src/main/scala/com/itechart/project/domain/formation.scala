package com.itechart.project.domain

object formation {

  final case class FormationId(value: Int)

  sealed trait FormationName

  object FormationName {
    case object FOUR_THREE_THREE extends FormationName
    case object FOUR_FOUR_TWO extends FormationName
    case object FOUR_FOUR_ONE_ONE extends FormationName
    case object FOUR_TWO_THREE_ONE extends FormationName
    case object FOUR_THREE_ONE_TWO extends FormationName
    case object FOUR_THREE_TWO_ONE extends FormationName
    case object FIVE_THREE_TWO extends FormationName
    case object THREE_FIVE_TWO extends FormationName
    case object THREE_THREE_THREE_ONE extends FormationName
    case object FIVE_FOUR_ONE extends FormationName
    case object THREE_FOUR_THREE extends FormationName
    case object FOUR_TWO_TWO_TWO extends FormationName
    case object FOUR_ONE_TWO_ONE_TWO extends FormationName
    case object FOUR_TWO_ONE_THREE extends FormationName
  }

  final case class Formation(
    id:   FormationId,
    name: FormationName
  )

}
