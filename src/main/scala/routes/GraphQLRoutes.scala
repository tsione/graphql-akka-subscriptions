package routes

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

object GraphQLRoutes {
  val routes: Route = pathSingleSlash {
    get {
      getFromResource("web/graphiql.html")
    }
  }
}
