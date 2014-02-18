package org.kaloz.excercise.marsrover

import akka.actor
import akka.actor.{ActorSystem, Props}
import org.kaloz.excercise.marsrover.{DisplayProvider, NasaHQ}

object MarsExpedition extends MarsExpeditionConfigurationParser {

  def main(args: Array[String]) {
    val input = scala.io.Source.fromInputStream(getClass.getClassLoader.getResourceAsStream(if (args.length == 1) args(0) else "input.txt")).mkString
    parseAll(marsExpeditionConfiguration, input) match {
      case Success(mec, _) => new MarsExpedition(mec).startExpedition
      case x => println(x)
    }
  }
}

class MarsExpedition(configuration: MarsExpeditionConfiguration) {

  import org.kaloz.excercise.marsrover.NasaHQ._

  val system = ActorSystem("MarsExpedition")
  val plateau = system.actorOf(Props(classOf[Plateau], configuration.definePlateau), name = "plateau")
  val NasaHQ = system.actorOf(Props[LiveNasaHQ], name = "NasaHQ")

  def startExpedition {
    NasaHQ ! StartExpedition(configuration.roverConfigurations)
  }
}

class LiveNasaHQ extends NasaHQ with ProductionDisplayProvider
