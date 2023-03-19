package UI.screens

import UI.util.*
import logic.GameInfo
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Table

class PercentageOverview(private val gameInfo: GameInfo, mainScreen: BaseScreen) : Table() {

    private val playerChooseTable = Table()
    private val informationTable = Table()

    init {
        add(ScrollPane(playerChooseTable)).width(mainScreen.stage.width/3).grow()
        addSeparatorVertical()
        add(ScrollPane(informationTable)).width(mainScreen.stage.width/3*2).grow().pad(25f).row()
        setPlayerChooseTable()
    }

    private fun setPlayerChooseTable() {
        playerChooseTable.clear()
        for (player in gameInfo.players) {
            val playerButton = player.toTextButton()
            playerButton.onClick {
                setInformationForPlayer(player)
            }
            playerChooseTable.add(playerButton).pad(10f).row()
        }
    }

    private fun setInformationForPlayer(player: String) {
        informationTable.clear()
        informationTable.add(player.toLabel(fontSize=Constants.headingFontSize)).pad(10f).row()
        informationTable.add("${100f-100f*gameInfo.roleSet.deathPercentageOfPlayer(player)}% alive".toLabel()).pad(5f).row()
        informationTable.addSeparator().pad(10f)
        informationTable.add("Percentages:".toLabel(fontSize=Constants.headingFontSize)).pad(5f).row()
        val percentages = gameInfo.roleSet.rolePercentagesOfPlayer(player)
        for (role in percentages.keys) {
            informationTable.add("${role.displayName}: ${100f*percentages[role]!!}%".toLabel()).pad(5f).row()
        }
    }
}