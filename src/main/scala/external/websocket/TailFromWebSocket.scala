package external.websocket

import akka.Done
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.ws.{Message, WebSocketRequest, WebSocketUpgradeResponse}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}

import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}

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

object TailFromWebSocket {
  def apply(uri: Uri)(implicit system: ActorSystem, materializer: ActorMaterializer): TailFromWebSocket = {
    new TailFromWebSocket(uri, Source.empty)
  }
}
