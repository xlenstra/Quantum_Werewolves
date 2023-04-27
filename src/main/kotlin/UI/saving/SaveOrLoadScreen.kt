package UI.saving

import UI.popup.ConfirmPopup
import UI.util.PickerScreen
import UI.util.*
import UI.util.UncivDateFormat.formatDate
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import launchOnGLThread
import logic.GameInfoPreview
import java.util.*


abstract class LoadOrSaveScreen(
    fileListHeaderText: String? = null
) : PickerScreen(disableScroll = true) {

    abstract fun onExistingSaveSelected(saveGameFile: FileHandle)

    protected var selectedSave = ""
        private set

    private val savesScrollPane = VerticalFileListScrollPane()
    protected val rightSideTable = Table()
    protected val deleteSaveButton = "Delete save".toTextButton(skin.get("negative", TextButton.TextButtonStyle::class.java))
    protected val showAutosavesCheckbox = CheckBox("Show autosaves", skin)

    init {
        savesScrollPane.onChange(::selectExistingSave)

        rightSideTable.defaults().pad(5f, 10f)

        showAutosavesCheckbox.isChecked = false
        showAutosavesCheckbox.onChange {
            updateShownSaves(showAutosavesCheckbox.isChecked)
        }
//        val ctrlA = KeyCharAndCode.ctrl('a')
//        showAutosavesCheckbox.keyShortcuts.add(ctrlA) { showAutosavesCheckbox.toggle() }
//        showAutosavesCheckbox.addTooltip(ctrlA)

        deleteSaveButton.disable()
        deleteSaveButton.onActivation { onDeleteClicked() }
//        deleteSaveButton.keyShortcuts.add(KeyCharAndCode.DEL)
//        deleteSaveButton.addTooltip(KeyCharAndCode.DEL)

        if (fileListHeaderText != null)
            topTable.add(fileListHeaderText.toLabel()).pad(10f).row()

        updateShownSaves(false)

        topTable.add(savesScrollPane)
        topTable.add(rightSideTable)
        topTable.pack()
    }

    open fun resetWindowState() {
        updateShownSaves(showAutosavesCheckbox.isChecked)
        deleteSaveButton.disable()
        descriptionLabel.setText("")
    }

    private fun onDeleteClicked() {
        if (selectedSave.isEmpty()) return
        ConfirmPopup(this, "Are you sure you want to delete this save?", "Delete save") {
            val result = try {
                if (game.files.deleteSave(selectedSave)) {
                    resetWindowState()
                    "$selectedSave deleted successfully."
                } else {
                    "Failed to delete $selectedSave."
                }
            } catch (ex: SecurityException) {
                "Insufficient permissions to delete $selectedSave."
            } catch (ex: Throwable) {
                "Failed to delete $selectedSave."
            }
            descriptionLabel.setText(result)
        }.open()
    }

    private fun updateShownSaves(showAutosaves: Boolean) {
        savesScrollPane.updateSaveGames(game.files, showAutosaves)
    }

    private fun selectExistingSave(saveGameFile: FileHandle) {
        deleteSaveButton.enable()

        selectedSave = saveGameFile.name()
        showSaveInfo(saveGameFile)
        rightSideButton.isVisible = true
        onExistingSaveSelected(saveGameFile)
    }

    private fun showSaveInfo(saveGameFile: FileHandle) {
        descriptionLabel.setText(Constants.loading)
        Concurrency.run("LoadMetaData") { // Even loading the game to get its metadata can take a long time on older phones
            val textToSet = try {
                val savedAt = Date(saveGameFile.lastModified())
                val game: GameInfoPreview = game.files.loadGamePreviewFromFile(saveGameFile)
                
                val startingPlayers = if (game.players.size > 7) game.players.size else game.players.joinToString()
                val livingPlayers = if (game.livingPlayers.size > 7) game.livingPlayers.size else game.livingPlayers.joinToString()

                // Format result for textToSet
                "${saveGameFile.name()}\nSaved at: ${savedAt.formatDate()}\n" +
                    "Staring players: $startingPlayers, of which $livingPlayers live\n" +
                    "${if (game.isDay) "Day" else "Night"} ${game.dayCounter}"
            } catch (ex: Exception) {
                "\nCould not load game!"
            }

            launchOnGLThread {
                descriptionLabel.setText(textToSet)
            }
        }
    }
}
