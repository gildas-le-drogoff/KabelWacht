// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Tobias Mueller and KabelWacht contributors

package com.github.muelli.kabelwacht.vpn.automation

import com.github.muelli.kabelwacht.data.MobileConditionMode
import com.github.muelli.kabelwacht.data.NetworkStateSnapshot
import com.github.muelli.kabelwacht.data.TunnelRunConditions
import com.github.muelli.kabelwacht.data.WifiConditionMode
import org.junit.Assert.assertEquals
import org.junit.Test

class AutomationDecisionTest {

    private val home = NetworkStateSnapshot(isConnected = true, isWifi = true, wifiSsid = "Home")
    private val mobile = NetworkStateSnapshot(isConnected = true, isMobile = true)

    // "Work": everywhere except the home Wi-Fi. "Other": defaults, i.e. everywhere.
    private val notAtHome = TunnelRunConditions(
        enabled = true,
        wifiMode = WifiConditionMode.EXCLUDE_LIST,
        wifiSsids = setOf("Home"),
    )
    private val everywhere = TunnelRunConditions(enabled = true)
    private val order = listOf("Other", "Work")

    @Test
    fun manually_started_tunnel_is_not_torn_down_by_its_conditions() {
        // The bug report: Work was switched on at home, where its rules say no.
        val action = decideAutomation(
            snapshot = home,
            conditions = mapOf("Work" to notAtHome, "Other" to everywhere),
            tunnelOrder = order,
            activeName = "Work",
            override = ManualOverride("Work", up = true),
        )
        assertEquals(AutomationAction.None, action)
    }

    @Test
    fun without_override_unmatched_managed_tunnel_is_stopped() {
        val action = decideAutomation(
            snapshot = home,
            conditions = mapOf("Work" to notAtHome),
            tunnelOrder = order,
            activeName = "Work",
            override = null,
        )
        assertEquals(AutomationAction.StopActive, action)
    }

    @Test
    fun running_tunnel_that_still_matches_is_kept() {
        // Both match on mobile; the connected one stays even though it is not first.
        val action = decideAutomation(
            snapshot = mobile,
            conditions = mapOf("Work" to notAtHome, "Other" to everywhere),
            tunnelOrder = order,
            activeName = "Work",
            override = null,
        )
        assertEquals(AutomationAction.None, action)
    }

    @Test
    fun first_matching_tunnel_in_list_order_wins() {
        val conditions = mapOf("Work" to notAtHome, "Other" to everywhere)
        assertEquals(
            AutomationAction.Start("Other"),
            decideAutomation(mobile, conditions, listOf("Other", "Work"), activeName = null, override = null),
        )
        assertEquals(
            AutomationAction.Start("Work"),
            decideAutomation(mobile, conditions, listOf("Work", "Other"), activeName = null, override = null),
        )
    }

    @Test
    fun matching_tunnel_replaces_one_whose_conditions_no_longer_match() {
        val action = decideAutomation(
            snapshot = home,
            conditions = mapOf("Work" to notAtHome, "Other" to everywhere),
            tunnelOrder = order,
            activeName = "Work",
            override = null,
        )
        assertEquals(AutomationAction.Start("Other"), action)
    }

    @Test
    fun manual_switch_off_starts_nothing() {
        val action = decideAutomation(
            snapshot = mobile,
            conditions = mapOf("Work" to notAtHome, "Other" to everywhere),
            tunnelOrder = order,
            activeName = null,
            override = ManualOverride("Work", up = false),
        )
        assertEquals(AutomationAction.None, action)
    }

    @Test
    fun override_for_a_tunnel_that_is_no_longer_up_does_not_block_automation() {
        // Switched on by hand, but it has since gone down (e.g. failed to connect).
        val action = decideAutomation(
            snapshot = mobile,
            conditions = mapOf("Other" to everywhere),
            tunnelOrder = order,
            activeName = null,
            override = ManualOverride("Work", up = true),
        )
        assertEquals(AutomationAction.Start("Other"), action)
    }

    @Test
    fun tunnel_without_conditions_is_left_running_when_nothing_matches() {
        val action = decideAutomation(
            snapshot = mobile,
            conditions = mapOf("Work" to notAtHome.copy(mobileMode = MobileConditionMode.NEVER)),
            tunnelOrder = order,
            activeName = "Other",
            override = null,
        )
        assertEquals(AutomationAction.None, action)
    }

    @Test
    fun conditions_of_deleted_tunnels_are_ignored() {
        val action = decideAutomation(
            snapshot = mobile,
            conditions = mapOf("Gone" to everywhere),
            tunnelOrder = order,
            activeName = null,
            override = null,
        )
        assertEquals(AutomationAction.None, action)
    }
}
