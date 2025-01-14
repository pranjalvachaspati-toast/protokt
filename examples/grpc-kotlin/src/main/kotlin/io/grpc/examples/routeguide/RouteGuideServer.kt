/*
 * Copyright 2020 gRPC authors.
 * Copyright 2021 Toast Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.grpc.examples.routeguide

import com.google.common.base.Stopwatch
import com.google.common.base.Ticker
import io.grpc.Server
import io.grpc.ServerBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Kotlin adaptation of RouteGuideServer from the Java gRPC example.
 */
class RouteGuideServer(
    val port: Int,
    val features: Collection<Feature> = Database.features(),
    val server: Server = ServerBuilder.forPort(port).addService(RouteGuideService(features)).build()
) {

    fun start() {
        server.start()
        println("Server started, listening on $port")
        Runtime.getRuntime().addShutdownHook(
            Thread {
                println("*** shutting down gRPC server since JVM is shutting down")
                this@RouteGuideServer.stop()
                println("*** server shut down")
            }
        )
    }

    fun stop() {
        server.shutdown()
    }

    fun blockUntilShutdown() {
        server.awaitTermination()
    }

    internal class RouteGuideService(
        private val features: Collection<Feature>,
        private val ticker: Ticker = Ticker.systemTicker()
    ) : RouteGuideGrpcKt.RouteGuideCoroutineImplBase() {
        private val routeNotes = ConcurrentHashMap<Point, MutableList<RouteNote>>()

        override suspend fun getFeature(request: Point): Feature =
            // No feature was found, return an unnamed feature.
            features.find { it.location == request } ?: Feature { location = request }

        override fun listFeatures(request: Rectangle): Flow<Feature> =
            features.asFlow().filter { it.exists() && it.location!! in request }

        override suspend fun recordRoute(requests: Flow<Point>): RouteSummary {
            var pointCount = 0
            var featureCount = 0
            var distance = 0
            var previous: Point? = null
            val stopwatch = Stopwatch.createStarted(ticker)
            requests.collect { request ->
                pointCount++
                if (getFeature(request).exists()) {
                    featureCount++
                }
                val prev = previous
                if (prev != null) {
                    distance += prev distanceTo request
                }
                previous = request
            }
            return RouteSummary {
                this.pointCount = pointCount
                this.featureCount = featureCount
                this.distance = distance
                this.elapsedTime = Durations.fromMicros(stopwatch.elapsed(TimeUnit.MICROSECONDS))
            }
        }

        override fun routeChat(requests: Flow<RouteNote>): Flow<RouteNote> = flow {
            requests.collect { note ->
                val notes: MutableList<RouteNote> = routeNotes.computeIfAbsent(note.location!!) {
                    Collections.synchronizedList(mutableListOf<RouteNote>())
                }
                for (prevNote in notes.toTypedArray()) { // thread-safe snapshot
                    emit(prevNote)
                }
                notes += note
            }
        }
    }
}

fun main() {
    val port = 8980
    val server = RouteGuideServer(port)
    server.start()
    server.blockUntilShutdown()
}
