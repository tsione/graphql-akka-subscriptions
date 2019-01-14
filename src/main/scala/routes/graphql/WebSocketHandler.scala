package routes.graphql

import akka.NotUsed
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source, SourceQueueWithComplete}
import akka.stream.{ActorMaterializer, KillSwitches, OverflowStrategy, SharedKillSwitch}
import com.google.inject.Inject
import graphql.GraphQL
import monix.execution.Scheduler
import sangria.ast.OperationType.Subscription
import sangria.execution.ExecutionScheme.Stream
import sangria.execution.Executor
import sangria.marshalling.sprayJson._
import sangria.parser.{QueryParser, SyntaxError}
import spray.json.{JsArray, JsNumber, JsObject, JsString, _}

import scala.util.{Failure, Success}

class WebSocketHandler @Inject()(graphQL: GraphQL)
                                (implicit val actorMaterializer: ActorMaterializer,
                                 implicit val scheduler: Scheduler) {

  def handleMessages: Flow[Message, Message, NotUsed] = {
    implicit val (queue, publisher) = Source.queue[Message](16, OverflowStrategy.backpressure)
      .toMat(Sink.asPublisher(false))(Keep.both)
      .run()
    implicit val killSwitches: SharedKillSwitch = KillSwitches.shared("WebSocketHandler")
    val incomingFlow = Flow[Message]
      .collect {
        case TextMessage.Strict(message) =>
          val JsObject(fields) = message.parseJson
          val JsString(query) = fields("query")
          val operation = fields.get("operationName") collect {
            case JsString(op) => op
          }
          val vars = fields.get("variables") match {
            case Some(obj: JsObject) => obj
            case _ => JsObject.empty
          }
          handleGraphQLQuery(query, operation, vars)
      }
      .to {
        Sink.onComplete {
          _ =>
            killSwitches.shutdown
            queue.complete
        }
      }
    Flow.fromSinkAndSource(incomingFlow, Source.fromPublisher(publisher))
  }

  private def handleGraphQLQuery(query: String, operation: Option[String], variables: JsObject = JsObject.empty)
                                (implicit queue: SourceQueueWithComplete[Message],
                                 killSwitches: SharedKillSwitch): Unit = {
    import sangria.streaming.akkaStreams._
    QueryParser.parse(query) match {
      case Success(queryAst) =>
        queryAst.operationType(None) match {
          case Some(Subscription) =>
            Executor.execute(schema = graphQL.schema, queryAst = queryAst, operationName = operation, variables = variables)
              .viaMat(killSwitches.flow)(Keep.none)
              .runForeach {
                result =>
                  reply(result.compactPrint)
              }
          case _ =>
            reply(s"Unsupported type: ${queryAst.operationType(None)}")
        }
      case Failure(e: SyntaxError) =>
        reply(JsObject(
          "syntaxError" -> JsString(e.getMessage),
          "locations" -> JsArray(
            JsObject(
              "line" -> JsNumber(e.originalError.position.line),
              "column" -> JsNumber(e.originalError.position.column)
            )
          )
        ).toString)
      case Failure(_) =>
        reply("Internal Server Error")
    }
  }

  private def reply(message: String)(implicit queue: SourceQueueWithComplete[Message]): Unit = {
    queue.offer(TextMessage(message))
  }
}
