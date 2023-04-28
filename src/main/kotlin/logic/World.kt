package logic

import UI.util.toLabel
import UI.util.toRole
import UI.util.widgets.ExpanderTab
import UI.util.withItem
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Table
import kotlinx.serialization.Serializable

@Serializable
class World() {

    var ID: Int = -1

    // Maps player name to role name
    lateinit var roleSet: MutableMap<String, String>
    
    private var werewolfOrder: List<String> = listOf()
    val livingPlayers = mutableListOf<String>()
    var worldIsPossible = true
    
    private val executedActions = mutableListOf<TargetedAction>()

    private val worldNightActions = WorldNightActions()
    
    private val examples: MutableMap<String, String> = mutableMapOf()

    constructor(roleSet: Map<String, Role>, worldID: Int) : this() {
        this.roleSet = roleSet.mapValues { it.value.name }.toMutableMap()
        this.ID = worldID
        werewolfOrder = roleSet.filter { it.value.team == Team.WEREWOLF }.keys.shuffled()
        livingPlayers.clear()
        livingPlayers.addAll(roleSet.keys.toMutableList())
    }
    
    fun gameHasEnded() = getWinningTeam() == null
    
    fun getWinningTeam(): Team? {
        val werewolves = roleSet.filter { it.value.toRole().team == Team.WEREWOLF }.keys
        val villagers = roleSet.filter { it.value.toRole().team == Team.VILLAGER }.keys
        return when {
            livingPlayers.all { it in villagers } -> Team.VILLAGER
            livingPlayers.all { it in werewolves } -> Team.WEREWOLF
            else -> null
        }
    }
    
    // region actions 
    
    private fun getPreformedActions(): Map<Int, List<TargetedAction>> {
        val actions = mutableMapOf<Int, MutableList<TargetedAction>>()
        var dayCount = 1
        for (action in executedActions) {
            if (action.action == Action.FINISH_NIGHT) {
                dayCount += 1
                continue
            }
            if (actions[dayCount] == null) 
                actions[dayCount] = mutableListOf()
            actions[dayCount]!!.add(action)
        }
        return actions
    }
    
    private fun getWolfIndex(player: String): Int {
        return werewolfOrder.indexOf(player)
    }
    
    private fun actionApplies(action: TargetedAction): Boolean {
        if (action.action == Action.LYNCH) return true
        if (action.action == Action.QUANTUM_DIE) return true
        if (action.action == Action.FINISH_NIGHT) return true
        if (action.performer !in livingPlayers) return false
        return roleSet[action.performer]!!.toRole().hasAction(action.action!!)
    }

