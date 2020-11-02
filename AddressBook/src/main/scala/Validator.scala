
trait Validator[T] {
  def validate(t: T): Option[ApiError]
}

object CreateContactValidator extends Validator[CreateContact] {
  def validate(createContact: CreateContact): Option[ApiError] =
    if (createContact.name.isEmpty)
      Some(ApiError.emptyTitleField)
    else
      None
}
object UpdateContactValidator extends Validator[UpdateContact] {
  def validate(updateContact: UpdateContact): Option[ApiError] =
    if (updateContact.name.isEmpty)
      Some(ApiError.emptyTitleField)
    else
      None
}
