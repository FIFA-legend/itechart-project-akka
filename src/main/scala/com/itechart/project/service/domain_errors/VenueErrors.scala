package com.itechart.project.service.domain_errors

object VenueErrors {

  sealed trait VenueError extends DomainError

  object VenueError {
    final case class InvalidVenueName(name: String) extends VenueError {
      override def message: String =
        s"Invalid venue name `$name`. Venue name must not be empty"
    }

    final case class DuplicateVenueName(name: String) extends VenueError {
      override def message: String =
        s"Duplicate venue name `$name`. Venue name must be unique"
    }

    final case class InvalidVenueCapacity(capacity: Int) extends VenueError {
      override def message: String =
        s"Invalid venue capacity `$capacity`. Venue capacity must not be negative"
    }

    final case class InvalidVenueCity(city: String) extends VenueError {
      override def message: String =
        s"Invalid venue city `$city`. Venue city must not be empty"
    }

    final case class InvalidVenueCountryId(id: Int) extends VenueError {
      override def message: String =
        s"Invalid venue country id `$id`. Country with id `$id` doesn't exist"
    }

    final case class VenueForeignKey(id: Int) extends VenueError {
      override def message: String =
        s"Venue with id `$id` can't be deleted because it's a part of foreign key"
    }
  }

}
