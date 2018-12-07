package external

case class ReaderSession private (value: String) {
  override def toString: String = value
}

object ReaderSession {
  val instance: ReaderSession = {
    new ReaderSession(scala.util.Random.alphanumeric.take(6).mkString)
  }

  def apply(): ReaderSession = instance
  def get: ReaderSession = instance
}
