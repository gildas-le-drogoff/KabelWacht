// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Tobias Mueller and KabelWacht contributors

package com.github.muelli.kabelwacht.vpn

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration

/**
 * Run [block], and after each failure wait until [connected] reports a network
 * (at most [connectTimeout]) plus [settleDelay] times the attempt number before
 * trying again, up to [maxAttempts] tries in all.
 *
 * Meant for bringing a tunnel up early after boot, when the endpoint's hostname
 * cannot resolve yet because no network is up. [onFailure] sees every failure;
 * the returned result carries the last one if no attempt succeeded.
 */
suspend fun <T> retryOnConnectivity(
    connected: Flow<Boolean>,
    maxAttempts: Int,
    connectTimeout: Duration,
    settleDelay: Duration,
    onFailure: (attempt: Int, error: Throwable) -> Unit = { _, _ -> },
    block: suspend () -> T,
): Result<T> {
    require(maxAttempts >= 1)
    var attempt = 1
    while (true) {
        val result = try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
        val error = result.exceptionOrNull() ?: return result
        onFailure(attempt, error)
        if (attempt >= maxAttempts) return result
        withTimeoutOrNull(connectTimeout) { connected.first { it } } ?: return result
        delay(settleDelay * attempt)
        attempt++
    }
}
