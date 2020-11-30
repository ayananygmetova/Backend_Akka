package node.cluster

import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.util.Timeout
import node.Node
import node.Node.{ClusterMembers, MembersRes, NodeServiceKey, Pong, Ready, ResultFromText}
import scala.concurrent.duration.DurationInt

object ClusterManager {
  sealed trait ClusterMessage
  case class Found(replyTo: ActorRef[ClusterMembers]) extends ClusterMessage
  case class GetMembers(replyTo: ActorRef[ClusterMembers]) extends ClusterMessage
  case class GetMessage(text: String, replyTo: ActorRef[ResultFromText]) extends ClusterMessage
  case class ReadyManager(replyTo: ActorRef[Node.Command]) extends ClusterMessage

  def apply(nodeName: String): Behavior[ClusterMessage] = Behaviors.setup { ctx =>
    Behaviors.receiveMessage { message =>
      implicit val timeout: Timeout = 3.seconds
      message match {
        case GetMembers(replyTo) =>
          ctx.ask[Receptionist.Command,Receptionist.Listing](ctx.system.receptionist, Receptionist.Find(NodeServiceKey, _)){
            case util.Success(value) =>
              val list = value.serviceInstances(NodeServiceKey)
              var membersList = List[String]()
              for(i <- list) {
                membersList :+= i.toString()
                println(i + " MEMBER")
              }
              replyTo ! ClusterMembers(membersList)
              Found(replyTo)
          }
        case Found(_)=>
          println("Found")
        case GetMessage(text, replyTo) =>
          replyTo ! ResultFromText(nodeName + " got " + text + " replyTo " + replyTo, "someRes")
        case ReadyManager(replyTo) =>
          replyTo ! Ready(nodeName)
      }
      Behaviors.same
    }
  }
}
