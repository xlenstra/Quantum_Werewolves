package logic

import kotlinx.serialization.Serializable


enum class Action(val displayString: String) {
    LYNCH( "Is Lynched"),
    QUANTUM_DIE( "Quantum Dies"),
    FINISH_NIGHT( "Finish Night"),
    EAT("Eats"),
    SEE("Sees"),
    OLDSEE("Sees"),
    BLESSES("Blesses"),
    GUARD("Guards"),
    INVITES("Invites"),
    SLUTS("Sleeps at"),
    MAKE_HAMSTER("Transforms into a Hamster"),
    POISONS("Poisons"),
    SHOOT("Shoots"),
    CHOOSE_EXAMPLE("Follows"),
    DEVILS_CHOICE("Chooses"),
    CURSES("Curses"),
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
            Action.SEE -> 
                if (target == null) {
                    "$performer ${action.displayString.lowercase()} no one"
                } else {
                    "$performer ${action.displayString.lowercase()} $target as ${targetRole?.displayName ?: "dead"}"
                }
            Action.OLDSEE -> 
                if (target == null) {
                    "$performer ${action.displayString.lowercase()} no one"
                } else {
                    "$performer ${action.displayString.lowercase()} $target as ${targetTeam?.displayName ?: "dead"}"
                }
            Action.MAKE_HAMSTER ->
                "$performer transforms ${(target ?: "no one")} into a hamster"
            Action.EAT, Action.SLUTS, Action.GUARD, Action.POISONS, Action.SHOOT, Action.CHOOSE_EXAMPLE, Action.DEVILS_CHOICE, Action.INVITES, Action.BLESSES, Action.CURSES ->
                "$performer ${action.displayString.lowercase()} ${target ?: "no one"}"
        }
    }
}

@Serializable
class WorldNightActions {
    fun reset() {
        killWolf = null
        wolfTargets.clear()
        
        seenPlayers.clear()
        
        guardedPlayers.clear()
        hamsterPlayers.clear()
        sleepsAt.clear()
        
        mutualKills.clear()
        
        playersThatDiedTonight.clear()
    }

    var killWolf: String? = null
    // Maps werewolves to the player they attack
    val wolfTargets = mutableMapOf<String, String>()

    val seenPlayers = mutableSetOf<String>()

    val blessedCursedPlayers = mutableMapOf<String, Int>()
    val guardedPlayers = mutableSetOf<String>()
    val hamsterPlayers = mutableSetOf<String>()
    // Maps people to the house they sleep at
    val sleepsAt = mutableMapOf<String, String>()
    
    val mutualKills = mutableMapOf<String, String>()
    
    val playersThatDiedTonight = mutableSetOf<String>()
    
    fun makeHamster(player: String) {
        hamsterPlayers.add(player)
        guardedPlayers.add(player)
    }
    
    fun getBlessedCursed(player: String): Int {
        return blessedCursedPlayers[player] ?: 0
    }
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

    fun removeAction(performer: String, action: Action) {
        actions[performer]?.remove(action.name)
    }
}