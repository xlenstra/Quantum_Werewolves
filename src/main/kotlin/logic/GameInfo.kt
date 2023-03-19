package logic

import kotlinx.serialization.Serializable

@Serializable
class GameInfo(val roleSet: RoleSet) {

    val livingPlayers = roleSet.playerList.toMutableSet()
    val players = roleSet.playerList.toMutableSet()

    private val nightActions = AllNightActions()

    val nextPeriodInfo = mutableListOf<String>()

    var isDay = true

    fun executeLynchAction(player: String) {
        nextPeriodInfo.clear()
        val lynchedPlayerInfo = roleSet.lynchPlayer(player)
        nextPeriodInfo.add(lynchedPlayerInfo)
        livingPlayers.remove(player)
        checkQuantumDeaths()
        isDay = false
    }

    fun executeNightActions() {
        nextPeriodInfo.clear()
        for (actionType in listOf(Action.GUARD, Action.SLUTS)) {
            for (action in getRelevantActions(actionType)) {
                roleSet.executeAction(action)
            }
        }
        performSeerActions()
        for (action in getRelevantActions(Action.EAT))
            roleSet.executeAction(action)
        roleSet.endNight()
        checkQuantumDeaths()
        isDay = true
    }

    /** Should only be used for actions that don't create information, such as werewolves eating */
    private fun getRelevantActions(action: Action): Set<TargetedAction> {
        val relevantActions = mutableSetOf<TargetedAction>()
        for (player in players) {
            if (roleSet.rolePercentagesOfPlayer(player)[action.role] == 0f) continue

            val target = nightActions.getAction(player, action)
            relevantActions.add(TargetedAction(player, Action.LYNCH, target))
        }
        return relevantActions
    }

    private fun performSeerActions() {
        for (player in players) {
            if (!livingPlayers.contains(player)) continue

            val target = nightActions.getAction(player, Action.SEE)
            if (target == null) {
                nextPeriodInfo.add("$player did not see anyone")
                continue
            }
            val seenRole = roleSet.seePlayer(player, target)
            nextPeriodInfo.add("$player saw $target as $seenRole")
        }
    }

    private fun checkQuantumDeaths() {
        for (player in livingPlayers) {
            if (roleSet.deathPercentageOfPlayer(player) == 0f) {
                livingPlayers.remove(player)
                nextPeriodInfo.add("$player died due to Quantum Effects")
            }
        }
    }
}