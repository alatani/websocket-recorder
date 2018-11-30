package external

import java.io.FileNotFoundException
import java.time.ZonedDateTime

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

object RetryContext {
  private val ResetSeconds = 3
  def run(action: RetryContext => Unit)(implicit actorSystem: ActorSystem): Unit = {
    action(
      RetryContext(action, 0, java.time.LocalDateTime.now())
    )
  }
}

case class RetryContext private (action: RetryContext => Unit, count: Int = 0, attemptedAt: java.time.LocalDateTime) {
  import scala.concurrent.duration._

  private def toReset(now: java.time.LocalDateTime) = now.isAfter(attemptedAt.plusSeconds(RetryContext.ResetSeconds))

  private def next = {
    val now = java.time.LocalDateTime.now
    if (toReset(now)) {
      RetryContext(action, 0, java.time.LocalDateTime.now)
    } else {
      RetryContext(action, count + 1, now)
    }
  }

  private def waitDuration(now: java.time.LocalDateTime) = {
    val waitMsec = if (toReset(now)) {
      1000
    } else {
      (1000 * Math.pow(2, count)).toInt
    }
    println(s"waiting ${waitMsec} ms")
    waitMsec.milliseconds
  }

  def retry()(implicit actorSystem: ActorSystem): Unit = {
    import actorSystem.dispatcher

    actorSystem.scheduler.scheduleOnce(
      waitDuration(java.time.LocalDateTime.now)
    ) {
      action(this.next)
    }
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
    RetryContext.run(
      attempt(uri, firstRequest, sink)
    )
  }

  private def attempt(uri: Uri, firstRequest: Source[Message, _], sink: Sink[Message, Future[Done]])(
      retryCtx: RetryContext): Unit = {
    import system.dispatcher

    val source = (firstRequest concatMat Source.maybe[Message])(Keep.right)

    val flow = Flow
      .fromSinkAndSourceMat(
        sink,
        source
      )(Keep.right)

    val retryingFlow = Flow[Message]
      .alsoTo(
        Sink.onComplete {
          case Failure(exception) => {
            println(exception.getMessage)
            retryCtx.retry()
          }
          case Success(done) => {
            println("connection closed?")
            retryCtx.retry()
          }
        }
      )
      .viaMat(flow)(Keep.right)

    val request = WebSocketRequest(uri)

    println(s"sending request to $uri")
    val (upgradeResponse, promise): (Future[WebSocketUpgradeResponse], Promise[Option[Message]]) =
      Http().singleWebSocketRequest(request, retryingFlow)

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

    TailFromWebSocket(bitflyer).startsWith(subscribe).into(sink)
  }

}
class TestReader()(
    implicit
    system: ActorSystem,
    materializer: ActorMaterializer
) {

  val bitflyer = "ws://127.0.0.1:4001"

  def apply(sink: Sink[Message, Future[Done]]): Unit = {
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
    val testReader = new TestReader()

    //    bfReader(sink)
    //    mexReader(sink)
    //    bfReader(sink)
    testReader(sink)

  }
}
