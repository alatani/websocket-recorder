package external

import akka.actor.ActorSystem
import akka.http.scaladsl.model.ws._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import akka.{Done, NotUsed}
import external.gcs.GcsSink
import external.websocket.RetryWebSocketSource

import scala.concurrent.Future

sealed trait Exchange {
  def name: String
}

object Exchanges {
  case object BitFlyer extends Exchange {
    def name = "bitFlyer"
  }
}

trait ExchangeSource {
  def apply(): Source[Message, NotUsed]
}

class BitMexSource()(implicit system: ActorSystem) extends ExchangeSource {
  val bitmex = "wss://www.bitmex.com/realtime?subscribe=liquidation,quote,trade"

  def apply(): Source[Message, NotUsed] = RetryWebSocketSource(bitmex)
}

class BitFlyerSource()(implicit system: ActorSystem) extends ExchangeSource {
  val bitflyer = "wss://ws.lightstream.bitflyer.com/json-rpc"

  def apply(): Source[Message, NotUsed] = {
    RetryWebSocketSource(
      bitflyer,
      Source.apply {
        scala.collection.immutable.Iterable(
          TextMessage("""{"jsonrpc":"2.0", "method": "subscribe", "params": {"channel":"lightning_ticker_BTC_JPY"}}"""),
        )
      }
    )

  }

}
class TestSource()(
    implicit system: ActorSystem
) extends ExchangeSource {
  val localhost = "ws://127.0.0.1:4001"

  def apply(): Source[Message, NotUsed] = {
    RetryWebSocketSource(
      localhost,
      Source.single(
        TextMessage(
          """{"jsonrpc":"2.0", "method": "subscribe", "params": {"channel":"lightning_ticker_BTC_JPY"}}""".stripMargin)
      )
    )
  }
}

trait WebSocketModules {
  implicit def system: ActorSystem
  implicit def materializer: ActorMaterializer
}
object WebSocketModulesImpl {
  implicit lazy val system: ActorSystem = ActorSystem()
  implicit lazy val materializer: ActorMaterializer = ActorMaterializer()
}

object DataCollector {
  def main(args: Array[String]) = {

    import WebSocketModulesImpl._

    val sink: Sink[Message, Future[Done]] =
      Sink.foreach[Message] {
        case message: TextMessage.Strict =>
          println(s"TextMessage.Strict: ${message.text}")
        case message => {

          println(s"Other: $message")
        }
      }

    val mexSource = new BitMexSource()
    val bfSource = new BitFlyerSource()
    val testSource = new TestSource()

    val idd = Flow.apply[Message]

    //    bfSource(sink)
//    mexSource().to(sink).run()
    //bfSource().to(sink).run()
//    testSource().to(sink).run()

    val res = (1 to 10000).map { i =>
      Thread.sleep(1)
      s"line: $i"
    }
    val countupString = Source(res)

//    val gcsSink = GcsSink("pandora-log").store("tsubaki/test-log/")(1000, 5.second)

    val gcsSink = new GcsSink().apply[String]("pandora-log", "tsubaki/test2", 100)

    countupString.to(gcsSink).run()

//    RunnableGraph
//      .fromGraph(GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
//        import GraphDSL.Implicits._
//        import akka.stream.ClosedShape
//
//        bfSource() ~> sink
//        mexSource() ~> sink
//
//        ClosedShape
//      })
//      .run()

    ()
  }
}
