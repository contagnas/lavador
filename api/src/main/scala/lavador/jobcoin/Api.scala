package lavador.jobcoin

import tapir._
import tapir.json.circe._
import io.circe.generic.auto._

object Api {
  /**
   * Anyone can send Jobcoins between any two addresses
   */
  val transferCoins: Endpoint[(Account, Account, Int), String, Transaction, Nothing] = endpoint
    .post
    .in("transferCoins")
    .description("Send jobcoins between two addresses")
    .in(query[String]("fromAccount").description("The account to transfer coins from").mapTo(Account))
    .in(query[String]("toAccount").description("The account to transfer coins to").mapTo(Account))
    .in(query[Int]("amount").description("The number of coins to send"))
    .errorOut(stringBody)
    .out(jsonBody[Transaction])

  /**
   * Anyone can create units of Jobcoins out of thin air
   */
  val createCoins: Endpoint[(Account, Int), String, Transaction, Nothing] = endpoint
    .post
    .in("createCoins")
    .description("Add jobcoins to an account")
    .in(query[String]("toAccount").description("The account to add coins to").mapTo(Account))
    .in(query[Int]("amount").description("The number of coins to add"))
    .errorOut(stringBody)
    .out(jsonBody[Transaction])

  val listTransactions: Endpoint[Unit, String, List[Transaction], Nothing] = endpoint
    .get
    .in("listTransactions")
    .description("Get a list of all transactions on the blockchain")
    .errorOut(stringBody)
    .out(jsonBody[List[Transaction]])

  val lookupAccount: Endpoint[Account, String, AccountBalance, Nothing] = endpoint
    .get
    .in("lookupAccount")
    .description("Get details on a specific jobcoin account")
    .in(query[String]("accountId").description("The account to look up").mapTo(Account))
    .errorOut(stringBody)
    .out(jsonBody[AccountBalance])

  val endpoints = List(transferCoins, createCoins, listTransactions, lookupAccount)
}
