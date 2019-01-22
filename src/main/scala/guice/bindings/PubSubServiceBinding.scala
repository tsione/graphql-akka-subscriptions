package guice.bindings

import models.post.PostWithType
import net.codingwell.scalaguice.ScalaModule
import publisher.{PubSubService, PubSubServiceImpl}

class PubSubServiceBinding extends ScalaModule {
  override def configure() {
    bind[PubSubService[PostWithType]].to[PubSubServiceImpl[PostWithType]]
  }
}
