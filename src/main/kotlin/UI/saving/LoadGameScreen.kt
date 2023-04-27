package UI.saving

import UI.Log
import UI.popup.Popup
import UI.popup.ToastPopup
import UI.util.*
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.utils.SerializationException
import launchOnGLThread
import java.io.FileNotFoundException

class LoadGameScreen() : LoadOrSaveScreen() {
    private val copySavedGameToClipboardButton = getCopyExistingSaveToClipboardButton()
    private val errorLabel = "".toLabel(Color.RED).apply { isVisible = false }

    companion object {
        private const val loadGame = "Load game"
        private const val loadFromCustomLocation = "Load from custom location"
        private const val loadFromClipboard = "Load copied data"
        private const val copyExistingSaveToClipboard = "Copy saved game to clipboard"

        /** Gets a translated exception message to show to the user.
         * @return The first returned value is the message, the second is signifying if the user can likely fix this problem. */
        fun getLoadExceptionMessage(
            ex: Throwable,
            primaryText: String = "Could not load game!"
        ): Pair<String, Boolean> {
            val errorText = StringBuilder(primaryText)

            val isUserFixable: Boolean
            errorText.appendLine()
            when (ex) {
//                is UncivShowableException -> {
//                    errorText.append("${ex.localizedMessage}")
//                    isUserFixable = true
//                }
                is SerializationException -> {
                    errorText.append("The file data seems to be corrupted.")
                    isUserFixable = false
                }
                is FileNotFoundException -> {
                    if (ex.cause?.message?.contains("Permission denied") == true) {
                        errorText.append("You do not have sufficient permissions to access the file.")
                        isUserFixable = true
                    } else {
                        isUserFixable = false
                    }
                }
                else -> {
                    errorText.append("Unhandled problem, ${ex::class.simpleName} ${ex.localizedMessage}")
                    isUserFixable = false
                }
            }
            return Pair(errorText.toString(), isUserFixable)
        }
    }

    init {
        setDefaultCloseAction()
        rightSideTable.initRightSideTable()
        rightSideButton.onActivation { onLoadGame() }
//        rightSideButton.keyShortcuts.add(KeyCharAndCode.RETURN)
        rightSideButton.isVisible = false
        pickerPane.bottomTable.background = skinStrings.getUiBackground(
            "LoadGameScreen/BottomTable",
            tintColor = skinStrings.skinConfig.clearColor
        )
        pickerPane.topTable.background = skinStrings.getUiBackground(
            "LoadGameScreen/TopTable",
            tintColor = skinStrings.skinConfig.clearColor
        )
    }

    override fun resetWindowState() {
        super.resetWindowState()
        copySavedGameToClipboardButton.disable()
        rightSideButton.setText(loadGame)
        rightSideButton.disable()
    }

    override fun onExistingSaveSelected(saveGameFile: FileHandle) {
        copySavedGameToClipboardButton.enable()
        rightSideButton.isVisible = true
        rightSideButton.setText("Load $selectedSave")
        rightSideButton.enable()
    }

    private fun Table.initRightSideTable() {
        add(getLoadFromClipboardButton()).row()
//        addLoadFromCustomLocationButton()
        add(errorLabel).row()
        add(deleteSaveButton).row()
        add(copySavedGameToClipboardButton).row()
        add(showAutosavesCheckbox).row()
    }

    private fun onLoadGame() {
        if (selectedSave.isEmpty()) return
        val loadingPopup = Popup(this)
        loadingPopup.addGoodSizedLabel(Constants.loading)
        loadingPopup.open()
        Concurrency.run(loadGame) {
            try {
                // This is what can lead to ANRs - reading the file and setting the transients, that's why this is in another thread
                val loadedGame = game.files.loadGameByName(selectedSave)
                game.loadGame(loadedGame)
            } catch (ex: Exception) {
                launchOnGLThread {
                    loadingPopup.close()
                    handleLoadGameException(ex)
                }
            }
        }
    }

    private fun getLoadFromClipboardButton(): TextButton {
        val pasteButton = loadFromClipboard.toTextButton()
        pasteButton.onActivation {
            Concurrency.run(loadFromClipboard) {
                try {
                    val clipboardContentsString = Gdx.app.clipboard.contents.trim()
                    val loadedGame = QuantumFiles.gameInfoFromString(clipboardContentsString)
                    game.loadGame(loadedGame)
                } catch (ex: Exception) {
                    launchOnGLThread {
                        handleLoadGameException(
                            ex,
                            "Could not load game from clipboard!"
                        )
                    }
                }
            }
        }
//        val ctrlV = KeyCharAndCode.ctrl('v')
//        pasteButton.keyShortcuts.add(ctrlV)
//        pasteButton.addTooltip(ctrlV)
        return pasteButton
    }

    private fun Table.addLoadFromCustomLocationButton() {
        if (!game.files.canLoadFromCustomSaveLocation()) return
        val loadFromCustomLocation = loadFromCustomLocation.toTextButton()
        loadFromCustomLocation.onClick {
            errorLabel.isVisible = false
            loadFromCustomLocation.setText(Constants.loading)
            loadFromCustomLocation.disable()
            Concurrency.run(Companion.loadFromCustomLocation) {
                game.files.loadGameFromCustomLocation { result ->
                    if (result.isError()) {
                        handleLoadGameException(
                            result.exception!!,
                            "Could not load game from custom location!"
                        )
                    } else if (result.isSuccessful()) {
                        Concurrency.run {
                            game.loadGame(result.gameData!!)
                        }
                    }
                }
            }
        }
        add(loadFromCustomLocation).row()
    }

    private fun getCopyExistingSaveToClipboardButton(): TextButton {
        val copyButton = copyExistingSaveToClipboard.toTextButton()
        copyButton.onActivation {
            Concurrency.run(copyExistingSaveToClipboard) {
                try {
                    val gameText = game.files.getSave(selectedSave).readString()
                    Gdx.app.clipboard.contents =
                        if (gameText[0] == '{') Gzip.zip(gameText) else gameText
                } catch (ex: Throwable) {
                    ex.printStackTrace()
                    ToastPopup("Could not save game to clipboard!", this@LoadGameScreen)
                }
            }
        }
        copyButton.disable()
//        val ctrlC = KeyCharAndCode.ctrl('c')
//        copyButton.keyShortcuts.add(ctrlC)
//        copyButton.addTooltip(ctrlC)
        return copyButton
    }

    private fun handleLoadGameException(
        ex: Exception,
        primaryText: String = "Could not load game!"
    ) {
        Log.error("Error while loading game", ex)
        val (errorText, isUserFixable) = getLoadExceptionMessage(ex, primaryText)

        if (!isUserFixable) {
            val cantLoadGamePopup = Popup(this@LoadGameScreen)
            cantLoadGamePopup.addGoodSizedLabel("It looks like your saved game can't be loaded!")
                .row()
            cantLoadGamePopup.addGoodSizedLabel("If you could copy your game data (\"Copy saved game to clipboard\" - ")
                .row()
            cantLoadGamePopup.addGoodSizedLabel("  paste into an email to yairm210@hotmail.com)")
                .row()
            cantLoadGamePopup.addGoodSizedLabel("I could maybe help you figure out what went wrong, since this isn't supposed to happen!")
                .row()
            cantLoadGamePopup.addCloseButton()
            cantLoadGamePopup.open()
        }

        Concurrency.runOnGLThread {
            errorLabel.setText(errorText)
            errorLabel.isVisible = true
        }
    }
}
