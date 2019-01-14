import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.google.inject.Guice
import guice.bindings.{AkkaBinding, MonixBinding}
import net.codingwell.scalaguice.InjectorExtensions._
import routes.graphql.GraphQLRoutes

object Main extends App {
  val injector = Guice.createInjector(new AkkaBinding(), new MonixBinding())
  implicit val system: ActorSystem = injector.instance[ActorSystem]
  implicit val materializer: ActorMaterializer = injector.instance[ActorMaterializer]
  Http().bindAndHandle(injector.instance[GraphQLRoutes].routes, "localhost", 9000)
}
