package com.itechart.project.service.domain_errors

object RefereeErrors {

  sealed trait RefereeError extends DomainError

  object RefereeError {
    final case class InvalidRefereeFirstName(name: String) extends RefereeError {
      override def message: String =
        s"Invalid referee first name `$name`. First name must not be empty"
    }

    final case class InvalidRefereeLastName(name: String) extends RefereeError {
      override def message: String =
        s"Invalid referee last name `$name`. Last name must not be empty"
    }

    final case class InvalidRefereeImage(image: String) extends RefereeError {
      override def message: String =
        s"Invalid referee image `$image`. Image must match regular expression: [0-9]+.(png|jpg|jpeg)"
    }

    final case class InvalidRefereeCountryId(id: Int) extends RefereeError {
      override def message: String =
        s"Invalid referee country id `$id`. Country with id `$id` doesn't exist"
    }

    final case class RefereeForeignKey(id: Int) extends RefereeError {
      override def message: String =
        s"Referee with id `$id` can't be deleted because it's a part of foreign key"
    }
  }

}
