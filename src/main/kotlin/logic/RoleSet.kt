package logic

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

enum class Role(val displayName: String) {
    WEREWOLF("Weerwolf"),
    VILLAGER("Burger"),
    SEER("Ziener"),
    GUARDIAN("Beschermengel"),
    HAMSTER("Weerhamster"),
    SLUT("Slet"),
}

@Serializable
data class RolesWorld(val roleSet: Map<String, Role>) {

    private val werewolfOrder: List<String> =
        roleSet.filter { it.value == Role.WEREWOLF }.keys.shuffled()
    val livingPlayers = roleSet.keys.toMutableList()
    var worldIsPossible = true

    private val worldNightActions = WorldNightActions()

    private fun actionApplies(action: TargetedAction): Boolean {
        if (action.action == Action.LYNCH) return true
        if (action.performer !in livingPlayers) return false

        val role = roleSet[action.performer]!!
        return when (action.action) {
            Action.EAT -> role == Role.WEREWOLF
            Action.SEE -> role == Role.SEER
            Action.GUARD -> role == Role.GUARDIAN
            Action.SLUTS -> role == Role.SLUT
            else -> true
        }
    }

    private fun getHighestLivingWerewolf(): String {
        return werewolfOrder.first { it in livingPlayers }
    }

    fun applyAction(action: TargetedAction) {
        if (!actionApplies(action)) return
        when (action.action) {
            Action.LYNCH -> {
                if (action.target !in livingPlayers)
                    worldIsPossible = false
                if (roleSet[action.target] != action.targetRole) {
                    worldIsPossible = false
                }
                livingPlayers.remove(action.target)
            }
            Action.EAT -> {
                if (werewolfOrder.none { it in livingPlayers }) {
                    println("Panic!")
                }
                if (action.performer == getHighestLivingWerewolf()) {
                    worldNightActions.wolfTarget = action.target
                    worldNightActions.killWolf = action.performer
                }
            }
            Action.SEE -> {
                // If the target is already dead, we don't need to do anything here
                if (action.targetRole == null) return
                if (roleSet[action.target] != action.targetRole) {
                    worldIsPossible = false
                }
                if (action.targetRole == Role.HAMSTER) {
                    livingPlayers.remove(action.target)
                }
            }
            Action.GUARD -> {
                worldNightActions.guardedPlayers.add(action.target!!)
            }
            Action.SLUTS -> {
                worldNightActions.guardedPlayers.add(action.performer!!)
                worldNightActions.sleepsAt[action.performer] = action.target!!
            }
            Action.FINISH_NIGHT -> {
                for (player in livingPlayers) {
                    if (roleSet[player] == Role.HAMSTER) {
                        worldNightActions.guardedPlayers.add(player)
                    }
                }
                if (worldNightActions.wolfTarget != null && worldNightActions.wolfTarget !in worldNightActions.guardedPlayers) {
                    livingPlayers.remove(worldNightActions.wolfTarget)
                    for ((guest, house) in worldNightActions.sleepsAt) {
                        if (house == worldNightActions.wolfTarget) {
                            livingPlayers.remove(guest)
                        }
                    }
                }
                worldNightActions.reset()
            }
        }
    }
}

@Serializable
class RoleSet {

    var startingRolesWorlds: List<RolesWorld> = listOf()
    var possibleRolesWorlds: List<RolesWorld> = listOf()

    var actionList = mutableListOf<TargetedAction>()
    var playerList = listOf<String>()

    constructor(worldCount: Int, players: List<String>, roleList: List<Role>) {
        val tempList = mutableListOf<RolesWorld>()
        playerList = players
        repeat(worldCount) {
            val shuffledRoles = roleList.shuffled()
            val roleSet = players.zip(shuffledRoles).toMap()
            val world = RolesWorld(roleSet)
            if (world !in tempList)
                tempList.add(world)
        }
        startingRolesWorlds = tempList
        possibleRolesWorlds = startingRolesWorlds
    }

    fun rolePercentagesOfAllPlayers(players: List<String>): Map<String, Map<Role, Float>> {
        val playerMap = mutableMapOf<String, Map<Role, Float>>()
        for (player in players) {
            playerMap[player] = rolePercentagesOfPlayer(player)
        }
        return playerMap
    }

    fun rolePercentagesOfPlayer(player: String): Map<Role, Float> {
        val roleCount = mutableMapOf<Role, Int>()
        for (world in possibleRolesWorlds) {
            if (player !in world.livingPlayers) continue
            val role = world.roleSet[player]!!
            roleCount[role] = roleCount.getOrDefault(role, 0) + 1
        }
        return roleCount.mapValues { it.value.toFloat() / possibleRolesWorlds.count { player in it.livingPlayers } }
    }

    fun deathPercentageOfAllPlayers(players: List<String>): Map<String, Float> =
        players.associateWith { deathPercentageOfPlayer(it) }

    fun deathPercentageOfPlayer(player: String): Float {
        val deathCount = possibleRolesWorlds.count { !it.livingPlayers.contains(player) }
        return deathCount.toFloat() / possibleRolesWorlds.size
    }

    fun executeAction(action: TargetedAction) {
        actionList.add(action)
        for (world in possibleRolesWorlds) {
            world.applyAction(action)
        }
        possibleRolesWorlds = possibleRolesWorlds.filter { it.worldIsPossible }
    }

    fun lynchPlayer(player: String): String {
        val role = possibleRolesWorlds.filter { it.livingPlayers.contains(player) }.random().roleSet[player]!!
        val action = TargetedAction(action= Action.LYNCH, target=player, targetRole=role)
        executeAction(action)
        println("$player is executed as $role")
        return "$player is executed as ${role.name}"
    }

    fun seePlayer(performer: String, target: String): String {
        val worldsWithLivingSeer =
            possibleRolesWorlds.filter {
                it.livingPlayers.contains(performer) && it.roleSet[performer] == Role.SEER
            }
        val worldsWithLivingTarget =
            worldsWithLivingSeer.filter { it.livingPlayers.contains(target) }
        if (worldsWithLivingTarget.isNotEmpty()) {
            val role = worldsWithLivingTarget.random().roleSet[target]!!
//            println("Seer $performer sees $target as $role")
            val action = TargetedAction(performer=performer, action= Action.SEE, target=target, targetRole=role)
            executeAction(action)
            return "Seer $performer sees $target as ${role.name}"
        } else {
//            println("Seer $performer sees $target as dead")
            val action = TargetedAction(performer=performer, action= Action.SEE, target=target, targetRole=null)
            executeAction(action)
            return "Seer $performer sees $target as dead"
        }
    }

    fun endNight() = executeAction(TargetedAction(action = Action.FINISH_NIGHT))
}

fun roleSetToFile(roleSet: RoleSet, fileName: String) {
    val json = Json.encodeToString(roleSet)
    File(fileName).writeText(json)
}

fun roleSetFromFile(fileName: String): RoleSet {
    val json = File(fileName).readText()
    return Json.decodeFromString(json)
}