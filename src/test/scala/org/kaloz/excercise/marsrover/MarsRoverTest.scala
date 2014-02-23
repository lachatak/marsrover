package org.kaloz.excercise.marsrover

import akka.actor._
import akka.testkit.{TestKit, TestActorRef, TestProbe}
import scala.Predef._
import org.kaloz.excercise.marsrover.Facing._
import org.kaloz.excercise.marsrover.MarsRoverController.{RoverDeployed, DeployRover, RoverAction}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import org.specs2.specification.Scope
import org.kaloz.excercise.marsrover.MarsRover.Position
import org.kaloz.excercise.marsrover.Plateau.{Ack, GotLost, Collusion}

class MarsRoverTest extends TestKit(ActorSystem("MarsRoverTest"))
with WordSpecLike
with BeforeAndAfterAll {

  val testPlateau = TestActorRef(new TestPlateau, "plateau")

  override def afterAll() {
    system.shutdown()
  }

  "MarsRover" should {
    "be deployed" in new scope {
      marsRoverController.send(marsRover, DeployRover)
      testPlateau.underlyingActor.testPlateauProbe.expectMsg(Position(RoverPosition(1, 2, E)))
      assert(marsRover.underlyingActor.marsRoverController == marsRoverController.ref)
    }

    "react on action command" in new scope {

      import Action._

      marsRoverController.send(marsRover, RoverAction(M))
      testPlateau.underlyingActor.testPlateauProbe.expectMsg(Position(RoverPosition(2, 2, E)))
    }

    "stop if collusion happens" in new scope {
      val terminationWatch = TestProbe()
      terminationWatch.watch(marsRover)
      testPlateau.underlyingActor.testPlateauProbe.send(marsRover, Collusion)
      terminationWatch.expectTerminated(marsRover)
    }

    "stop if got lost" in new scope {
      val terminationWatch = TestProbe()
      terminationWatch.watch(marsRover)
      testPlateau.underlyingActor.testPlateauProbe.send(marsRover, GotLost)
      terminationWatch.expectTerminated(marsRover)
    }

    "inform the controller if the deployment was successful" in new scope {
      marsRover.underlyingActor.marsRoverController = marsRoverController.ref
      testPlateau.underlyingActor.testPlateauProbe.send(marsRover, Ack)
      marsRoverController.expectMsg(RoverDeployed)
    }

    "inform the controller if the action was successful" in new scope {
      marsRover.underlyingActor.marsRoverController = marsRoverController.ref
      marsRover.underlyingActor.roverState = RoverState.READY
      testPlateau.underlyingActor.testPlateauProbe.send(marsRover, Ack)
      marsRoverController.expectMsg(Position(RoverPosition(1, 2, E)))
    }
  }

  private trait scope extends Scope {
    val marsRoverController = TestProbe()
    val marsRover = TestActorRef(new MarsRover(RoverPosition(1, 2, E)))
  }

}

class TestPlateau extends Actor {

  var testPlateauProbe = TestProbe()(context.system)

  def receive = {
    case x => testPlateauProbe.ref forward x
  }
}