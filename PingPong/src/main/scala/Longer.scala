import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.coding.Coders
import akka.http.scaladsl.marshalling.ToResponseMarshaller
import akka.http.scaladsl.model.StatusCodes.MovedPermanently
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.unmarshalling.FromRequestUnmarshaller
import akka.pattern.ask
import akka.http.scaladsl.server.Route
import akka.util.Timeout

import scala.concurrent.{ExecutionContext, Future}
object Longer extends App {
  // types used by the API routes
  type Money = Double // only for demo purposes, don't try this at home!
  type TransactionResult = String

  case class User(name: String)

  case class Order(email: String, amount: Money)

  case class Update(order: Order)

  case class OrderItem(i: Int, os: Option[String], s: String)

  // marshalling would usually be derived automatically using libraries
  implicit val orderUM: FromRequestUnmarshaller[Order] = ???
  implicit val orderM: ToResponseMarshaller[Order] = ???
  implicit val orderSeqM: ToResponseMarshaller[Seq[Order]] = ???
  implicit val timeout: Timeout = ??? // for actor asks
  implicit val ec: ExecutionContext = ???
  implicit val sys: ActorSystem = ???

  // backend entry points
  def myAuthenticator: Authenticator[User] = ???

  def retrieveOrdersFromDB: Future[Seq[Order]] = ???

  def myDbActor: ActorRef = ???

  def processOrderRequest(id: Int, complete: Order => Unit): Unit = ???

  lazy val binding = Http().newServerAt("localhost", 8080).bind(topLevelRoute)
  // ...

  lazy val topLevelRoute: Route =
  // provide top-level path structure here but delegate functionality to subroutes for readability
    concat(
      path("orders")(ordersRoute),
      // extract URI path element as Int
      pathPrefix("order" / IntNumber)(orderRoute),
      pathPrefix("documentation")(documentationRoute),
      path("oldApi" / Remaining) { pathRest =>
        redirect("http://oldapi.example.com/" + pathRest, MovedPermanently)
      }
    )

  // For bigger routes, these sub-routes can be moved to separate files
  lazy val ordersRoute: Route =
    authenticateBasic(realm = "admin area", myAuthenticator) { user =>
      concat(
        get {
          encodeResponseWith(Coders.Deflate) {
            complete {
              // unpack future and marshal custom object with in-scope marshaller
              retrieveOrdersFromDB
            }
          }
        },
        post {
          // decompress gzipped or deflated requests if required
          decodeRequest {
            // unmarshal with in-scope unmarshaller
            entity(as[Order]) { order =>
              complete {
                // ... write order to DB
                "Order received"
              }
            }
          }
        }
      )
    }

  def orderRoute(orderId: Int): Route =
    concat(
      pathEnd {
        concat(
          put {
            // form extraction from multipart or www-url-encoded forms
            formFields("email", "total".as[Money]).as(Order) { order =>
              complete {
                // complete with serialized Future result
                (myDbActor ? Update(order)).mapTo[TransactionResult]
              }
            }
          },
          get {
            // debugging helper
            logRequest("GET-ORDER") {
              // use in-scope marshaller to create completer function
              completeWith(instanceOf[Order]) { completer =>
                // custom
                processOrderRequest(orderId, completer)
              }
            }
          })
      },
      path("items") {
        get {
          // parameters to case class extraction
          parameters("size".as[Int], "color".optional, "dangerous".withDefault("no"))
            .as(OrderItem) { orderItem =>
              // ... route using case class instance created from
              // required and optional query parameters
            }
        }
      })

  lazy val documentationRoute: Route =
  // optionally compresses the response with Gzip or Deflate
  // if the client accepts compressed responses
    encodeResponse {
      // serve up static content from a JAR resource
      getFromResourceDirectory("docs")
    }
}