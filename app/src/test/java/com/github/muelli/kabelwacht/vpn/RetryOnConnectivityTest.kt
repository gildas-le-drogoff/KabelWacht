// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Tobias Mueller and KabelWacht contributors

package com.github.muelli.kabelwacht.vpn

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class RetryOnConnectivityTest {

    @Test
    fun first_success_needs_no_network() = runBlocking {
        var calls = 0
        val result = retryOnConnectivity(MutableStateFlow(false), 3, 1.seconds, 1.milliseconds) { ++calls }
        assertEquals(Result.success(1), result)
        assertEquals(1, calls)
    }

    @Test
    fun retries_once_the_network_comes_up() = runBlocking {
        val connected = MutableStateFlow(false)
        var calls = 0
        val failures = mutableListOf<Int>()
        launch {
            delay(50)
            connected.value = true
        }
        val result = retryOnConnectivity(
            connected,
            maxAttempts = 3,
            connectTimeout = 5.seconds,
            settleDelay = 1.milliseconds,
            onFailure = { attempt, _ -> failures += attempt },
        ) {
            calls++
            check(connected.value) { "no DNS yet" }
            "up"
        }
        assertEquals(Result.success("up"), result)
        assertEquals(2, calls)
        assertEquals(listOf(1), failures)
    }

    @Test
    fun gives_up_when_no_network_appears() = runBlocking {
        var calls = 0
        val result = retryOnConnectivity(MutableStateFlow(false), 3, 50.milliseconds, 1.milliseconds) {
            calls++
            error("offline")
        }
        assertTrue(result.isFailure)
        assertEquals(1, calls)
    }

    @Test
    fun attempts_are_bounded() = runBlocking {
        var calls = 0
        val failures = mutableListOf<Int>()
        val result = retryOnConnectivity(
            MutableStateFlow(true),
            maxAttempts = 3,
            connectTimeout = 1.seconds,
            settleDelay = 1.milliseconds,
            onFailure = { attempt, _ -> failures += attempt },
        ) {
            calls++
            error("bad config")
        }
        assertEquals("bad config", result.exceptionOrNull()?.message)
        assertEquals(3, calls)
        assertEquals(listOf(1, 2, 3), failures)
    }
}