    fun applyAction(action: TargetedAction) {
        if (!actionApplies(action)) return
        
        executedActions.add(action)
        when (action.action!!) {
            Action.LYNCH -> {
                if (roleSet[action.target] != action.targetRole!!.name) {
                    worldIsPossible = false
                }
                killAndHandleDeath(action.target!!)
            }
            Action.QUANTUM_DIE -> {
                if (roleSet[action.target] != action.targetRole!!.name) {
                    worldIsPossible = false
                }
            }
            Action.EAT -> {
                if (action.target == null) return
                worldNightActions.wolfTargets[action.performer!!] = action.target
            }
            Action.SEE -> {
                if (action.targetRole == null) return
                if (action.target == null) return
                if (action.target !in livingPlayers) return
                if (worldNightActions.getBlessedCursed(action.performer!!) < 0) return
                if (roleSet[action.target]!!.toRole().getRoleSeenAs() != action.targetRole) {
                    worldIsPossible = false
                }
                worldNightActions.seenPlayers.add(action.target)
            }
            Action.OLDSEE -> {
                if (action.targetTeam == null) return
                if (action.target == null) return
                if (action.target !in livingPlayers) return
                if (worldNightActions.getBlessedCursed(action.performer!!) < 0) return
                if (roleSet[action.target]!!.toRole().getTeamOldSeenAs() != action.targetTeam) {
                    worldIsPossible = false
                }
                worldNightActions.seenPlayers.add(action.target)
            }
            Action.BLESSES -> {
                if (action.target == null) return
                if (action.target !in livingPlayers) return
                worldNightActions.blessedCursedPlayers[action.target] = worldNightActions.getBlessedCursed(action.performer!!) + 1
            }
            Action.CURSES -> {
                if (action.target == null) return
                if (action.target !in livingPlayers) return
                worldNightActions.blessedCursedPlayers[action.target] = worldNightActions.getBlessedCursed(action.performer!!) - 1
            }
            Action.MAKE_HAMSTER -> {
                if (action.target == null) return
                if (action.target !in livingPlayers) return
                if (worldNightActions.getBlessedCursed(action.performer!!) < 0) return
                worldNightActions.hamsterPlayers.add(action.target)
                worldNightActions.guardedPlayers.add(action.target)
            }
            Action.GUARD -> {
                if (action.target == null) return
                if (action.target !in livingPlayers) return
                if (worldNightActions.getBlessedCursed(action.performer!!) < 0) return
                worldNightActions.guardedPlayers.add(action.target)
            }
            Action.SLUTS -> {
                if (action.target == null) return
                if (action.target !in livingPlayers) return
                if (worldNightActions.getBlessedCursed(action.performer!!) < 0) return
                worldNightActions.guardedPlayers.add(action.performer)
                worldNightActions.sleepsAt[action.performer] = action.target
            }
            Action.INVITES -> {
                if (action.target == null) return
                if (action.target !in livingPlayers) return
                if (worldNightActions.getBlessedCursed(action.performer!!) < 0) return
                worldNightActions.guardedPlayers.add(action.target)
                worldNightActions.sleepsAt[action.target] = action.performer
            }
            Action.POISONS -> {
                if (action.target == null) return
                if (worldNightActions.getBlessedCursed(action.performer!!) < 0) return
                worldNightActions.playersThatDiedTonight.add(action.target)
            }
            Action.SHOOT -> {
                if (action.target == null) return
                if (worldNightActions.getBlessedCursed(action.performer!!) < 0) return
                worldNightActions.mutualKills[action.performer] = action.target
            }
            Action.CHOOSE_EXAMPLE -> {
                if (action.target == null) return
                examples[action.performer!!] = action.target
            }
            Action.DEVILS_CHOICE -> {
                if (action.target == null) return
                when (val targetRole = roleSet[action.target]!!.toRole()) {
                    Role.SEER, Role.OLD_SEER, Role.WEREWOLF, Role.APPRENTICE_SEER -> {
                        roleSet[action.performer!!] = targetRole.name
                        if (targetRole == Role.WEREWOLF)
                            werewolfOrder = werewolfOrder.withItem(action.performer)
                    }
                    else -> roleSet[action.performer!!] = Role.VILLAGER.name
                }
            }
            Action.FINISH_NIGHT -> {
                finishNight()
            }
        }
    }
    
