package websocket_recorder

import org.scalatest.{FunSuite, Matchers}

class ReaderSessionTest extends FunSuite with Matchers {

  test("同一性") {
    val a = ReaderSession()
    Thread.sleep(100)
    val b = ReaderSession()
    Thread.sleep(900)
    val c = ReaderSession.get

    a shouldBe b
    b shouldBe c
    a shouldBe c

  }
}
