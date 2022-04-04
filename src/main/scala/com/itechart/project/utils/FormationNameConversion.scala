package com.itechart.project.utils

import com.itechart.project.domain.formation.FormationName

object FormationNameConversion {

  def formationNameToPrettyString(formationName: FormationName): String = formationName match {
    case FormationName.FOUR_THREE_THREE      => "4-3-3"
    case FormationName.FOUR_FOUR_TWO         => "4-4-2"
    case FormationName.FOUR_FOUR_ONE_ONE     => "4-4-1-1"
    case FormationName.FOUR_TWO_THREE_ONE    => "4-2-3-1"
    case FormationName.FOUR_THREE_ONE_TWO    => "4-3-1-2"
    case FormationName.FOUR_THREE_TWO_ONE    => "4-3-2-1"
    case FormationName.FIVE_THREE_TWO        => "5-3-2"
    case FormationName.THREE_FIVE_TWO        => "3-5-2"
    case FormationName.THREE_THREE_THREE_ONE => "3-3-3-1"
    case FormationName.FIVE_FOUR_ONE         => "5-4-1"
    case FormationName.THREE_FOUR_THREE      => "3-4-3"
    case FormationName.FOUR_TWO_TWO_TWO      => "4-2-2-2"
    case FormationName.FOUR_ONE_TWO_ONE_TWO  => "4-1-2-1-2"
    case FormationName.FOUR_TWO_ONE_THREE    => "4-2-1-3"
  }

  def prettyStringToFormationName(string: String): FormationName = string match {
    case "4-3-3"     => FormationName.FOUR_THREE_THREE
    case "4-4-2"     => FormationName.FOUR_FOUR_TWO
    case "4-4-1-1"   => FormationName.FOUR_FOUR_ONE_ONE
    case "4-2-3-1"   => FormationName.FOUR_TWO_THREE_ONE
    case "4-3-1-2"   => FormationName.FOUR_THREE_ONE_TWO
    case "4-3-2-1"   => FormationName.FOUR_THREE_TWO_ONE
    case "5-3-2"     => FormationName.FIVE_THREE_TWO
    case "3-5-2"     => FormationName.THREE_FIVE_TWO
    case "3-3-3-1"   => FormationName.THREE_THREE_THREE_ONE
    case "5-4-1"     => FormationName.FIVE_FOUR_ONE
    case "3-4-3"     => FormationName.THREE_FOUR_THREE
    case "4-2-2-2"   => FormationName.FOUR_TWO_TWO_TWO
    case "4-1-2-1-2" => FormationName.FOUR_ONE_TWO_ONE_TWO
    case "4-2-1-3"   => FormationName.FOUR_TWO_ONE_THREE
  }

}
