import kotlinx.serialization.Serializable
import kotlin.random.Random

@Serializable
data class Data(val a: Int, val b: String)

fun main(args: Array<String>) {
    val playerList = listOf("Xander", "Ruben", "Fenne", "Maries", "Theo", "Nina", "Arjan")
//    val rolesList = listOf(Role.WEREWOLF, Role.WEREWOLF, Role.SEER, Role.VILLAGER, Role.VILLAGER, Role.VILLAGER, Role.VILLAGER)
//
//    val roleSet = RoleSet(1000, playerList, rolesList)
//
//    roleSetToFile(roleSet, "test.json")



    val roleSet = roleSetFromFile("test.json")

    var livingPlayers = playerList

    while (roleSet.possibleRolesWorlds.size > 1 && livingPlayers.size > 1) {
        val playerPercentages = roleSet.getPlayerPercentages(playerList)
        val deadPercentage = roleSet.getDeathPercentages(playerList)
        livingPlayers = playerList.filter { deadPercentage[it]!! != 1f }
        if (livingPlayers.isEmpty()) break
        for (player in livingPlayers) {
            if (Role.WEREWOLF in playerPercentages[player]!!.keys) {
                val target = livingPlayers.filter { it != player }.random()
                roleSet.executeAction(Action(action=Actions.EAT, performer=player, target=target))
                println("$player eats $target")
            }
        }
        for (player in livingPlayers) {
            if (Role.SEER in playerPercentages[player]!!.keys) {
                val target = livingPlayers.filter { it != player }.random()
                roleSet.seePlayer(player, target)
            }
        }
        val lynchTarget = livingPlayers.random()
        roleSet.lynchPlayer(lynchTarget)
    }
    print(roleSet.possibleRolesWorlds[0])
}



