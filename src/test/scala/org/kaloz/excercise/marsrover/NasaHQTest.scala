package org.kaloz.excercise.marsrover

import org.specs2.mutable.Specification
import org.specs2.specification.{Scope, AllExpectations}
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import akka.actor._
import akka.testkit.{TestActorRef, TestProbe}
import scala.Predef._
import org.kaloz.excercise.marsrover.Facing._
import org.kaloz.excercise.marsrover.Action._
import org.kaloz.excercise.marsrover.NasaHQ.StartExpedition
import org.kaloz.excercise.marsrover.Display.ShowPositions
import org.kaloz.excercise.marsrover.MarsRoverController.Disaster

@RunWith(classOf[JUnitRunner])
class NasaHQTest extends Specification with AllExpectations {

  implicit val system = ActorSystem("MarsExpeditionTest")

  "NasaHQ" should {
    "start be able to start an expedition" in new scope {

      NasaHQ ! StartExpedition(List(RoverConfiguration(RoverPosition(1, 2, N), List(L)), RoverConfiguration(RoverPosition(3, 3, E), List(M))))

      controllers.size mustEqual 2
    }

    "initiate showing positions at the end of simulation" in new scope {

      NasaHQ ! StartExpedition(List(RoverConfiguration(RoverPosition(1, 2, N), List(L)), RoverConfiguration(RoverPosition(3, 3, E), List(M))))

      poisonAllControllers

      display.expectMsg(ShowPositions)
    }

    "not accept new StartExpedition if we have a running one" in new scope {

      NasaHQ ! StartExpedition(List(RoverConfiguration(RoverPosition(1, 2, N), List(L)), RoverConfiguration(RoverPosition(3, 3, E), List(M))))
      NasaHQ ! StartExpedition(List(RoverConfiguration(RoverPosition(1, 2, N), List(L)), RoverConfiguration(RoverPosition(3, 3, E), List(M))))
      NasaHQ ! StartExpedition(List(RoverConfiguration(RoverPosition(1, 2, N), List(L)), RoverConfiguration(RoverPosition(3, 3, E), List(M))))

      controllers.size mustEqual 2
    }

    "not register disaster without expedition" in new scope {

      NasaHQ ! Disaster(TestProbe().ref)

      NasaHQ.underlyingActor.disaster mustEqual false
    }

    "be able to register disaster during expedition" in new scope {

      NasaHQ ! StartExpedition(List(RoverConfiguration(RoverPosition(1, 2, N), List(L)), RoverConfiguration(RoverPosition(3, 3, E), List(M))))
      NasaHQ ! Disaster(TestProbe().ref)

      NasaHQ.underlyingActor.disaster mustEqual true
    }
  }

  private trait scope extends Scope {
    val display = TestProbe()
    var controllers = List.empty[TestProbe]

    implicit val actorFactory = (actorFactory: ActorRefFactory, props: Props, name: String) => props.actorClass() match {
      case d if d.isAssignableFrom(classOf[Display]) => display.ref
      case c if c.isAssignableFrom(classOf[MarsRoverController]) =>
        val controller = TestProbe()
        controllers = controller :: controllers
        controller.ref
    }
    val NasaHQ = TestActorRef(new NasaHQ)
    
    def poisonAllControllers = controllers.foreach(_.ref ! PoisonPill)
  }

  step(system.shutdown)
}
