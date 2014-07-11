import sbt._

object Version {

  val marsrover = "1.0.0"
  val akka      = "2.3.4"
  val play      = "2.3.0"
  val scala     = "2.11.1"
  val scalaTest = "2.2.0"
  val spec2     = "2.3.7"
  val raphaeljs = "2.1.2-1"
  val bootstrap = "3.1.1-1"
  val jline     = "2.10.2"
  val junit     = "4.11"
  val mockito   = "1.9.5"
}

object Library {
  val marsRoverApi    = "org.kaloz.excercise"   %% "marsroverapi"                  % Version.marsrover
  val akkaActor       = "com.typesafe.akka"     %% "akka-actor"                    % Version.akka
  val akkaRemote      = "com.typesafe.akka"     %% "akka-remote"                   % Version.akka
  val akkaCluster     = "com.typesafe.akka"     %% "akka-cluster"                  % Version.akka
  val akkaContrib     = "com.typesafe.akka"     %% "akka-contrib"                  % Version.akka
  val akkaPersistence = "com.typesafe.akka"     %% "akka-persistence-experimental" % Version.akka
  val akkaTestkit     = "com.typesafe.akka"     %% "akka-testkit"                  % Version.akka
  val jline           = "org.scala-lang"        %  "jline"                         % Version.jline
  val junit           = "junit"                 %  "junit"                         % Version.junit
  val mockito         = "org.mockito"           %  "mockito-core"                  % Version.mockito
  val scalaTest       = "org.scalatest"         %% "scalatest"                     % Version.scalaTest
  val spec2           = "org.specs2"            %% "specs2"                        % Version.spec2
  val playWebJar      = "org.webjars"           %% "webjars-play"                  % Version.play
  val raphaeljsWebJar = "org.webjars"           %  "raphaeljs"                     % Version.raphaeljs
  val bootstrapWebJar = "org.webjars"           %  "bootstrap"                     % Version.bootstrap
}

object Dependencies {

  import Library._

  val marsrover = List(
    marsRoverApi,
    akkaActor,
    akkaPersistence,
    akkaContrib,
    akkaRemote,
    jline,
    mockito       % "test",
    junit         % "test",
    akkaTestkit   % "test",
    spec2         % "test",
    scalaTest     % "test"
  )

  val marsroverplay = List(
    marsRoverApi,
    akkaRemote,
    akkaContrib,
    playWebJar,
    raphaeljsWebJar,
    bootstrapWebJar
  )

  val marsroverapi = List(
    akkaActor
  )
}