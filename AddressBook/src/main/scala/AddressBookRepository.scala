import java.util.UUID

import AddressBook.AddressBookNotFound

import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}

trait AddressBook {
  def all(): Future[Vector[Contact]]
  def create(createContact:CreateContact): Future[Contact]
  def update(id: String, updateAddressBook: UpdateContact): Future[Contact]
  def delete(id: String): Future[Vector[Contact]]
  def get(id: String): Future[Contact]
}

object AddressBook{
  final case class AddressBookNotFound(id: String) extends Exception(s"Address Book with id $id not found.")
}


class InMemoryAddressBookRepository(addressBook:Seq[Contact] =Seq.empty )(implicit  ex:ExecutionContext) extends AddressBook {
  private var contacts: Vector[Contact] = addressBook.toVector

  override def all(): Future[Vector[Contact]] = Future.successful(contacts)
  override def create(createContact: CreateContact): Future[Contact] = Future.successful {
    val contact = Contact(
      id = UUID.randomUUID().toString,
      name = createContact.name,
      address = createContact.address,
      city = createContact.city,
      phone = createContact.phone
    )
    contacts = contacts :+ contact
    contact
  }

  override def update(id: String, updateAddressBook: UpdateContact): Future[Contact] = {
    contacts.find(_.id == id) match {
      case Some(foundAddress) =>
        val newAddress = updateHelper(foundAddress, updateAddressBook)
        contacts = contacts.map(t=> if (t.id == id) newAddress else t)
        Future.successful(newAddress)
      case None => Future.failed(AddressBookNotFound(id))
    }
  }
  private def updateHelper(book: Contact, updateBook: UpdateContact): Contact={
    val t1 = updateBook.name.map(name => book.copy(name = name)).getOrElse(book)
    val t2 = updateBook.address.map(address => book.copy(address = address)).getOrElse(t1)
    val t3 = updateBook.city.map(city => book.copy(city = city)).getOrElse(t2)
    val t4 = updateBook.phone.map(phone => book.copy(phone = phone)).getOrElse(t3)
    t4
  }

  override def delete(id: String): Future[Vector[Contact]] = {
    contacts = contacts.filterNot(item => item.id==id)
    Future.successful(contacts)
  }

  override def get(id: String): Future[Contact] = {
    val todo = contacts.filter(item => item.id==id).head
    Future.successful(todo)
  }
}