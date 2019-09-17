package lavador.mixer

import scala.concurrent.duration.FiniteDuration

case class MaxMixerFee(value: Double) extends AnyVal
case class MaxTimeToExecute(value: FiniteDuration) extends AnyVal
