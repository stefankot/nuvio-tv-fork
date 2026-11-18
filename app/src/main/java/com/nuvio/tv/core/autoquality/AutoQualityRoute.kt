package com.nuvio.tv.core.autoquality

/**
 * forge M4b (contract E): which way a plan goes, from the switches. Pure, so the matrix is tested on the JVM.
 * - RenewAddress → RENEW (always; not a change).
 * - Switch IMMEDIATE_4B → IMMEDIATE.
 * - Switch PLANNED_4A → HOT_SWAP when Echo and its hot swap are on; otherwise down = IMMEDIATE, up = DROP
 *   (Echo drops an upgrade it cannot swap in quietly).
 * - Stay / NoAlternative → NONE.
 */
enum class AutoQualityRoute { NONE, RENEW, IMMEDIATE, HOT_SWAP, DROP }

fun routeFor(plan: Plan, echoEnabled: Boolean = AutoQualityFlags.ECHO_ENABLED, hotSwapEnabled: Boolean = AutoQualityFlags.HOT_SWAP_ENABLED): AutoQualityRoute =
    when (plan) {
        Plan.Stay, Plan.NoAlternative -> AutoQualityRoute.NONE
        is Plan.RenewAddress -> AutoQualityRoute.RENEW
        is Plan.Switch -> when {
            plan.way == Plan.Way.IMMEDIATE_4B -> AutoQualityRoute.IMMEDIATE
            echoEnabled && hotSwapEnabled -> AutoQualityRoute.HOT_SWAP
            plan.isUpgrade -> AutoQualityRoute.DROP
            else -> AutoQualityRoute.IMMEDIATE
        }
    }
