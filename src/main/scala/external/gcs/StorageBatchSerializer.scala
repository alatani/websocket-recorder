package external.gcs

// Tのリストから、GCS上に配置するためのInputStreamを生成する
trait StorageBatchSerializer[T] {
  def encode[T](grouped: Seq[T]): java.io.InputStream

  def contentType: String // = "application/json"

  def extension: String
}

object StorageBatchSerializer {
  implicit object string extends StorageBatchSerializer[String] {
    def encode[T](grouped: Seq[T]): java.io.InputStream = {
      val str = grouped.mkString("\n")

      import java.io.ByteArrayInputStream
      import java.nio.charset.StandardCharsets
      new ByteArrayInputStream(str.getBytes(StandardCharsets.UTF_8))
    }

    def contentType: String = "text/plain" // = "application/json"

    def extension: String = ".txt"
  }
}
