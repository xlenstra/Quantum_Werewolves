package UI.screens

import UI.popup.PickOptionPopup
import UI.util.*
import UI.util.widgets.HorizontalTabber
import com.badlogic.gdx.scenes.scene2d.ui.Table
import logic.GameInfo

class ActionScreen(private val gameInfo: GameInfo, private val mainScreen: BaseScreen) {

    val contents: HorizontalTabber
    
    init {
        contents = HorizontalTabber(mainScreen, gameInfo.players.toList()) { player, _ ->
            createActionListFor(player)
        }
    }
    
    private fun createActionListFor(player: String): Table {
        val actionTable = Table(BaseScreen.skin)
        actionTable.add(player.toLabel(fontSize=Constants.headingFontSize)).pad(10f).row()
        actionTable.addSeparator().pad(10f)
        actionTable.add("Actions:".toLabel(fontSize=Constants.headingFontSize)).pad(5f).row()
        val roles = gameInfo.worldSet.rolePercentagesOfPlayer(player).keys.sorted()
        for (role in roles) {
            for (action in role.actions) {
                if (!gameInfo.playerCanPreformAction(player, action)) continue
                
                actionTable.add("${role.displayName} $player ${action.displayString.lowercase()}:".toLabel()).pad(5f)
                val target = gameInfo.nightActions.getAction(player, action) ?: "Select"
                val changeTargetButton = target.toTextButton()
                changeTargetButton.onClick {
                    var playerChoices = gameInfo.livingPlayers.toList()
                    if (gameInfo.nightActions.getAction(player, action) != null) {
                        playerChoices = playerChoices.withItem("No One")
                    }
                    PickOptionPopup(mainScreen, playerChoices, "Select target for $action") { target ->
                        if (target == "No One") {
                            gameInfo.nightActions.removeAction(player, action)
                        }
                        else {
                            gameInfo.nightActions.setAction(player, action, target)
                        }
                        update(player)
                    }.open()
                }
                actionTable.add(changeTargetButton).row()
            }
        }
        return actionTable
    }
    
    fun update(defaultSelection: String? = null) {
        contents.update(defaultSelection)
    }
}
