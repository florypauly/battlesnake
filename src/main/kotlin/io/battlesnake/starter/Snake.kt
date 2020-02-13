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
            return mapOf("color" to "#ff00ff", "headType" to "beluga", "tailType" to "bolt")
        }

        /**
         * /move is called by the engine for each turn the snake has.
         *
         * @param moveRequest a map containing the JSON sent to this snake. See the spec for details of what this contains.
         * @return a response back to the engine containing snake movement values.
         */
        fun move(moveRequest: JsonNode): Map<String, String> {
            val turn = moveRequest.get("turn").asInt();

            if (turn % 4 == 0) return mapOf("move" to "right")
            else if (turn % 4 == 1) return mapOf("move" to "down")
            else if (turn % 4 == 2) return mapOf("move" to "left")
            else if (turn % 4 == 3) return mapOf("move" to "up")

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
