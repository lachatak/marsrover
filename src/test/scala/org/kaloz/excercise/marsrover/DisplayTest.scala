package org.kaloz.excercise.marsrover

import akka.actor._
import akka.testkit._
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import org.kaloz.excercise.marsrover.Display.{PositionsDisplayed, RegisterPosition, ShowPositions}
import Facing._
import scala.concurrent.duration._

class DisplayTest extends TestKit(ActorSystem("DisplayTest"))
with WordSpecLike
with BeforeAndAfterAll
with ImplicitSender {

  override def afterAll() {
    system.shutdown()
  }

  val display = system.actorOf(Props(new DisplayTest), "Display")

  "Display" should {

    "print the result" in new scope {
      display ! RegisterPosition(rover1.ref, RoverPosition(1, 2, E))
      display ! RegisterPosition(rover2.ref, RoverPosition(2, 3, E))

      EventFilter.info(start = "Last known position", occurrences = 2) intercept {
        display.tell(ShowPositions, sender.ref)
      }

      sender.expectMsg(PositionsDisplayed)
    }

    "print the result despite the fact that it was restarted" in new scope {
      display ! RegisterPosition(rover1.ref, RoverPosition(1, 2, E))
      display ! RegisterPosition(rover1.ref, RoverPosition(1, 3, E))
      display ! RegisterPosition(rover2.ref, RoverPosition(2, 3, W))
      display ! RegisterPosition(rover2.ref, RoverPosition(2, 2, W))

      display ! Exception

      display.tell(ShowPositions, sender.ref)
      sender.expectMsg(PositionsDisplayed)
    }
  }

  private trait scope {
    val rover1 = TestProbe()
    val rover2 = TestProbe()
    val sender = TestProbe()
  }

  private class DisplayTest extends Display {
    override val scheduleSnapshot = false
    override val scheduleFailure = false
    override val initialDelayForSnapshot = 0 second
    override val scheduledSnapshots = 10 second
  }

}


