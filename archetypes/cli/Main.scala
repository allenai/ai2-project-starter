package org.allenai.${name}

import scopt._

object Main extends App {
  case class Config(foo: String = "", bar: String = "")

  val parser = new OptionParser[Config]("${name}") {
    head("${name}", "0.1")
    opt[String]('f', "foo") required() valueName("<foo>") action { (x, c) =>
      c.copy(foo = x)
    } text("Foo is an important argument")

    opt[String]('b', "bar") required() valueName("<bar>") action { (x, c) =>
      c.copy(bar = x)
    } text("Bar is an important argument")
  }

  parser.parse(args, Config()) map { config =>
    println(config)
    sys.exit(0)
  }
}
