case class Contact(id:String, name: String, address: String, city: String, phone: String)
case class CreateContact(name: String, address: String, city: String, phone: String)
case class UpdateContact(name: Option[String], address: Option[String], city: Option[String], phone: Option[String])
