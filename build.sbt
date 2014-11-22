import sbtrelease._
import ReleaseStateTransformations._

organization := "org.allenai"

name := "project-starter"

enablePlugins(CliPlugin, ReleasePlugin)

val releaseArtifact = ReleaseStep(action = st => {
  val extracted = Project.extract(st)
  val (newState, env) = extracted.runTask(assembly, st)
  newState
})

libraryDependencies ++= Seq(
  Dependencies.allenAiCommon,
  Dependencies.akkaActor,
  Dependencies.sprayClient,
  Dependencies.typesafeConfig
)

ReleaseKeys.releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  releaseArtifact,
  tagRelease,
  setNextVersion,
  commitNextVersion,
  pushChanges
)
