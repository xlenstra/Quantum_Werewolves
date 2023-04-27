package UI.popup

import UI.saving.LoadGameScreen
import UI.saving.SaveGameScreen
import UI.screens.GameScreen
import UI.screens.NewGameScreen
import logic.GameSetupInfo

class GameScreenMenuPopup(gameScreen: GameScreen) : Popup(gameScreen) {

    init {
        defaults().fillX()
        val game = gameScreen.game

        addButton("Main menu") {
            game.goToMainMenu()
        }.row()
        addButton("Load game") {
            close()
            game.pushScreen(LoadGameScreen())
        }.row()
        addButton("Save game") {
            close()
            game.pushScreen(SaveGameScreen(gameScreen.gameInfo))
        }.row()

        addButton("Start new game") {
            close()
            val newGameSetupInfo = GameSetupInfo(gameScreen.gameInfo)
            game.pushScreen(NewGameScreen(newGameSetupInfo))
        }.row()
        addCloseButton()
        pack()
    }
}