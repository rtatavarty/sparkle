resolvers += "Typesafe repository" at "https://dl.bintray.com/typesafe/maven-releases/"

// The Play plugin
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.5.9")

// web plugins

addSbtPlugin("com.typesafe.sbt" % "sbt-coffeescript" % "1.0.0")

// addSbtPlugin("com.typesafe.sbt" % "sbt-less" % "1.0.6")

addSbtPlugin("org.irundaia.sbt" % "sbt-sassify" % "1.4.6")

addSbtPlugin("com.typesafe.sbt" % "sbt-scalariform" % "1.3.0")

addSbtPlugin("com.heroku" % "sbt-heroku" % "0.5.3")

// Packaging plugin

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.0.1")