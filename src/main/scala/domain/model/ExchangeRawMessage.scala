package domain.model

import io.circe.generic.auto._, io.circe.syntax._, io.circe.generic.extras._
import java.time.ZonedDateTime

case class ExchangeRawMessage[T](
    @JsonKey("exchange") exchange: Exchange,
    @JsonKey("channel") channel: Channel,
    @JsonKey("receivedAt") receivedAt: ZonedDateTime,
    @JsonKey("payload") payload: T
)

object ExchangeRawMessage {

  import io.circe._

  import domain.support.JST
  def ofNow[T](exchange: Exchange, channel: Channel, payload: T): ExchangeRawMessage[T] = {
    val now = ZonedDateTime.now(JST)
    new ExchangeRawMessage(exchange, channel, now, payload)
  }

  type Json = ExchangeRawMessage[io.circe.Json]

  import io.circe.generic.semiauto._
  implicit def decoder[T: Decoder]: Decoder[ExchangeRawMessage[T]] = deriveDecoder
  implicit def encoder[T: Encoder]: Encoder[ExchangeRawMessage[T]] = deriveEncoder

}
