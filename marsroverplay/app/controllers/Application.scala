package controllers

import controllers.actor.ClientRequestHandlerActor.RegisterWebSocket
import controllers.actor.WebSocketChannel
import play.api.libs.iteratee.{Concurrent, Iteratee}
import play.api.mvc._
import play.libs.Akka

import scala.concurrent.ExecutionContext.Implicits.global

object Application extends Controller {

  def index = Action {
    Ok(views.html.marsrover.render)
  }

  def startdisplay = WebSocket.using[String] { request =>
    val (out, channel) = Concurrent.broadcast[String]
    val webSocketChannel = Akka.system.actorOf(WebSocketChannel.props(channel), name = "WebSocketChannel-" + request.id.toString)
    Global.clientRequestHandler ! RegisterWebSocket(webSocketChannel)
    val in = Iteratee.foreach[String] { msg => if (msg == "GET POOL") channel.push("POOL:" + webSocketChannel.path.name);}
    (in, out)
  }
}