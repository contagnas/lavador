package lavador.jobcoin

import cats.effect.concurrent.Ref
import cats.effect.{ConcurrentEffect, ContextShift, Timer}
import fs2.Stream
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.Logger
import tapir.docs.openapi._
import tapir.openapi.circe.yaml._
import tapir.swagger.http4s.SwaggerHttp4s

object JobcoinServer {
  private val docs = Api.endpoints.toOpenAPI("Jobcoin", "0.0.1").toYaml

  def stream[F[_]: ConcurrentEffect](
    coinCreator: Account,
    transactions: Ref[F, List[Transaction]]
  )(implicit T: Timer[F], C: ContextShift[F]
  ): Stream[F, Nothing] = {

    val httpApp = Router(
      "/" -> new JobcoinRoutes[F](coinCreator, transactions).routes,
      "/docs" -> new SwaggerHttp4s(docs).routes,
    ).orNotFound

    val finalHttpApp = Logger.httpApp(true, true)(httpApp)
    BlazeServerBuilder[F]
      .bindHttp(8080, "0.0.0.0")
      .withHttpApp(finalHttpApp)
      .serve
      .drain
    }
}