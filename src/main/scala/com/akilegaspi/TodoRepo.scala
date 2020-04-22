package com.akilegaspi

import zio. {
  Has,
  ZLayer,
  IO
}
import java.sql.Connection
import com.akilegaspi.Types._

import scala.Exception
import java.sql.Statement
import java.{util => ju}
import zio.Task
import zio.Queue
import scala.collection.mutable
import zio.RIO



object TodoRepo {

  type TodoRepo = Has[TodoRepo.Service]

  trait Service {
    def putTodo(data: Todo): Task[String]
    def listTodos: Task[List[Todo]]
    def getTodo(id: String): Task[Option[Todo]]
  }

  val jdbc: ZLayer[Has[Connection], Nothing, TodoRepo] = ZLayer.fromFunction { connection =>
    new Service {
      def putTodo(data: Todo): Task[String] = Task {
        val id = ju.UUID.randomUUID().toString()
        val statement = connection.get.createStatement()
        statement.setQueryTimeout(30)
        statement.executeUpdate(s"INSERT INTO todo VALUES(${id}, ${data.name}, ${data.description})")
        id
      }
      def getTodo(id: String): Task[Option[Todo]] = Task {
        val statement = connection.get.createStatement()
        val result = statement.executeQuery(s"SELECT * FROM todo WHERE id = $id")
        if(result.first()) {
          val name = result.getString("name")
          val desc = result.getString("description")
          Some(Todo(name, desc))
        } else None
      }

      def listTodos: Task[List[Todo]] = Task {
        val statement = connection.get.createStatement()
        val res = statement.executeQuery(s"SELECT * FROM todo")
        var results = List.empty[Todo]
        while(res.next()){
          val name = res.getString("name")
          val desc = res.getString("description")
          results = results :+ Todo(name, desc)
        }
        results
      }
    }
  }

  val inMemory: ZLayer[mutable.Map[String, Todo], Nothing, TodoRepo] = ZLayer.fromFunction { map =>
    new Service {
      def putTodo(data: Todo): Task[String] = Task {
        val id = ju.UUID.randomUUID().toString()
        map += (id -> data)
        id
      }

      def getTodo(id: String): zio.Task[Option[Todo]] = Task {
        map.get(id)
      }

      def listTodos: zio.Task[List[Types.Todo]] = Task {
        map.toList.map(_._2)
      }
    }
  }

  val test: ZLayer[Any, Nothing, TodoRepo] = ZLayer.succeed {
    new Service {
      def putTodo(data: Todo): Task[String] = Task.succeed("fakeId")
      def getTodo(id: String): Task[Option[Todo]] = Task.succeed(None)

      def listTodos: zio.Task[List[Types.Todo]] = Task.succeed(List.empty[Todo])
    }
  }

  def putTodo(data: Todo): RIO[TodoRepo, String] =
    RIO.accessM(_.get.putTodo(data))

  def getTodo(id: String): RIO[TodoRepo, Option[Todo]] =
    RIO.accessM(_.get.getTodo(id))

  def listTodos: RIO[TodoRepo, List[Todo]] =
    RIO.accessM(_.get.listTodos)

}
