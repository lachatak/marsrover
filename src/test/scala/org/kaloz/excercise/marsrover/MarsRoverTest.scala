package org.kaloz.excercise.marsrover

import akka.actor._
import akka.testkit.{TestKit, TestActorRef, TestProbe}
import scala.Predef._
import org.scalatest.{Matchers, BeforeAndAfterAll, WordSpecLike}
import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator.Subscribe
import org.kaloz.excercise.marsrover.NasaHQ.{RoverRegistered, RegisterRover}
import org.kaloz.excercise.marsrover.Facing._
import org.kaloz.excercise.marsrover.Action._
import org.kaloz.excercise.marsrover.MarsRover._
import org.kaloz.excercise.marsrover.MarsRoverController.{RoverDeployed, RoverAction}
import org.kaloz.excercise.marsrover.Plateau.{Ack, GotLost, Collusion}

class MarsRoverTest extends TestKit(ActorSystem("MarsRoverTest"))
with WordSpecLike
with Matchers
with BeforeAndAfterAll {

  override def afterAll() {
    system.shutdown()
  }

  "MarsRover" should {
    "be able to register to the cluster" in new scope {

      val mediator = DistributedPubSubExtension(system).mediator
      mediator ! Subscribe("registration", testActor)

      expectMsg(RegisterRover(marsRover))
    }

    "be able to handle registration" in new scope {

      nasaHQ.send(marsRover, RoverRegistered)

      marsRover.underlyingActor.roverState should be(RoverState.REGISTERED)
    }

    "be deployed" in new scope with registered with plateau {
      this: scope =>

      marsRoverController.send(marsRover, DeployRover(RoverPosition(1, 2, E)))

      marsRover.underlyingActor.marsRoverController should equal(marsRoverController.ref)
      marsRover.underlyingActor.actualRoverPosition should equal(RoverPosition(1, 2, E))

      expectMsg(Position(RoverPosition(1, 2, E), marsRover))
    }

    "react on action command" in new scope with registered with plateau {
      this: scope =>

      marsRoverController.send(marsRover, DeployRover(RoverPosition(1, 2, E)))
      expectMsg(Position(RoverPosition(1, 2, E), marsRover))

      marsRoverController.send(marsRover, RoverAction(M))
      expectMsg(Position(RoverPosition(2, 2, E), marsRover))
    }

    "stop if collusion happens" in new scope with registered with plateau {
      this: scope =>

      terminationWatch.watch(marsRover)

      marsRoverController.send(marsRover, DeployRover(RoverPosition(1, 2, E)))
      expectMsg(Position(RoverPosition(1, 2, E), marsRover))

      marsRover ! Collusion
      terminationWatch.expectTerminated(marsRover)
    }

    "stop if got lost" in new scope with registered with plateau {
      this: scope =>

      terminationWatch.watch(marsRover)

      marsRoverController.send(marsRover, DeployRover(RoverPosition(1, 2, E)))
      expectMsg(Position(RoverPosition(1, 2, E), marsRover))

      marsRover ! GotLost
      terminationWatch.expectTerminated(marsRover)
    }

    "inform the controller if the deployment was successful" in new scope with registered with plateau {
      this: scope =>

      marsRoverController.send(marsRover, DeployRover(RoverPosition(1, 2, E)))
      expectMsg(Position(RoverPosition(1, 2, E), marsRover))

      marsRover.underlyingActor.marsRoverController = marsRoverController.ref
      marsRover ! Ack
      marsRoverController.expectMsg(RoverDeployed)
    }

    "inform the controller if the action was successful" in new scope with registered with plateau {
      this: scope =>

      marsRoverController.send(marsRover, DeployRover(RoverPosition(1, 2, E)))
      expectMsg(Position(RoverPosition(1, 2, E), marsRover))

      marsRover.underlyingActor.marsRoverController = marsRoverController.ref
      marsRover.underlyingActor.roverState = RoverState.READY

      marsRover ! Ack
      marsRoverController.expectMsg(Position(RoverPosition(1, 2, E)))
    }
  }

  private trait scope {

    val terminationWatch = TestProbe()
    val marsRoverController = TestProbe()
    val nasaHQ = TestProbe()
    val marsRover = TestActorRef(new TestMarsRover)
  }

  private trait registered {
    this: scope =>

    nasaHQ.send(marsRover, RoverRegistered)
  }

  private trait plateau {
    this: scope =>

    val mediator = DistributedPubSubExtension(system).mediator
    mediator ! Subscribe("position", testActor)
  }

}

class TestMarsRover extends MarsRover {

  import scala.concurrent.duration._

  override val movementSpeed = 0 millis
  override val turningSpeed = 0 millis
}