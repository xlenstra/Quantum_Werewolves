package logic

import UI.popup.PickOptionPopup
import UI.screens.VictoryScreen
import UI.util.toRole
import UI.util.withItem
import kotlinx.serialization.Serializable
import logic.BackwardsCompatibility.fixOldBugs

@Serializable
class GameInfo() {

    @Volatile
    var customSaveLocation: String? = null
    
    lateinit var livingPlayers: MutableSet<String>// = roleSet.playerList.toMutableSet()
    lateinit var players: MutableSet<String>// = roleSet.playerList.toMutableSet()
    lateinit var gameName: String

    lateinit var nightActions: AllNightActions
    val nextPeriodInfo = mutableListOf<String>()
    
    var dayCounter = 1
    var isDay = false
    
    lateinit var roleSet: RoleSet
    
    private val witchesWithoutPotion = mutableListOf<String>()
    
    constructor(roleSet: RoleSet, gameName: String) : this() {
        this.roleSet = roleSet
        players = roleSet.playerList.toMutableSet()
        livingPlayers = roleSet.playerList.toMutableSet()
        nightActions = AllNightActions(players.toList())
        this.gameName = gameName
        if (this.gameName == "")
            this.gameName = "Quantum Wakkerdam"
    }
    
    /** Lynches a player. Returns whether a hunter was killed, causing another death */
    fun lynch(player: String): Boolean {
        nextPeriodInfo.clear()
        val role = roleSet.lynchPlayer(player)
        nextPeriodInfo.add("$player was lynched as ${role.displayName}")
        livingPlayers.remove(player)
        checkQuantumDeaths()
        checkForAndHandleWin()
        return role.mapToActions().contains(Action.SHOOT)
    }

    /** Kills a player as by a hunter. Returns whether a hunter was killed, causing another death */
    fun dayHunterKill(player: String): Boolean {
        val role = roleSet.lynchPlayer(player)
        nextPeriodInfo.add("$player was shot by a hunter, and died as ${role.displayName}")
        livingPlayers.remove(player)
        checkQuantumDeaths()
        checkForAndHandleWin()
        return role.mapToActions().contains(Action.SHOOT)
    }
    
    fun goToNight() {
        isDay = false
    }

    fun executeNightActions() {
        nextPeriodInfo.clear()
        for (actionType in listOf(Action.GUARD, Action.SLUTS, Action.SHOOT)) {
            for (action in getRelevantActions(actionType)) {
                roleSet.executeAction(action)
            }
        }
        for (action in getRelevantActions(Action.CHOOSE_EXAMPLE)) {
            if (dayCounter != 1) continue
            roleSet.executeAction(action)
        }
        for (action in getRelevantActions(Action.DEVILS_CHOICE)) {
            if (dayCounter != 2) continue
            roleSet.executeAction(action)
        }
        for (action in getRelevantActions(Action.POISONS)) {
            roleSet.executeAction(action)
            if (action.action == Action.POISONS && action.target != null)
                witchesWithoutPotion.add(action.performer!!)
        }
        performSeerActions()
        performOldSeerActions()
        for (action in getRelevantActions(Action.EAT))
            roleSet.executeAction(action)
        roleSet.endNight()
        
        checkQuantumDeaths()
        checkForAndHandleWin()
        isDay = true

        nightActions.reset()
        dayCounter++
    }

    /** Should only be used for actions that don't create information, such as werewolves eating */
    private fun getRelevantActions(action: Action): Set<TargetedAction> {
        val relevantActions = mutableSetOf<TargetedAction>()
        for (player in players) {
            if (action.performingRole !in roleSet.rolePercentagesOfPlayer(player)) continue
            if (action == Action.POISONS && player in witchesWithoutPotion) continue
            
            val target = nightActions.getAction(player, action)
            relevantActions.add(TargetedAction(player, action, target))
        }
        return relevantActions
    }

    private fun performSeerActions() {
        for (player in players) {
            if (!livingPlayers.contains(player)) continue
            if (Action.SEE.performingRole !in roleSet.rolePercentagesOfPlayer(player)) continue

            val target = nightActions.getAction(player, Action.SEE)
            if (target == null) {
                nextPeriodInfo.add("$player did not see anyone")
                continue
            }
            val seenRole: Role? = roleSet.seePlayer(player, target)
            if (seenRole == null) {
                nextPeriodInfo.add("$player saw $target as dead")
                continue
            }
            nextPeriodInfo.add("$player saw $target as ${seenRole.displayName}")
        }
    }
    
    private fun performOldSeerActions() {
        for (player in players) {
            if (!livingPlayers.contains(player)) continue
            if (Action.OLDSEE.performingRole !in roleSet.rolePercentagesOfPlayer(player)) continue

            val target = nightActions.getAction(player, Action.OLDSEE)
            if (target == null) {
                nextPeriodInfo.add("$player did not see anyone")
                continue
            }
            val seenTeam: Team? = roleSet.oldSeePlayer(player, target)
            if (seenTeam == null) {
                nextPeriodInfo.add("$player saw $target as dead")
                continue
            }
            nextPeriodInfo.add("$player saw $target as ${seenTeam.displayName}")
        }
    }

    private fun checkQuantumDeaths() {
        val playersToDie = mutableSetOf<String>()
        for (player in livingPlayers) {
            if (roleSet.deathPercentageOfPlayer(player) == 1f) {
                playersToDie.add(player)
                val role = roleSet.quantumKillPlayer(player)
                nextPeriodInfo.add("$player died due to Quantum Effects and was ${role.displayName}")
            }
        }
        // This is done to avoid ConcurrentModificationException
        if (playersToDie.isEmpty()) return
        livingPlayers.removeAll(playersToDie)
        checkQuantumDeaths()
    }
    
    fun playerCanPreformAction(player: String, action: Action): Boolean {
        if (action.performingRole !in roleSet.rolePercentagesOfPlayer(player)) return false
        if (action == Action.POISONS && player in witchesWithoutPotion) return false
        if (action == Action.CHOOSE_EXAMPLE && dayCounter != 1) return false
        if (action == Action.DEVILS_CHOICE && dayCounter != 2) return false
        
        return true
    }
    
    private fun checkForAndHandleWin() {
        val winningTeam = roleSet.getWinningTeam()
        if (winningTeam != null) {
            QuantumWerewolfGame.Current.pushScreen(VictoryScreen(this))
        }
    }

    fun getWinningTeam(): Team? {
        return roleSet.getWinningTeam()
    }
    
    fun fixBugs() {
        if (!this::gameName.isInitialized) {
            gameName = "Quantum Weerwolven"
        }
    }
}

class GameInfoPreview() {
    var livingPlayers = mutableSetOf<String>()
    var players = mutableSetOf<String>()
    var dayCounter = 0
    var isDay = false
    
    constructor(gameInfo: GameInfo) : this() {
        livingPlayers = gameInfo.livingPlayers
        players = gameInfo.players
        dayCounter = gameInfo.dayCounter
        isDay = gameInfo.isDay
    }
}

object BackwardsCompatibility {
    fun GameInfo.fixOldBugs() {
        fixBugs()
        for (world in roleSet.possibleRoleWorlds) {
            world.fixBugs()
        }
    }   
}