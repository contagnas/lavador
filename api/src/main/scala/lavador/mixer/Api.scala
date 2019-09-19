package lavador.mixer

import io.circe.generic.auto._
import lavador.jobcoin.Address
import tapir._
import tapir.json.circe._

import scala.concurrent.duration._

object Api {
  val getDepositAddress: Endpoint[Unit, String, Address, Nothing] = endpoint
    .get
    .in("getDepositAddress")
    .description("Get a deposit address to access the mixer")
    .errorOut(stringBody)
    .out(jsonBody[Address])

  val runMixer: Endpoint[(List[Address], Address, MaxMixerFee, MaxTimeToExecute), String, MixerReceipt, Nothing] = endpoint
    .post
    .in("runMixer")
    .in(query[List[String]]("toAddresss")
      .description("Address IDs to transfer coins to")
      .validate(Validator.minSize(1))
      .map(ss => ss.map(Address.apply))(_.map(_.value)))
    .in(query[String]("fromAddress")
      .description("The mixer deposit address to mix from")
      .map(Address.apply)(_.value))
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

  val endpoints = List(getDepositAddress, runMixer)
}
