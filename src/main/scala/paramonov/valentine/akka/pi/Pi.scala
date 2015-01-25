package paramonov.valentine.akka.pi

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.routing.FromConfig

import scala.concurrent.duration._

object Pi extends App {
  calculate(numberOfWorkers = 4, numberOfElements = 10000, numberOfMessages = 10000)

  sealed trait PiMessage

  case object Calculate extends PiMessage

  case class Work(start: Int, numberOfElements: Int) extends PiMessage

  case class Result(value: Double) extends PiMessage

  case class PiApproximation(pi: Double, duration: Duration)

  class Worker extends Actor {
    def calculatePiFor(start: Int, numberOfElements: Int): Double =
      (start until (start + numberOfElements)).foldLeft(0d)((acc, i) => acc + (1 - (i % 2d) * 2) / (2 * i + 1))

    def receive = {
      case Work(start, numberOfElements) =>
        sender ! Result(calculatePiFor(start, numberOfElements))
    }
  }

  class Master(numberOfWorkers: Int, numberOfMessages: Int, numberOfElements: Int, listener: ActorRef) extends Actor {
    var pi: Double = _
    var numberOfResults: Int = _
    val start: Long = System.currentTimeMillis()

    val workerRouter = context.actorOf(FromConfig.props(Props[Worker]), name = "workerRouter")

    def receive = {
      case Calculate =>
        (0 until numberOfMessages).map(i => workerRouter ! Work(i * numberOfElements, numberOfElements))
      case Result(value) =>
        pi += value
        numberOfResults += 1
        if (numberOfResults == numberOfMessages) {
          listener ! PiApproximation(4 * pi, duration = (System.currentTimeMillis() - start).millis)
          context.stop(self)
        }
    }
  }

  class Listener extends Actor {
    def receive = {
      case PiApproximation(pi, duration) =>
        println("\n\tPi approximation: \t\t%s\n\tCalculation time: \t%s".format(pi, duration))
        context.system.shutdown()
    }
  }

  def calculate(numberOfWorkers: Int, numberOfElements: Int, numberOfMessages: Int) = {
    val system = ActorSystem("PiSystem")
    val listener = system.actorOf(Props[Listener], name = "listener")
    val master = system.actorOf(Props(new Master(numberOfWorkers, numberOfMessages, numberOfElements, listener)), name = "master")
    master ! Calculate
  }
}