package publisher

import akka.NotUsed
import akka.stream.scaladsl.Source
import javax.inject.Inject
import monix.execution.Scheduler
import monix.reactive.subjects.PublishSubject
import sangria.schema.Action

class PubSubServiceImpl[T] @Inject()(implicit val scheduler: Scheduler) extends PubSubService[T] {

  private lazy val source = PublishSubject[T]

  override def publish(event: T): Unit = source.onNext(event)

  override def subscribe: Source[Action[Nothing, T], NotUsed] = {
    Source.fromPublisher(source.toReactivePublisher[T]).map {
      element =>
        Action(element)
    }
  }
}
