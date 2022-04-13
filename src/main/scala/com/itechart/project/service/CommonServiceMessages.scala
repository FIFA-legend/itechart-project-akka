package com.itechart.project.service

import com.itechart.project.service.domain_errors.DomainError

object CommonServiceMessages {

  trait ErrorWrapper {
    def errors: List[DomainError]
  }

  object Requests {
    case object GetAllEntities
    case class GetEntityByT[T](criteria: T)
    case class AddOneEntity[T](entity: T)
    case class UpdateEntity[T](entity: T)
    case class RemoveEntity[T](criteria: T)
  }

  object Responses {
    case class OneFoundEntity[T](maybeEntity: Option[T])
    case class OneEntityAdded[T](entity: T)
    case class ValidationErrors(wrapper: ErrorWrapper)
    case object UpdateCompleted
    case object UpdateFailed
    case object RemoveCompleted
    case object RemoveFailed
    case object InternalServerError
  }

}
