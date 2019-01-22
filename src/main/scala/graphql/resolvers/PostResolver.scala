package graphql.resolvers

import com.google.inject.Inject
import models.post.Post
import repositories.PostRepository

import scala.concurrent.{ExecutionContext, Future}

class PostResolver @Inject()(postRepository: PostRepository)
                            (implicit val executionContext: ExecutionContext) {
  def posts: Future[List[Post]] = postRepository.findAll()

  def addPost(title: String, content: String): Future[Post] = postRepository.create(Post(title = title, content = content))

  def findPost(id: Long): Future[Option[Post]] = postRepository.find(id)

  def updatePost(id: Long, title: String, content: String): Future[Post] = postRepository.update(Post(Some(id), title, content))

  def deletePost(id: Long): Future[Post] = postRepository.delete(id)
}
