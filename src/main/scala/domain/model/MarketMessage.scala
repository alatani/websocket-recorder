package domain.model

import java.time.ZonedDateTime

case class MarketMessage[T](timestamp: ZonedDateTime, payload: T)
