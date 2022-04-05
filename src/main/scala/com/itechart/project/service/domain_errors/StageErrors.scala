package com.itechart.project.service.domain_errors

object StageErrors {

  sealed trait StageError {
    def message: String
  }

  object StageError {
    final case class InvalidStageName(name: String) extends StageError {
      override def message: String =
        s"Invalid stage name `$name`. Stage name must not be empty"
    }

    final case class DuplicateStageName(name: String) extends StageError {
      override def message: String = s"Duplicate stage name `$name`. Stage name must be unique"
    }

    final case class StageForeignKey(id: Int) extends StageError {
      override def message: String = s"Stage with id `$id` can't be deleted because it's a part of foreign key"
    }
  }

}
