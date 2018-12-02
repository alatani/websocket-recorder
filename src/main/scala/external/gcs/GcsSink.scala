package external.gcs

import java.nio.charset.StandardCharsets

import akka.NotUsed
import akka.stream.scaladsl._
import com.google.cloud.storage.Bucket
import com.google.cloud.storage.BucketInfo
import com.google.cloud.storage.StorageOptions

class GcsSink(bucket: Bucket) {
  import scala.concurrent.duration._

  def store(path: String, contentType: String = "application/json")(
      maxChunkSize: Int = 10000,
      groupedWithin: FiniteDuration = 60.seconds): Sink[String, NotUsed] = {

    Flow[String].groupedWithin(10000, groupedWithin).to {
      Sink.foreach { grouped =>
//        val content = grouped.mkString("\n").getBytes(StandardCharsets.UTF_8)
        val content = grouped.mkString("\n").getBytes(StandardCharsets.UTF_8)

        println(content)
        println("-------------")
        //        bucket.create(path, content, contentType)
      }
    }

  }

}

object GcsSink {
  def apply(bucketName: String): GcsSink = {
    // Instantiates a client
    val storage = StorageOptions.getDefaultInstance.getService
    // Creates the new bucket
    val bucket = storage.create(BucketInfo.of(bucketName))

    new GcsSink(bucket)
  }
}
