package lavador.jobcoin

import io.circe.generic.auto._
import tapir._
import tapir.json.circe._

object Api {
  val lookupAddress: Endpoint[Address, String, AddressDetails, Nothing] = endpoint
      .get
      .in("addresses")
      .description("Get the balance and list of transactions for an address")
      .in(path[String].example("BobsAddress").map(Address.apply)(_.value))
      .errorOut(stringBody)
      .out(jsonBody[AddressDetails])

  val transactions: Endpoint[Unit, String, List[Transaction], Nothing] = endpoint
    .get
    .in("transactions")
    .description("Get a list of all Jobcoin transactions")
    .errorOut(stringBody)
    .out(jsonBody[List[Transaction]])

  val sendCoins: Endpoint[(Address, Address, BigDecimal), UnsuccessfulTransaction, SuccessfulTransaction, Nothing] = endpoint
    .post
    .in("transactions")
    .description("Send Jobcoins from one address to another.")
    .in(query[String]("fromAddress")
      .description("The address sending the Jobcoins")
      .example("BobsAddress")
      .map(Address.apply)(_.value))
    .in(query[String]("toAddress")
      .description("The address receiving the Jobcoins")
      .example("AlicesAddress")
      .map(Address.apply)(_.value))
    .in(query[String]("amount")
      .description("The number of Jobcoins to send, as a string.")
      .example("30.1")
      .map(BigDecimal.apply)(_.toString))
    .errorOut(jsonBody[UnsuccessfulTransaction])
    .out(jsonBody[SuccessfulTransaction])

  val endpoints = List(lookupAddress, transactions, sendCoins)
}
