package UI.saving

import UI.popup.ConfirmPopup
import UI.popup.ToastPopup
import UI.util.*
import UI.util.widgets.UncivTextField
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Table
import launchOnGLThread
import logic.GameInfo


class SaveGameScreen(val gameInfo: GameInfo) : LoadOrSaveScreen("Current saves") {
    private val gameNameTextField = UncivTextField.create("Saved game name")

    init {
        setDefaultCloseAction()

        rightSideTable.initRightSideTable()

        rightSideButton.setText("Save game")
        rightSideButton.onActivation {
            if (game.files.getSave(gameNameTextField.text).exists())
                ConfirmPopup(
                    this,
                    "Overwrite existing file?",
                    "Overwrite",
                ) { saveGame() }.open()
            else saveGame()
        }
//        rightSideButton.keyShortcuts.add(KeyCharAndCode.RETURN)
        rightSideButton.enable()

        stage.keyboardFocus = gameNameTextField
    }

    private fun Table.initRightSideTable() {
        addGameNameField()

        val copyJsonButton = "Copy to clipboard".toTextButton()
        copyJsonButton.onActivation { copyToClipboardHandler() }
//        val ctrlC = KeyCharAndCode.ctrl('c')
//        copyJsonButton.keyShortcuts.add(ctrlC)
//        copyJsonButton.addTooltip(ctrlC)
        add(copyJsonButton).row()

//        addSaveToCustomLocation()
        add(deleteSaveButton).row()
        add(showAutosavesCheckbox).row()
    }

    private fun Table.addGameNameField() {
        gameNameTextField.setTextFieldFilter { _, char -> char != '\\' && char != '/' }
        val defaultSaveName = "${gameInfo.gameName} - ${if (gameInfo.isDay) "day" else "night"} ${gameInfo.dayCounter}"
        gameNameTextField.text = defaultSaveName
        gameNameTextField.setSelection(0, defaultSaveName.length)

        add("Saved game name".toLabel()).row()
        add(gameNameTextField).width(300f).row()
        stage.keyboardFocus = gameNameTextField
    }

    private fun copyToClipboardHandler() {
        Concurrency.run("Copy game to clipboard") {
            // the Gzip rarely leads to ANRs
            try {
                Gdx.app.clipboard.contents = QuantumFiles.gameInfoToString(gameInfo, forceZip = true)
            } catch (ex: Throwable) {
                ex.printStackTrace()
                launchOnGLThread {
                    ToastPopup("Could not save game to clipboard!", this@SaveGameScreen)
                }
            }
        }
    }

    private fun Table.addSaveToCustomLocation() {
        if (!game.files.canLoadFromCustomSaveLocation()) return
        val saveToCustomLocation = "Save to custom location".toTextButton()
        val errorLabel = "".toLabel(Color.RED)
        saveToCustomLocation.onClick {
            errorLabel.setText("")
            saveToCustomLocation.setText("Saving...")
            saveToCustomLocation.disable()
            Concurrency.runOnNonDaemonThreadPool("Save to custom location") {
                game.files.saveGameToCustomLocation(gameInfo, gameNameTextField.text) { result ->
                    if (result.isError()) {
                        errorLabel.setText("Could not save game to custom location!")
                        result.exception?.printStackTrace()
                    } else if (result.isSuccessful()) {
                        game.popScreen()
                    }
                    saveToCustomLocation.enable()
                }
            }
        }
        add(saveToCustomLocation).row()
        add(errorLabel).row()
    }

    private fun saveGame() {
        rightSideButton.setText("Saving...")
        Concurrency.runOnNonDaemonThreadPool("SaveGame") {
            game.files.saveGame(gameInfo, gameNameTextField.text) {
                launchOnGLThread {
                    if (it != null) ToastPopup("Could not save game!", this@SaveGameScreen)
                    else QuantumWerewolfGame.Current.popScreen()
                }
            }
        }
    }

    override fun onExistingSaveSelected(saveGameFile: FileHandle) {
        gameNameTextField.text = saveGameFile.name()
    }

}
