package UI.screens

import UI.util.BaseScreen
import UI.util.addSeparator
import UI.util.toLabel
import UI.util.widgets.HorizontalTabber
import com.badlogic.gdx.scenes.scene2d.ui.Table
import logic.GameInfo

class WorldsScreen(private val gameInfo: GameInfo, private val mainScreen: BaseScreen) {

    val contents = HorizontalTabber(
        mainScreen,
        (gameInfo.worldSet.possibleWorlds).map { it.ID.toString() }.toList()
    ) { worldId, screenWidth -> getInformationForWorld(worldId, screenWidth) }

    fun update() {
        contents.tabs = (gameInfo.worldSet.possibleWorlds).map { it.ID.toString() }.toList()
        contents.update()
    }

    private fun getInformationForWorld(worldId: String, screenWidth: Float): Table {
        val tableWidth = screenWidth/2f - 5f
        val informationTable = Table()
        informationTable.add("World $worldId".toLabel(fontSize = Constants.headingFontSize)).colspan(3).pad(10f).row()
        informationTable.addSeparator(colSpan = 3).pad(10f).row()
        val world = gameInfo.worldSet.possibleWorlds.firstOrNull { it.ID == worldId.toInt() } ?: return informationTable
        informationTable.add(world.getPlayerDetails()).top().growX().prefWidth(tableWidth).padTop(10f)
//        informationTable.addSeparatorVertical()
        informationTable.add(world.getActionTable()).top().growX().prefWidth(tableWidth).padTop(10f).row()
        return informationTable
    }
}