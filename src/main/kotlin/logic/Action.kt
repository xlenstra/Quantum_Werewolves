package logic

import kotlinx.serialization.Serializable


enum class Action(val role: Role?) {
    LYNCH(null),
    FINISH_NIGHT(null),
    EAT(Role.WEREWOLF),
    SEE(Role.SEER),
    GUARD(Role.GUARDIAN),
    SLUTS(Role.SLUT),
}

@Serializable
data class TargetedAction(
    val performer: String? = null, // Who preforms the action?
    val action: Action, // What action?
    val target: String? = null, // Who is the target of the action?
    val targetRole: Role? = null // What role does the target have?
)

@Serializable
class WorldNightActions {
    fun reset() {
        killWolf = null
        wolfTarget = null
        guardedPlayers.clear()
        sleepsAt.clear()
    }

    var killWolf: String? = null
    var wolfTarget: String? = null
    val guardedPlayers = mutableListOf<String>()
    val sleepsAt = mutableMapOf<String, String>()
}

@Serializable
class AllNightActions() {
    private val actions = mutableMapOf<String,MutableMap<Action, String>>()

    constructor(players: List<String>) : this() {
        for (player in players) {
            actions[player] = mutableMapOf()
        }
    }

    fun reset() {
        for (player in actions.keys) {
            actions[player]!!.clear()
        }
    }

    fun addAction(performer: String, action: Action, target: String) {
        actions[performer]!![action] = target
    }

    fun getAction(performer: String, action: Action): String? {
        return actions[performer]?.get(action)
    }
}