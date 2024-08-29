ThisBuild / organization := "io.simplifier"
ThisBuild / version := sys.env.get("VERSION").getOrElse("NA")
ThisBuild / scalaVersion := "2.12.15"

ThisBuild / useCoursier := true



lazy val contentRepoPlugin = (project in file("."))
  .settings(
    name := "ContentRepositoryPlugin",
    assembly / assemblyJarName := "contentRepoPlugin.jar",
    assembly / test := {},
    assembly / assemblyMergeStrategy := {
      case x if x.endsWith("module-info.class") => MergeStrategy.discard
      case "META-INF/native-image/native-image.properties" => MergeStrategy.discard
      case "META-INF/native-image/reflect-config.json" => MergeStrategy.discard
      case "META-INF/native-image/resource-config.json" => MergeStrategy.discard
      case x =>
        val oldStrategy = (assembly / assemblyMergeStrategy).value
        oldStrategy(x)
    },
    libraryDependencies ++= Seq(
      "mysql"                   %  "mysql-connector-java"    % "5.1.47"                                  ,
      "com.oracle.database.jdbc" % "ojdbc11-production"      % "23.4.0.24.05" pomOnly() exclude("com.oracle.database.xml", "xmlparserv2"),
      "com.h2database"          %  "h2"                      % "1.3.166"     withSources() withJavadoc(),
      "org.scalatest"           %% "scalatest"               % "3.1.4"       withSources() withJavadoc(),
      "org.mockito"             %% "mockito-scala"           % "1.17.7"     % "test"                     ,
      "io.github.simplifier-ag" %% "simplifier-plugin-base"  % "1.0.0"       withSources()
    )
  )

//Security Options for Java >= 18
val moduleSecurityRuntimeOptions = Seq(
  "--add-opens=java.base/java.lang=ALL-UNNAMED",
  "--add-opens=java.base/java.util=ALL-UNNAMED",
  "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED",
  "--add-opens=java.base/sun.security.jca=ALL-UNNAMED",
  // used by W3CXmlUtil
  "--add-exports=java.xml/com.sun.org.apache.xalan.internal.xsltc.trax=ALL-UNNAMED"
)

run / javaOptions ++= moduleSecurityRuntimeOptions
run / fork := true