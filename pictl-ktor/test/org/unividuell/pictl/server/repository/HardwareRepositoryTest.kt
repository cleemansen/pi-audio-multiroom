package org.unividuell.pictl.server.repository

import org.junit.Test
import java.time.Duration

class HardwareRepositoryTest {

    val sut = HardwareRepository()

    @Test
    fun `it should print the java version`() {
        sut.shutdown(delay = Duration.ofSeconds(5))
    }
}