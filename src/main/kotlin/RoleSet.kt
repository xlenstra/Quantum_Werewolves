import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

enum class Role {
    WEREWOLF,
    VILLAGER,
    SEER,
}

@Serializable
data class RolesWorld(val roleSet: Map<String, Role>) {

    private val werewolfOrder: List<String> =
        roleSet.filter { it.value == Role.WEREWOLF }.keys.shuffled()
    val livingPlayers = roleSet.keys.toMutableList()
    var worldIsPossible = true

    private fun actionApplies(action: Action): Boolean {
        if (action.action == Actions.LYNCH) return true
        if (action.performer !in livingPlayers) return false

        val role = roleSet[action.performer]!!
        return when (action.action) {
            Actions.EAT -> role == Role.WEREWOLF
            Actions.SEE -> role == Role.SEER
            else -> true
        }
    }

    private fun getHighestLivingWerewolf(): String {
        return werewolfOrder.first { it in livingPlayers }
    }

    fun applyAction(action: Action) {
        if (!actionApplies(action)) return
        when (action.action) {
            Actions.EAT -> {
                if (werewolfOrder.none { it in livingPlayers }) {
                    println("Panic!")
                }
                if (action.performer == getHighestLivingWerewolf()) {
                    livingPlayers.remove(action.target)
                }
            }
            Actions.SEE -> {
                if (roleSet[action.target] != action.targetRole) {
                    worldIsPossible = false
                }
            }
            Actions.LYNCH -> {
                if (action.target !in livingPlayers)
                    worldIsPossible = false
                if (roleSet[action.target] != action.targetRole) {
                    worldIsPossible = false
                }
                livingPlayers.remove(action.target)
            }
            else -> {}
        }
    }
}

@Serializable
class RoleSet {

    var startingRolesWorlds: List<RolesWorld> = listOf()
    var possibleRolesWorlds: List<RolesWorld> = listOf()

    var actionList = mutableListOf<Action>()
    var playerList = listOf<String>()

    constructor(worldCount: Int, players: List<String>, roleList: List<Role>) {
        val tempList = mutableListOf<RolesWorld>()
        playerList = players
        repeat(worldCount) {
            val shuffledRoles = roleList.shuffled()
            val roleSet = players.zip(shuffledRoles).toMap()
            tempList.add(RolesWorld(roleSet))
        }
        startingRolesWorlds = tempList
        possibleRolesWorlds = startingRolesWorlds
    }

    fun getPlayerPercentages(players: List<String>): Map<String, Map<Role, Float>> {
        val playerMap = mutableMapOf<String, Map<Role, Float>>()
        for (player in players) {
            playerMap[player] = getRolePercentages(player)
        }
        return playerMap
    }

    fun getRolePercentages(player: String): Map<Role, Float> {
        val roleCount = mutableMapOf<Role, Int>()
        for (world in possibleRolesWorlds) {
            if (player !in world.livingPlayers) continue
            val role = world.roleSet[player]!!
            roleCount[role] = roleCount.getOrDefault(role, 0) + 1
        }
        return roleCount.mapValues { it.value.toFloat() / possibleRolesWorlds.count { player in it.livingPlayers } }
    }

    fun getDeathPercentages(players: List<String>): Map<String, Float> =
        players.associateWith { getDeathPercentage(it) }

    fun getDeathPercentage(player: String): Float {
        val deathCount = possibleRolesWorlds.count { !it.livingPlayers.contains(player) }
        return deathCount.toFloat() / possibleRolesWorlds.size
    }

    fun executeAction(action: Action) {
        actionList.add(action)
        for (world in possibleRolesWorlds) {
            world.applyAction(action)
        }
        possibleRolesWorlds = possibleRolesWorlds.filter { it.worldIsPossible }
    }

    fun lynchPlayer(player: String) {
        val role = possibleRolesWorlds.filter { it.livingPlayers.contains(player) }.random().roleSet[player]!!
        val action = Action(action=Actions.LYNCH, target=player, targetRole=role)
        executeAction(action)
        println("$player is executed as $role")
    }

    fun seePlayer(performer: String, target: String) {
        val possibleRoles = possibleRolesWorlds
            .filter {
                it.livingPlayers.contains(performer)
                        && it.livingPlayers.contains(target)
                        && it.roleSet[performer] == Role.SEER
            }
        if (possibleRoles.size == 0)
            println("Panic!")
        val role = possibleRoles.random()
            .roleSet[target]!!
        println("Seer $performer sees $target as $role")
        val action = Action(performer=performer, action=Actions.SEE, target=target, targetRole=role)
        executeAction(action)
    }
}

fun roleSetToFile(roleSet: RoleSet, fileName: String) {
    val json = Json.encodeToString(roleSet)
    File(fileName).writeText(json)
}

fun roleSetFromFile(fileName: String): RoleSet {
    val json = File(fileName).readText()
    return Json.decodeFromString(json)
}