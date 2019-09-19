package lavador.mixer

import cats.effect.{ExitCode, IO, IOApp}
import cats.effect.IO._
import org.http4s.implicits._
import cats.implicits._

import lavador.jobcoin.Account

import scala.concurrent.ExecutionContext.Implicits.global

object Main extends IOApp {
  implicit val contextShift2 = IO.contextShift(global)

  def run(args: List[String]) = {
    val mixerAccount = Account("m1x3r")
    val jobcoinClient = new JobcoinClient[IO](uri"http://localhost:8080")

    for {
      server <- MixerServer.stream[IO](mixerAccount, jobcoinClient)
    } yield server
  }.compile.drain.as(ExitCode.Success)
}
