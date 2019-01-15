package routes.graphql

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives.{extractRequest, get, getFromResource, path, pathSingleSlash, _}
import akka.http.scaladsl.server.Route
import com.google.inject.Inject
import graphql.GraphQL
import spray.json.{JsObject, JsString, JsValue}

class GraphQLRoutes @Inject()(graphQL: GraphQL,
                              httpHandler: HttpHandler,
                              webSocketHandler: WebSocketHandler) {
  val routes: Route =
    pathSingleSlash {
      get {
        getFromResource("web/graphiql.html")
      }
    } ~ path("graphql") {
      extractRequest {
        _ =>
          get {
            handleWebSocketMessages(webSocketHandler.handleMessages)
          } ~ post {
            entity(as[JsValue]) {
              requestJson =>
                val JsObject(fields) = requestJson

                val JsString(query) = fields("query")

                val operation = fields.get("operationName") collect {
                  case JsString(op) ⇒ op
                }

                val vars = fields.get("variables") match {
                  case Some(obj: JsObject) ⇒ obj
                  case _ ⇒ JsObject.empty
                }
                httpHandler.handleQuery(query, operation, vars)
            }
          }
      }
    }
}
