package org.kaloz.excercise.marsrover

import akka.actor._
import akka.testkit.{TestKit, TestActorRef, TestProbe}
import scala.Predef._
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import org.kaloz.excercise.marsrover.MarsRoverController.{RoverAction, StartRover}
import org.kaloz.excercise.marsrover.MarsRover.{Position, DeployRover}
import org.kaloz.excercise.marsrover.Facing._
import org.kaloz.excercise.marsrover.Action._
import org.kaloz.excercise.marsrover.Display.RegisterPosition

class MarsRoverControllerTest extends TestKit(ActorSystem("MarsRoverControllerTest"))
with WordSpecLike
with BeforeAndAfterAll {

  override def afterAll() {
    system.shutdown()
  }

  "MarsRoverController" should {
    "be able to start its rover" in new scope {

      marsRoverController ! StartRover
      marsRover.expectMsg(DeployRover(RoverPosition(1, 2, E)))
    }

    "send the next action to the rover if it has reported back the position" in new scope {

      marsRover.send(marsRoverController, Position(RoverPosition(1, 3, E)))
      display.expectMsg(RegisterPosition(marsRover.ref, RoverPosition(1, 3, E)))
      marsRover.expectMsg(RoverAction(M))
    }

    "stop if there is no more action in the queue" in {

      val marsRover = TestProbe()
      val display = TestProbe()
      val terminationWatch = TestProbe()

      val marsRoverController = TestActorRef(new MarsRoverController(RoverConfiguration(RoverPosition(1, 2, E), Nil), marsRover.ref, display.ref))
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

  private trait scope {

    val marsRover = TestProbe()
    val display = TestProbe()

    val marsRoverController = TestActorRef(new MarsRoverController(RoverConfiguration(RoverPosition(1, 2, E), List(M)), marsRover.ref, display.ref))
  }

}