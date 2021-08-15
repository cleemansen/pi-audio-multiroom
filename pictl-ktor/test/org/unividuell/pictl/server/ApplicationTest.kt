package org.unividuell.pictl.server

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.every
import io.mockk.mockk
import org.koin.dsl.module
import org.koin.test.check.checkModules
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {

    @Test
    fun testRoot() {
        withTestApplication({ piCtl(testing = true) }) {
            handleRequest(HttpMethod.Get, "/hello").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("HELLO WORLD!", response.content)
            }
        }
    }

    @Test
    fun `check modules`() = checkModules {
        val mockApplication = mockk<Application>() {
            every { environment } returns mockk {
                every { config } returns mockk(relaxed = true) {
                    every { property("ktor.deployment.port") } returns mockk {
                        every { getString() } returns "58"
                    }
                }
            }
        }
        modules(module { single<Application> { mockApplication } }, applicationModule, useCaseModule)
    }
}
