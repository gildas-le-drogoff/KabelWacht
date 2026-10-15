// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Tobias Mueller and KabelWacht contributors

package com.github.muelli.kabelwacht.vpn.automation

import com.github.muelli.kabelwacht.data.NetworkStateSnapshot
import com.github.muelli.kabelwacht.data.TunnelRunConditions

/** What the [AutomationEngine] should do after a network or conditions change. */
sealed interface AutomationAction {
    data object None : AutomationAction
    data class Start(val name: String) : AutomationAction
    data object StopActive : AutomationAction
}

/**
 * The user's most recent manual toggle, honoured until the physical network
 * changes: a tunnel they switched on stays on, and after they switched one off
 * automation starts nothing.
 */
data class ManualOverride(val name: String, val up: Boolean)

/**
 * Decide which tunnel automation wants, as a pure function of its inputs.
 *
 * [tunnelOrder] is the order the tunnel list shows (and holds only tunnels that
 * exist); when several tunnels' conditions match, a running one that still
 * matches is kept, otherwise the first in that order wins.
 */
fun decideAutomation(
    snapshot: NetworkStateSnapshot,
    conditions: Map<String, TunnelRunConditions>,
    tunnelOrder: List<String>,
    activeName: String?,
    override: ManualOverride?,
): AutomationAction {
    if (override != null) {
        // Switched on by hand: leave it alone, whatever its conditions say.
        if (override.up && override.name == activeName) return AutomationAction.None
        // Switched off by hand: do not start anything behind the user's back.
        if (!override.up) return AutomationAction.None
    }

    val matching = tunnelOrder.filter { conditions[it]?.shouldRun(snapshot) == true }
    if (activeName != null && activeName in matching) return AutomationAction.None

    val candidate = matching.firstOrNull()
    if (candidate != null) return AutomationAction.Start(candidate)

    // Nothing matches: only a tunnel automation manages is brought down; one
    // without run conditions was started by other means and stays.
    if (activeName != null && conditions[activeName]?.enabled == true) {
        return AutomationAction.StopActive
    }
    return AutomationAction.None
}
