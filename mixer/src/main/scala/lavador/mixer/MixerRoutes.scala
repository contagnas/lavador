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

class MixerRoutes(
  mixerAddress: Address,
  jobcoinClient: JobcoinClient[IO]
)(
  implicit options: Http4sServerOptions[IO],
  cs: ContextShift[IO],
  timer: Timer[IO],
) {
  private def eff[A, E](a: => A): EitherT[IO, E, A] = EitherT.right(IO(a))

  def getDepositAddress(u: Unit): IO[Either[String, Address]] =
    eff(Address(UUID.randomUUID().toString)).value

  private def makeMixerDeposits(
    deposits: List[MixerDeposit]
  ): IO[Either[String, List[Unit]]] = {
    deposits.map {
      case MixerDeposit(toAddress, delay, amount) =>
        for {
          _ <- EitherT.right[String](timer.sleep(delay))
          _ <- EitherT(jobcoinClient.transferCoins(mixerAddress, toAddress, amount)).leftMap(_.status)
        } yield ()
    }.parSequence.value
  }

  private case class MixerDeposit(toAddress: Address, delay: FiniteDuration, amount: BigDecimal) {
    def transaction(startTime: ZonedDateTime) = Transaction(
      timestamp = startTime.plusNanos(delay.toNanos),
      toAddress = toAddress,
      fromAddress = Some(mixerAddress),
      amount = amount
    )
  }

  private implicit class MixerDepositList(mdl: List[MixerDeposit]) {
    def toReceipt(fee: BigDecimal, dateTime: ZonedDateTime) = MixerReceipt(
      fee = fee,
      transactions = mdl.map(_.transaction(dateTime))
    )
  }

  private def allocateDeposits(
    addresses: List[Address],
    maxTimeToExecute: MaxTimeToExecute,
    amount: BigDecimal
  ): List[MixerDeposit] = {
    val weights = addresses.map(_ => math.random)
    val totalWeight = weights.sum
    val normalizedWeights = weights.map(_ / totalWeight)

    val depositAmounts = normalizedWeights.map(w => (w * amount))
    val delays = addresses.map(_ => math.random * maxTimeToExecute.value)
      .map(_.asInstanceOf[FiniteDuration]) // safe to cast since 0 < math.random < 1, not infinite

    addresses.zip(depositAmounts).zip(delays).map { case ((address, addressAmount), delay) =>
      MixerDeposit(address, delay, addressAmount)
    }
  }

  private def startDeposits(mixerDeposits: List[MixerDeposit]): IO[Unit] = {
    IO.shift *> makeMixerDeposits(mixerDeposits).start
  }.map(_ => ())

  def runMixer(
    toAddresses: List[Address],
    fromAddress: Address,
    maxMixerFee: MaxMixerFee,
    maxTimeToExecute: MaxTimeToExecute
  ): IO[Either[String, MixerReceipt]] = {
    for {
      addressDetails <- EitherT(jobcoinClient.lookupAddress(fromAddress))
      fee <- eff(math.random * maxMixerFee.value / 100)
      coinsToMix = ((1 - fee) * addressDetails.balance)
      feeTaken = addressDetails.balance - coinsToMix
      _ <- EitherT(jobcoinClient.transferCoins(fromAddress, mixerAddress, addressDetails.balance)).leftMap(_.status)
      now <- EitherT.right[String](timer.clock.realTime(MILLISECONDS))
      nowZdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(now), ZoneId.systemDefault())
      mixerDeposits = allocateDeposits(toAddresses, maxTimeToExecute, coinsToMix)
      _ <- EitherT.right[String](startDeposits(mixerDeposits))
      receipt = mixerDeposits.toReceipt(feeTaken, nowZdt)
    } yield receipt
  }.value

  val routes = List(
    Api.getDepositAddress.toRoutes(getDepositAddress),
    Api.runMixer.toRoutes((runMixer _).tupled)
  ).reduce(_ <+> _)
}