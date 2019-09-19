package lavador.mixer

import cats.effect.ConcurrentEffect
import io.circe.generic.auto._
import lavador.jobcoin.{Address, AddressDetails, SuccessfulTransaction, Transaction, UnsuccessfulTransaction, Api => JobcoinApi}
import org.http4s.Uri
import org.http4s.circe._

import tapir.client.http4s._

import scala.concurrent.ExecutionContext

class JobcoinClient[F[_]: ConcurrentEffect](uri: Uri)(implicit ec: ExecutionContext) {
  private implicit val unsuccessfulTransactionDecoder = jsonOf[F, UnsuccessfulTransaction]
  private implicit val successfulTransactionDecoder = jsonOf[F, SuccessfulTransaction]
  def transferCoins(fromAddress: Address, toAddress: Address, amount: BigDecimal): F[Either[UnsuccessfulTransaction, SuccessfulTransaction]] = {
    JobcoinApi.sendCoins.toHttp4sRequest(uri).apply((fromAddress, toAddress, amount))
  }

  private implicit val addressDetailsDecoder = jsonOf[F, AddressDetails]
  def lookupAddress(address: Address): F[Either[String, AddressDetails]] =
    JobcoinApi.lookupAddress.toHttp4sRequest(uri).apply(address)
}
