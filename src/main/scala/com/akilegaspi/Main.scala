package com.akilegaspi

import zio._
import zio.console._
import java.sql.{ Connection, DriverManager }
import Types._
import zio.UIO
import TodoRepo._

object MyApp extends App {

  type TodoEnv = Console with TodoRepo



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

  val initializeSqlite: Task[Connection] = for {
    connection <-  Task(DriverManager.getConnection("jdbc:sqlite:ziodemo.db"))
    _ <- initializeDbOrNot(connection)
  } yield connection

  val sqliteLayer: Layer[Throwable, Has[Connection]] =
    ZLayer.fromAcquireRelease(initializeSqlite)(conn => UIO(conn.close()))

  val jdbcLayer: Layer[Throwable, TodoRepo] = sqliteLayer >>> TodoRepo.jdbc

  def inMemoryLayer(mapRef: Ref[Map[String, Todo]]): Layer[Nothing, TodoRepo] =  ZLayer.succeed(mapRef) >>> TodoRepo.inMemory

  def mainWithInMemoryLayer(mapRef: Ref[Map[String, Todo]]) = main.provideCustomLayer(inMemoryLayer(mapRef))

  val mainWithJDBCLayer = main.provideCustomLayer(jdbcLayer)

  def run(args: List[String]) = (for {
    ref <- Ref.make(Map.empty[String, Todo])
    _ <- mainWithInMemoryLayer(ref).forever
  } yield ()).foldM(e => putStrLn(e.getMessage()).flatMap(_ => UIO.succeed(1)), _ => UIO.succeed(0))


  def initializeDbOrNot(connection: Connection): UIO[Unit] = UIO {
    val dbMeta = connection.getMetaData()
    val rs = dbMeta.getTables(null, null, "%", null)
    while(rs.next()){
      println(rs.getString(3))
    }
  }

  val getTodoAndPrint: RIO[Console with TodoRepo, Unit] = for {
    _ <- putStrLn("Getting your todo")
    _ <- putStr("Input the ID of your TODO: ")
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
    _ <- putStrLn(s"The id of your TODO: $id")
  } yield ()

  def printAllTodos(todos: List[Todo]): RIO[Console, Unit] = ZIO.foreach_(todos.zipWithIndex){ case (todo, index) =>
    putStrLn(s"[$index] Name: ${todo.name} Description: ${todo.description}")
  }

  def printTodo(todo: Todo) =
    putStrLn(s"Name: ${todo.name} Description: ${todo.description}")

}
