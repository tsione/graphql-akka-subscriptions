package graphql.schemas

import akka.stream.ActorMaterializer
import com.google.inject.Inject
import graphql.resolvers.PostResolver
import models.post.PostType.{ADD, DELETE, UPDATE}
import models.post.{Post, PostType, PostWithType}
import publisher.PubSubService
import publisher.RichPubSubService._
import sangria.macros.derive._
import sangria.schema.{Argument, Field, ObjectType, _}
import sangria.streaming.akkaStreams._

import scala.concurrent.ExecutionContext

class PostSchema @Inject()(postResolver: PostResolver)
                          (implicit val pubSubService: PubSubService[PostWithType],
                           actorMaterializer: ActorMaterializer,
                           executionContext: ExecutionContext) {

  implicit val postModel: ObjectType[Unit, Post] = deriveObjectType[Unit, Post](ObjectTypeName("Post"))
  implicit val postTypeModel: EnumType[PostType.Value] = deriveEnumType[PostType.Value](IncludeValues("ADD", "UPDATE", "DELETE"))
  implicit val postWithTypeModel: ObjectType[Unit, PostWithType] = deriveObjectType[Unit, PostWithType](ObjectTypeName("PostWithType"))

  val queries: List[Field[Unit, Unit]] = List(
    Field(
      name = "posts",
      fieldType = ListType(postModel),
      resolve = _ => postResolver.posts
    ),
    Field(
      name = "findPost",
      fieldType = OptionType(postModel),
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
      fieldType = postModel,
      arguments = List(
        Argument("title", StringType),
        Argument("content", StringType)
      ),
      resolve = {
        sangriaContext =>
          val addedPost = postResolver.addPost(
            sangriaContext.args.arg[String]("title"),
            sangriaContext.args.arg[String]("content")
          )
          addedPost.map(post => {
            println("Post added...")
            PostWithType(ADD, post)
          }).pub
          addedPost
      }
    ),
    Field(
      name = "updatePost",
      fieldType = postModel,
      arguments = List(
        Argument("id", LongType),
        Argument("title", StringType),
        Argument("content", StringType)
      ),
      resolve = {
        sangriaContext =>
          val updatedPost = postResolver.updatePost(
            sangriaContext.args.arg[Long]("id"),
            sangriaContext.args.arg[String]("title"),
            sangriaContext.args.arg[String]("content")
          )
          updatedPost.map(post => PostWithType(UPDATE, post)).pub
          updatedPost
      }
    ),
    Field(
      name = "deletePost",
      fieldType = postModel,
      arguments = List(
        Argument("id", LongType)
      ),
      resolve = { sangriaContext =>
        val deletedPost = postResolver.deletePost(sangriaContext.args.arg[Long]("id"))
        deletedPost.map(post => PostWithType(DELETE, post)).pub
        deletedPost
      }
    )
  )
  val subscriptions: List[Field[Unit, Unit]] = List(
    Field.subs(
      name = "postUpdates",
      fieldType = postWithTypeModel,
      resolve = _ => {
        println("Subscribed")
        pubSubService.subscribe
      }
    )
  )
}
