package UI.screens

import UI.util.*
import UI.util.widgets.TabbedPager
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Table
import logic.GameInfo
import kotlin.math.roundToInt

class PeriodOverviewScreen(private val gameInfo: GameInfo, mainScreen: BaseScreen) : Table() {

    init {
        update()
    }
    
    fun update() {
        clear()
        add("Period Overview Screen".toLabel(fontSize = Constants.headingFontSize)).row()
        if (gameInfo.isDay) {
            add("Day ${gameInfo.dayCounter}".toLabel()).pad(5f).row()
        } else {
            add("Night ${gameInfo.dayCounter}".toLabel()).pad(5f).row()
        }
        add("Worlds left: ${gameInfo.roleSet.possibleRoleWorlds.size}".toLabel()).pad(5f).row()
        addSeparator().pad(10f)
        add("Living Players".toLabel(fontSize = Constants.headingFontSize)).pad(5f).row()
        add(gameInfo.livingPlayers.joinToString(", ").toLabel()).pad(5f).row()
        add("Dead Players".toLabel(fontSize = Constants.headingFontSize)).pad(5f).row()
        add(gameInfo.players.filter { it !in gameInfo.livingPlayers }.joinToString(", ").toLabel()).pad(5f).row()
        add(getCopyAlivePercentageToClipboardButton(gameInfo.roleSet.deathPercentageOfAllPlayers(gameInfo.players.toList()))).pad(10f).row()
        if (gameInfo.nextPeriodInfo.isNotEmpty()) {
            addSeparator().pad(10f)
            add("Info from previous period".toLabel(fontSize = Constants.headingFontSize)).pad(5f).row()
            for (line in gameInfo.nextPeriodInfo) {
                add(line.toLabel()).pad(5f).row()
            }
        }
    }
    
    private fun getCopyAlivePercentageToClipboardButton(percentages: Map<String, Float>): Button {
        val button = "Copy alive percentages to clipboard".toTextButton()
        button.onClick {
            val percentagesString = percentages.map { "${it.key}\t${100 - (100f * it.value).roundToInt()}%" }.joinToString("\n")
            Gdx.app.clipboard.contents = percentagesString
        }
        return button
    }
}