package websocket_recorder

import java.time.ZonedDateTime

import akka.actor.ActorSystem
import akka.http.scaladsl.model.ws._
import akka.stream.ActorMaterializer
import websocket_recorder.gcs.{GcsSink, TimestampedMessage}

trait WebSocketModules {
  implicit def system: ActorSystem
  implicit def materializer: ActorMaterializer
}
object WebSocketModulesImpl {
  implicit lazy val system: ActorSystem = ActorSystem()
  implicit lazy val materializer: ActorMaterializer = ActorMaterializer()
}

object WebsocketRecorder {
  def main(args: Array[String]) = {
    import WebSocketModulesImpl._

    val websocketSource = RetryWebSocketSource("wss://example.com")
      .collect {
        case m: TextMessage.Strict =>
          TimestampedMessage(ZonedDateTime.now, m.text)
      }

    val gcsSink = new GcsSink().apply[String]("test-bucket", "somewhere/to/save")

    websocketSource.to(gcsSink)

    ()
  }
}
