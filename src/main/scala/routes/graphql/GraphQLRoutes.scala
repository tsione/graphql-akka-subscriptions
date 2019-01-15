package routes.graphql

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives.{get, getFromResource, path, pathSingleSlash, _}
import akka.http.scaladsl.server.Route
import com.google.inject.Inject
import graphql.GraphQL
import spray.json.JsValue

class GraphQLRoutes @Inject()(graphQL: GraphQL,
                              httpHandler: HttpHandler,
                              webSocketHandler: WebSocketHandler) {
  val routes: Route =
    (pathSingleSlash & get) {
      getFromResource("web/graphiql.html")
    } ~ path("graphql") {
      get {
        handleWebSocketMessages(webSocketHandler.handleMessages)
      } ~ post {
        entity(as[JsValue]) {
          requestJson =>
            val (query, operation, variables) = JsonUtils.parseGraphQLQuery(requestJson)
            httpHandler.handleQuery(query, operation, variables)
        }
      }
    }
}
