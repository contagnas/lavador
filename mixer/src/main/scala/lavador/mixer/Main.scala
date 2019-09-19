package lavador.mixer

import cats.effect.{ExitCode, IO, IOApp}
import cats.effect.IO._
import org.http4s.implicits._
import cats.implicits._

import lavador.jobcoin.Address

import scala.concurrent.ExecutionContext.Implicits.global

object Main extends IOApp {
  def run(args: List[String]) = {
    val mixerAddress = Address("mixer")
    val jobcoinClient = new JobcoinClient[IO](uri"https://jobcoin.gemini.com/wackiness/api")

    for {
      server <- MixerServer.stream(mixerAddress, jobcoinClient)
    } yield server
  }.compile.drain.as(ExitCode.Success)
}
