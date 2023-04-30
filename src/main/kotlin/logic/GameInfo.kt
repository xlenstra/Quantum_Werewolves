package logic

import UI.screens.VictoryScreen
import kotlinx.serialization.Serializable

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

    lateinit var worldSet: WorldSet
    
    private var roleSet: WorldSet? = null

    private val witchesWithoutPotion = mutableListOf<String>()

    constructor(worldSet: WorldSet, gameName: String) : this() {
        this.worldSet = worldSet
        players = worldSet.playerList.toMutableSet()
        livingPlayers = worldSet.playerList.toMutableSet()
        nightActions = AllNightActions(players.toList())
        this.gameName = gameName
        if (this.gameName == "")
            this.gameName = "Quantum Wakkerdam"
    }

    /** Lynches a player. Returns whether a hunter was killed, causing another death */
    fun lynch(player: String): Boolean {
        nextPeriodInfo.clear()
        val role = worldSet.lynchPlayer(player)
        nextPeriodInfo.add("$player was lynched as ${role.displayName}")
        livingPlayers.remove(player)
        checkQuantumDeaths()
        checkForAndHandleWin()
        return role.hasAction(Action.SHOOT)
    }

    /** Kills a player as by a hunter. Returns whether a hunter was killed, causing another death */
    fun dayHunterKill(player: String): Boolean {
        val role = worldSet.lynchPlayer(player)
        nextPeriodInfo.add("$player was shot by a hunter, and died as ${role.displayName}")
        livingPlayers.remove(player)
        checkQuantumDeaths()
        checkForAndHandleWin()
        return role.hasAction(Action.SHOOT)
    }

    fun goToNight() {
        isDay = false
    }

    fun executeNightActions() {
        nextPeriodInfo.clear()
        val simpleActions = listOf(
            Action.CHOOSE_EXAMPLE,
            Action.BLESSES,
            Action.CURSES,
            Action.MAKE_HAMSTER,
            Action.GUARD,
            Action.SLUTS,
            Action.INVITES,
            Action.SHOOT,
            Action.DEVILS_CHOICE,
            Action.POISONS,
            Action.EAT,
        )
        for (actionType in simpleActions) {
            for (action in getRelevantActions(actionType)) {
                worldSet.executeAction(action)
                if (actionType == Action.POISONS && action.target != null) {
                    witchesWithoutPotion.add(action.performer!!)
                }
            }
        }
        performSeerActions()
        performOldSeerActions()
        
        worldSet.endNight()

        checkQuantumDeaths()
        checkForAndHandleWin()
        isDay = true

        nightActions.reset()
        dayCounter++
    }

    /** Should only be used for actions that don't create information, such as werewolves eating */
    private fun getRelevantActions(action: Action): Set<TargetedAction> {
        val relevantActions = mutableSetOf<TargetedAction>()
        for (player in livingPlayers) {
            if (!playerCanPreformAction(player, action)) continue

            val target = nightActions.getAction(player, action)
            relevantActions.add(TargetedAction(player, action, target))
        }
        return relevantActions
    }

    private fun performSeerActions() {
        for (player in livingPlayers) {
            if (!worldSet.rolePercentagesOfPlayer(player).keys.any { it.hasAction(Action.SEE) }) continue

            val target = nightActions.getAction(player, Action.SEE)
            if (target == null) {
                nextPeriodInfo.add("$player did not see anyone")
                continue
            }
            val seenRole: Role? = worldSet.seePlayer(player, target)
            if (seenRole == null) {
                nextPeriodInfo.add("$player saw $target as dead")
                continue
            }
            nextPeriodInfo.add("$player saw $target as ${seenRole.displayName}")
        }
    }

    private fun performOldSeerActions() {
        for (player in livingPlayers) {
            if (!worldSet.rolePercentagesOfPlayer(player).keys.any { it.hasAction(Action.OLDSEE) }) continue

            val target = nightActions.getAction(player, Action.OLDSEE)
            if (target == null) {
                nextPeriodInfo.add("$player did not see anyone")
                continue
            }
            val seenTeam: Team? = worldSet.oldSeePlayer(player, target)
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
            if (worldSet.deathPercentageOfPlayer(player) == 1f) {
                playersToDie.add(player)
                val role = worldSet.quantumKillPlayer(player)
                nextPeriodInfo.add("$player died due to Quantum Effects and was ${role.displayName}")
            }
        }
        // This is done to avoid ConcurrentModificationException
        if (playersToDie.isEmpty()) return
        livingPlayers.removeAll(playersToDie)
        checkQuantumDeaths()
    }

    fun playerCanPreformAction(player: String, action: Action): Boolean {
        if (action == Action.POISONS && player in witchesWithoutPotion) return false
        if (action == Action.CHOOSE_EXAMPLE && dayCounter != 1) return false
        if (action == Action.DEVILS_CHOICE && dayCounter != 2) return false
        if (!worldSet.rolePercentagesOfPlayer(player).keys.any { it.hasAction(action) }) return false

        return true
    }

    private fun checkForAndHandleWin() {
        val winningTeam = worldSet.getWinningTeam()
        if (winningTeam != null) {
            QuantumWerewolfGame.Current.pushScreen(VictoryScreen(this))
        }
    }

    fun getWinningTeam(): Team? {
        return worldSet.getWinningTeam()
    }

    fun fixBugs() {
        if (!this::gameName.isInitialized) {
            gameName = "Quantum Weerwolven"
        }
        if (roleSet != null) {
            worldSet = roleSet!!
            roleSet = null
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
        for (world in worldSet.possibleWorlds) {
            world.fixBugs()
        }
    }
}