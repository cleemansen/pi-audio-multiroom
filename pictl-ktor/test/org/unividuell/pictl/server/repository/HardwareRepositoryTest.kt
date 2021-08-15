package org.unividuell.pictl.server.repository

class HardwareRepositoryTest {

    lateinit var sut: HardwareRepository

//    @Test
//    fun `it should respect the delay for shutdown`() {
//        withTestApplication({
//            piCtl(testing = true)
//            (environment.config as MapApplicationConfig).apply {
//                put("ktor.deployment.environment", "test")
//                put("ktor.application.slimserver.host", "no://op")
//            }
//            sut = HardwareRepository()
//        }) {
//            runBlocking {
//                sut.shutdownAsync(delay = Duration.ofSeconds(2.5.toLong())).await()
//            }
//        }
//    }

}