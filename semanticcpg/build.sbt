name := "semanticcpg"

val flatGraphVersion = "0.0.0+106-0015a507"

libraryDependencies ++= Seq(
  "io.joern" %% "joern-generated" % flatGraphVersion,
  "io.shiftleft"  %% "codepropertygraph" % Versions.cpg,
  "com.michaelpollmeier" %% "scala-repl-pp" % Versions.scalaReplPP,
  "org.json4s"    %% "json4s-native"     % Versions.json4s,
  "org.scala-lang.modules" %% "scala-xml" % "2.2.0",
  "commons-lang" % "commons-lang" % "2.6",
  "org.scalatest" %% "scalatest"         % Versions.scalatest % Test
)

Compile/compile/scalacOptions ++= Seq(
  // TODO remove
  "-Wconf:any:silent", // silence warnings for now
)
