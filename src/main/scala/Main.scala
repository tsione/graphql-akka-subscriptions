import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import routes.GraphQLRoutes.routes

object Main extends App {
  implicit val system: ActorSystem = ActorSystem("akka-quickstart-scala")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  Http().bindAndHandle(routes, "localhost", 9000)
}
