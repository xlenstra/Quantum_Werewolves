package logic

import UI.util.toRole

class GameSetupInfo(gameInfo: GameInfo) {
    val worldCount = gameInfo.roleSet.startingWorldCount
    val gameName = gameInfo.gameName
    val players = gameInfo.players
    val roles = gameInfo.roleSet.possibleRoleWorlds.firstOrNull()?.roleSet?.values?.map { it.toRole() } ?: listOf()
}
