package repositories

import com.google.inject.Inject
import models.errors.{AlreadyExists, NotFound}
import models.post.Post

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

class PostRepository @Inject()(implicit val executionContext: ExecutionContext) extends Repository[Post] {

  val postCollection: mutable.ArrayBuffer[Post] = mutable.ArrayBuffer.empty[Post]

  override def create(post: Post): Future[Post] = synchronized {
    postCollection.find(_.title == post.title).fold {
      Future {
        val newPost = post.copy(
          id = {
            val allIds = postCollection.flatMap(_.id)
            if (allIds.nonEmpty) Some(allIds.max + 1L) else Some(1L)
          }
        )
        postCollection += newPost
        newPost
      }
    } {
      _ => Future.failed(AlreadyExists(s"Post with title='${post.title}' already exists."))
    }
  }

  override def find(id: Long): Future[Option[Post]] = Future.successful {
    postCollection.find(_.id.contains(id))
  }

  override def findAll(): Future[List[Post]] = Future.successful {
    postCollection.toList
  }

  override def update(post: Post): Future[Post] = synchronized {
    post.id match {
      case Some(id) =>
        find(id).flatMap {
          case Some(_) =>
            val foundPostIndex = postCollection.indexWhere(_.id == post.id)
            postCollection(foundPostIndex) = post
            Future.successful(post)
          case _ => Future.failed(NotFound(s"Can't find post with id=${post.id}."))
        }
      case _ => Future.failed(NotFound("Post's id wasn't provided."))
    }
  }

  override def delete(id: Long): Future[Post] = synchronized {
    postCollection.indexWhere(_.id.contains(id)) match {
      case -1 => Future.failed(NotFound(s"Can't find post with id=$id."))
      case personIndex => Future(postCollection.remove(personIndex))
    }
  }
}
