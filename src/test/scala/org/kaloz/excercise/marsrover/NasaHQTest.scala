package org.kaloz.excercise.marsrover

import akka.actor._
import akka.testkit.{EventFilter, TestKit, TestActorRef, TestProbe}
import scala.Predef._
import org.kaloz.excercise.marsrover.Facing._
import org.kaloz.excercise.marsrover.Action._
import org.kaloz.excercise.marsrover.NasaHQ.StartExpedition
import org.kaloz.excercise.marsrover.Display.ShowPositions
import org.kaloz.excercise.marsrover.MarsRoverController.Disaster
import org.scalatest.{OneInstancePerTest, BeforeAndAfterAll, WordSpecLike}

class NasaHQTest extends TestKit(ActorSystem("NasaHQTest"))
with WordSpecLike
with BeforeAndAfterAll
with OneInstancePerTest {

  override def afterAll() {
    system.shutdown()
  }

  "NasaHQ" should {
    "start be able to start an expedition" in new scope {

      nasaHQ ! StartExpedition(List(RoverConfiguration(RoverPosition(1, 2, N), List(L)), RoverConfiguration(RoverPosition(3, 3, E), List(M))))

      assert(nasaHQ.underlyingActor.display == display.ref)
      assert(controllers.size == 2)
    }

    "initiate showing positions at the end of simulation" in new scope {

      nasaHQ ! StartExpedition(List(RoverConfiguration(RoverPosition(1, 2, N), List(L)), RoverConfiguration(RoverPosition(3, 3, E), List(M))))

      poisonAllControllers

      display.expectMsg(ShowPositions)
    }

    "print the successful result of the expedition" in new scope {
      nasaHQ ! StartExpedition(List(RoverConfiguration(RoverPosition(1, 2, N), List(L)), RoverConfiguration(RoverPosition(3, 3, E), List(M))))

      EventFilter.info(pattern = ".*successfully.*", occurrences = 1) intercept {
        poisonAllControllers
      }
    }

    "print failure result of the expedition" in new scope {
      nasaHQ ! StartExpedition(List(RoverConfiguration(RoverPosition(1, 2, N), List(L)), RoverConfiguration(RoverPosition(3, 3, E), List(M))))

      nasaHQ ! Disaster(TestProbe().ref)

      EventFilter.info(pattern = ".*disaster.*", occurrences = 1) intercept {
        poisonAllControllers
      }
    }

    "not accept new StartExpedition if we have a running one" in new scope {

      nasaHQ ! StartExpedition(List(RoverConfiguration(RoverPosition(1, 2, N), List(L)), RoverConfiguration(RoverPosition(3, 3, E), List(M))))

      EventFilter.warning(start="unhandled", occurrences = 1) intercept {
        nasaHQ ! StartExpedition(List(RoverConfiguration(RoverPosition(1, 2, N), List(L)), RoverConfiguration(RoverPosition(3, 3, E), List(M))))
      }

      assert(controllers.size == 2)
    }

    "not register disaster without expedition" in new scope {

      nasaHQ ! Disaster(TestProbe().ref)

      assert(nasaHQ.underlyingActor.disaster == false)
    }

    "be able to register disaster during expedition" in new scope {

      nasaHQ ! StartExpedition(List(RoverConfiguration(RoverPosition(1, 2, N), List(L)), RoverConfiguration(RoverPosition(3, 3, E), List(M))))
      nasaHQ ! Disaster(TestProbe().ref)

      assert(nasaHQ.underlyingActor.disaster == true)
    }
  }

  private trait scope {
    val display = TestProbe()
    var controllers = List.empty[TestProbe]

    implicit val actorFactory = (actorFactory: ActorRefFactory, props: Props, name: String) => props.actorClass() match {
      case d if d.isAssignableFrom(classOf[Display]) => display.ref
      case c if c.isAssignableFrom(classOf[MarsRoverController]) =>
        val controller = TestProbe()
        controllers = controller :: controllers
        controller.ref
    }
    val nasaHQ = TestActorRef(new NasaHQ)

    def poisonAllControllers = controllers.foreach(_.ref ! PoisonPill)
  }

}
