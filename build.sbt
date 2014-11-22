organization := "org.allenai"

name := "project-starter"

enablePlugins(CliPlugin)

libraryDependencies ++= Seq(
  Dependencies.allenAiCommon,
  Dependencies.akkaActor,
  Dependencies.sprayClient,
  Dependencies.typesafeConfig
)
