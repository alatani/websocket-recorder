package domain.model

import io.circe.Decoder.Result
import io.circe._

case class Channel(name: String)

object Channel {

  implicit val decoder: Decoder[Channel] = new Decoder[Channel] {
    override def apply(c: HCursor): Result[Channel] = c.as[String].map(Channel.apply)
  }
  implicit val encoder: Encoder[Channel] = new Encoder[Channel] {
    override def apply(a: Channel): Json = Json.fromString(a.name)
  }
}
