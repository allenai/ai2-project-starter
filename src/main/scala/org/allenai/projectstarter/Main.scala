package org.allenai.projectstarter

import org.allenai.common.Config._
import org.allenai.common.json._

import akka.actor._
import com.typesafe.config.{ ConfigFactory, Config }
import scopt._
import spray.client.pipelining._
import spray.http._
import spray.httpx.SprayJsonSupport._
import spray.json._
import spray.json.DefaultJsonProtocol._

import java.nio.file.Files
import scala.sys.process._
import scala.util.Failure
import scala.util.Success

object Main extends App {
  println("Start you some project!")

  case class Archetype(id: String, plugin: String)

  case class Config(
    archetype: Archetype = Archetype("", ""),
    name: String = ""
  )

  val archetypes = Seq(
    Archetype("cli", "CliPlugin"),
    Archetype("webservice", "WebServicePlugin"),
    Archetype("webapp", "WebappPlugin"),
    Archetype("library", "LibraryPlugin")
  )

  val parser = new OptionParser[Config]("ai2-project-starter") {
    head("ai2-project-starter", "0.1")
    opt[String]('a', "archetype") required() valueName("<webapp|webservice|library|cli>") action { (x, c) =>
      archetypes.find(_.id == x) match {
        case Some(archetype) => c.copy(archetype = archetype)
        case None => throw new IllegalArgumentException("Invalid archetype")
      }
    } text("The project archetype")

    opt[String]('n', "name") required() valueName("<project-name>") action { (x, c) =>
      val NamePatt = "^([a-z]+)$".r
      x match {
        case NamePatt(name) => c.copy(name = name)
        case _ => throw new IllegalArgumentException("name must be lowercase and contain only alpha characters")
      }
    } text("The name of the project directory to create.")
  }

  parser.parse(args, Config()) map { config =>
    import java.io.File
    val creds = new File(System.getProperty("user.home")).toPath.resolve(".bintray/.credentials")

    if (!creds.toFile.exists) {
      sys.error("You must have bintray credentials stored in ~/.bintray/.credentials")
      sys.exit(1)
    }
    val c = ConfigFactory.parseFile(creds.toFile)
    val user = c[String]("user")
    val authToken = c[String]("password")

    implicit val system = ActorSystem("ai2-starter")
    import system.dispatcher

    val url = "https://api.bintray.com/packages/allenai/sbt-plugins/allenai-sbt-plugins/versions/_latest"

    val pipeline = (
      addCredentials(BasicHttpCredentials(user, authToken))
        ~> sendReceive
        ~> unmarshal[JsObject]
    )

    val futureResponse = pipeline(Get(url))

    futureResponse onSuccess {
      case js =>
        val pluginsVersion = js[String]("name")
        val tmp = Files.createTempDirectory("ai2-starter")
        tmp.toFile.deleteOnExit
        println(tmp.toFile.getAbsolutePath)

        s"git clone --depth 1 git@github.com:allenai/ai2-project-starter.git ${tmp.toFile.getAbsolutePath}".!!

        // get the plugin template
        val archetypeDir = tmp.resolve(s"archetypes/${config.archetype.id}")
        val packageDir = s"src/main/scala/org/allenai/${config.name}"
        val main = archetypeDir.resolve("Main.scala")
        val mainContent = io.Source.fromFile(main.toFile).getLines map { line =>
          line.replace("${name}", config.name)
        }

        s"mkdir -p ${config.name}/$packageDir".!!

        s"mkdir -p ${config.name}/project".!!

        // create a project/plugins.sbt
        val pluginsContent =
          s"""addSbtPlugin("org.allenai.plugins" % "allenai-sbt-plugins" % "${pluginsVersion}")"""

        // TODO get the sbt version dynamically?
        val buildPropsContent = "sbt.version=0.13.7"

        val buildContent =
          s"""|name := "${config.name}"
              |
              |enablePlugins(${config.archetype.plugin})""".stripMargin

        val projDir = new File(System.getProperty("user.dir")).toPath.resolve(config.name)
        Files.write(projDir.resolve("project/build.properties"), buildPropsContent.getBytes)
        Files.write(projDir.resolve("project/plugins.sbt"), pluginsContent.getBytes)
        Files.write(projDir.resolve("build.sbt"), buildContent.getBytes)

        Files.write(projDir.resolve(s"${packageDir}/Main.scala"), mainContent.mkString("\n").getBytes)

        s"rm -rf ${tmp.toFile.getAbsoluteFile}".!!
        system.shutdown()
        sys.exit(0)
    }

    futureResponse onFailure {
      case cause =>
        system.shutdown()
        sys.error("Failed to make request to bintray: " + cause.getMessage)
    }

    sys.addShutdownHook(system.shutdown())
    system.awaitTermination()
    // get latest from bintray
  } getOrElse {
    // arguments are bad, error message will have been displayed
  }
}
