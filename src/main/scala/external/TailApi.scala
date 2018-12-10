package external

import akka.Done
import akka.http.scaladsl.model.ws._
import akka.stream.scaladsl._
import domain.model.ExchangeRawMessage

import scala.concurrent.Future

object TailApi {
  def main(args: Array[String]) = {

    import WebSocketModulesImpl._

    val sink: Sink[ExchangeRawMessage.Json, Future[Done]] =
      Sink.foreach { message =>
        import io.circe.syntax._
        println(s"TextMessage.Strict: ${message.asJson.noSpaces}")
      }

    val mexSource = new BitMexSource()
    val bfSource = new BitFlyerSource()

    val idd = Flow.apply[Message]

//    val res = mexSource()
//    mexSource().recover { case e => throw e }.to(sink).run()
    bfSource().recover { case e => throw e }.to(sink).run()
//    mexSource().to(sink).run()
    //bfSource().to(sink).run()
//    testSource().to(sink).run()

    ()
  }
}
