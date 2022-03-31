package com.itechart.project.service.domain_errors

object CountryErrors {

  sealed trait CountryError {
    def message: String
  }

  object CountryError {
    final case class InvalidCountryName(name: String) extends CountryError {
      override def message: String =
        s"Invalid country name `$name`. Country name must match regular expression: [A-Z][A-Za-z]+"
    }

    final case class DuplicateCountryName(name: String) extends CountryError {
      override def message: String = s"Duplicate country name `$name`. Country name must be unique"
    }

    final case class InvalidCountryCode(code: String) extends CountryError {
      override def message: String =
        s"Invalid country code `$code`. Country code must match regular expression: [a-z]{2}"
    }

    final case class DuplicateCountryCode(code: String) extends CountryError {
      override def message: String = s"Duplicate country code `$code`. Country code must be unique"
    }

    final case class InvalidCountryContinent(continent: String) extends CountryError {
      override def message: String =
        s"Invalid country continent `$continent`. Country continent must be: Africa, Asia, Europe, Oceania, North America or South America"
    }

    final case class CountryForeignKey(id: Int) extends CountryError {
      override def message: String = s"Country with id `$id` can't be deleted because it's a part of foreign key"
    }
  }

}
