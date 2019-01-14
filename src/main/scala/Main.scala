import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.google.inject.Guice
import guice.bindings.AkkaBinding
import net.codingwell.scalaguice.InjectorExtensions._
import routes.graphql.GraphQLRoutes.routes

object Main extends App {
  val injector = Guice.createInjector(new AkkaBinding())
  implicit val system: ActorSystem = injector.instance[ActorSystem]
  implicit val materializer: ActorMaterializer = injector.instance[ActorMaterializer]
  Http().bindAndHandle(routes, "localhost", 9000)
}
