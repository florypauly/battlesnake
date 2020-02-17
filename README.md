starter-snake-kotlin
===

[![Build Status](https://travis-ci.org/athenian-programming/starter-snake-kotlin.svg?branch=master)](https://travis-ci.org/athenian-programming/starter-snake-kotlin)

A simple [Battlesnake](http://battlesnake.io) written in Kotlin.

Visit [https://docs.battlesnake.io](https://docs.battlesnake.io) 
for API documentation and instructions for creating a game.

This snake is built using a lightweight http server Spark framework - [http://sparkjava.com/documentation](http://sparkjava.com/documentation)

[![Deploy](https://www.herokucdn.com/deploy/button.png)](https://heroku.com/deploy)

Requirements
---

- Install [JDK 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
- Install [Kotlin](https://kotlinlang.org)
- Install [Gradle](https://gradle.org/install/)

Running the snake
---

Assign the `mainName` variable in [build.gradle](build.gradle) to the proper Snake classname.

Use [ngrok](https://ngrok.com) to make a locally running snake visible to the BattleSnake server.

```bash
./gradlew run
```

The snake will start up on port 8080.

Run the tests
---

```bash
./gradlew test
```

Executable UberJar
---

Build the uberjar `build/libs/starter-snake-kotlin.jar` with:

```bash
./gradlew stage
```

Run the uberjar with:

```bash
java -jar build/libs/starter-snake-kotlin.jar
```


Deploying to Heroku
---

1) Create a new Heroku app with:
```
heroku create [APP_NAME]
```

2) Deploy code to Heroku with:
```
git push heroku master
```

3) Open Heroku app in browser with:
```
heroku open
```
or visit [http://APP_NAME.herokuapp.com](http://APP_NAME.herokuapp.com).

4) View server logs with the `heroku logs` command with:
```
heroku logs --tail
```
