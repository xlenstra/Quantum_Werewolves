package logic

import UI.util.toRole
import kotlinx.serialization.Serializable

/**
 * Class for managing the entire set of worlds. 
 * Creates the worlds and allows for getting combined information from the worlds,
 * as well as preforming actions on all worlds simultaneously.
 */
@Serializable
class WorldSet() {

    var possibleWorlds: List<World> = listOf()

    var actionList = mutableListOf<TargetedAction>()
    var playerList = listOf<String>()
    
    var startingWorldCount = 100

    constructor(worldCount: Int, players: List<String>, roleList: List<Role>) : this() {
        startingWorldCount = worldCount
        
        val tempList = mutableListOf<World>()
        playerList = players
        repeat(worldCount) { index ->
            val shuffledRoles = roleList.shuffled()
            val roleSet = players.zip(shuffledRoles).toMap()
            val world = World(roleSet, index)
            if (world !in tempList)
                tempList.add(world)
        }
        possibleWorlds = tempList
    }
    
    // region percentages

    fun rolePercentagesOfAllPlayers(players: List<String>): Map<String, Map<Role, Float>> {
        val playerMap = mutableMapOf<String, Map<Role, Float>>()
        for (player in players) {
            playerMap[player] = rolePercentagesOfPlayer(player)
        }
        return playerMap
    }

    /**
     * Returns a map of roles to the percentage of worlds in which the player has that role
     * If a player is a certain role in no worlds, the role will not be in the map 
     */
    fun rolePercentagesOfPlayer(player: String): Map<Role, Float> {
        val roleCount = mutableMapOf<Role, Int>()
        for (world in possibleWorlds) {
            if (player !in world.livingPlayers) continue
            val role = world.roleSet[player]!!.toRole()
            roleCount[role] = roleCount.getOrDefault(role, 0) + 1
        }
        return roleCount.mapValues { it.value.toFloat() / possibleWorlds.count { player in it.livingPlayers } }
    }

    fun deathPercentageOfAllPlayers(players: List<String>): Map<String, Float> =
        players.associateWith { deathPercentageOfPlayer(it) }

    fun deathPercentageOfPlayer(player: String): Float {
        val deathCount = possibleWorlds.count { !it.livingPlayers.contains(player) }
        return deathCount.toFloat() / possibleWorlds.size
    }
    
    // endregion
    // region actions

    fun executeAction(action: TargetedAction) {
        actionList.add(action)
        for (world in possibleWorlds) {
            world.applyAction(action)
        }
        possibleWorlds = possibleWorlds.filter { it.worldIsPossible }
    }

    /**
     * Lynches the player [player], updating all worlds accordingly.
     * Returns the role of the lynched player
     */
    fun lynchPlayer(player: String): Role {
        val role = possibleWorlds.filter { it.livingPlayers.contains(player) }.random().roleSet[player]!!.toRole()
        val action = TargetedAction(action= Action.LYNCH, target=player, targetRole=role)
        executeAction(action)
        return role
    }

    /**
     * Handles the death of a player dead in all worlds, 
     * returning a randomly chose role among all possible worlds
     */
    fun quantumKillPlayer(player: String): Role {
        val role = possibleWorlds.random().roleSet[player]!!.toRole()
        val action = TargetedAction(action=Action.QUANTUM_DIE,target=player,targetRole=role)
        executeAction(action)
        return role
    }

    /**
     * 
     */
    fun seePlayer(performer: String, target: String): Role? {
        val validWorlds = possibleWorlds.filter {
            it.livingPlayers.contains(performer)
            && it.livingPlayers.contains(target)
            && it.roleSet[performer]!!.toRole() == Role.SEER
        }
        val roleSeen = 
            if (validWorlds.isNotEmpty())
                validWorlds.random().roleSet[target]!!.toRole().getRoleSeenAs() 
            else null
        val action = TargetedAction(performer=performer, action= Action.SEE, target=target, targetRole=roleSeen)
        executeAction(action)
        return roleSeen
    }

    fun oldSeePlayer(performer: String, target: String): Team? {
        val validWorlds = possibleWorlds.filter {
            it.livingPlayers.contains(performer)
            && it.livingPlayers.contains(target)
            && it.roleSet[performer]!!.toRole() == Role.OLD_SEER
        }
        val teamSeen =
            if (validWorlds.isNotEmpty())
                validWorlds.random().roleSet[target]!!.toRole().getTeamOldSeenAs()
            else null
        val action = TargetedAction(performer=performer, action= Action.OLDSEE, target=target, targetTeam=teamSeen)
        executeAction(action)
        return teamSeen
    }

    fun endNight() = executeAction(TargetedAction(action = Action.FINISH_NIGHT))
    
    // endregion
    
    fun getWinningTeam(): Team? {
        val worldCount = possibleWorlds.size
        val worldCountByTeam = possibleWorlds.groupingBy { it.getWinningTeam() }.eachCount()
        val winningTeam = worldCountByTeam.maxByOrNull { it.value }?.key
        return if (winningTeam != null && worldCountByTeam[winningTeam] == worldCount) winningTeam 
        else null
    }
}