package external.gcs

import java.nio.charset.StandardCharsets
import java.time.ZonedDateTime

import akka.NotUsed
import akka.stream.scaladsl._
import com.google.cloud.storage.StorageOptions

import scala.concurrent.duration._

class GcsSink() {

  def apply[T: StorageBatchSerializer](
      bucketName: String,
      path: String,
      maxChunkSize: Int = 10000,
      groupedWithin: FiniteDuration = 20.seconds)(implicit namingPolicy: BlobNamingPolicy): Sink[T, NotUsed] = {

    // Instantiates a client
    val storage = StorageOptions.getDefaultInstance.getService
    // Creates the new bucket
    val bucket = storage.get(bucketName)

//    def dumpToGcsSink = Sink.foreach[(ZonedDateTime, Seq[T])] {
    def dumpToGcsSink =
      Flow[(ZonedDateTime, Seq[T])]
        .map {
          case (timestamp, grouped) =>
            //        val content = grouped.mkString("\n").getBytes(StandardCharsets.UTF_8)
            val content = grouped.mkString("\n").getBytes(StandardCharsets.UTF_8)

            println(s"lines: ${grouped.size}-------------")

            val serializer = implicitly[StorageBatchSerializer[T]]

            val blobName = namingPolicy.blobName(path, serializer.extension, timestamp)

            if (scala.util.Random.nextDouble() < 0.95) {
              println(s"creating to $blobName")
              bucket.create(blobName, serializer.encode(grouped), serializer.contentType)
            } else {
              println(s"exception!!!")
              throw new Exception("test exception!!!")
            }
        }
        .recover { case e => throw e }
        .toMat(Sink.ignore)(Keep.right)

    Flow[T]
      .groupedWithin(maxChunkSize, groupedWithin)
      .map { grouped =>
        val timestamp = ZonedDateTime.now()
        (timestamp, grouped)
      }
      .async
      .to {
        RestartSink.withBackoff(1.second, 5.second, 0.1) { () =>
          val sink = dumpToGcsSink
          sink
        }
      }
  }

}

object GcsSink {}
