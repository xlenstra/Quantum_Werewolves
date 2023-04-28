package UI.screens

import UI.images.ImageGetter
import UI.popup.GameScreenMenuPopup
import UI.popup.PickOptionPopup
import UI.saving.LoadGameScreen
import UI.saving.SaveGameScreen
import UI.util.*
import UI.util.widgets.NonScrollablePage
import UI.util.widgets.TabbedPager
import logic.GameInfo
import com.badlogic.gdx.graphics.Color

class GameScreen(val gameInfo: GameInfo) : PickerScreen() {

    // -150f is the height of the picker screen bottom bar
    // -75f is determined empirically
    private val centerAreaHeight = stage.height - 75f - 150f

    private val overviewScreen: PeriodOverviewScreen
    private val percentageOverview: PercentageOverview
    private val actionScreen: ActionScreen
    private val worldScreen: WorldsScreen

    private val tabbedPager = TabbedPager(
        stage.width, stage.width,
        centerAreaHeight, centerAreaHeight,
        separatorColor = Color.WHITE,
    )

    private val menuButton = ImageGetter.getImage("OtherIcons/MenuIcon")

    init {
        closeButton.isVisible = false

        tabbedPager.setFillParent(true)
        topTable.add(tabbedPager).grow()
        overviewScreen = PeriodOverviewScreen(gameInfo, this)
        percentageOverview = PercentageOverview(gameInfo, this)
        actionScreen = ActionScreen(gameInfo, this)
        worldScreen = WorldsScreen(gameInfo, this)

        tabbedPager.addPage("Overview", overviewScreen)
        tabbedPager.addPage("Percentage", NonScrollablePage(percentageOverview.contents))
        tabbedPager.addPage("Actions", NonScrollablePage(actionScreen.contents))
        if (gameInfo.isDay)
            tabbedPager.setPageDisabled("Actions", true)
        tabbedPager.addPage("Worlds", NonScrollablePage(worldScreen.contents))
        tabbedPager.selectPage(0)

        menuButton.onClick {
            GameScreenMenuPopup(this).open()
        }
        tabbedPager.leftHeaderElements.add(menuButton).size(50f).pad(10f)
        
        autoSaveThenEnableButton()
        update()
    }

    private fun update() {
        rightSideButton.disable() // First update the screen before allowing going to the next period
        rightSideButton.clearListeners()
        overviewScreen.update()
        percentageOverview.update()
        actionScreen.update()
        worldScreen.update()
        tabbedPager.setPageDisabled("Actions", gameInfo.isDay)
        if (gameInfo.isDay) {
            rightSideButton.setText("Lynch player and end day")
            rightSideButton.onClick {
                handleRightSideButtonDayClick()
            }
        } else {
            rightSideButton.setText("End night")
            rightSideButton.onClick {
                gameInfo.executeNightActions()
                update()
                tabbedPager.selectPage(0)
            }
        }
        autoSaveThenEnableButton() // Enables the right side button when done
    }

    private fun handleRightSideButtonDayClick() {
        var hunterWasLynched = false
        val lynchPlayerPopup =
            PickOptionPopup(
                this,
                gameInfo.livingPlayers.toList(),
                "Who will be lynched?"
            ) { playerName ->
                hunterWasLynched = gameInfo.lynch(playerName)
                gameInfo.goToNight()
                update()
                tabbedPager.selectPage(0)
            }
        lynchPlayerPopup.closeListeners.add {
            if (hunterWasLynched) {
                PickOptionPopup(
                    this,
                    gameInfo.livingPlayers.toList(),
                    "The Jager has died. Who do they shoot?"
                ) { playerName ->
                    gameInfo.dayHunterKill(playerName)
                    update()
                }.open()
            }
        }
        lynchPlayerPopup.open()
    }

    fun autoSaveThenEnableButton() {
//        waitingForAutoSave = true
        QuantumWerewolfGame.Current.files.requestAutoSave(gameInfo).invokeOnCompletion {
            // only enable the user to next turn once we've saved the current one
            rightSideButton.enable()
        }
    }
}