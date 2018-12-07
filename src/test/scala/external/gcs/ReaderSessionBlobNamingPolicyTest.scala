package external.gcs

import domain.support.JST
import java.time.ZonedDateTime

import external.ReaderSession
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FunSuite, Matchers}

class ReaderSessionBlobNamingPolicyTest extends FunSuite with Matchers with MockFactory {

  val testee = new ReaderSessionBlobNamingPolicy()

  val digest = scala.util.Random.alphanumeric.take(5).mkString
  val now = ZonedDateTime.of(2024, 2, 29, 1, 0, 30, 12345678, JST)

  test("check path") {
    val session = ReaderSession().value
    val blobName = testee.blobName("hoge/fuga", "json", now, digest)
    println(blobName)
    blobName shouldBe s"hoge/fuga/year=2024/month=02/day=29/hour=01/min=00/sec=30.sess=${session}.digest=${digest}.json"
  }

  test("check extension with period") {
    val session = ReaderSession().value
    val blobName = testee.blobName("hoge/fuga", ".json", now, digest)
    blobName shouldBe s"hoge/fuga/year=2024/month=02/day=29/hour=01/min=00/sec=30.sess=${session}.digest=${digest}.json"
  }

}
