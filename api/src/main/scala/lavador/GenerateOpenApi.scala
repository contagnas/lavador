package lavador

import tapir.docs.openapi._
import tapir.openapi.OpenAPI
import tapir.openapi.circe.yaml._

object GenerateOpenApi extends App {
  val docs: OpenAPI = (mixer.Api.endpoints ++ jobcoin.Api.endpoints).toOpenAPI("JobCoin", "1.0")
  println(docs.toYaml)
}
