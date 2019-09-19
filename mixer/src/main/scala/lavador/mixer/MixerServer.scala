package lavador.mixer

import cats.Parallel
import cats.effect.{ConcurrentEffect, ContextShift, Timer}
import fs2.Stream
import lavador.jobcoin.Account
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

  def stream[F[_]: ConcurrentEffect: Parallel](mixerAccount: Account, jobcoinClient: JobcoinClient[F])(
    implicit T: Timer[F], C: ContextShift[F]
  ): Stream[F, Nothing] = {
    val httpApp = Router(
      "/" -> new MixerRoutes[F](mixerAccount, jobcoinClient).routes,
      "/docs" -> new SwaggerHttp4s(docs).routes,
    ).orNotFound

    val finalHttpApp = Logger.httpApp(true, true)(httpApp)

    BlazeServerBuilder[F]
      .bindHttp(8081, "0.0.0.0")
      .withHttpApp(finalHttpApp)
      .serve
      .drain
  }
}