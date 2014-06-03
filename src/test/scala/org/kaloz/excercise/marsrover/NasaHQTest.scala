package org.kaloz.excercise.marsrover

import akka.actor._
import akka.testkit.{EventFilter, TestKit, TestActorRef, TestProbe}
import scala.Predef._
import org.scalatest.{Matchers, OneInstancePerTest, BeforeAndAfterAll, WordSpecLike}
import org.kaloz.excercise.marsrover.NasaHQ.{RoverRegistered, RegisterRover}
import org.kaloz.excercise.marsrover.MarsRoverController.{Disaster, StartRover}
import org.kaloz.excercise.marsrover.Display.ShowPositions
import org.kaloz.excercise.marsrover.Action._
import org.kaloz.excercise.marsrover.Facing._

class NasaHQTest extends TestKit(ActorSystem("NasaHQTest"))
with WordSpecLike
with Matchers
with BeforeAndAfterAll
with OneInstancePerTest {

  override def afterAll() {
    system.shutdown()
  }

  "NasaHQ" should {
    "subscribe registration topic" in {

      implicit val actorFactory = (actorFactory: ActorRefFactory, props: Props, name: String) => TestProbe().ref

      EventFilter.info(start = "Subscribed to registration topic!", occurrences = 1) intercept {
        TestActorRef(new NasaHQ(Nil))
      }
    }

    "be able to create a diplay actor" in new scope {

      nasaHQ.underlyingActor.display should equal(display.ref)
    }

    "accept rover registrations after it has been started" in new scope {

      EventFilter.info(start = "Expedition still needs 1", occurrences = 1) intercept {
        nasaHQ ! RegisterRover(rover1.ref)
      }

      rover1.expectMsg(RoverRegistered)
    }

    "be able to start registered rovers" in new scope {

      EventFilter.info(start = "Expedition still needs 1", occurrences = 1) intercept {
        nasaHQ ! RegisterRover(rover1.ref)
      }

      EventFilter.info(start = "Expedition is ready to be kicked!", occurrences = 1) intercept {
        nasaHQ ! RegisterRover(rover2.ref)
      }

      rover1.expectMsg(RoverRegistered)
      rover2.expectMsg(RoverRegistered)

      controllers.size should equal(2)

      controllers.foreach(_.expectMsg(StartRover))

    }

    "initiate showing positions at the end of simulation" in new scope with registration {
      this: scope =>

      poisonAllControllers

      display.expectMsg(ShowPositions)
    }

    "print the successful result of the expedition" in new scope with registration {
      this: scope =>

      EventFilter.info(pattern = ".*successfully.*", occurrences = 1) intercept {
        poisonAllControllers
      }
    }

    "print failure result of the expedition" in new scope {

      nasaHQ ! RegisterRover(rover1.ref)
      nasaHQ ! RegisterRover(rover2.ref)

      nasaHQ ! Disaster(TestProbe().ref)

      EventFilter.info(pattern = ".*disaster.*", occurrences = 1) intercept {
        poisonAllControllers
      }
    }

    "not be able to register disaster without runnig expedition" in new scope {

      nasaHQ ! Disaster(TestProbe().ref)

      nasaHQ.underlyingActor.disaster should equal(false)
    }

    "be able to register disaster during expedition" in new scope with registration {
      this: scope =>

      nasaHQ ! Disaster(TestProbe().ref)

      nasaHQ.underlyingActor.disaster should equal(true)
    }
  }

  private trait scope {

    val display = TestProbe()
    val rover1 = TestProbe()
    val rover2 = TestProbe()
    var controllers = List.empty[TestProbe]

    implicit val actorFactory = (actorFactory: ActorRefFactory, props: Props, name: String) => props.actorClass() match {
      case d if d.isAssignableFrom(classOf[Display]) => display.ref
      case c if c.isAssignableFrom(classOf[MarsRoverController]) =>
        val controller = TestProbe()
        controllers = controller :: controllers
        controller.ref
    }
    val nasaHQ = TestActorRef(new NasaHQ(List(RoverConfiguration(RoverPosition(1, 2, N), List(L)), RoverConfiguration(RoverPosition(3, 3, E), List(M)))))

    def poisonAllControllers = controllers.foreach(_.ref ! PoisonPill)
  }

  private trait registration {
    this: scope =>

    nasaHQ ! RegisterRover(rover1.ref)
    nasaHQ ! RegisterRover(rover2.ref)
  }

}
