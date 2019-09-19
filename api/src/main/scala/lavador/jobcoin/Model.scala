package lavador.jobcoin

import java.time.ZonedDateTime

import io.circe.{Decoder, Encoder}


case class Address(
  value: String
) extends AnyVal

object Address {
  implicit val encodeAddress: Encoder[Address] = Encoder.encodeString.contramap(_.value)
  implicit val decodeAddress: Decoder[Address] = Decoder.decodeString.map(Address(_))
}

case class Transaction(
  timestamp: ZonedDateTime,
  toAddress: Address,
  fromAddress: Option[Address],
  amount: BigDecimal
)

case class AddressDetails(
  balance: BigDecimal,
  transactions: List[Transaction]
)

case class SuccessfulTransaction(
  status: String
)

case class UnsuccessfulTransaction(
  status: String
)

