package domain

import java.time.ZoneId
import java.util.Locale

package object support {
  val JST: ZoneId = ZoneId.of("Asia/Tokyo")
  val UTC: ZoneId = ZoneId.of("UTC")
  val JAPAN: Locale = Locale.JAPAN
  val JPY: String = "JPY"
  val ja: String = "ja"

  // http://qiita.com/suin/items/61d121bef4d99a701543
  implicit class stringImprovements(val s: String) {
    import scala.util.control.Exception._
    def toIntOpt = catching(classOf[NumberFormatException]) opt s.toInt
    def toLongOpt = catching(classOf[NumberFormatException]) opt s.toLong
  }

  /** Adds chaining methods `tap` and `pipe` to every type.
    */
  implicit class ChainingOps[A](self: A) {
    def tap[U](f: A => U): A = {
      f(self)
      self
    }
    def pipe[B](f: A => B): B = f(self)
  }

}
