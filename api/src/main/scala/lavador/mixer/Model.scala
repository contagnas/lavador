package lavador.mixer

import java.time.ZonedDateTime

import lavador.jobcoin.Transaction

import scala.concurrent.duration.FiniteDuration

case class MaxMixerFee(value: Double) extends AnyVal
case class MaxTimeToExecute(value: FiniteDuration) extends AnyVal

case class MixerReceipt(
  fee: BigDecimal,
  transactions: List[Transaction]
)
