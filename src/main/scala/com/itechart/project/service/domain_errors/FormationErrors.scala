package com.itechart.project.service.domain_errors

object FormationErrors {

  sealed trait FormationError extends DomainError

  object FormationError {
    final case class InvalidFormationName(name: String) extends FormationError {
      override def message: String =
        s"Invalid formation name `$name`. Formation name must exist"
    }
  }

}
