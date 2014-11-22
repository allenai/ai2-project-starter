import sbtrelease._
import ReleaseStateTransformations._

organization := "org.allenai"

name := "project-starter"

enablePlugins(CliPlugin, ReleasePlugin)

val releaseArtifact = ReleaseStep(action = st => {
  assembly
  st
})

libraryDependencies ++= Seq(
  Dependencies.allenAiCommon,
  Dependencies.akkaActor,
  Dependencies.sprayClient,
  Dependencies.typesafeConfig
)

ReleaseKeys.releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,              // : ReleaseStep
  inquireVersions,                        // : ReleaseStep
  runTest,                                // : ReleaseStep
  setReleaseVersion,                      // : ReleaseStep
  commitReleaseVersion,                   // : ReleaseStep, performs the initial git checks
  releaseArtifact,
  tagRelease,                             // : ReleaseStep
  setNextVersion,                         // : ReleaseStep
  commitNextVersion,                      // : ReleaseStep
  pushChanges                             // : ReleaseStep, also checks that an upstream branch is properly configured
)
