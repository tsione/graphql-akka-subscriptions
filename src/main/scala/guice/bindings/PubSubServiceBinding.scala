package guice.bindings

import models.Post
import net.codingwell.scalaguice.ScalaModule
import publisher.{PubSubService, PubSubServiceImpl}

class PubSubServiceBinding extends ScalaModule {
  override def configure() {
    bind[PubSubService[Post]].to[PubSubServiceImpl[Post]]
  }
}
