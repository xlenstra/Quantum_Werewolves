import kotlinx.serialization.Serializable
import logic.Action
import logic.Role
import logic.TargetedAction
import logic.roleSetFromFile

@Serializable
data class Data(val a: Int, val b: String)

fun main(args: Array<String>) {
    val playerList = listOf("Xander", "Ruben", "Fenne", "Maries", "Theo", "Nina", "Arjan", "Bart", "Luc", "Max", "Alex")
//    val rolesList = listOf(logic.Role.WEREWOLF, logic.Role.WEREWOLF, logic.Role.WEREWOLF, logic.Role.SEER, logic.Role.GUARDIAN, logic.Role.HAMSTER, logic.Role.SLUT, logic.Role.VILLAGER, logic.Role.VILLAGER, logic.Role.VILLAGER, logic.Role.VILLAGER)
//
//    val roleSet = logic.RoleSet(5000, playerList, rolesList)
//
//    logic.roleSetToFile(roleSet, "test.json")
//
//    return

    val roleSet = roleSetFromFile("test.json")
    println(roleSet.possibleRolesWorlds.size)

    val livingPlayers = playerList.toMutableList()

    while (roleSet.possibleRolesWorlds.size > 1 && livingPlayers.size > 1) {
        val playerPercentages = roleSet.rolePercentagesOfAllPlayers(playerList)
        val deadPercentage = roleSet.deathPercentageOfAllPlayers(playerList)
        val quantumDeadPlayers = playerList.filter { deadPercentage[it]!! == 1f && it in livingPlayers}
        if (quantumDeadPlayers.isNotEmpty())
            println("Quantum dead players: $quantumDeadPlayers")
        livingPlayers.removeAll(quantumDeadPlayers)
        if (livingPlayers.isEmpty()) break
        for (player in livingPlayers) {
            if (Role.WEREWOLF in playerPercentages[player]!!.keys) {
                if (livingPlayers.size == 1)
                    continue
                val target = livingPlayers.filter { it != player }.random()
                roleSet.executeAction(TargetedAction(action= Action.EAT, performer=player, target=target))
                println("$player eats $target")
            }
        }
        for (player in livingPlayers) {
            if (Role.SEER in playerPercentages[player]!!.keys) {
                val target = livingPlayers.filter { it != player }.random()
                roleSet.seePlayer(player, target)
            }
        }
        for (player in livingPlayers) {
            if (Role.GUARDIAN in playerPercentages[player]!!.keys) {
                val target = livingPlayers.random()
                roleSet.executeAction(TargetedAction(action= Action.GUARD, performer=player, target=target))
                println("$player guards $target")
            }
        }
        for (player in livingPlayers) {
            if (Role.SLUT in playerPercentages[player]!!.keys) {
                val target = livingPlayers.random()
                roleSet.executeAction(TargetedAction(action= Action.SLUTS, performer = player, target=target))
                println("$player spends the night at $target")
            }
        }
        val lynchTarget = livingPlayers.random()
        roleSet.lynchPlayer(lynchTarget)
        livingPlayers.remove(lynchTarget)
        val wolfPercentage = livingPlayers.sumOf { roleSet.rolePercentagesOfPlayer(it)[Role.WEREWOLF]?.toDouble() ?: 0.0 }
        if (wolfPercentage == 0.0) {
            println("The werewolves have been killed!")
            break
        }
    }
    println(livingPlayers)
    println(roleSet.possibleRolesWorlds.size)
    for (possibleWorld in roleSet.possibleRolesWorlds) {
        println(possibleWorld.roleSet)
    }
}



