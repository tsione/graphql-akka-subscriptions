package routes.graphql

import spray.json.{JsObject, JsString, JsValue}

object JsonUtils {
  def parseGraphQLQuery(graphQLQuery: JsValue): (String, Option[String], JsObject) = {
    val JsObject(fields) = graphQLQuery
    val JsString(query) = fields("query")
    val operation = fields.get("operationName") collect {
      case JsString(op) => op
    }
    val variables = fields.get("variables") match {
      case Some(obj: JsObject) => obj
      case _ => JsObject.empty
    }
    (query, operation, variables)
  }
}
