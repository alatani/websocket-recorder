package domain.model

import io.circe.Decoder.Result
import io.circe._

abstract class Exchange(val name: String)

object Exchange {
  case object BitFlyer extends Exchange("bitflyer")
  case object BitMex extends Exchange("bitmex")

  case class Unknown(override val name: String) extends Exchange(name)

  def of(name: String) = name match {
    case BitFlyer.name => BitFlyer
    case BitMex.name   => BitMex
    case _             => Unknown(name)
  }

  implicit val decoder: Decoder[Exchange] = new Decoder[Exchange] {
    override def apply(c: HCursor): Result[Exchange] = c.as[String].map(Exchange.of)
  }
  implicit val encoder: Encoder[Exchange] = new Encoder[Exchange] {
    override def apply(a: Exchange): Json = Json.fromString(a.name)
  }

}
