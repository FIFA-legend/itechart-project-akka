package com.itechart.project.domain

import com.itechart.project.domain.football_match.MatchId
import com.itechart.project.domain.user.Email
import eu.timepit.refined.types.string.NonEmptyString

object mail {

  final case class Mail(to: Email, subject: NonEmptyString, contents: NonEmptyString, id: MatchId)

}
