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
        werewolfOrder = roleSet.filter { it.value == Role.WEREWOLF }.keys.shuffled()
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
        return action.action!!.performingRole!!.name == roleSet[action.performer]!!
    }

    fun applyAction(action: TargetedAction) {
        if (!actionApplies(action)) return
        
        executedActions.add(action)
        when (action.action!!) {
            Action.LYNCH -> {
                if (roleSet[action.target] != action.targetRole!!.name) {
                    worldIsPossible = false
                }
                killAndHandleDeath(action.target!!, true)
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
                // If the target is already dead, we don't need to do anything here
                if (action.targetRole == null) return
                if (roleSet[action.target]!!.toRole().getRoleSeenAs() != action.targetRole) {
                    worldIsPossible = false
                }
                if (action.targetRole == Role.HAMSTER) {
                    killAndHandleDeath(action.target!!)
                }
            }
            Action.OLDSEE -> {
                if (action.targetTeam == null) return
                if (action.target == null) return
                if (roleSet[action.target]!!.toRole().getRoleSeenAs().team != action.targetTeam) {
                    worldIsPossible = false
                }
                if (roleSet[action.target]!! == Role.HAMSTER.name) {
                    killAndHandleDeath(action.target)
                }
            }
            Action.GUARD -> {
                if (action.target == null) return
                worldNightActions.guardedPlayers.add(action.target)
            }
            Action.SLUTS -> {
                if (action.target == null) return
                if (action.target !in livingPlayers) return
                worldNightActions.guardedPlayers.add(action.performer!!)
                worldNightActions.sleepsAt[action.performer] = action.target
            }
            Action.POISONS -> {
                if (action.target == null) return
                if (action.target !in livingPlayers) {
                    worldIsPossible = false
                    return
                }
                killAndHandleDeath(action.target, true)
            }
            Action.SHOOT -> {
                if (action.target == null) return
                worldNightActions.mutualKills[action.performer!!] = action.target
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
                for (player in livingPlayers) {
                    if (roleSet[player] == Role.HAMSTER.name) {
                        worldNightActions.guardedPlayers.add(player)
                    }
                }
                val killWolf = werewolfOrder.firstOrNull {
                    val target = worldNightActions.wolfTargets[it]
                    it in livingPlayers
                    && target in livingPlayers
                    && roleSet.getOrDefault(target, null) != Role.WEREWOLF.name
                }
                val wolfTarget = worldNightActions.wolfTargets[killWolf]
                
                if (wolfTarget != null) {
                    if (wolfTarget !in worldNightActions.guardedPlayers)
                        killAndHandleDeath(wolfTarget)
                    for ((guest, house) in worldNightActions.sleepsAt) {
                        if (house == wolfTarget) {
                            killAndHandleDeath(guest)
                        }
                    }
                }
                for ((killed, target) in worldNightActions.mutualKills) {
                    if (killed !in livingPlayers) { // Killed this night
                        killAndHandleDeath(target, true)
                    }
                }
                worldNightActions.reset()
            }
        }
    }
    
    private fun killAndHandleDeath(player: String, dropWorldIfDead: Boolean = false) {
        if (dropWorldIfDead && player !in livingPlayers) {
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