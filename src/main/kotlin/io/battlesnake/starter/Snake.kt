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
        private var bodyX = ArrayList<Int>()
        private var bodyY = ArrayList<Int>()
        private var bodyLength = 0
        private var headX = 0
        private var headY = 0
        private var foodX = 0
        private var foodY = 0
        private var isThereFood = false
        private var width = 0
        private var height = 0

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
         * /ping is called by the play application during the tournament or on play.battlesnake.io to make sure your
         * snake is still alive.
         *
         * @return an empty response.
         */
        fun ping(): Map<String, String> {
            return emptyMap()
        }

        /**
         * /start is called by the engine when a game is first run.
         *
         * @param startRequest a map containing the JSON sent to this snake. See the spec for details of what this contains.
         * @return a response back to the engine containing the snake setup values.
         */
        fun start(startRequest: JsonNode): Map<String, String> {
            // Set the body length
            bodyLength = 3
            headX = startRequest["you"]["body"][0]["x"].asInt()
            headY = startRequest["you"]["body"][0]["y"].asInt()

            bodyX.add(headX)
            bodyY.add(headY)

            width = startRequest["board"]["width"].asInt()
            height = startRequest["board"]["height"].asInt()

            return mapOf("color" to "#ff00ff", "headType" to "beluga", "tailType" to "bolt")
        }

        /**
         * /move is called by the engine for each turn the snake has.
         *
         * @param moveRequest a map containing the JSON sent to this snake. See the spec for details of what this contains.
         * @return a response back to the engine containing snake movement values.
         */
        fun move(moveRequest: JsonNode): Map<String, String> {

            /*
            val turn = moveRequest.get("turn").asInt();
            if (turn % 4 == 0) return mapOf("move" to "right")
            else if (turn % 4 == 1) return mapOf("move" to "down")
            else if (turn % 4 == 2) return mapOf("move" to "left")
            else if (turn % 4 == 3) return mapOf("move" to "up")
            */

            headX = moveRequest["you"]["body"][0]["x"].asInt()
            headY = moveRequest["you"]["body"][0]["y"].asInt()

            bodyX.add(0, headX)
            bodyY.add(0, headY)

            // Is there any food?
            if (isAFoodExist(moveRequest)) {
                // Did the snake get a food?
                // If so, make it grows!
                isFoodGot (moveRequest)
            }

            // Update the tale's place
            if (bodyLength < bodyX.size) {
                bodyX.remove(bodyX.size - 1)
                bodyY.remove(bodyY.size - 1)
            }

            return seekFood(moveRequest)
        }

        fun isAFoodExist(moveRequest: JsonNode): Boolean {
            if (moveRequest["board"]["food"].size() != 0) {
                isThereFood = true

                foodX = moveRequest["board"]["food"][0]["x"].asInt()
                foodY = moveRequest["board"]["food"][0]["y"].asInt()

                return true
            }

            return false
        }

        fun isFoodGot (moveRequest: JsonNode) {
            if ((foodX == headX) && (foodY == headY)) {
                bodyLength++
                isThereFood = false
            }
        }

        fun seekFood (moveRequest: JsonNode): Map<String, String> {
            // If there is a food
            if (isThereFood) {
                var relativeX = headX - foodX
                var relativeY = headY - foodY

                // You don't have to worry about crashing a wall
                // You need to worry about your body and other snakes
                if (relativeX > 0) {
                    if (    !(bodyX.contains(headX - 1) && bodyY.contains(headY) &&
                            bodyX.indexOf(headX - 1) == bodyY.indexOf(headY))
                    ) {
                        return mapOf("move" to "left")
                    }
                } else if (relativeX < 0) {
                    if (    !(bodyX.contains(headX + 1) && bodyY.contains(headY) &&
                                    bodyX.indexOf(headX + 1) == bodyY.indexOf(headY))) {
                        return mapOf("move" to "right")
                    }
                } else {
                    if (relativeY > 0) {
                        return mapOf("move" to "down")
                    } else {
                        return mapOf("move" to "up")
                    }
                }

                if (relativeY > 0) {
                    if (    !(bodyX.contains(headX) && bodyY.contains(headY - 1) &&
                                    bodyX.indexOf(headX) == bodyY.indexOf(headY - 1))
                    ) {
                        return mapOf("move" to "down")
                    }
                } else if (relativeY < 0) {
                    if (    !(bodyX.contains(headX) && bodyY.contains(headY + 1) &&
                                    bodyX.indexOf(headX) == bodyY.indexOf(headY + 1))
                    ) {
                        return mapOf("move" to "up")
                    }
                } else {
                    if (relativeX > 0) {
                        return mapOf("move" to "left")
                    } else {
                        return mapOf("move" to "right")
                    }
                }
            }

            return mapOf("move" to "right")
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
    }
}
