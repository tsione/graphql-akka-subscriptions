package routes.graphql

import akka.http.scaladsl.server.Directives.{get, getFromResource, pathSingleSlash}
import akka.http.scaladsl.server.Route
import com.google.inject.Inject
import graphql.GraphQL

class GraphQLRoutes @Inject()(graphQL: GraphQL,
                              webSocketHandler: WebSocketHandler) {
  val routes: Route = pathSingleSlash {
    get {
      getFromResource("web/graphiql.html")
    }
  }
}
