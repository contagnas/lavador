package lavador.mockcoin


// Each Jobcoin has an "address" that is just an arbitrary string
case class AccountId(value: String) extends AnyVal
case class Account(id: AccountId, balance: Int)
case class Transaction(fromAccount: AccountId, toAccount: AccountId, amount: Int)
