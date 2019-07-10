package com.wendro.homecontroller

import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.application.*
import io.ktor.auth.Authentication
import io.ktor.auth.UserIdPrincipal
import io.ktor.auth.authenticate
import io.ktor.auth.basic
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.http.content.resource
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.jackson.jackson
import io.ktor.request.path
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.slf4j.event.Level
import java.util.concurrent.TimeUnit

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    environment.monitor.subscribe(ApplicationStarted){
        println("My app is ready to roll")
    }
    environment.monitor.subscribe(ApplicationStopped){
        println("Time to clean up")
    }

    val server = embeddedServer(Netty, 8080) {

        install(ContentNegotiation) {
            jackson {
                enable(SerializationFeature.INDENT_OUTPUT)
            }
        }
        install(CallLogging) {
            level = Level.INFO
            filter { call -> call.request.path().startsWith("/") }
        }
        install(DefaultHeaders) {
            header("X-Engine", "Ktor") // will send this header with each response
        }
        install(Authentication) {
            basic {
                realm = "ktor"
                validate { credentials ->
                    if (credentials.name == "user" && credentials.password == "secret") {
                        UserIdPrincipal(credentials.name)
                    } else {
                        null
                    }
                }
            }
        }

        routing {
            authenticate {
                static("/app/") {
                    resource("/", "secured/index.html")
                    resources("secured")
                }
                get("/app/json") {
                    call.respond(mapOf("hello" to "world"))
                }
            }
            static {
                resource("/", "unsecured/index.html")
                resources("unsecured")
            }
        }
    }
    server.start(false)
    Runtime.getRuntime().addShutdownHook(Thread {
        server.stop(1, 5, TimeUnit.SECONDS)
    })
    Thread.currentThread().join()
}
