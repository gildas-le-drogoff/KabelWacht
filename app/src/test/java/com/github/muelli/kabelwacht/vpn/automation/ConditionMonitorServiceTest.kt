// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Tobias Mueller and KabelWacht contributors

package com.github.muelli.kabelwacht.vpn.automation

import android.content.pm.ServiceInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class ConditionMonitorServiceTest {

    private val special = ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
    private val location = ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION

    @Test
    fun without_location_only_special_use_is_declared() {
        assertEquals(listOf(special), ConditionMonitorService.foregroundServiceTypeAttempts(35, false))
    }

    @Test
    fun with_location_falls_back_to_special_use() {
        // Android 14+ refuses the location type from BOOT_COMPLETED (#34), so
        // there must always be a declaration without it to fall back to.
        assertEquals(
            listOf(special or location, special),
            ConditionMonitorService.foregroundServiceTypeAttempts(35, true),
        )
    }

    @Test
    fun before_android_14_there_is_no_special_use_type() {
        assertEquals(listOf(location, 0), ConditionMonitorService.foregroundServiceTypeAttempts(33, true))
        assertEquals(listOf(0), ConditionMonitorService.foregroundServiceTypeAttempts(33, false))
    }
}
