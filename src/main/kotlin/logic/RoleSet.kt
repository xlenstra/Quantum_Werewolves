package logic

import UI.util.toRole
import kotlinx.serialization.Serializable

@Serializable
class RoleSet() {

    var possibleRoleWorlds: List<World> = listOf()

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
        possibleRoleWorlds = tempList
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
        for (world in possibleRoleWorlds) {
            if (player !in world.livingPlayers) continue
            val role = world.roleSet[player]!!.toRole()
            roleCount[role] = roleCount.getOrDefault(role, 0) + 1
        }
        return roleCount.mapValues { it.value.toFloat() / possibleRoleWorlds.count { player in it.livingPlayers } }
    }

    fun deathPercentageOfAllPlayers(players: List<String>): Map<String, Float> =
        players.associateWith { deathPercentageOfPlayer(it) }

    fun deathPercentageOfPlayer(player: String): Float {
        val deathCount = possibleRoleWorlds.count { !it.livingPlayers.contains(player) }
        return deathCount.toFloat() / possibleRoleWorlds.size
    }
    
    // endregion
    // region actions

    fun executeAction(action: TargetedAction) {
        actionList.add(action)
        for (world in possibleRoleWorlds) {
            world.applyAction(action)
        }
        possibleRoleWorlds = possibleRoleWorlds.filter { it.worldIsPossible }
    }

    /**
     * Lynches the player [player], updating all worlds accordingly.
     * Returns the role of the lynched player
     */
    fun lynchPlayer(player: String): Role {
        val role = possibleRoleWorlds.filter { it.livingPlayers.contains(player) }.random().roleSet[player]!!.toRole()
        val action = TargetedAction(action= Action.LYNCH, target=player, targetRole=role)
        executeAction(action)
        return role
    }

    /**
     * Handles the death of a player dead in all worlds, 
     * returning a randomly chose role among all possible worlds
     */
    fun quantumKillPlayer(player: String): Role {
        val role = possibleRoleWorlds.random().roleSet[player]!!.toRole()
        val action = TargetedAction(action=Action.QUANTUM_DIE,target=player,targetRole=role)
        executeAction(action)
        return role
    }

    /**
     * 
     */
    fun seePlayer(performer: String, target: String): Role? {
        val worldsWithLivingSeer =
            possibleRoleWorlds.filter {
                it.livingPlayers.contains(performer) && it.roleSet[performer] == Role.SEER.name
            }
        val worldsWithLivingTarget =
            worldsWithLivingSeer.filter { it.livingPlayers.contains(target) }
        return if (worldsWithLivingTarget.isNotEmpty()) {
            val role = worldsWithLivingTarget.random().roleSet[target]!!.toRole().getRoleSeenAs()
            val action = TargetedAction(performer=performer, action= Action.SEE, target=target, targetRole=role)
            executeAction(action)
            role
        } else {
            val action = TargetedAction(performer=performer, action= Action.SEE, target=target, targetRole=null)
            executeAction(action)
            null
        }
    }

    fun oldSeePlayer(performer: String, target: String): Team? {
        val worldsWithLivingSeer =
            possibleRoleWorlds.filter {
                it.livingPlayers.contains(performer) && it.roleSet[performer] == Role.OLD_SEER.name
            }
        val worldsWithLivingTarget =
            worldsWithLivingSeer.filter { it.livingPlayers.contains(target) }
        return if (worldsWithLivingTarget.isNotEmpty()) {
            val team = worldsWithLivingTarget.random().roleSet[target]!!.toRole().getRoleSeenAs().team
            val action = TargetedAction(performer=performer, action= Action.OLDSEE, target=target, targetTeam=team)
            executeAction(action)
            team
        } else {
            val action = TargetedAction(performer=performer, action= Action.OLDSEE, target=target, targetTeam=null)
            executeAction(action)
            null
        }
    }

    fun endNight() = executeAction(TargetedAction(action = Action.FINISH_NIGHT))
    
    // endregion
    
    fun getWinningTeam(): Team? {
        val worldCount = possibleRoleWorlds.size
        val worldCountByTeam = possibleRoleWorlds.groupingBy { it.getWinningTeam() }.eachCount()
        val winningTeam = worldCountByTeam.maxByOrNull { it.value }?.key
        return if (winningTeam != null && worldCountByTeam[winningTeam] == worldCount) winningTeam 
        else null
    }
}