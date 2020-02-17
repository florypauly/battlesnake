package io.battlesnake.starter

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KLogging
import spark.Request
import spark.Response
import spark.Spark.get
import spark.Spark.port
import spark.Spark.post

/**
 * Snake server that deals with requests from the snake engine.
 * Just boiler plate code.  See the readme to get started.
 * It follows the spec here: https://github.com/battlesnakeio/docs/tree/master/apis/snake
 */
object Snake : KLogging() {
    private val JSON_MAPPER = ObjectMapper()
    private val HANDLER = Handler()

    /**
     * Main entry point.
     *
     * @param args are ignored.
     */
    @JvmStatic
    fun main(args: Array<String>) {
        val port = Integer.parseInt(System.getProperty("PORT") ?: "8080")
        logger.info { "Listening on port: $port" }
        port(port)

        get("/") { _, _ ->
            "Battlesnake documentation can be found at " +
            "<a href=\"https://docs.battlesnake.io\">https://docs.battlesnake.io</a>."
        }
        post("/start",
             { req, res -> HANDLER.process(req, res) },
             { JSON_MAPPER.writeValueAsString(it) })
        post("/ping",
             { req, res -> HANDLER.process(req, res) },
             { JSON_MAPPER.writeValueAsString(it) })
        post("/move",
             { req, res -> HANDLER.process(req, res) },
             { JSON_MAPPER.writeValueAsString(it) })
        post(
                "/end",
                { req, res -> HANDLER.process(req, res) },
                { JSON_MAPPER.writeValueAsString(it) })
    }

    /**
     * Handler class for dealing with the routes set up in the main method.
     */
    class Handler {
        /**
         * Generic processor that prints out the request and response from the methods.
         *
         * @param req
         * @param res
         * @return
         */
        fun process(req: Request, res: Response): Map<String, String>? {
            return try {
                val uri = req.uri()
                logger.info { "$uri called with: ${req.body()}" }
                val snakeResponse: Map<String, String> =
                        when (uri) {
                            "/ping"  -> ping()
                            "/start" -> start(JSON_MAPPER.readTree(req.body()))
                            "/move"  -> move(JSON_MAPPER.readTree(req.body()))
                            "/end"   -> end(JSON_MAPPER.readTree(req.body()))
                            else     -> throw IllegalAccessError("Strange call made to the snake: $uri")
                        }

                logger.info { "Responding with: ${JSON_MAPPER.writeValueAsString(snakeResponse)}" }
                snakeResponse
            } catch (e: Exception) {
                logger.warn(e) { "Something went wrong!" }
                null
            }

        }

        /**
         * /end is called by the engine when a game is complete.
         *
         * @param endRequest a map containing the JSON sent to this snake. See the spec for details of what this contains.
         * @return responses back to the engine are ignored.
         */
        fun end(endRequest: JsonNode): Map<String, String> {
            return emptyMap()
        }

        /**
         * /ping is called by the play application during the tournament or on play.battlesnake.io to make sure your
         * snake is still alive.
         *
         * @return an empty response.
         */
        fun ping(): Map<String, String> {
            return emptyMap()
        }

        private var bodyX = ArrayList<Int>()
        private var bodyY = ArrayList<Int>()
        private var bodyLength = 0
        private var headX = 0
        private var headY = 0
        private var isThereFood = false
        private var width = 0
        private var height = 0
        private var foodPositionX = ArrayList<Int>()
        private var foodPositionY = ArrayList<Int>()
        private var upX = 0
        private var upY = 0
        private var downX = 0
        private var downY = 0
        private var leftX = 0
        private var leftY = 0
        private var rightX = 0
        private var rightY = 0

        /**
         * /start is called by the engine when a game is first run.
         *
         * @param startRequest a map containing the JSON sent to this snake. See the spec for details of what this contains.
         * @return a response back to the engine containing the snake setup values.
         */
        fun start(startRequest: JsonNode): Map<String, String> {
            // Make a board
            setBoard(startRequest)

            // set snake body
            setSnakeBody(startRequest)

            // set food positions
            setFoodPosition(startRequest)

            return mapOf("color" to "#00ff00", "headType" to "beluga", "tailType" to "bolt")
        }

        /**
         * /move is called by the engine for each turn the snake has.
         *
         * @param moveRequest a map containing the JSON sent to this snake. See the spec for details of what this contains.
         * @return a response back to the engine containing snake movement values.
         */
        fun move(moveRequest: JsonNode): Map<String, String> {

            // set snake body
            setSnakeBody(moveRequest)

            // set food positions
            setFoodPosition(moveRequest)

            if (isThereFood) {
                return seekFood(moveRequest)
            }

            return walkAround(moveRequest)
        }

