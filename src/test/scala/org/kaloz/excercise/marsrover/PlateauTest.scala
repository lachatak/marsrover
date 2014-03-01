package org.kaloz.excercise.marsrover

import akka.actor.ActorSystem
import org.kaloz.excercise.marsrover.Facing._
import akka.testkit.{TestKit, TestProbe, TestActorRef}
import org.kaloz.excercise.marsrover.MarsRover.Position
import org.kaloz.excercise.marsrover.Plateau.{Collusion, GotLost, Ack}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

class PlateauTest extends TestKit(ActorSystem("PlateauTest"))
with WordSpecLike
with BeforeAndAfterAll {

  "Plateau" should {
    "track rover positions" in new scope {

      rover1.send(plateau, Position(RoverPosition(1, 2, E), rover1.ref))
      rover2.send(plateau, Position(RoverPosition(2, 3, E), rover2.ref))

      rover1.expectMsg(Ack)
      rover2.expectMsg(Ack)

      assert(plateau.underlyingActor.roverPositions.size == 2)

      assert(plateau.underlyingActor.roverPositions(rover1.ref) == RoverPosition(1, 2, E))
      assert(plateau.underlyingActor.roverPositions(rover2.ref) == RoverPosition(2, 3, E))
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
