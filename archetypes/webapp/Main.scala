ackage org.allenai.${name}

import akka.actor.ActorSystem
import spray.routing.SimpleRoutingApp
import spray.http.StatusCodes

object Main extends App with SimpleRoutingApp {
  implicit val system = ActorSystem("${name}")

  // This cache-header instructs clients to cache resources for 1 year.  We attach this to
  // the routes responsible for serving static resources and use cache-breaking query string
  // variables generated at buidl time to force clients to download the most recent resources
  // when necessary.
  val cacheControl =
        `Cache-Control`(public, `max-age`(60L * 60L * 24L * 365L)))

  startServer(interface = "localhost", port = 8080) {
    pathPrefix("api") {
      path("hello") {
        get {
          complete {
            "hello, world!"
          }
        }
      } ~
      complete(StatusCodes.NotFound)
    } ~
    pathEndOrSingleSlash(getFromFile("public/index.html")) ~
    respondWithHeaders(cacheControl) {
      getFromDirectory("public")
    } ~
    getFromFile("public/index.html")
  }
}
