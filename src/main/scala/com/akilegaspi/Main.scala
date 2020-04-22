package com.akilegaspi

import zio._
import zio.console._
import java.sql.{ Connection, DriverManager }
import Types._
import zio.UIO
import TodoRepo._

object MyApp extends App {

  type TodoEnv = Console with TodoRepo

  val initializeSqlite: UIO[Connection] = for {
    connection <-  UIO(DriverManager.getConnection("jdbc:sqlite:ziodemo.db"))
    _ <- createDb(connection)
  } yield connection

  val sqliteLayer: Layer[Nothing, Has[Connection]] =
    ZLayer.fromAcquireRelease(initializeSqlite)(conn => UIO(conn.close()))

  val jdbcLayer: Layer[Nothing, TodoRepo] = sqliteLayer >>> TodoRepo.jdbc
  
  val dependencies = Console.live ++ jdbcLayer

  def run(args: List[String]) = (for {
    _ <- main.provideLayer(dependencies)
  } yield ()).fold(_ => 1, _ => 0)

  def createDb(connection: Connection): UIO[Unit] = UIO {
    val statement = connection.createStatement()
    ()
  }

  val main = for {
    _ <- putStrLn("====================")
    _ <- putStrLn("These are your TODOs")
    _ <- putStrLn("====================")
    todos <- TodoRepo.listTodos
    _ <- printAllTodos(todos)
    _ <- putStrLn("Menu Options")
    _ <- putStrLn("1. Create a new todo")
    _ <- putStrLn("2. Print a todo by id")
    _ <- putStrLn("Other input exits")
    input <- getStrLn
    _ <- input match {
      case "1" => createTodo
      case "2" => getTodoAndPrint
      case other => UIO.unit
    }
  } yield ()

  val getTodoAndPrint: RIO[Console with TodoRepo, Unit] = for {
    _ <- putStrLn("Getting your todo")
    _ <- putStr("Input the ID of your TODO")
    id <- getStrLn
    todoOp <- TodoRepo.getTodo(id)
    _ <- todoOp match {
      case Some(todo) => printTodo(todo)
      case None => putStrLn("The TODO for that ID doesn't exist")
    }
  } yield ()

  val createTodo: RIO[Console with TodoRepo, Unit] = for {
    _ <- putStrLn("Creating a new TODO")
    _ <- putStr("Input the title of your TODO: ")
    name <- getStrLn
    _ <- putStr("Input the description of your TODO: ")
    desc <- getStrLn
    _ <- putStrLn("Your TODO to be saved")
    _ <- putStrLn(s"Name: $name")
    _ <- putStrLn(s"Description: $desc")
    id <- TodoRepo.putTodo(Todo(name, desc))
    _ <- putStrLn("The id of your TODO: $id")
  } yield ()

  def printAllTodos(todos: List[Todo]): RIO[Console, Unit] = ZIO.foreach_(todos.zipWithIndex){ case (todo, index) =>
    putStrLn(s"[$index] Name: ${todo.name} Description: ${todo.description}")
  }

  def printTodo(todo: Todo) =
    putStrLn(s"Name: ${todo.name} Description: ${todo.description}")

}
