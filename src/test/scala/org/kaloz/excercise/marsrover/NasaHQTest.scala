package org.kaloz.excercise.marsrover

import org.specs2.mutable.Specification
import org.specs2.specification.AllExpectations
import org.specs2.mock.Mockito
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import akka.actor._
import akka.testkit.{TestActorRef, TestProbe}
import org.kaloz.excercise.marsrover.NasaHQ.StartExpedition
import org.kaloz.excercise.marsrover.NasaHQ.StartExpedition
import scala.Predef._
import org.kaloz.excercise.marsrover.NasaHQ.StartExpedition
import org.kaloz.excercise.marsrover.NasaHQ

@RunWith(classOf[JUnitRunner])
class NasaHQTest extends Specification with AllExpectations with Mockito {

  implicit val system = ActorSystem("PaymentSystemTest")

  "A NasaHQ" should {
    "record a successful open and advance" in {

      val probe = TestProbe()

      val displayActorFactory = (actorFactory:ActorRefFactory, props:Props, name:String) => probe.ref
      val NasaHQ = TestActorRef(new NasaHQ(displayActorFactory))

      NasaHQ ! StartExpedition

      success
    }
  }
}
