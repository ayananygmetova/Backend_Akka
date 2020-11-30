package api

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem, Scheduler}
import akka.http.scaladsl.server.{Directives, Route}
import akka.util.Timeout
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import node.Node
import node.Node.{ClusterMembers, Found, Pong, ResultFromText, WordsCounterResult}
import node.cluster.ClusterManager.ClusterMessage
case class PostText(text: String)
trait Router {
  def route: Route
}

class NodeRouter(node:ActorRef[Node.Command])(implicit system: ActorSystem[_], ex:ExecutionContext)
  extends  Router
    with  Directives {
  implicit val timeout: Timeout = 5.seconds
  implicit val scheduler: Scheduler = system.scheduler
  override def route: Route =
    concat(
      pathPrefix("process") {
        path(IntNumber) { n =>
          get {
            val processFuture: Future[Pong] = node.ask(ref => Node.FindNumberPing(n, ref))(timeout,scheduler).mapTo[Pong]
            onSuccess(processFuture){response =>
              complete(response)
            }

          }
        }
      },
      path("members"){
        get{
          val processFuture: Future[ClusterMembers] = node.ask(ref => Node.GetClusterMembers(ref))(timeout,scheduler).mapTo[ClusterMembers]
          onSuccess(processFuture){ response =>
            complete(response)
          }
        }
      },
      path("counter"){
          post{
            entity(as[PostText]) { someText =>
            {
              val processFuture: Future[WordsCounterResult] = node.ask(ref => Node.CountWordFrom(someText.text, ref))(timeout,scheduler).mapTo[WordsCounterResult]
              onSuccess(processFuture){ response =>
                complete(response)
              }
            }
            }
          }
      }
    )
}




