package org.unividuell.pictl.server

import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.features.logging.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.content.*
import io.ktor.jackson.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.util.date.*
import io.ktor.websocket.*
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.ktor.di
import org.kodein.di.singleton
import org.slf4j.event.Level
import org.unividuell.pictl.server.controller.audioRoutes
import org.unividuell.pictl.server.repository.SlimboxCometLongPollingRepository
import org.unividuell.pictl.server.repository.SlimboxJsonRpcRepository
import org.unividuell.pictl.server.usecase.GetCurrentSongInteractor
import java.time.Duration

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.piCtl(testing: Boolean = false) {
    val client = HttpClient(CIO) {
        install(HttpTimeout) {
        }
        install(JsonFeature) {
            serializer = GsonSerializer()
        }
        install(Logging) {
            level = LogLevel.ALL
        }
    }

    di {
        bind() from singleton { client }
        bind<GetCurrentSongInteractor.DataSource>() with singleton { SlimboxJsonRpcRepository(di) }
        bind() from singleton { GetCurrentSongInteractor(di) }
        bind() from singleton { SlimboxCometLongPollingRepository(di) }
    }

    lifecycleMonitor()

    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
        }
    }

    install(Compression) {
        gzip {
            priority = 1.0
        }
        deflate {
            priority = 10.0
            minimumSize(1024) // condition
        }
    }

    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/") }
    }

    install(ConditionalHeaders)

    install(CachingHeaders) {
        options { outgoingContent ->
            when (outgoingContent.contentType?.withoutParameters()) {
                ContentType.Text.CSS -> CachingOptions(CacheControl.MaxAge(maxAgeSeconds = 24 * 60 * 60), expires = null as? GMTDate?)
                else -> null
            }
        }
    }

    install(DefaultHeaders) {
        header("X-Engine", "Ktor") // will send this header with each response
    }

    install(ShutDownUrl.ApplicationCallFeature) {
        // The URL that will be intercepted (you can also use the application.conf's ktor.deployment.shutdown.url key)
        shutDownUrl = "/ktor/application/shutdown"
        // A function that will be executed to get the exit code of the process
        exitCodeSupplier = { 0 } // ApplicationCall.() -> Int
    }

    environment.log.info("current environment is $envKind")

    routing {
        get("/hello") {
            call.respondText("HELLO WORLD!", contentType = ContentType.Text.Plain)
        }

        // Static feature. Try to access `/static/ktor_logo.svg`
        static("/") {
            resources("pictl-vue")
        }

        get("/json/jackson") {
            call.respond(mapOf("hello" to "world"))
        }
    }
}

data class JsonSampleClass(val hello: String)

