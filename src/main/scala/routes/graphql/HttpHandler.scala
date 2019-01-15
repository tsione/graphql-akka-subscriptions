package routes.graphql

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.marshalling.sse.EventStreamMarshalling._
import akka.http.scaladsl.model.StatusCodes.{BadRequest, InternalServerError, OK}
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.StandardRoute
import akka.stream.ActorMaterializer
import com.google.inject.Inject
import graphql.GraphQL
import monix.execution.Scheduler
import sangria.ast.OperationType.Subscription
import sangria.execution.ExecutionScheme.Stream
import sangria.execution.{ErrorWithResolver, Executor, QueryAnalysisError}
import sangria.marshalling.sprayJson._
import sangria.parser.{QueryParser, SyntaxError}
import spray.json.JsObject

import scala.util.control.NonFatal
import scala.util.{Failure, Success}

class HttpHandler @Inject()(graphQL: GraphQL)
                           (implicit val actorMaterializer: ActorMaterializer,
                            implicit val scheduler: Scheduler) {
  def handleQuery(query: String, operation: Option[String], variables: JsObject = JsObject.empty): StandardRoute =
    QueryParser.parse(query) match {
      case Success(queryAst) =>
        queryAst.operationType(operation) match {
          case Some(Subscription) =>
            import sangria.streaming.akkaStreams._
            complete(Executor.prepare(
              schema = graphQL.schema,
              queryAst = queryAst,
              operationName = operation,
              variables = variables
            )
              .map {
                preparedQuery =>
                  ToResponseMarshallable(preparedQuery.execute().map(r => ServerSentEvent(r.compactPrint)).recover {
                    case NonFatal(error) =>
                      ServerSentEvent(error.getMessage)
                  })
              }
              .recover {
                case error: QueryAnalysisError => ToResponseMarshallable(BadRequest -> error.resolveError)
                case error: ErrorWithResolver => ToResponseMarshallable(InternalServerError -> error.resolveError)
              })
          case _ =>
            complete(ToResponseMarshallable(Executor.execute(
              schema = graphQL.schema,
              queryAst = queryAst,
              operationName = operation,
              variables = variables
            ).map(OK -> _)
              .recover {
                case error: QueryAnalysisError => BadRequest -> error.resolveError
                case error: ErrorWithResolver => InternalServerError -> error.resolveError
              }))
        }
      case Failure(e: SyntaxError) => complete(BadRequest, e.toString)
      case Failure(_) => complete(InternalServerError)
    }
}
