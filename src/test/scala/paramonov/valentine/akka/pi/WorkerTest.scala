package paramonov.valentine.akka.pi

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.WordSpecLike
import paramonov.valentine.akka.pi.Pi.{Result, Work, Worker}

class WorkerTest(_system: ActorSystem) extends TestKit(_system) with WordSpecLike with ImplicitSender {
  def this() = this(ActorSystem("PiSystem"))

  "A Worker" must {
    "calculate pi from 0 to 3" in {
      val worker = system.actorOf(Props[Worker])
      worker ! Work(0, 3)
      expectMsg(Result(76d / 105))
    }
  }
}
