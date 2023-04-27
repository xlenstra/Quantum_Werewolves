package UI.screens

import UI.util.*
import UI.util.widgets.HorizontalTabber
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.ui.Button
import logic.GameInfo
import com.badlogic.gdx.scenes.scene2d.ui.Table
import logic.Role
import kotlin.math.roundToInt

class PercentageOverview(private val gameInfo: GameInfo, private val mainScreen: BaseScreen) {

    val contents: HorizontalTabber
    
    init {
        contents = HorizontalTabber(mainScreen, gameInfo.players.toList()) { player, _ ->
            getInformationForPlayer(player)
        }
    }
    
    private fun getInformationForPlayer(player: String): Table {
        val informationTable = Table()
        informationTable.add(player.toLabel(fontSize=Constants.headingFontSize)).pad(10f).row()
        informationTable.add("${100f-100f*gameInfo.roleSet.deathPercentageOfPlayer(player)}% alive".toLabel()).pad(5f).row()
        informationTable.addSeparator().pad(10f)
        informationTable.add("Percentages:".toLabel(fontSize=Constants.headingFontSize)).pad(5f).row()
        val percentages = gameInfo.roleSet.rolePercentagesOfPlayer(player)
        for (role in percentages.keys.sorted()) {
            informationTable.add("${role.displayName}: ${100f*percentages[role]!!}%".toLabel()).pad(5f).row()
        }
        informationTable.add(copyPercentagesToClipboard(percentages)).pad(10f).row()
        return informationTable
    }
    
    private fun copyPercentagesToClipboard(percentages: Map<Role, Float>): Button {
        val copyButton = "Copy role percentages to clipboard".toTextButton()
        copyButton.onClick {
            val percentagesString = percentages.map { "${it.key.displayName}\t${(100f * it.value).roundToInt()}%" }.joinToString("\n")
            Gdx.app.clipboard.contents = percentagesString
        }
        return copyButton
    }

    fun update() {
        contents.update()
    }
}