package logic

import kotlinx.serialization.Serializable


enum class Action(val performingRole: Role?, val displayName: String) {
    LYNCH(null, "Lynches"),
    QUANTUM_DIE(null, "Quantum Dies"),
    FINISH_NIGHT(null, "Finish Night"),
    EAT(Role.WEREWOLF, "Eats"),
    SEE(Role.SEER, "Sees"),
    OLDSEE(Role.OLD_SEER, "Sees"),
    GUARD(Role.GUARDIAN, "Guards"),
    SLUTS(Role.SLUT, "Sleeps at"),
    POISONS(Role.WITCH, "Poisons"),
    SHOOT(Role.HUNTER, "Shoots"),
    CHOOSE_EXAMPLE(Role.SMALL_WILD_ONE, "Follows"),
    DEVILS_CHOICE(Role.DEVIL, "Chooses")
}

@Serializable
data class TargetedAction(
    val performer: String? = null, // Who preforms the action?
    val action: Action? = null, // What action? -- must be nullable for the serializer, should never be null
    val target: String? = null, // Who is the target of the action?
    val targetRole: Role? = null, // What role does the target have?
    val targetTeam: Team? = null, // What team does the target have?
) {
    fun getTextDisplay(): String {
        return when (action!!) {
            Action.LYNCH -> "$target is lynched as ${targetRole!!.displayName}"
            Action.QUANTUM_DIE -> "$target dies due to Quantum Mechanics as ${targetRole!!.displayName}"
            Action.FINISH_NIGHT -> ""
            Action.EAT, Action.SLUTS, Action.GUARD, Action.POISONS, Action.SHOOT, Action.CHOOSE_EXAMPLE, Action.DEVILS_CHOICE -> 
                "$performer ${action.displayName.lowercase()} ${target ?: "no one"}"
            Action.SEE -> 
                if (target == null) {
                    "$performer ${action.displayName.lowercase()} no one"
                } else {
                    "$performer ${action.displayName.lowercase()} $target as ${targetRole?.displayName ?: "dead"}"
                }
            Action.OLDSEE -> 
                if (target == null) {
                    "$performer ${action.displayName.lowercase()} no one"
                } else {
                    "$performer ${action.displayName.lowercase()} $target as ${targetTeam?.displayName ?: "dead"}"
                }
        }
    }
}

@Serializable
class WorldNightActions {
    fun reset() {
        killWolf = null
        wolfTargets.clear()
        guardedPlayers.clear()
        sleepsAt.clear()
        mutualKills.clear()
    }

    var killWolf: String? = null
    val guardedPlayers = mutableListOf<String>()
    // Maps werewolves to the player they attack
    val wolfTargets = mutableMapOf<String, String>()
    // Maps people to the house they sleep at
    val sleepsAt = mutableMapOf<String, String>()
    val mutualKills = mutableMapOf<String, String>()
}

@Serializable
class AllNightActions() {
    private val actions = mutableMapOf<String, MutableMap<String, String>>()

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

    fun setAction(performer: String, action: Action, target: String) {
        actions[performer]!![action.name] = target
    }

    fun getAction(performer: String, action: Action): String? {
        return actions[performer]?.get(action.name)
    }
}