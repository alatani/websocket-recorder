package websocket_recorder.gcs

// Tのリストから、GCS上に配置するためのInputStreamを生成する
trait StorageBatchSerializer[T] {
  def encode[T](grouped: Seq[T]): java.io.InputStream

  def contentType: String // = "application/json"

  def extension: String
}

object StorageBatchSerializer {
  implicit object string extends StorageBatchSerializer[String] {
    def encode[T](grouped: Seq[T]): java.io.InputStream = {
      import java.io.ByteArrayInputStream
      import java.nio.charset.StandardCharsets

      val builder = new StringBuilder()
      grouped.foreach { line =>
        builder.append(line + "\n")
      }
      new ByteArrayInputStream(builder.mkString.getBytes(StandardCharsets.UTF_8))
    }

    def contentType: String = "text/plain" // = "application/json"

    def extension: String = ".txt"
  }
}
