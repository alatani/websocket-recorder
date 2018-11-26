package external

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.ActorSystem
import akka.Done
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ws._
import akka.http.scaladsl.settings.ClientConnectionSettings
import akka.util.ByteString

import scala.concurrent.{Future, Promise}

object WebSocketClientFlow {
  def main(args: Array[String]) = {
//    import system.dispatcher

    /*
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    val bitflyer = "wss://ws.lightstream.bitflyer.com/json-rpc"
    val bitmex = "wss://www.bitmex.com/realtime"

    val defaultSettings = ClientConnectionSettings(system)


    val bitflyerOutgoing =
      Source.single(
        TextMessage(
          """{"jsonrpc":"2.0", "method": "subscribe", "params": {"channel":"lightning_ticker_BTC_JPY"}}""".stripMargin)
      )

    val source = bitflyerOutgoing.concat(Source.maybe[TextMessage.Strict])

    val r = bitflyerOutgoing.concat(Source.maybe)
    val abc = Source.maybe.concat(bitflyerOutgoing)

    val sink: Sink[Message, Future[Done]] =
      Sink.foreach[Message] {
        case message: TextMessage.Strict =>
          println(message.text)
      }

    val aa = (bitflyerOutgoing concatMat Source.maybe[TextMessage])(Keep.right)
//    outgoing concat Sink.foreach(println)
//    outgoing concat sink
//    outgoing.concat(Sink.foreach[TextMessage](println))

    // using emit "one" and "two" and then keep the connection open
//    val flow: Flow[Message, Message, Promise[Option[Message]]] =
    val flow =
      Flow.fromSinkAndSourceMat(
        Sink.foreach(println), //Sink.foreach(println),
        aa //,Source.maybe[Message]
      )(Keep.right)
    println("------- 1")

//    val (upgradeResponse, promise) =
//      Http().singleWebSocketRequest(WebSocketRequest(bitflyer), flow)
     */

    println("waiting")
    Thread.sleep(1000 * 10)

    println("------- 2")
    // at some later time we want to disconnect
    // promise.success(None)
  }
}
