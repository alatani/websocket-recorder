package external

import akka.Done
import akka.actor.ActorSystem
import akka.http.scaladsl.model.ws._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import external.gcs.GcsSink
import external.websocket.TailFromWebSocket

import scala.concurrent.Future

sealed trait Exchange {
  def name: String
}
object Exchanges {
  case object BitFlyer extends Exchange {
    def name = "bitFlyer"
  }
}

class BitMexReader()(
    implicit
    system: ActorSystem,
    materializer: ActorMaterializer
) {
  val bitmex = "wss://www.bitmex.com/realtime?subscribe=liquidation,quote,trade"

  def apply(sink: Sink[Message, _]): Unit = {
    TailFromWebSocket(bitmex).into(sink)
  }
}

class BitFlyerReader()(
    implicit
    system: ActorSystem,
    materializer: ActorMaterializer
) {

  val bitflyer = "wss://ws.lightstream.bitflyer.com/json-rpc"

  def apply(sink: Sink[Message, _]): Unit = {
    val subscribe =
      Source.single(
        TextMessage(
          """{"jsonrpc":"2.0", "method": "subscribe", "params": {"channel":"lightning_ticker_BTC_JPY"}}""".stripMargin)
      )

    TailFromWebSocket(bitflyer).startsWith(subscribe).into(sink)
  }

}
class TestReader()(
    implicit
    system: ActorSystem,
    materializer: ActorMaterializer
) {

  val bitflyer = "ws://127.0.0.1:4001"

  def apply(sink: Sink[Message, _]): Unit = {
    val subscribe =
      Source.single(
        TextMessage(
          """{"jsonrpc":"2.0", "method": "subscribe", "params": {"channel":"lightning_ticker_BTC_JPY"}}""".stripMargin)
      )
//    TailFromWebSocket(bitflyer).startsWith(subscribe).into(sink)
    TailFromWebSocket(bitflyer).startsWith(subscribe).into(sink)
  }
}

trait WebSocketModules {
  implicit def system: ActorSystem
  implicit def materializer: ActorMaterializer

  implicit def tailFromWebSocket: TailFromWebSocket
}
object WebSocketModulesImpl {
  implicit lazy val system: ActorSystem = ActorSystem()
  implicit lazy val materializer: ActorMaterializer = ActorMaterializer()
}

object DataCollector {
  def main(args: Array[String]) = {

    import WebSocketModulesImpl._

    import scala.concurrent.duration._

    val sink: Sink[Message, Future[Done]] =
      Sink.foreach[Message] {
        case message: TextMessage.Strict =>
          println(s"TextMessage.Strict: ${message.text}")
        case message =>
          println(s"Other: $message")
      }

    val mexReader = new BitMexReader()
    val bfReader = new BitFlyerReader()
    val testReader = new TestReader()

    //    bfReader(sink)
    //    mexReader(sink)

//    import scala.con
    val gcsSink = GcsSink("pandora-log").store("tsubaki/test-log/")(1000, 5.second)

    val bfSink = Flow[Message]
      .collect {
        case text: TextMessage.Strict => text.text
      }
      .to(gcsSink)
//    bfReader(bfSink)

    testReader(sink)

  }
}
