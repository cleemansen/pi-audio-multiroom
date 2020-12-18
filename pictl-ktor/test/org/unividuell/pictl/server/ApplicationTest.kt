package org.unividuell.pictl.server

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.routing.*
import io.ktor.server.testing.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {
    @Test
    fun testRoot() {
        withTestApplication({ piCtl(testing = true) }) {
            handleRequest(HttpMethod.Get, "/").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("HELLO WORLD!", response.content)
            }
        }
    }

    @Test
    fun `there should be an audio websocket`() {
        withTestApplication({ audioModule(testing = true) }) {

            handleWebSocketConversation("/audio/echo") { incoming, outgoing ->
                val textMessage = listOf("HELLO", "WORLD")
                for (msg in textMessage) {
                    outgoing.send(Frame.Text(msg))
                    assertThat((incoming.receive() as Frame.Text).readText()).isEqualTo(msg)
                }
            }
        }
    }
}
