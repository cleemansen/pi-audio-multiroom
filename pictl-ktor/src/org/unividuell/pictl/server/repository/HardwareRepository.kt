package org.unividuell.pictl.server.repository

import java.time.Duration

class HardwareRepository {

    fun shutdown(delay: Duration) {
        val p = ProcessBuilder()
//            .command("sleep ${delay.seconds};sudo shutdown now")
            .command("java", "-version")
            .inheritIO()
            .start()

    }

}