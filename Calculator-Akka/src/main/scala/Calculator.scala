import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.Behavior

object Calculator {

  import UserInteraction._

  sealed trait CalculatorState
  case object AccumulateFirst extends CalculatorState
  case object AccumulateSecond extends CalculatorState

  def apply(): Behavior[Accumulate] = {
    process(AccumulateFirst, new StringBuilder, new StringBuilder, '+')
  }

  private def process(state: CalculatorState, first: StringBuilder,
                      second: StringBuilder, operator: Char): Behavior[Accumulate] =
    Behaviors.receive { (context, message) =>
      if (state == AccumulateFirst) {
        if (message.sign.isDigit) {
          first.append(message.sign)
          process(state, first, second, operator)
        }
        else
          process(AccumulateSecond, first, second, message.sign)
      }
      else if (state == AccumulateSecond) {
        if (message.sign.isDigit) {
          second.append(message.sign)
          process(state, first, second, operator)
        }
        else {
          val computation = context.spawn(Computation(), "computation")
          computation ! Compute(first.toString(), second.toString(), operator, message.replyTo)
          Behaviors.same
        }
      }
      else
        Behaviors.same
    }
}
