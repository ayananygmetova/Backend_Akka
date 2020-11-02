
import akka.actor.typed.ActorSystem
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import  io.circe.generic.auto._
import akka.http.scaladsl.server.{Directives, Route}

import scala.concurrent.ExecutionContext

trait Router {
  def route: Route
}

class MyRouter(val addressBook: AddressBook)(implicit system: ActorSystem[_],  ex:ExecutionContext)
  extends  Router
    with  Directives
    with HealthCheckRoute
    with ValidatorDirectives
    with AddressBookDirectives
{

  def addressBookRoute = {
    concat(
    pathPrefix("address-book") {
        pathEndOrSingleSlash {
          concat(
            get {
              complete(addressBook.all())
            },
            post {
              entity(as[CreateContact]) { createContact =>
                validateWith(CreateContactValidator)(createContact) {
                  handleWithGeneric(addressBook.create(createContact)) { contact =>
                    complete(contact)
                  }
                }
              }
            }
          )
        }
    },
      pathPrefix("address-book" / Segment){ id =>
        concat(
        put{
          entity(as[UpdateContact]){ updateBook =>
            validateWith(UpdateContactValidator)(updateBook){
              handle(addressBook.update(id, updateBook)){
                case AddressBook.AddressBookNotFound(_)=>
                  ApiError.contactNotFound(id)
                case _ =>
                  ApiError.generic
              }{ book =>
              complete(book)
              }
            }
          }
        },
          delete{
            complete(addressBook.delete(id))
          },
          get{
            complete(addressBook.get(id))
          }
        )
      }
    )

  }

  override def route = {
    concat(
      healthCheck,
      addressBookRoute
    )
  }
}