package graphql.schemas

import akka.stream.ActorMaterializer
import com.google.inject.Inject
import graphql.resolvers.PostResolver
import models.Post
import publisher.PubSubService
import sangria.macros.derive.{ObjectTypeName, deriveObjectType}
import sangria.schema._
import sangria.streaming.akkaStreams._

import scala.concurrent.ExecutionContext

class PostSchema @Inject()(postResolver: PostResolver)
                          (implicit val pubSubService: PubSubService[Post],
                           actorMaterializer: ActorMaterializer,
                           executionContext: ExecutionContext) {

  implicit val postType: ObjectType[Unit, Post] = deriveObjectType[Unit, Post](ObjectTypeName("Post"))

  val queries: List[Field[Unit, Unit]] = List(
    Field(
      name = "posts",
      fieldType = ListType(postType),
      resolve = _ => postResolver.posts
    ),
    Field(
      name = "findPost",
      fieldType = OptionType(postType),
      arguments = List(
        Argument("id", LongType)
      ),
      resolve =
        sangriaContext =>
          postResolver.findPost(sangriaContext.args.arg[Long]("id"))
    )
  )
  val mutations: List[Field[Unit, Unit]] = List(
    Field(
      name = "addPost",
      fieldType = postType,
      arguments = List(
        Argument("title", StringType),
        Argument("content", StringType)
      ),
      resolve = sangriaContext =>
        postResolver.addPost(
          sangriaContext.args.arg[String]("title"),
          sangriaContext.args.arg[String]("content")
        )
    ),
    Field(
      name = "updatePost",
      fieldType = postType,
      arguments = List(
        Argument("id", LongType),
        Argument("title", StringType),
        Argument("content", StringType)
      ),
      resolve = sangriaContext =>
        postResolver.updatePost(
          sangriaContext.args.arg[Long]("id"),
          sangriaContext.args.arg[String]("title"),
          sangriaContext.args.arg[String]("content")
        )
    ),
    Field(
      name = "deletePost",
      fieldType = BooleanType,
      arguments = List(
        Argument("id", LongType)
      ),
      resolve =
        sangriaContext =>
          postResolver.deletePost(sangriaContext.args.arg[Long]("id"))
    )
  )
  val subscriptions: List[Field[Unit, Unit]] = List(
    Field.subs(
      name = "postUpdates",
      fieldType = postType,
      resolve = _ => pubSubService.subscribe
    )
  )
}
