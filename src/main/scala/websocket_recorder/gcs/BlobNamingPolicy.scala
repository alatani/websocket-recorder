package websocket_recorder.gcs

import java.time.ZonedDateTime

import websocket_recorder.ReaderSession

trait BlobNamingPolicy {
  def blobName(basePath: String,
               extension: String,
               now: ZonedDateTime,
               digest: String = BlobNamingPolicy.randomDigest): String
}

object BlobNamingPolicy {
  implicit val instance: BlobNamingPolicy = new ReaderSessionBlobNamingPolicy

  def randomDigest: String = scala.util.Random.alphanumeric.take(4).mkString
}

class ReaderSessionBlobNamingPolicy extends BlobNamingPolicy {

  def blobName(basePath: String,
               extension: String,
               now: ZonedDateTime,
               digest: String = BlobNamingPolicy.randomDigest): String = {
    val session = ReaderSession().value
    val nExtension = if (extension.startsWith(".")) extension.drop(1) else extension

    Seq(
      basePath,
      f"year=${now.getYear}%04d",
      f"month=${now.getMonthValue}%02d",
      f"day=${now.getDayOfMonth}%02d",
      f"hour=${now.getHour}%02d",
      f"min=${now.getMinute}%02d",
      f"sec=${now.getSecond}%02d.sess=${session}.digest=${digest}.$nExtension",
    ).mkString("/")
  }
}
