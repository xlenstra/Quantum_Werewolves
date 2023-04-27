package UI.screens

import UI.util.*
import com.badlogic.gdx.scenes.scene2d.ui.Table
import logic.GameInfo

class VictoryScreen(private val gameInfo: GameInfo) : PickerScreen() {

    init {
        val table = Table()
        table.add("Winning team: ${gameInfo.getWinningTeam()}".toLabel(fontSize = Constants.headingFontSize))
            .pad(10f).row()
        
        rightSideButton.setText("Back to Main Menu")
        rightSideButton.onClick {
            game.goToMainMenu()
        }
        rightSideButton.enable()
        
        val finalWorld = gameInfo.roleSet.possibleRoleWorlds.randomOrNull()
        if (finalWorld != null) {
            table.add("Final world: ${finalWorld.ID}".toLabel(fontSize = Constants.headingFontSize)).pad(10f).row()
            table.add(finalWorld.getPlayerDetails()).pad(10f).row()
            table.add(finalWorld.getActionTable()).pad(10f).row()
        }

        topTable.add(table)
    }
}