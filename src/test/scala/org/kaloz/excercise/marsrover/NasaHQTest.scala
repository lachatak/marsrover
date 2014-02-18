package org.kaloz.excercise.marsrover

import org.specs2.mutable.Specification
import org.specs2.specification.AllExpectations
import org.specs2.mock.Mockito
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import akka.actor.{ActorRef, ActorSystem, Props, Actor}
import akka.testkit.{TestActorRef, TestProbe}
import org.kaloz.excercise.marsrover.NasaHQ.StartExpedition

@RunWith(classOf[JUnitRunner])
class NasaHQTest extends Specification with AllExpectations with Mockito {

  implicit val system = ActorSystem("PaymentSystemTest")

  "A NasaHQ" should {
    "record a successful open and advance" in {

      val probe = TestProbe()

      trait TestDisplayProvider extends DisplayProvider{
        this:Actor =>
        implicit val system = context.system
        val display = probe.ref
      }


      val actor = system.actorOf(Props(new NasaHQ with TestDisplayProvider))

      val NasaHQ = TestActorRef[TestNasaHQ]


      NasaHQ ! StartExpedition

      success
    }
  }
}

class TestNasaHQ extends NasaHQ with TestDisplayProvider
