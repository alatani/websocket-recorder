package external.websocket

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.ws.{Message, WebSocketRequest}
import akka.stream.scaladsl.{Keep, RestartSource, Source}

import scala.concurrent.duration._

object RetryWebSocketSource {
  def apply(uri: Uri, firstRequest: Source[Message, NotUsed] = Source.empty[Message])(
      implicit system: ActorSystem): Source[Message, NotUsed] = {

    RestartSource
      .withBackoff(1.second, 60.second, randomFactor = 0.1) { () =>
        val source = (firstRequest concatMat Source.maybe[Message])(Keep.right)
        val request = WebSocketRequest(uri)
        println(s"sending request to $uri")
        source.via(Http().webSocketClientFlow(request))
      }
  }
}
