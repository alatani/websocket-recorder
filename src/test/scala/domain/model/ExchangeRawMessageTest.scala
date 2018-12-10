package domain.model

import org.scalamock.scalatest.MockFactory
import org.scalatest.{FunSuite, Matchers}

class ExchangeRawMessageTest extends FunSuite with Matchers with MockFactory {
  import io.circe._
  import io.circe.syntax._
  import io.circe.parser._

  import io.circe.generic.auto._
  test("encode decode") {

    val json = parse("""{"a":"b","c":"d"}""").toOption.get

    val testee = ExchangeRawMessage.ofNow(Exchange.BitFlyer, Channel("test"), json)

    println(testee.asJson.pretty(Printer.indented("  ")))

    true shouldBe true

  }

}
