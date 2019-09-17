package lavador.jobcoin

import cats.data.EitherT
import cats.effect.concurrent.Ref
import cats.effect.{ContextShift, Effect, IO, LiftIO, Sync}
import cats.implicits._
import org.http4s.HttpRoutes
import tapir.server.http4s._

class JobcoinRoutes[F[_]: Sync: Effect: LiftIO]
  (coinCreator: Account, transactions: Ref[F, List[Transaction]])
  (implicit serverOptions: Http4sServerOptions[F], contextShift: ContextShift[F]) {

  private def createCoins(toAccount: Account,  amount: Int): F[Either[String, Transaction]] = {
    val transaction = Transaction(coinCreator, toAccount, amount)
    transactions.update(transaction :: _)
      .map(_ => Right(transaction))
  }

  private def listTransactions(u: Unit): F[Either[String, List[Transaction]]] =
    transactions.get.map(currentTransactions => Right(currentTransactions))

  private def pure[A](a: A): F[A] = implicitly[LiftIO[F]].liftIO(IO.pure(a))
  private def lookupAccount(account: Account): F[Either[String, AccountBalance]] =
    if (account == coinCreator) {
      pure(Right(AccountBalance(account, Int.MaxValue)))
    } else {
      transactions.get.map { currentTransactions =>
        val currentBalance = currentTransactions.foldLeft(0) {
          case (acc, Transaction(from, _, amt)) if account == from => acc - amt
          case (acc, Transaction(_, to, amt)) if account == to => acc + amt
          case (acc, _) => acc
        }
        Right(AccountBalance(account, currentBalance))
      }
    }

  private def sufficientFunds(fromAccount: Account, amount: Int): F[Either[String, Unit]] = {
    for {
      accountBalance <- lookupAccount(fromAccount)
      sufficient: Either[String, Unit] = accountBalance.flatMap { ab =>
        if (ab.balance >= amount)
          Right(())
        else
          Left(s"Insufficient funds: $fromAccount has (${ab.balance} < $amount) coins")
      }
    } yield sufficient
  }

  private def transferCoins(
    fromAccount: Account,
    toAccount: Account,
    amount: Int
  ): F[Either[String, Transaction]] = {
    if (fromAccount == toAccount)
      return pure(Left("An account may not transfer funds to itself"))

    val transaction = Transaction(fromAccount, toAccount, amount)

    val addedTransaction: EitherT[F, String, Unit] = for {
      _ <- EitherT(sufficientFunds(fromAccount, amount))
      _ <- EitherT[F, String, Unit](transactions.update(transaction :: _).map(Right(_)))
    } yield ()

    addedTransaction.map(_ => transaction).value
  }

  val routes: HttpRoutes[F] = List(
    Api.createCoins.toRoutes((createCoins _).tupled),
    Api.listTransactions.toRoutes(listTransactions),
    Api.lookupAccount.toRoutes(lookupAccount),
    Api.transferCoins.toRoutes((transferCoins _).tupled),
  ).reduce(_ <+> _)
}