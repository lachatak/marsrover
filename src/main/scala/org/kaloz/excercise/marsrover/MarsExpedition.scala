package org.kaloz.excercise.marsrover

import akka.actor._

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
  val plateau = system.actorOf(Plateau.props(configuration.definePlateau), name = "plateau")
  val actorFactory = (actorFactory: ActorRefFactory, props: Props, name: String) => actorFactory.actorOf(props, name)
  val nasaHQ = system.actorOf(NasaHQ.props(actorFactory), name = "NasaHQ")

  def startExpedition {
    nasaHQ ! StartExpedition(configuration.roverConfigurations)
  }
}
