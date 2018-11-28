package external

import akka.Done
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ws._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._

import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}

sealed trait Exchange {
  def name: String
}
object Exchanges {

  case object BitFlyer extends Exchange {
    def name = "bitFlyer"
  }

}

trait StreamApiReader[E <: Exchange] {}

object TailFromWebSocket {
  def apply(uri: Uri)(implicit system: ActorSystem, materializer: ActorMaterializer): TailFromWebSocket = {
    new TailFromWebSocket(uri, Source.empty)
  }
}

class TailFromWebSocket private (uri: Uri, firstRequest: Source[Message, _])(
    implicit system: ActorSystem,
    materializer: ActorMaterializer
) {

  import akka.http.scaladsl.model.Uri

  def startsWith(firstRequest: Source[Message, _]) = new TailFromWebSocket(uri, firstRequest)

  def into(sink: Sink[Message, Future[Done]]): Unit = {
    attempt(uri, firstRequest, sink)
  }

  private def attempt(uri: Uri, firstRequest: Source[Message, _], sink: Sink[Message, Future[Done]])(
      attemptCount: Int): Unit = {
    import system.dispatcher

    val flow = Flow.fromSinkAndSourceMat(
      sink,
      (firstRequest concatMat Source.maybe[Message])(Keep.right)
    )(Keep.right)

    val request = WebSocketRequest(uri)

    val (upgradeResponse, promise): (Future[WebSocketUpgradeResponse], Promise[Option[Message]]) =
      Http().singleWebSocketRequest(request, flow)

    upgradeResponse.onComplete {
      case Failure(exception) => {
        println(exception.getMessage)
        attempt(uri, firstRequest, sink)(attemptCount + 1)
      }
      case Success(value) => println(s"(attempt = $attemptCount)successfully conneced to $uri ")
    }

    flow.alsoTo(
      Sink.onComplete {
        case Failure(exception) => {
          println(exception.getMessage)
          attempt(uri, firstRequest, sink)(attemptCount + 1)
        }
        case Success(done) => {
          println("connection closed?")
          attempt(uri, firstRequest, sink)(attemptCount + 1)
        }
      }
    )
  }

}

class BitMexReader()(
    implicit
    system: ActorSystem,
    materializer: ActorMaterializer
) {
  val bitmex = "wss://www.bitmex.com/realtime?subscribe=liquidation,quote,trade"

  def apply(sink: Sink[Message, Future[Done]]): Unit = {
    TailFromWebSocket(bitmex).into(sink)
  }
}

class BitFlyerReader()(
    implicit
    system: ActorSystem,
    materializer: ActorMaterializer
) {

  val bitflyer = "wss://ws.lightstream.bitflyer.com/json-rpc"

  def apply(sink: Sink[Message, Future[Done]]): Unit = {
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

//    bfReader(sink)
//    mexReader(sink)
    bfReader(sink)

  }
}
