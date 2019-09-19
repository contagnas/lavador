package lavador.mixer

import java.time.{Instant, ZoneId, ZonedDateTime}
import java.util.UUID

import cats.Parallel
import cats.data.EitherT
import cats.effect._
import cats.implicits._
import lavador.jobcoin.{Account, Transaction}
import org.http4s.HttpRoutes
import tapir.server.http4s._

import scala.concurrent.duration.{FiniteDuration, _}

class MixerRoutes[F[_]: Sync: Effect: Parallel](
  mixerAccount: Account,
  jobcoinClient: JobcoinClient[F]
)(
  implicit options: Http4sServerOptions[F],
  cs: ContextShift[F],
  timer: Timer[F]
) {
  private def eff[A, E](a: A): EitherT[F, E, A] = EitherT.right(implicitly[LiftIO[F]].liftIO(IO(a)))

  def getDepositAccount(u: Unit): F[Either[String, Account]] =
    eff(Account(UUID.randomUUID().toString)).value

  private def makeMixerDeposits(toAccounts: List[Account], maxTimeToExecute: MaxTimeToExecute, amount: Int): F[Either[String, List[Transaction]]] = {
    val weights = toAccounts.map(_ => math.random)
    val totalWeight = weights.sum
    val normalizedWeights = weights.map(_ / totalWeight)

    val depositAmounts = normalizedWeights.map(w => (w * amount).toInt)
    val deposited = depositAmounts.sum
    val leftover = amount - deposited
    val finalDeposits = (depositAmounts.head + leftover) :: depositAmounts.tail

    val delays = toAccounts.map(_ => math.random * maxTimeToExecute.value)
      .map(_.asInstanceOf[FiniteDuration]) // safe to cast since 0 < math.random < 1, not infinite

    val transactions: List[F[Either[String, Transaction]]] = toAccounts.zip(finalDeposits).zip(delays)
      .filter { case ((_, amt), _) => amt > 0 } // no empty transactions allowed
      .map { case ((account, accountDepositAmt), delay) =>
        for {
          _ <- timer.sleep(delay)
          transaction <- jobcoinClient.transferCoins(mixerAccount, account, accountDepositAmt)
        } yield transaction
      }

    transactions.parSequence.map(_.sequence)
  }

  def runMixer(
    toAccounts: List[Account],
    fromAccount: Account,
    maxMixerFee: MaxMixerFee,
    maxTimeToExecute: MaxTimeToExecute
  ): F[Either[String, MixerReceipt]] = {
    for {
      accountBalance <- EitherT(jobcoinClient.lookupAccount(fromAccount))
      fee <- eff(math.random * maxMixerFee.value / 100)
      coinsToDeposit = ((1 - fee) * accountBalance.balance).toInt
      takenFee = accountBalance.balance - coinsToDeposit
      _ <- EitherT(jobcoinClient.transferCoins(fromAccount, mixerAccount, accountBalance.balance))
      transactions <- EitherT(makeMixerDeposits(toAccounts, maxTimeToExecute, coinsToDeposit))
      now <- EitherT.right[String](timer.clock.realTime(MILLISECONDS))
      dueTime = now + maxTimeToExecute.value.toMillis
      dueZdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(dueTime), ZoneId.systemDefault())
      receipt = MixerReceipt(takenFee, dueZdt, transactions)
    } yield receipt
  }.value

  val routes = List(
    Api.getDepositAccount.toRoutes(getDepositAccount),
    Api.runMixer.toRoutes((runMixer _).tupled)
  ).reduce(_ <+> _)
}