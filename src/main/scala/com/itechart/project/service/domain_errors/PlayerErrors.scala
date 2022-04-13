package com.itechart.project.service.domain_errors

object PlayerErrors {

  sealed trait PlayerError extends DomainError

  object PlayerError {
    final case class InvalidPlayerFirstName(name: String) extends PlayerError {
      override def message: String =
        s"Invalid player first name `$name`. Player first name must not be empty"
    }

    final case class InvalidPlayerLastName(name: String) extends PlayerError {
      override def message: String =
        s"Invalid player last name `$name`. Player last name must not be empty"
    }

    final case class InvalidPlayerAge(age: Int) extends PlayerError {
      override def message: String =
        s"Invalid player age `$age`. Player age must be greater than 16"
    }

    final case class InvalidPlayerWeight(weight: Int) extends PlayerError {
      override def message: String =
        s"Invalid player weight `$weight`. Player weight must be greater than 40"
    }

    final case class InvalidPlayerHeight(height: Int) extends PlayerError {
      override def message: String =
        s"Invalid player height `$height`. Player first height must be greater than 100"
    }

    final case class InvalidPlayerImage(image: String) extends PlayerError {
      override def message: String =
        s"Invalid player image `$image`. Image must match regular expression: [0-9]+.(png|jpg|jpeg)"
    }

    final case class InvalidPlayerCountryId(id: Int) extends PlayerError {
      override def message: String =
        s"Invalid player country id `$id`. Country with id `$id` doesn't exist"
    }

    final case class PlayerForeignKey(id: Long) extends PlayerError {
      override def message: String =
        s"Player with id `$id` can't be deleted because it's a part of foreign key"
    }
  }

}
