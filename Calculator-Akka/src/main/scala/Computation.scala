import UserInteraction.{ Compute, PrintResult }
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

object Computation {

  def apply(): Behavior[Compute] = Behaviors.receive { (context, message) =>
    message.operator match {
      case '+' => {
        message.replyTo ! PrintResult(message.first.toInt + message.second.toInt)
        Behaviors.same
      }
      case '-' => {
        message.replyTo ! PrintResult(message.first.toInt - message.second.toInt)
        Behaviors.same
      }
      case '*' => {
        message.replyTo ! PrintResult(message.first.toInt * message.second.toInt)
        Behaviors.same
      }
      case '/' => {
        message.replyTo ! PrintResult(message.first.toDouble / message.second.toInt)
        Behaviors.same
      }
    }
  }
}
