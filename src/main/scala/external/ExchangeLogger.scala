package external

import java.time.ZonedDateTime

import akka.actor.ActorSystem
import akka.http.scaladsl.model.ws._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import akka.{Done, NotUsed}
import domain.model.{Channel, Exchange, ExchangeRawMessage}
import external.gcs.TimestampedMessage
import external.websocket.RetryWebSocketSource
import io.circe.{CursorOp, DecodingFailure}

import scala.concurrent.Future

trait ExchangeSource {
  type Payload
  def apply(): Source[ExchangeRawMessage[Payload], NotUsed]
}

class BitMexSource()(implicit system: ActorSystem) extends ExchangeSource {
  import io.circe.parser._

  val bitmex = "wss://www.bitmex.com/realtime?subscribe=liquidation,quote,trade"

  type Payload = io.circe.Json
  def apply(): Source[ExchangeRawMessage.Json, NotUsed] =
    RetryWebSocketSource(bitmex)
      .collect { case m: TextMessage.Strict => parse(m.text) }
      .map {
        case Right(json) => { println(json); json }
//        case Right(json) => json
        case Left(e) => throw e
      }
      .map { json =>
        json.hcursor.downField("table").as[String].map { channel =>
          ExchangeRawMessage.ofNow(Exchange.BitMex, Channel(channel), json)
        }
      }
      .collect {
        case Right(m) => m
      }
//      .map {
//        case Right(m) => m
//        case Left(e)  => throw e
//      }
}

class BitFlyerSource()(implicit system: ActorSystem) extends ExchangeSource {
  import io.circe.parser._

  val bitflyer = "wss://ws.lightstream.bitflyer.com/json-rpc"

  type Payload = io.circe.Json
  def apply(): Source[ExchangeRawMessage.Json, NotUsed] = {
    RetryWebSocketSource(
      bitflyer,
      Source.apply {
        scala.collection.immutable.Iterable((for {
//          channel <- Seq("ticker", "board", "executions", "board_snapshot")
          channel <- Seq("ticker", "board", "executions")
          //          cur <- Seq("FX_BTC_JPY", "BTC_JPY")
          cur <- Seq("FX_BTC_JPY")
        } yield {
          TextMessage(
            s"""{"jsonrpc":"2.0", "method": "subscribe", "params": {"channel":"lightning_${channel}_${cur}"}}""")
        }): _*)
      }
    ).collect { case m: TextMessage.Strict => parse(m.text) }
      .map {
        case Right(json) => json
        case Left(e)     => throw e
      }
      .map { json =>
        (for {
//          innerJson <- json.hcursor.downField("params").downField("message").as[io.circe.Json]
          channel <- json.hcursor.downField("params").downField("channel").as[String]
        } yield ExchangeRawMessage.ofNow(Exchange.BitFlyer, Channel(channel), json)) match {
          case Right(message) => message
          case Left(e)        => throw e
        }
      }
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

object ExchangeLogger {
  def main(args: Array[String]) = {

    import WebSocketModulesImpl._

    val sink: Sink[ExchangeRawMessage.Json, Future[Done]] =
      Sink.foreach[ExchangeRawMessage.Json] { msg =>
        println(msg.payload.noSpaces)
      }

    val mexSource = new BitMexSource()
    val bfSource = new BitFlyerSource()

    //bfSource(sink)
    //mexSource().to(sink).run()
    //bfSource().to(sink).run()
    //testSource().to(sink).run()

    val countupString = Source
      .fromIterator { () =>
        Iterator
          .iterate(1) { i =>
            Thread.sleep(3)
            i + 1
          }
          .take(1000)
      }
      .map(i => f"$i%04d")
      .map(i => TimestampedMessage[String](ZonedDateTime.now, i))

//    val gcsSink = new GcsSink().apply[String]("tsubaki", "rawlog/test2", 10)
//    countupString.to(gcsSink).run()

//    bfSource().to(sink)
    mexSource().recover { case e => throw e }.to(sink).run()

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
