package lavador.jobcoin

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._

import lavador.jobcoin.Api


object Main extends IOApp {
  def run(args: List[String]) =
    JobcoinServer.stream[IO].compile.drain.as(ExitCode.Success)
}
