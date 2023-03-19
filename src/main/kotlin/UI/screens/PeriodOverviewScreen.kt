package UI.screens

import UI.util.BaseScreen
import UI.util.addSeparator
import UI.util.toLabel
import com.badlogic.gdx.scenes.scene2d.ui.Table
import logic.GameInfo

class PeriodOverviewScreen(gameInfo: GameInfo, mainScreen: BaseScreen, isDay: Boolean) : Table() {

    init {
        add("Period Overview Screen".toLabel(fontSize = Constants.headingFontSize)).row()
        if (isDay) {
            add("Day".toLabel()).pad(5f).row()
        } else {
            add("Night".toLabel()).pad(5f).row()
        }
        addSeparator().pad(10f)
        add("Living Players".toLabel(fontSize = Constants.headingFontSize)).pad(5f).row()
        add(gameInfo.livingPlayers.joinToString(", ").toLabel()).pad(5f).row()
        val deadPlayers = 
        add("Dead Players".toLabel(fontSize = Constants.headingFontSize)).pad(5f).row()
        add(gameInfo.players.filter { it !in gameInfo.livingPlayers }.joinToString(", ").toLabel()).pad(5f).row()
        if (gameInfo.nextPeriodInfo.isNotEmpty()) {
            addSeparator().pad(10f)
            add("Info from previous period".toLabel(fontSize = Constants.headingFontSize)).pad(5f).row()
            for (line in gameInfo.nextPeriodInfo) {
                add(line.toLabel()).pad(5f).row()
            }
        }
    }
}