package lavador.mixer

import cats.effect.{Clock, ContextShift, IO, Timer}
import lavador.jobcoin.{Address, AddressDetails, SuccessfulTransaction, UnsuccessfulTransaction}
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class MixerRoutesTest extends FlatSpec with Matchers {
  implicit val mockTimer: Timer[IO] = new Timer[IO] {
    override def clock: Clock[IO] = new Clock[IO] {
      override def realTime(unit: TimeUnit): IO[Long] = IO.pure(0)
      override def monotonic(unit: TimeUnit): IO[Long] = IO.pure(0)
    }
    override def sleep(duration: FiniteDuration): IO[Unit] = IO.pure(())
  }

  implicit val mockContextShift: ContextShift[IO] = new ContextShift[IO] {
    override def shift: IO[Unit] = IO.pure(())
    override def evalOn[A](ec: ExecutionContext)(fa: IO[A]): IO[A] = fa
  }

  private class MockJobcoin {
    val transferCalls: ArrayBuffer[(Address, Address, BigDecimal)] = mutable.ArrayBuffer()
    val lookupCalls: ArrayBuffer[Address] = mutable.ArrayBuffer()

    val client: JobcoinClient[IO] = new JobcoinClient[IO] {
      override def transferCoins(
        fromAddress: Address,
        toAddress: Address,
        amount: BigDecimal
      ): IO[Either[UnsuccessfulTransaction, SuccessfulTransaction]] = {
        transferCalls += ((fromAddress, toAddress, amount))
        IO.pure(Right(SuccessfulTransaction("OK")))
      }

      override def lookupAddress(address: Address): IO[Either[String, AddressDetails]] = {
        lookupCalls += address
        IO.pure(Right(AddressDetails(accountBalance, Nil)))
      }
    }
  }

  val mixerAddress = Address("testMixer")
  val accountBalance = BigDecimal(100)

  "getDepositAddress" should "never fail to make a deposit address" in {
    val jobcoin = new MockJobcoin
    val mixerRoutes = new MixerRoutes(mixerAddress, jobcoin.client)
    mixerRoutes.getDepositAddress(()).unsafeRunSync.isRight shouldBe true
  }

  it should "not use the jobcoin client to make a new address" in {
    val jobcoin = new MockJobcoin
    val mixerRoutes = new MixerRoutes(mixerAddress, jobcoin.client)
    jobcoin.transferCalls.isEmpty shouldBe true
    jobcoin.lookupCalls.isEmpty shouldBe true
  }

  "runMixer" should "make transfers to the toAddresses and the mixer account" in {
    val jobcoin = new MockJobcoin
    val mixerRoutes = new MixerRoutes(mixerAddress, jobcoin.client)

    val toAddresses = List(Address("1"), Address("2"), Address("3"))
    mixerRoutes.runMixer(
      toAddresses,
      Address("from"),
      MaxMixerFee(2.0),
      MaxTimeToExecute(1.hour)
    ).unsafeRunSync

    val accountsTransferedTo = jobcoin.transferCalls.map { case (_, from, _) => from }.toSet
    accountsTransferedTo shouldBe (mixerAddress :: toAddresses).toSet
    jobcoin.transferCalls.length shouldBe toAddresses.length + 1
  }

  it should "only make deposits to the toAddresses from the mixer account" in {
    val jobcoin = new MockJobcoin
    val mixerRoutes = new MixerRoutes(mixerAddress, jobcoin.client)

    val toAddresses = List(Address("1"), Address("2"), Address("3"))
    mixerRoutes.runMixer(
      toAddresses,
      Address("from"),
      MaxMixerFee(2.0),
      MaxTimeToExecute(1.hour)
    ).unsafeRunSync

    jobcoin.transferCalls.collect {
      case (fromAddress, toAddress, _) if toAddresses.contains(toAddress) =>
        fromAddress
    }.toSet shouldBe Set(mixerAddress)
  }

  it should "deposit the entire balance of the from address into the mixer address" in {
    val jobcoin = new MockJobcoin
    val mixerRoutes = new MixerRoutes(mixerAddress, jobcoin.client)

    val toAddresses = List(Address("1"), Address("2"), Address("3"))
    mixerRoutes.runMixer(
      toAddresses,
      Address("from"),
      MaxMixerFee(2.0),
      MaxTimeToExecute(1.hour)
    ).unsafeRunSync

    jobcoin.transferCalls.collect {
      case (fromAddress, toAddress, amt) if toAddress == mixerAddress && fromAddress == Address("from") =>
        amt
    }.sum shouldBe accountBalance
  }

  // TODO: Other good tests:
  // deposits are made according to requested timeline (easy by inspecting the calls to timer.sleep)
  // the net amount sent to the mixer is betweeen 0 and the max fee
}
