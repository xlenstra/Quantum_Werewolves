package logic

import UI.util.toRole

class GameSetupInfo(gameInfo: GameInfo) {
    val worldCount = gameInfo.worldSet.startingWorldCount
    val gameName = gameInfo.gameName
    val players = gameInfo.players
    val roles = gameInfo.worldSet.possibleWorlds.firstOrNull()?.roleSet?.values?.map { it.toRole() }?.sorted() ?: listOf()
}
