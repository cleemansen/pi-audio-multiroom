package org.unividuell.pictl.server

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.jackson.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.util.date.*
import org.koin.dsl.module
import org.koin.ktor.ext.Koin
import org.slf4j.event.Level
import java.io.PrintWriter
import java.io.StringWriter


fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.piCtl(testing: Boolean = false) {
    log.info("starting module `pi-ctl`")

    install(Koin) {
        org.koin.logger.SLF4JLogger()
        modules(
            module { single<Application> { this@piCtl } },
            applicationModule,
            useCaseModule
        )
    }

    lifecycleMonitor()

    install(ContentNegotiation) {
        jackson {
            enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT)
        }
    }

    install(StatusPages) {
        exception<Throwable> { cause ->
            application.log.warn("global exception handler!", cause)
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            cause.printStackTrace(pw)
            call.respond(HttpStatusCode.InternalServerError, "Internal Server Error: $cause\n${sw}")
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
        filter { call -> !call.request.path().startsWith("/metrics") }
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

    log.info("current environment is $envKind")

    routing {
        get("/hello") {
            call.respondText("HELLO WORLD!", contentType = ContentType.Text.Plain)
        }

        // Static feature. Try to access `/static/ktor_logo.svg`
        static("/") {
            resources("pictl-vue")
            defaultResource("pictl-vue/index.html")
        }
    }
}

