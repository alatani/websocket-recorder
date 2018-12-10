package external.gcs

import java.time.ZonedDateTime

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl._
import com.google.cloud.storage.{StorageException, StorageOptions}
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.Future
import scala.concurrent.duration._

case class TimestampedMessage[T](timestamp: ZonedDateTime, payload: T)

class GcsSink() extends LazyLogging {
  import akka.pattern.Patterns.after

  def backoffRetry(initialDuration: FiniteDuration, maxDuration: FiniteDuration)(gcsRequest: () => Future[Unit])(
      implicit actorSystem: ActorSystem): Future[Unit] = {

    def attempt(initialDuration: FiniteDuration, count: Int)(gcsRequest: () => Future[Unit]): Future[Unit] = {
      import actorSystem.dispatcher
      val base = 2.0
      gcsRequest().recoverWith {
        case e: StorageException =>
          val nextDuration: FiniteDuration =
            if (count == 0) initialDuration
            else {
              val poweredDuration = initialDuration * (Math.ceil(Math.pow(base, count)) min Int.MaxValue).toInt
              maxDuration min poweredDuration
            }
          logger.warn(s"${e.getLocalizedMessage} / ${e.getMessage}")
          logger.warn(s"retrying in ${nextDuration.toSeconds} seconds")
          after(nextDuration, actorSystem.scheduler, actorSystem.dispatcher, Future.successful(1)).flatMap { _ =>
            attempt(initialDuration, count + 1)(gcsRequest)
          }
      }
    }
    attempt(initialDuration, 0)(gcsRequest)

  }

  def apply[T: StorageBatchSerializer](bucketName: String,
                                       path: String,
                                       maxChunkSize: Int = 10000,
                                       groupedWithin: FiniteDuration = 20.seconds,
                                       parallelism: Int = 4)(
      implicit namingPolicy: BlobNamingPolicy,
      actorSystem: ActorSystem): Sink[TimestampedMessage[T], NotUsed] = {

    // Instantiates a client
    val storage = StorageOptions.getDefaultInstance.getService
    // Creates the new bucket
    val bucket = storage.get(bucketName)

    Flow[TimestampedMessage[T]]
      .groupedWithin(maxChunkSize, groupedWithin)
      .collect {
        case grouped if grouped.nonEmpty =>
          // groupedはnonEmptyなのでminByは成功する
          val timestamp = grouped.map(_.timestamp).minBy(_.toInstant.getEpochSecond)

          val serializer = implicitly[StorageBatchSerializer[T]]

          val blob = serializer.encode(grouped.map(_.payload))
          val blobName = namingPolicy.blobName(path, serializer.extension, timestamp)
          (blobName, blob, serializer.contentType)
      }
      .async
      .to {
        Sink.foreachAsync(parallelism) {
          case (blobName, blob, contentType) =>
            backoffRetry(1.second, 60.second) { () =>
              import actorSystem.dispatcher
              Future {
                val serializer = implicitly[StorageBatchSerializer[T]]

                logger.info(s"uploading to $blobName")
                bucket.create(blobName, blob, contentType)
              }
            }
        }
      }

  }
}