        private fun seekFood (moveRequest: JsonNode): Map<String, String> {
            var relativeX = headX - foodPositionX.get(0)
            var relativeY = headY - foodPositionY.get(0)


            // if food locates left
            if (relativeX > 0) {
                if (isCollide(leftX, leftY)) {
                        return mapOf("move" to "left")
                }
            } else if (relativeX < 0) {
                if (!isCollide(rightX, rightY)) {
                    return mapOf("move" to "right")
                }
            } else {
                if (relativeY > 0) {
                    if (!isCollide(upX, upY)) {
                        return mapOf("move" to "up")
                    }
                } else if (relativeY < 0) {
                    if (!isCollide(downX, downY)) {
                        return mapOf("move" to "down")
                    }
                }
            }

            if (relativeY > 0) {
                if (!isCollide(downX, downY)) {
                    return mapOf("move" to "down")
                }
            } else if (relativeY < 0) {
                if (!isCollide(upX, upY)) {
                    return mapOf("move" to "up")
                }
            } else {
                if (relativeX > 0) {
                    if (!isCollide(leftX, leftY)) {
                        return mapOf("move" to "left")
                    }
                } else if (relativeX < 0)  {
                    if (!isCollide(rightX, rightY)) {
                        return mapOf("move" to "right")
                    }
                }
            }

            // Return 'right' move anyway
            // Improve this later
            return mapOf("move" to "right")
        }

        private fun walkAround(moveRequest: JsonNode): Map<String, String> {
            // Check up
            if (!isCollide(upX, upY)) {
                return mapOf("move" to "up")
            }

            // Check down
            if (!isCollide(downX, downY)) {
                return mapOf("move" to "down")
            }

            // Check right
            if (!isCollide(rightX, rightY)) {
                return mapOf("move" to "right")
            }

            // Check left
            if (!isCollide(leftX, leftY)) {
                return mapOf("move" to "left")
            }

            return mapOf("move" to "right")
        }

        private fun isCollide (checkX:Int, checkY:Int): Boolean {
            // Improve later to check enemy snakes body
            return isSelfDestructive(checkX, checkY) && isWall(checkX, checkY)
        }

        private fun isSelfDestructive (checkX:Int, checkY:Int): Boolean {
            for (i in 0..(bodyX.size - 1)) {
                if (checkX == bodyX[i] && checkY == bodyY[i]) {
                    return true
                }
            }

            return false
        }

        private fun isWall (checkX:Int, checkY:Int): Boolean {
            if (    checkX < 0 || checkX > width ||
                    checkY < 0 || checkY > height) {
                return true
            }

            return false
        }

        private fun setFoodPosition(moveRequest: JsonNode) {
            if (moveRequest["board"]["food"].size() != 0) {
                isThereFood = true

                // clear food positions
                foodPositionX.clear()
                foodPositionY.clear()

                for (i in 0..(moveRequest["board"]["food"].size() - 1)) {
                    foodPositionX.add(moveRequest["board"]["food"][i]["x"].asInt())
                    foodPositionY.add(moveRequest["board"]["food"][i]["y"].asInt())
                }
            } else {
                isThereFood = false
            }
        }

        private fun setBoard(moveRequest: JsonNode) {
            width = moveRequest["board"]["width"].asInt()
            height = moveRequest["board"]["height"].asInt()
        }

        private fun setSnakeBody(moveRequest: JsonNode) {
            bodyX.clear()
            bodyY.clear()

            for (i in 0..moveRequest["you"]["body"].size() - 1) {
                bodyX.add(moveRequest["you"]["body"][i]["x"].asInt())
                bodyY.add(moveRequest["you"]["body"][i]["y"].asInt())
            }

            // set head
            headX = moveRequest["you"]["body"][0]["x"].asInt()
            headY = moveRequest["you"]["body"][0]["y"].asInt()

            // set UpRightDownLeft
            setUpRightDownLeft()

            // set body length
            bodyLength = bodyX.size
        }

        private fun setUpRightDownLeft() {
            // set up
            upX = headX
            upY = headY - 1

            // set down
            downX = headX
            downY = headY + 1

            // set left
            leftX = headX - 1
            leftY = headY

            // set right
            rightX = headX + 1
            rightY = headY
        }
    }
}
