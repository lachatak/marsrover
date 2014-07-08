package org.kaloz.excercise.marsrover

import akka.actor.ActorSystem
import akka.testkit.{TestActorRef, TestKit, TestProbe}
import org.kaloz.excercise.marsrover.MarsRover.Position
import org.kaloz.excercise.marsrover.Plateau.{Ack, Collusion, GotLost}
import org.kaloz.excercise.marsrover.api.Facing._
import org.kaloz.excercise.marsrover.api.{PlateauConfiguration, RoverPosition}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class PlateauTest extends TestKit(ActorSystem("PlateauTest"))
with WordSpecLike
with Matchers
with BeforeAndAfterAll {

  override def afterAll() {
    system.shutdown()
  }

  "Plateau" should {
    "track rover positions" in new scope {

      rover1.send(plateau, Position(RoverPosition(1, 2, E), rover1.ref))
      rover2.send(plateau, Position(RoverPosition(2, 3, E), rover2.ref))

      rover1.expectMsg(Ack)
      rover2.expectMsg(Ack)

      plateau.underlyingActor.roverPositions.size should equal(2)

      plateau.underlyingActor.roverPositions(rover1.ref) should equal(RoverPosition(1, 2, E))
      plateau.underlyingActor.roverPositions(rover2.ref) should equal(RoverPosition(2, 3, E))
    }

    "report collusion if rovers standing on the same area" in new scope {

      rover1.send(plateau, Position(RoverPosition(1, 2, E), rover1.ref))
      rover2.send(plateau, Position(RoverPosition(1, 2, E), rover2.ref))

      rover1.expectMsg(Ack)
      rover1.expectMsg(Collusion)
      rover2.expectMsg(Collusion)
    }

    "report lost if the rover gets outside of the plateau" in new scope {

      rover1.send(plateau, Position(RoverPosition(6, 5, E), rover1.ref))

      rover1.expectMsg(GotLost)
    }
  }

  private trait scope {
    val plateau = TestActorRef(new Plateau(PlateauConfiguration(5, 5)))
    val rover1 = TestProbe()
    val rover2 = TestProbe()
  }

}
