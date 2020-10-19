import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, Behavior }

object UserInteraction {

  sealed trait UserInteractionEvent
  final case class ReadInput(brain: ActorRef[Accumulate]) extends UserInteractionEvent
  final case class PrintResult(result: Double) extends UserInteractionEvent

  final case class Accumulate(sign: Char, replyTo: ActorRef[UserInteractionEvent])
  final case class Compute(first: String, second: String, operator: Char, replyTo: ActorRef[UserInteractionEvent])

  def apply(): Behavior[UserInteractionEvent] =
    Behaviors.receive { (context, message) =>
      message match {
        case ReadInput(calculator) => {
          var input = ""
          while(input!="=") {
            input = scala.io.StdIn.readLine()
            calculator ! Accumulate(input.charAt(0), context.self)
          }
          Behaviors.same
        }
        case PrintResult(result) => {
          println(result)
          Behaviors.stopped
        }
      }
    }
}
