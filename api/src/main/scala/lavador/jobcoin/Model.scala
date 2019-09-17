package lavador.jobcoin


// Each Jobcoin has an "address" that is just an arbitrary string
case class Account(id: String) extends AnyVal
case class AccountBalance(account: Account, balance: Int)
case class Transaction(fromAccount: Account, toAccount: Account, amount: Int)
