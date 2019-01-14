package routes.graphql

import akka.http.scaladsl.server.Directives.{get, getFromResource, pathSingleSlash}
import akka.http.scaladsl.server.Route

object GraphQLRoutes {
  val routes: Route = pathSingleSlash {
    get {
      getFromResource("web/graphiql.html")
    }
  }
}
