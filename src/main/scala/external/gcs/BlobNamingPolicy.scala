package external.gcs

import java.time.ZonedDateTime

import external.ReaderSession

trait BlobNamingPolicy {
  def blobName(basePath: String, extension: String, now: ZonedDateTime): String
}

object BlobNamingPolicy {
  implicit val instance: BlobNamingPolicy = new ReaderSessionBlobNamingPolicy
}

class ReaderSessionBlobNamingPolicy extends BlobNamingPolicy {

  def blobName(basePath: String, extension: String, now: ZonedDateTime): String = {
    val msecOfSecond = now.getNano / 1000 / 1000
    val session = ReaderSession().value

    val nExtension = if (extension.startsWith(".")) extension.drop(1) else extension

    Seq(
      basePath,
      f"year=${now.getYear}%04d",
      f"month=${now.getMonthValue}%02d",
      f"day=${now.getDayOfMonth}%02d",
      f"hour=${now.getHour}%02d",
      f"min=${now.getMinute}%02d",
      f"sec=${now.getSecond}%02d.${msecOfSecond}%03d.${session}.$nExtension",
    ).mkString("/")
  }
}
