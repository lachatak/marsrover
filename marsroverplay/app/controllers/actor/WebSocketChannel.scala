package controllers.actor

import akka.actor.{Actor, ActorLogging, Props}
import org.kaloz.excercise.marsrover.api._
import play.api.libs.iteratee.Concurrent
import play.api.libs.json._

case class Send(data: String)

object WebSocketChannel {
  def props(channel: Concurrent.Channel[String]): Props = Props(new WebSocketChannel(channel))
}

class WebSocketChannel(channel: Concurrent.Channel[String]) extends Actor with ActorLogging {


  implicit object DisplayStateWriter extends Writes[DisplayState] with DefaultWrites {
    def writes(displayState: DisplayState) = Json.obj(
      "state" -> Json.arr(displayState.state.foldLeft(List.empty[Json.JsValueWrapper])((arr, state) => Json.obj(
        "name" -> Json.toJson(state._1),
        "x" -> Json.toJson(state._2.head.x.intValue()),
        "y" -> Json.toJson(state._2.head.y.intValue()),
        "facing" -> Json.toJson(state._2.head.facing.toString)
      ) :: arr): _*))
  }

  implicit object ClosePositionsWriter extends Writes[DisplayFinalPositions] with DefaultWrites {
    def writes(displayFinalPositions: DisplayFinalPositions) = Json.obj(
      "final" -> Json.arr(displayFinalPositions.displayState.state.foldLeft(List.empty[Json.JsValueWrapper])((arr, state) => Json.obj(
        "name" -> Json.toJson(state._1),
        "x" -> Json.toJson(state._2.head.x.intValue()),
        "y" -> Json.toJson(state._2.head.y.intValue()),
        "facing" -> Json.toJson(state._2.head.facing.toString)
      ) :: arr): _*))
  }

  implicit object PlateauConfigurationWriter extends Writes[PlateauConfiguration] with DefaultWrites {
    def writes(configuration: PlateauConfiguration) = Json.obj(
      "config" -> Json.obj(
        "x" -> Json.toJson(configuration.x.intValue()),
        "y" -> Json.toJson(configuration.y.intValue())
      ))
  }

  implicit object CollusionWriter extends Writes[Collusion] with DefaultWrites {
    def writes(collusion: Collusion) = Json.obj(
      "collusions" -> Json.arr(collusion.collusions.foldLeft(List.empty[Json.JsValueWrapper])((arr, position) => Json.obj(
        "x" -> Json.toJson(position._1.intValue()),
        "y" -> Json.toJson(position._2.intValue())
      ) :: arr): _*))
  }

  def receive = {
    case Send(data) =>
      log.info("Data to push {} ", data)
      channel.push(data)
    case position@DisplayState(_) =>
      log.info("State to push {} ", position)

      val positions = position.state.values.map(v => (v.head.x, v.head.y))
      val duplicatesItem = positions groupBy { x => x} filter { case (_, lst) => lst.size > 1} keys

      if (duplicatesItem.nonEmpty) {
        channel.push(Json.stringify(Json.toJson(Collusion(duplicatesItem))))
      }

      channel.push(Json.stringify(Json.toJson(position)))
    case config@PlateauConfiguration(_, _) =>
      log.info("Config to push {} ", config)
      channel.push(Json.stringify(Json.toJson(config)))
    case position@DisplayFinalPositions(_) =>
      log.info("Close positions to push {} ", position)
      channel.push(Json.stringify(Json.toJson(position)))
  }

  case class Collusion(collusions: Iterable[(Integer, Integer)])

}