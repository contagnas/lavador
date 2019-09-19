package lavador.mixer

import io.circe.generic.auto._
import lavador.jobcoin.Account
import tapir._
import tapir.json.circe._

import scala.concurrent.duration._

object Api {
  val getDepositAccount: Endpoint[Unit, String, Account, Nothing] = endpoint
    .get
    .in("getDepositAccount")
    .description("Get a deposit account to access the mixer")
    .errorOut(stringBody)
    .out(jsonBody[Account])

  val runMixer: Endpoint[(List[Account], Account, MaxMixerFee, MaxTimeToExecute), String, MixerReceipt, Nothing] = endpoint
    .post
    .in("runMixer")
    .in(query[List[String]]("toAccounts").description("Account IDs to transfer coins to")
      .validate(Validator.minSize(1))
      .map(ss => ss.map(Account))(_.map(_.id)))
    .in(query[String]("fromAccount")
      .description("The mixer deposit account to mix from")
      .mapTo(Account))
    .in(query[Double]("maxMixerFee")
      .description("The maximum mixer fee to charge, as a percentage. A random charge will be applied up to this value. Use higher fees for more anonymity.")
      .validate(Validator.min(0.0))
      .mapTo(MaxMixerFee))
    .in(query[Int]("maxTransactionSeconds")
      .description("The maximum time (in seconds) to wait before making deposits. Use longer durations for more anonymity.")
      .validate(Validator.min(0))
      .map[MaxTimeToExecute](s => MaxTimeToExecute(s.seconds))(_.value.toSeconds.toInt))
    .errorOut(stringBody)
    .out(jsonBody[MixerReceipt])

  val endpoints = List(getDepositAccount, runMixer)
}
