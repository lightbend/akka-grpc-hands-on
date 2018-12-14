lazy val akkaHttpVersion = "10.1.5"
lazy val akkaVersion    = "2.5.19"


lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization    := "grpc.demo",
      scalaVersion    := "2.12.8"
    )),
    name := "akka-grpc-demo",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http"            % akkaHttpVersion,
      "com.lightbend.akka.discovery" %% "akka-discovery-config" % "0.20.0",
      "org.scalatest"     %% "scalatest"            % "3.0.5"         % Test
    )
  )

// in build.sbt:
enablePlugins(AkkaGrpcPlugin)

akkaGrpcGeneratedSources := Seq(AkkaGrpc.Client)

// ALPN agent
enablePlugins(JavaAgent)
javaAgents += "org.mortbay.jetty.alpn" % "jetty-alpn-agent" % "2.0.9" % "runtime;test"
