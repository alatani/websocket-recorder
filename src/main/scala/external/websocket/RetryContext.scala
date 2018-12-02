package external.websocket

import akka.actor.ActorSystem
import com.typesafe.scalalogging.LazyLogging

case class RetryContext private (action: RetryContext => Unit, count: Int = 0, attemptedAt: java.time.LocalDateTime)
    extends LazyLogging {
  import scala.concurrent.duration._

  private def toReset(now: java.time.LocalDateTime) = now.isAfter(attemptedAt.plusSeconds(RetryContext.ResetSeconds))

  private def next = {
    // TODO: other retry plicies
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
    logger.debug(s"waiting ${waitMsec} ms")
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

object RetryContext {
  private val ResetSeconds = 3
  def run(action: RetryContext => Unit)(implicit actorSystem: ActorSystem): Unit = {
    action(
      RetryContext(action, 0, java.time.LocalDateTime.now())
    )
  }
}
