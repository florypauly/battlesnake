package io.battlesnake.starter

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException

class SnakeTest {

    private lateinit var handler: Snake.Handler

    @BeforeEach
    internal fun setUp() {
        handler = Snake.Handler()
    }

    @Test
    @Throws(IOException::class)
    internal fun pingTest() {
        val response = handler.ping()
        assertEquals("{}", response.toString())
    }

    @Test
    @Throws(IOException::class)
    internal fun startTest() {
        val startRequest = OBJECT_MAPPER.readTree("{}")
        val response = handler.start(startRequest)
        assertEquals("#ff00ff", response["color"])
    }

    @Test
    @Throws(IOException::class)
    internal fun moveTest() {
        val moveRequest = OBJECT_MAPPER.readTree("{}")
        val response = handler.move(moveRequest)
        assertEquals("right", response["move"])
    }

    @Test
    @Throws(IOException::class)
    internal fun endTest() {
        val endRequest = OBJECT_MAPPER.readTree("{}")
        val response = handler.end(endRequest)
        assertEquals(0, response.size)
    }

    companion object {
        private val OBJECT_MAPPER = ObjectMapper()

        init {
            OBJECT_MAPPER.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
        }
    }
}