package org.allenai.${name}

import akka.actor.ActorSystem
import spray.routing.SimpleRoutingApp

object Main extends App with SimpleRoutingApp {
  implicit val system = ActorSystem("${name}")

  startServer(interface = "localhost", port = 8080) {
    path("hello") {
      get {
        complete {
          "hello, world!"
        }
      }
    }
  }
}
