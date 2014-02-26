package org.kaloz.excercise.marsrover

import akka.actor._
import akka.testkit.{EventFilter, TestKit, TestActorRef, TestProbe}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import org.kaloz.excercise.marsrover.Display.{RegisterPosition, ShowPositions}
import Facing._

class DisplayTest extends TestKit(ActorSystem("DisplayTest"))
with WordSpecLike
with BeforeAndAfterAll {

  override def afterAll() {
    system.shutdown()
  }

  "Display" should {
    "register rover positions for rovers" in new scope {

      display ! RegisterPosition(rover1.ref, RoverPosition(1, 2, E))
      display ! RegisterPosition(rover2.ref, RoverPosition(2, 3, E))

      assert(display.underlyingActor.roverPositions.size == 2)

      assert(display.underlyingActor.roverPositions(rover1.ref) == RoverPosition(1, 2, E))
      assert(display.underlyingActor.roverPositions(rover2.ref) == RoverPosition(2, 3, E))
    }

    "print the result" in new scope {
      display ! RegisterPosition(rover1.ref, RoverPosition(1, 2, E))
      display ! RegisterPosition(rover2.ref, RoverPosition(2, 3, E))

      assert(display.underlyingActor.roverPositions.size == 2)

      EventFilter.info(start = "Last known position", occurrences = 2) intercept {
        display ! ShowPositions
      }
    }
  }

  private trait scope {
    val display = TestActorRef(new Display)
    val rover1 = TestProbe()
    val rover2 = TestProbe()
  }

}


