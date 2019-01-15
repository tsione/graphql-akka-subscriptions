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
import spray.json.{JsObject, _}

import scala.util.{Failure, Success}

class WebSocketHandler @Inject()(graphQL: GraphQL)
                                (implicit val actorMaterializer: ActorMaterializer,
                                 val scheduler: Scheduler) {
  def handleMessages: Flow[Message, Message, NotUsed] = {
    implicit val (queue, publisher) = Source.queue[Message](16, OverflowStrategy.backpressure)
      .toMat(Sink.asPublisher(false))(Keep.both)
      .run()
    implicit val killSwitches: SharedKillSwitch = KillSwitches.shared("WebSocketHandler")
    val incomingFlow = Flow[Message]
      .collect {
        case TextMessage.Strict(message) =>
          val (query, operation, variables) = JsonUtils.parseGraphQLQuery(message.parseJson)
          handleGraphQLQuery(query, operation, variables)
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
                                 killSwitches: SharedKillSwitch) = {
    import sangria.streaming.akkaStreams._
    QueryParser.parse(query) match {
      case Success(queryAst) =>
        queryAst.operationType(None) match {
          case Some(Subscription) =>
            Executor.execute(graphQL.schema, queryAst, (), (), operation, variables)
              .viaMat(killSwitches.flow)(Keep.none)
              .runForeach {
                result =>
                  reply(result.compactPrint)
              }
          case _ => reply(s"Unsupported type: ${queryAst.operationType(None)}")
        }
      case Failure(e: SyntaxError) => reply(e.toString)
      case Failure(_) => reply("Internal Server Error")
    }
  }

  private def reply(message: String)(implicit queue: SourceQueueWithComplete[Message]): Unit = {
    queue.offer(TextMessage(message))
  }
}
