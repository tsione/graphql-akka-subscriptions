package repositories

import scala.concurrent.Future

trait Repository[T] {
  def create(item: T): Future[T]

  def find(id: Long): Future[Option[T]]

  def findAll(): Future[List[T]]

  def update(item: T): Future[T]

  def delete(id: Long): Future[Boolean]
}
