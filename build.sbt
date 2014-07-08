name := "marsroverroot"

lazy val marsroverplay = (project in file("marsroverplay")).enablePlugins(PlayScala).dependsOn(marsroverapi).aggregate(marsroverapi)

lazy val marsrover = project
  .dependsOn(marsroverapi).aggregate(marsroverapi)

lazy val marsroverapi = project
