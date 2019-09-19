package lavador.mixer

import cats.effect.{ContextShift, IO, Timer}
import fs2.Stream
import lavador.jobcoin.Address
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.Logger
import tapir.docs.openapi._
import tapir.openapi.circe.yaml._
import tapir.swagger.http4s.SwaggerHttp4s


object MixerServer {
  private val docs = Api.endpoints.toOpenAPI("Mixer", "0.0.1").toYaml
    .replace("!!int '0.0", "0.0")
    .replace("minSize", "minItems")

  def stream(mixerAccount: Address, jobcoinClient: JobcoinClient[IO])(
    implicit T: Timer[IO], C: ContextShift[IO]
  ): Stream[IO, Nothing] = {
    val httpApp = Router(
      "/" -> new MixerRoutes(mixerAccount, jobcoinClient).routes,
      "/docs" -> new SwaggerHttp4s(docs).routes,
    ).orNotFound

    val finalHttpApp = Logger.httpApp(true, true)(httpApp)

    BlazeServerBuilder[IO]
      .bindHttp(8081, "0.0.0.0")
      .withHttpApp(finalHttpApp)
      .serve
      .drain
  }
}