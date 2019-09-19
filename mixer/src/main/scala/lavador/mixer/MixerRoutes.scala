package lavador.mixer

import java.time.{Instant, ZoneId, ZonedDateTime}
import java.util.UUID

import cats.Parallel
import cats.data.EitherT
import cats.effect._
import cats.implicits._
import lavador.jobcoin.{Address, Transaction}
import tapir.server.http4s._

import scala.concurrent.duration.{FiniteDuration, _}

class MixerRoutes[F[_]: Sync: Effect: Parallel](
  mixerAddress: Address,
  jobcoinClient: JobcoinClient[F]
)(
  implicit options: Http4sServerOptions[F],
  cs: ContextShift[F],
  timer: Timer[F]
) {
  private def eff[A, E](a: A): EitherT[F, E, A] = EitherT.right(implicitly[LiftIO[F]].liftIO(IO(a)))

  def getDepositAddress(u: Unit): F[Either[String, Address]] =
    eff(Address(UUID.randomUUID().toString)).value

  private def makeMixerDeposits(
    toAddresses: List[Address],
    maxTimeToExecute: MaxTimeToExecute,
    amount: BigDecimal,
    startTime: ZonedDateTime
  ): F[Either[String, List[Transaction]]] = {
    val weights = toAddresses.map(_ => math.random)
    val totalWeight = weights.sum
    val normalizedWeights = weights.map(_ / totalWeight)

    val depositAmounts = normalizedWeights.map(w => (w * amount))

    val delays = toAddresses.map(_ => math.random * maxTimeToExecute.value)
      .map(_.asInstanceOf[FiniteDuration]) // safe to cast since 0 < math.random < 1, not infinite

    val transactions: List[F[Either[String, Transaction]]] = toAddresses.zip(depositAmounts).zip(delays)
      .filter { case ((_, amt), _) => amt > 0 } // no empty transactions allowed
      .map { case ((toAddress, addressDepositAmt), delay) => {
        for {
          _ <- EitherT.right[String](timer.sleep(delay))
          _ <- EitherT(jobcoinClient.transferCoins(mixerAddress, toAddress, addressDepositAmt)).leftMap(_.status)

          transaction = Transaction(
            timestamp = startTime.plusNanos(delay.toNanos),
            toAddress = toAddress,
            fromAddress = Some(mixerAddress),
            amount = addressDepositAmt
          )
        } yield transaction
      }.value }

    transactions.parSequence.map(_.sequence)
  }

  def runMixer(
    toAddresses: List[Address],
    fromAddress: Address,
    maxMixerFee: MaxMixerFee,
    maxTimeToExecute: MaxTimeToExecute
  ): F[Either[String, MixerReceipt]] = {
    for {
      addressDetails <- EitherT(jobcoinClient.lookupAddress(fromAddress))
      fee <- eff(math.random * maxMixerFee.value / 100)
      coinsToDeposit = ((1 - fee) * addressDetails.balance)
      takenFee = addressDetails.balance - coinsToDeposit
      _ <- EitherT(jobcoinClient.transferCoins(fromAddress, mixerAddress, addressDetails.balance)).leftMap(_.status)
      now <- EitherT.right[String](timer.clock.realTime(MILLISECONDS))
      nowZdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(now), ZoneId.systemDefault())
      transactions <- EitherT(makeMixerDeposits(toAddresses, maxTimeToExecute, coinsToDeposit, nowZdt))
      receipt = MixerReceipt(takenFee, nowZdt, transactions)
    } yield receipt
  }.value

  val routes = List(
    Api.getDepositAddress.toRoutes(getDepositAddress),
    Api.runMixer.toRoutes((runMixer _).tupled)
  ).reduce(_ <+> _)
}