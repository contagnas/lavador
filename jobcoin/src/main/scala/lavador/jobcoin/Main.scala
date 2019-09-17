package lavador.jobcoin

import cats.effect.concurrent.Ref
import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import fs2.Stream

object Main extends IOApp {
  val coinCreator = Account("b3rn4nk3")
  val transactionList: List[Transaction] = Nil

  def run(args: List[String]): IO[ExitCode] = {
    for {
      transactions <- Stream.eval(Ref[IO].of(transactionList))
      server <- JobcoinServer.stream(coinCreator, transactions)
    } yield server
  }.compile.drain.as(ExitCode.Success)
}
