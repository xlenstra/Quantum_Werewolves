package UI.screens

import UI.popup.Popup
import UI.util.*
import logic.GameInfo
import com.badlogic.gdx.graphics.Color

class DayScreen(gameInfo: GameInfo) : PickerScreen() {
    // 50 normal button height + 2*10 topTable padding + 2 Separator + 2*5 centerTable padding
    // Since a resize recreates this screen this should be fine as a val
    private val centerAreaHeight = stage.height - 82f


    private val tabbedPager = TabbedPager(
        stage.width, stage.width,
        centerAreaHeight, centerAreaHeight,
        separatorColor = Color.WHITE
    )

    init {
        closeButton.isVisible = false

        tabbedPager.setFillParent(true)
        topTable.add(tabbedPager).grow()
        tabbedPager.addPage(
            "Overview",
            PeriodOverviewScreen(gameInfo, this, true)
        )
        tabbedPager.addPage(
            "Percentage",
            PercentageOverview(gameInfo, this)
        )
        tabbedPager.selectPage(0)
        
        rightSideButton.setText("Lynch player and End Day")
        rightSideButton.onClick {
            val lynchPlayerPopup = LynchPlayerPopup(this, gameInfo)
            lynchPlayerPopup.open()
        }
        rightSideButton.enable()
    }
}

class LynchPlayerPopup(mainScreen: BaseScreen, private val gameInfo: GameInfo) : Popup(mainScreen) {
    
    init {
        add("Wie wordt gelynchd?".toLabel()).pad(5f).row()
        addSeparator().pad(10f)
        for (playerName in gameInfo.livingPlayers) {
            addLynchPlayerButton(playerName)
        }
        addCloseButton()
    }
    
    private fun addLynchPlayerButton(playerName: String) {
        addButton(playerName) {
            gameInfo.lynchAndGoToNight(playerName)
            QuantumWerewolfGame.Current.popScreen()
            QuantumWerewolfGame.Current.pushScreen(DayScreen(gameInfo))
            close()
        }.fillX().row()
    }
}