package node

import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Scheduler}
import akka.cluster.typed.Cluster
import akka.util.Timeout
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityRef, EntityTypeKey}
import node.cluster.{ClusterListener, ClusterManager}
import service.WordCountService

import scala.concurrent.duration.DurationInt

object Node {
  val NodeServiceKey: ServiceKey[Command] = ServiceKey[Command]("node-service-key")
  sealed trait Command
  final case class FindNumberPing(n: Int, replyTo: ActorRef[Pong]) extends Command
  final case class Pong(nodeName: String, res: BigInt) extends Command
  final case class MembersRes(members: List[Any]) extends Command
  final case class GetClusterMembers(replyTo: ActorRef[ClusterMembers]) extends Command
  final case class ClusterMembers(members: List[String]) extends Command
  final case class CountWordFrom(text: String, replyTo: ActorRef[WordsCounterResult]) extends Command
  final case class ResultFromText(nodeName: String, res: String) extends Command
  final case class WordsCounterResult(nodeName: String, res: Map[String, Int]) extends Command
  case class Found(list: Set[ActorRef[Command]]) extends Command
  case class AskToBeReady(node: ActorRef[Command]) extends Command
  case class Ready(nodeName: String) extends Command
  case class Result(nodeName: String, res: String) extends Command

  def apply(): Behavior[Command] = {
    Behaviors.setup[Command] { context =>
      implicit def system: ActorSystem[Nothing] = context.system
      implicit def scheduler: Scheduler = context.system.scheduler
      implicit lazy val timeout: Timeout = Timeout(5.seconds)
      val sharding = ClusterSharding(system)
      val TypeKey = EntityTypeKey[WordCountService.Ping]("Counter")
      val shardRegion: ActorRef[ShardingEnvelope[WordCountService.Ping]] =
        sharding.init(Entity(TypeKey)(createBehavior = entityContext => WordCountService(entityContext.entityId)))
      context.system.receptionist ! Receptionist.Register(NodeServiceKey, context.self)
      val cluster = Cluster(system)
      println(s"Starting node with roles: ${Cluster(system)}")
      println(cluster.manager + " Master node is ready.", cluster.selfMember.address)
      val cm = context.spawnAnonymous(ClusterManager("cluster_manager"))
      val listener = context.spawnAnonymous(ClusterListener("cluster_listener"))
      val countService = context.spawnAnonymous(WordCountService("word_countService_node?"))
      Behaviors.receiveMessage { message => {
        message match {
          case Pong(nodeName, res) =>
            println(s"nodeName:$nodeName result:$res")
          case GetClusterMembers(replyTo) =>
            cm ! ClusterManager.GetMembers(replyTo)
          case MembersRes(members) =>
            println(members)
          case CountWordFrom(text, replyTo) =>
            println(replyTo + " send text " + context.self + " " + cm + " send withGetMessage")
            shardRegion ! ShardingEnvelope("counter-1", WordCountService.Ping(text, replyTo))
            shardRegion ! ShardingEnvelope("counter-2", WordCountService.Ping(text, replyTo))
//            countService ! WordCountService.Ping(text, replyTo)
          case Result(nodeName, res) =>
            println(nodeName + ": " + res)
          case Found(list) =>
            println("MEMBERS-TEST2 " + list)
          case AskToBeReady(node) =>
            cm ! ClusterManager.ReadyManager(node)
          case Ready(nodeName) =>
            println(nodeName + " is ready!")
        }
        Behaviors.same
      }
      }
    }
  }
}
