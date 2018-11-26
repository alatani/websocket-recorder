package external

import java.util.concurrent.atomic.AtomicInteger

import scala.util.{Try, Success, Failure}
import akka.actor.ActorSystem
import akka.Done
import akka.http.javadsl.ConnectionContext
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ws._
import akka.http.scaladsl.settings.ClientConnectionSettings
import akka.util.ByteString

import scala.concurrent.{Future, Promise}

sealed trait Exchange {
  def name: String
}
object Exchanges {

  case object BitFlyer extends Exchange {
    def name = "bitFlyer"
  }

}

trait StreamApiReader[E <: Exchange] {}

class TailFromWebSocket()(
    implicit system: ActorSystem,
    materializer: ActorMaterializer
) {

  import akka.http.scaladsl.model.{HttpHeader, Uri}
  def apply(uri: Uri, firstRequest: Source[Message, _])(sink: Sink[Message, Future[Done]]): Promise[Option[Message]] = {
    import system.dispatcher

    Source.maybe

    val flow = Flow.fromSinkAndSourceMat(
      sink,
      (firstRequest concatMat Source.maybe[Message])(Keep.right)
    )(Keep.right)

    val request = WebSocketRequest(uri)

//    val hoge = Http().webSocketClientFlow(request).joinMat()
    val (upgradeResponse, closed) = Http().singleWebSocketRequest(request, flow)

    flow.alsoTo(
      Sink.onComplete { done =>
        done
        ()
      }
    )

//    Http().singleWebSocketRequest()

//    ClientConnectionSettings()
//    upgradeResponse.map{ r =>
//
//    }

//    val connected = upgradeResponse.map { upgrade =>
//      // just like a regular http request we can access response status which is available via upgrade.response.status
//      // status code 101 (Switching Protocols) indicates that server support WebSockets
//      if (upgrade.response.status == StatusCodes.SwitchingProtocols) {
//        Done
//      } else {
//        throw new RuntimeException(s"Connection failed: ${upgrade.response.status}")
//      }
//    }
    closed

    //TODO: here error handling
    closed
  }
}

class BitMexReader()(
    implicit
    tailFromWebSocket: TailFromWebSocket,
    system: ActorSystem,
    materializer: ActorMaterializer
) {
  val bitmex = "wss://www.bitmex.com/realtime?subscribe=liquidation,quote,trade"

  def apply(sink: Sink[Message, Future[Done]]): Promise[Option[Message]] = {
    tailFromWebSocket(bitmex, Source.empty)(sink)
  }
}

class BitFlyerReader()(
    implicit
    tailFromWebSocket: TailFromWebSocket,
    system: ActorSystem,
    materializer: ActorMaterializer
) {

  val bitflyer = "wss://ws.lightstream.bitflyer.com/json-rpc"

  def apply(sink: Sink[Message, Future[Done]]): Promise[Option[Message]] = {
    val subscribe =
      Source.single(
        TextMessage(
          """{"jsonrpc":"2.0", "method": "subscribe", "params": {"channel":"lightning_ticker_BTC_JPY"}}""".stripMargin)
      )
    /*
    val flow =
      Flow.fromSinkAndSourceMat(
        Sink.foreach[Message](println), //Sink.foreach(println),
        (bitflyerOutgoing concatMat Source.maybe[Message])(Keep.right)
      )(Keep.right)

    val (upgradeResponse, promise) = Http().singleWebSocketRequest(WebSocketRequest(bitflyer), flow)
    //TODO: here error handling
    promise
     */
    tailFromWebSocket(bitflyer, subscribe)(sink)
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

  implicit lazy val tailFromWebSocket: TailFromWebSocket = new TailFromWebSocket()
}

object DataCollector {
  def main(args: Array[String]) = {
//    implicit val system = ActorSystem()
//    implicit val materializer = ActorMaterializer()

    import WebSocketModulesImpl._

    val sink: Sink[Message, Future[Done]] =
      Sink.foreach[Message] {
        case message: TextMessage.Strict =>
          println(s"TextMessage.Strict: ${message.text}")
        case message =>
          println(s"Other: $message")
      }

    val mexReader = new BitMexReader()
    val bfReader = new BitFlyerReader()

//    val promise = bfReader(sink)
//    val promise = mexReader(sink)
    val _ = bfReader(sink)

  }
}
