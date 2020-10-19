import akka.NotUsed
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Terminated}

object Main {

  def apply(): Behavior[NotUsed] =
    Behaviors.setup { context =>
      val userInteraction = context.spawn(UserInteraction(), "userinteraction")
      val calculator = context.spawn(Calculator(), "calculator")
      userInteraction ! UserInteraction.ReadInput(calculator)
      Behaviors.same
    }

  def main(args: Array[String]): Unit = {
    ActorSystem(Main(), "Main")
  }
}
