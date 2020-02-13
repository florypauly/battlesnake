package io.battlesnake.dsl

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KLogging
import spark.Request
import spark.Response
import spark.Spark.get
import spark.Spark.port
import spark.Spark.post

fun snake(init: Snake.() -> Unit): Snake {
    val snake = Snake()
    init.invoke(snake)
    return snake
}

/**
 * Snake server that deals with requests from the snake engine.
 * Just boiler plate code.  See the readme to get started.
 * It follows the spec here: https://github.com/battlesnakeio/docs/tree/master/apis/snake
 */
class Snake {
    private var ping: () -> Map<String, String> = { emptyMap() }
    private var start: (startRequest: JsonNode) -> Map<String, String> = { emptyMap() }
    private var move: (moveRequest: JsonNode) -> Map<String, String> = { emptyMap() }
    private var end: (endRequest: JsonNode) -> Map<String, String> = { emptyMap() }

    fun onPing(func: () -> Map<String, String>) {
        ping = func
    }

    fun onStart(func: (startRequest: JsonNode) -> Map<String, String>) {
        start = func
    }

    fun onMove(func: (moveRequest: JsonNode) -> Map<String, String>) {
        move = func
    }

    fun onEnd(func: (endRequest: JsonNode) -> Map<String, String>) {
        end = func
    }

    /**
     * Generic processor that prints out the request and response from the methods.
     *
     * @param req
     * @param res
     * @return
     */
    private fun process(req: Request, res: Response): Map<String, String> {
        val uri = req.uri()
        logger.info { "$uri called with: ${req.body()}" }
        return try {
            val responseMap: Map<String, String> =
                when (uri) {
                    "/ping" -> ping.invoke()
                    "/start" -> start.invoke(JSON_MAPPER.readTree(req.body()))
                    "/move" -> move.invoke(JSON_MAPPER.readTree(req.body()))
                    "/end" -> end.invoke(JSON_MAPPER.readTree(req.body()))
                    else -> throw IllegalAccessError("Strange call made to the snake: $uri")
                }

            logger.info { "Responding with: ${JSON_MAPPER.writeValueAsString(responseMap)}" }
            responseMap
        } catch (e: Exception) {
            logger.warn(e) { "Something went wrong with $uri" }
            emptyMap()
        }
    }

    fun run(port: Int = 8080) {
        port(port)

        get("/") { _, _ ->
            "Battlesnake documentation can be found at " +
                    "<a href=\"https://docs.battlesnake.io\">https://docs.battlesnake.io</a>."
        }
        post("/start",
            { req, res -> process(req, res) },
            { JSON_MAPPER.writeValueAsString(it) })
        post("/ping",
            { req, res -> process(req, res) },
            { JSON_MAPPER.writeValueAsString(it) })
        post("/move",
            { req, res -> process(req, res) },
            { JSON_MAPPER.writeValueAsString(it) })
        post(
            "/end",
            { req, res -> process(req, res) },
            { JSON_MAPPER.writeValueAsString(it) })
    }

    companion object : KLogging() {
        private val JSON_MAPPER = ObjectMapper()

        /**
         * Main entry point.
         *
         * @param args are ignored.
         */
        @JvmStatic
        fun main(args: Array<String>) {
            val port = Integer.parseInt(System.getProperty("PORT") ?: "8080")
            logger.info { "Listening on port: $port" }

            snake {
                onPing { emptyMap() }
                onStart { mapOf("color" to "#ff00ff", "headType" to "beluga", "tailType" to "bolt") }
                onMove { mapOf("move" to "right") }
                onEnd { emptyMap() }
            }.run(port)
        }
    }
}
