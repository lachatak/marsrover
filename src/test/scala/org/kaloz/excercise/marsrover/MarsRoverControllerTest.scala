package org.kaloz.excercise.marsrover

import org.specs2.specification.Scope
import akka.actor._
import akka.testkit.{TestKit, TestActorRef, TestProbe}
import scala.Predef._
import org.kaloz.excercise.marsrover.Facing._
import org.kaloz.excercise.marsrover.Action._
import org.kaloz.excercise.marsrover.MarsRoverController.{RoverAction, DeployRover}
import org.kaloz.excercise.marsrover.MarsRover.Position
import org.kaloz.excercise.marsrover.Display.RegisterPosition
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

class MarsRoverControllerTest extends TestKit(ActorSystem("MarsRoverControllerTest"))
with WordSpecLike
with BeforeAndAfterAll {

  override def afterAll() {
    system.shutdown()
  }

  "MarsRoverController" should {
    "create its own rover" in new scope {
      assert(marsRoverController.underlyingActor.marsRover == marsRover.ref)
    }

    "send the next action to the rover if it has reported back the position" in new scope {
      marsRover.expectMsg(DeployRover)
      marsRover.send(marsRoverController, Position(RoverPosition(1, 3, E)))
      display.expectMsg(RegisterPosition(marsRover.ref, RoverPosition(1, 3, E)))
      marsRover.expectMsg(RoverAction(M))
    }

    "stop if there is no more action in the queue" in {
      val marsRover = TestProbe()
      val display = TestProbe()
      val terminationWatch = TestProbe()

      implicit val actorFactory = (actorFactory: ActorRefFactory, props: Props, name: String) => marsRover.ref

      val marsRoverController = TestActorRef(new MarsRoverController(RoverConfiguration(RoverPosition(1, 2, E), List.empty), display.ref))
      terminationWatch.watch(marsRoverController)
      marsRover.send(marsRoverController, Position(RoverPosition(1, 3, E)))
      terminationWatch.expectTerminated(marsRoverController)
    }

    "stop if there is a problem with a rover" in new scope {
      val terminationWatch = TestProbe()
      terminationWatch.watch(marsRoverController)
      marsRover.ref ! PoisonPill
      terminationWatch.expectTerminated(marsRoverController)
    }
  }

  private trait scope extends Scope {
    val marsRover = TestProbe()
    val display = TestProbe()

    implicit val actorFactory = (actorFactory: ActorRefFactory, props: Props, name: String) => marsRover.ref

    val marsRoverController = TestActorRef(new MarsRoverController(RoverConfiguration(RoverPosition(1, 2, E), List(M)), display.ref))
  }

}