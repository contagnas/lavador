package lavador.mixer

import cats.effect.ConcurrentEffect
import io.circe.generic.auto._
import lavador.jobcoin.{Account, AccountBalance, Transaction, Api => JobcoinApi}
import org.http4s.Uri
import org.http4s.circe._
import tapir.client.http4s._

import scala.concurrent.ExecutionContext

class JobcoinClient[F[_]: ConcurrentEffect](uri: Uri)(implicit ec: ExecutionContext) {
  implicit val transactionDecoder = jsonOf[F, Transaction]
  def transferCoins(fromAccount: Account, toAccount: Account, amount: Int): F[Either[String, Transaction]] =
    JobcoinApi.transferCoins.toHttp4sRequest(uri).apply((fromAccount, toAccount, amount))

  implicit val accountBalanceDecoder = jsonOf[F, AccountBalance]
  def lookupAccount(account: Account): F[Either[String, AccountBalance]] =
    JobcoinApi.lookupAccount.toHttp4sRequest(uri).apply(account)
}
