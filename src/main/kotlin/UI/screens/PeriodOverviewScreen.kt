package UI.screens

import UI.util.*
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Table
import logic.GameInfo
import logic.Role
import kotlin.math.roundToInt

class PeriodOverviewScreen(private val gameInfo: GameInfo, mainScreen: BaseScreen) : Table() {

    init {
        update()
    }

    fun update() {
        clear()
        add("Period Overview Screen".toLabel(fontSize = Constants.headingFontSize)).pad(5f).padTop(10f).row()
        val dayNightString = if (gameInfo.isDay) "Day" else "Night"
        add("$dayNightString ${gameInfo.dayCounter}".toLabel()).pad(5f).row()
        add("Worlds left: ${gameInfo.worldSet.possibleWorlds.size}".toLabel()).pad(5f).row()

        addSeparator().pad(10f)

        add("Living Players".toLabel(fontSize = Constants.headingFontSize)).pad(5f).row()
        add(gameInfo.livingPlayers.joinToString(", ").toLabel()).pad(5f).row()

        val deadPlayers = gameInfo.players.filter { it !in gameInfo.livingPlayers }
        if (deadPlayers.any()) {
            add("Dead Players".toLabel(fontSize = Constants.headingFontSize)).pad(5f).row()
            add(deadPlayers.joinToString(", ").toLabel()).pad(5f).row()
        }

        add(getCopyAlivePercentageToClipboardButton(
            gameInfo.worldSet.deathPercentageOfAllPlayers(gameInfo.players.toList())
        )).pad(10f).row()

        add(getCopyAllPercentagesToClipboardButton(
            gameInfo.worldSet.rolePercentagesOfAllPlayers(gameInfo.players.toList())
        )).pad(10f).row()

        if (gameInfo.nextPeriodInfo.isNotEmpty()) {
            addSeparator().pad(10f)
            add("Info from previous period".toLabel(fontSize = Constants.headingFontSize)).pad(5f)
                .row()
            for (line in gameInfo.nextPeriodInfo) {
                add(line.toLabel()).pad(5f).row()
            }
        }
    }

    private fun getCopyAlivePercentageToClipboardButton(percentages: Map<String, Float>): Button {
        val button = "Copy alive percentages to clipboard".toTextButton()
        button.onClick {
            val percentagesString =
                percentages.map { "${it.key}\t${100 - (100f * it.value).roundToInt()}%" }.joinToString("\n")
            
            Gdx.app.clipboard.contents = percentagesString
        }
        return button
    }

    private fun getCopyAllPercentagesToClipboardButton(percentages: Map<String, Map<Role, Float>>): Button {
        val button = "Copy all role percentages to clipboard".toTextButton()
        button.onClick {
            val rolesList = gameInfo.worldSet.possibleWorlds.first().roleSet.values.map { it.toRole() }.sorted().distinct()
            val rolesString = "\t${rolesList.joinToString("\t") { it.displayName }}"
            val percentageStrings = mutableListOf<String>()
            for ((player, playerPercentages) in percentages) {
                percentageStrings.add(
                    "$player\t${rolesList.joinToString("\t") { 
                        role -> "${(100 * (playerPercentages[role] ?: 0f)).roundToInt()}%" 
                    }}"
                )
            }
            Gdx.app.clipboard.contents = "$rolesString\n${percentageStrings.joinToString("\n")}"
        }
        return button
    }
}