    private fun finishNight() {
        // Add passive role actions
        for (player in livingPlayers) {
            if (roleSet[player] == Role.HAMSTER.name) {
                if (worldNightActions.getBlessedCursed(player) < 0) continue
                worldNightActions.guardedPlayers.add(player)
                worldNightActions.hamsterPlayers.add(player)
            }
        }

        // Seen hamsters die
        for (player in worldNightActions.seenPlayers) {
            if (player in worldNightActions.hamsterPlayers) {
                worldNightActions.playersThatDiedTonight.add(player)
            }
        }

        // Wolves kill
        val killWolf = werewolfOrder.firstOrNull { wolf ->
            val target = worldNightActions.wolfTargets[wolf]

            wolf in livingPlayers
            && target != null
            && target in livingPlayers
            && roleSet[target]!!.toRole().canBeEaten()
        }
        val wolfTarget = worldNightActions.wolfTargets[killWolf]
        if (wolfTarget != null && worldNightActions.getBlessedCursed(killWolf!!) <= 0) {
            if (wolfTarget !in worldNightActions.guardedPlayers)
                worldNightActions.playersThatDiedTonight.add(wolfTarget)
            for ((guest, house) in worldNightActions.sleepsAt) {
                if (house == wolfTarget) {
                    worldNightActions.playersThatDiedTonight.add(guest)
                }
            }
        }

        fun hunterKill(hunter: String) {
            val target = worldNightActions.mutualKills[hunter] ?: return
            worldNightActions.playersThatDiedTonight.add(target)
            if (roleSet[target]!!.toRole().hasAction(Action.SHOOT)) {
                hunterKill(target)
            }
        }

        // Hunters shoot recursively if they died
        for ((killed, target) in worldNightActions.mutualKills) {
            if (roleSet[killed]!!.toRole().hasAction(Action.SHOOT)) {
                hunterKill(target)
            }
        }

        // Kill all players that died tonight
        for (player in worldNightActions.playersThatDiedTonight) {
            killAndHandleDeath(player)
        }

        worldNightActions.reset()
    }
    
    private fun killAndHandleDeath(player: String) {
        // Players can't die twice
        if (player !in livingPlayers) {
            worldIsPossible = false
        }
        val role = roleSet[player]!!.toRole()
        if (role == Role.SEER || role == Role.OLD_SEER) {
            val apprentice = roleSet.filter { it.value == Role.APPRENTICE_SEER.name }.keys.randomOrNull()
            if (apprentice != null) {
                roleSet[apprentice] = role.name
            }
        }
        for ((performer, target) in examples) {
            if (target == player) {
                roleSet[performer] = Role.WEREWOLF.name
                werewolfOrder = werewolfOrder.withItem(performer)
            }
        }
        livingPlayers.remove(player)
    }
    
    // endregion
    
    
    fun getPlayerDetails(): Table {
        val informationTable = Table()
        informationTable.add("Roles:".toLabel(fontSize = Constants.headingFontSize)).pad(5f).row()
        for ((player, role) in roleSet) {
            var roleDescription = role.toRole().displayName
            if (role == Role.WEREWOLF.name) {
                roleDescription += getWolfDescription(getWolfIndex(player))
            }
            val playerIsAlive = livingPlayers.contains(player)
            val textColour = if (playerIsAlive) Color.LIME else Color.RED
            informationTable.add("$player: $roleDescription".toLabel(fontColor = textColour)).pad(5f).row()
        }
        return informationTable
    }
    
    fun getActionTable(): Table {
        val actionTable = Table()
        actionTable.add("Actions:".toLabel(fontSize = Constants.headingFontSize)).pad(5f).row()
        for ((day, actions) in getPreformedActions()) {
            val expanderHeader = if (day == 1) "Night $day" else "Day and Night $day"
            val dayExpander = ExpanderTab(
                expanderHeader,
                startsOutOpened = false,
            ) {
                for (action in actions) {
                    it.add(action.getTextDisplay().toLabel()).pad(5f).row()
                }
            }
            actionTable.add(dayExpander).fillX().pad(5f).row()
        }
        return actionTable
    }

    companion object {
        private val wolfNames = listOf("Alpha", "Beta", "Gamma", "Delta", "Epsilon", "Zeta", "Eta", "Theta", "Iota", "Kappa", "Lambda", "Mu", "Nu", "Xi", "Omicron", "Pi", "Rho", "Sigma", "Tau", "Upsilon", "Phi", "Chi", "Psi", "Omega")

        fun getWolfDescription(index: Int): String {
            if (index < 0 || index >= wolfNames.size) return " ($index)"
            return " (${wolfNames[index]})"
        }
    }

    fun fixBugs() {
        for ((player, role) in roleSet) {
            if (role.toRole() == Role.WEREWOLF && player !in werewolfOrder) {
                werewolfOrder = werewolfOrder.withItem(player)
            }
        }
    }
}