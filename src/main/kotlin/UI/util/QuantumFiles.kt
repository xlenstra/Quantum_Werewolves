package UI.util

import logic.RoleSet
import UI.saving.CustomFileLocationHelper
import UI.saving.Gzip
import UI.saving.fromJsonFile
import UI.saving.json
import com.badlogic.gdx.Files
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.SerializationException
import java.io.File
import java.io.Writer

private const val SAVE_FILES_FOLDER = "SaveFiles"
private const val MULTIPLAYER_FILES_FOLDER = "MultiplayerGames"
private const val AUTOSAVE_FILE_NAME = "Autosave"
const val SETTINGS_FILE_NAME = "GameSettings.json"

class QuantumFiles(
    /**
     * This is necessary because the Android turn check background worker does not hold any reference to the actual [com.badlogic.gdx.Application],
     * which is normally responsible for keeping the [Gdx] static variables from being garbage collected.
     */
    private val files: Files,
    private val customFileLocationHelper: CustomFileLocationHelper? = null,
    private val preferExternalStorage: Boolean = false
) {
    init {
    }
    //region Data


    //endregion
    //region Helpers

    fun getSave(gameName: String): FileHandle {
        return getSave(SAVE_FILES_FOLDER, gameName)
    }
    fun getMultiplayerSave(gameName: String): FileHandle {
        return getSave(MULTIPLAYER_FILES_FOLDER, gameName)
    }

    private fun getSave(saveFolder: String, gameName: String): FileHandle {
        val location = "${saveFolder}/$gameName"
        val localFile = files.local(location)
        val externalFile = files.external(location)

        val toReturn = if (preferExternalStorage && files.isExternalStorageAvailable && (externalFile.exists() || !localFile.exists())) {
            externalFile
        } else {
            localFile
        }

        return toReturn
    }

    /**
     * @throws GdxRuntimeException if the [path] represents a directory
     */
    fun fileWriter(path: String, append: Boolean = false): Writer {
        val file = if (preferExternalStorage && files.isExternalStorageAvailable) {
            files.external(path)
        } else {
            files.local(path)
        }
        return file.writer(append)
    }

    fun getMultiplayerSaves(): Sequence<FileHandle> {
        return getSaves(MULTIPLAYER_FILES_FOLDER)
    }

    fun getSaves(autoSaves: Boolean = true): Sequence<FileHandle> {
        val saves = getSaves(SAVE_FILES_FOLDER)
        val filteredSaves = if (autoSaves) { saves } else { saves.filter { !it.name().startsWith(AUTOSAVE_FILE_NAME) }}
        return filteredSaves
    }

    private fun getSaves(saveFolder: String): Sequence<FileHandle> {
        val localFiles = files.local(saveFolder).list().asSequence()

        val externalFiles = if (files.isExternalStorageAvailable) {
            files.external(saveFolder).list().asSequence()
        } else {
            emptySequence()
        }

        return localFiles + externalFiles
    }

    fun canLoadFromCustomSaveLocation() = customFileLocationHelper != null

    /**
     * @return `true` if successful.
     * @throws SecurityException when delete access was denied
     */
    fun deleteSave(gameName: String): Boolean {
        return deleteSave(getSave(gameName))
    }

    /**
     * @return `true` if successful.
     * @throws SecurityException when delete access was denied
     */
    fun deleteMultiplayerSave(gameName: String): Boolean {
        return deleteSave(getMultiplayerSave(gameName))
    }

    /**
     * Only use this with a [FileHandle] obtained by one of the methods of this class!
     *
     * @return `true` if successful.
     * @throws SecurityException when delete access was denied
     */
    fun deleteSave(file: FileHandle): Boolean {
        return file.delete()
    }

    interface ChooseLocationResult {
        val location: String?
        val exception: Exception?

        fun isCanceled(): Boolean = location == null && exception == null
        fun isError(): Boolean = exception != null
        fun isSuccessful(): Boolean = location != null
    }

    //endregion
    //region Saving

//    fun saveGame(game: logic.GameInfo, GameName: String, saveCompletionCallback: (Exception?) -> Unit = { if (it != null) throw it }): FileHandle {
//        val file = getSave(GameName)
//        saveGame(game, file, saveCompletionCallback)
//        return file
//    }

    /**
     * Only use this with a [FileHandle] obtained by one of the methods of this class!
     */
//    fun saveGame(game: logic.GameInfo, file: FileHandle, saveCompletionCallback: (Exception?) -> Unit = { if (it != null) throw it }) {
//        try {
//            debug("Saving logic.GameInfo %s to %s", game.gameId, file.path())
//            file.writeString(gameInfoToString(game), false)
//            saveCompletionCallback(null)
//        } catch (ex: Exception) {
//            saveCompletionCallback(ex)
//        }
//    }

    /**
     * Overload of function saveGame to save a GameInfoPreview in the MultiplayerGames folder
     */
//    fun saveGame(game: GameInfoPreview, gameName: String, saveCompletionCallback: (Exception?) -> Unit = { if (it != null) throw it }): FileHandle {
//        val file = getMultiplayerSave(gameName)
//        saveGame(game, file, saveCompletionCallback)
//        return file
//    }

    /**
     * Only use this with a [FileHandle] obtained by one of the methods of this class!
     */
//    fun saveGame(game: GameInfoPreview, file: FileHandle, saveCompletionCallback: (Exception?) -> Unit = { if (it != null) throw it }) {
//        try {
//            debug("Saving GameInfoPreview %s to %s", game.gameId, file.path())
//            json().toJson(game, file)
//            saveCompletionCallback(null)
//        } catch (ex: Exception) {
//            saveCompletionCallback(ex)
//        }
//    }

    class CustomSaveResult(
        override val location: String? = null,
        override val exception: Exception? = null
    ) : ChooseLocationResult

    /**
     * [gameName] is a suggested name for the file. If the file has already been saved to or loaded from a custom location,
     * this previous custom location will be used.
     *
     * Calls the [saveCompleteCallback] on the main thread with the save location on success, an [Exception] on error, or both null on cancel.
     */
//    fun saveGameToCustomLocation(game: logic.GameInfo, gameName: String, saveCompletionCallback: (CustomSaveResult) -> Unit) {
//        val saveLocation = game.customSaveLocation ?: Gdx.files.local(gameName).path()
//        val gameData = try {
//            gameInfoToString(game)
//        } catch (ex: Exception) {
//            Concurrency.runOnGLThread { saveCompletionCallback(CustomSaveResult(exception = ex)) }
//            return
//        }
//        debug("Saving logic.GameInfo %s to custom location %s", game.gameId, saveLocation)
//        customFileLocationHelper!!.saveGame(gameData, saveLocation) {
//            if (it.isSuccessful()) {
//                game.customSaveLocation = it.location
//            }
//            saveCompletionCallback(it)
//        }
//    }

    //endregion
    //region Loading

    fun loadGameByName(gameName: String) =
        loadGameFromFile(getSave(gameName))

    fun loadGameFromFile(gameFile: FileHandle): RoleSet {
        val gameData = gameFile.readString()
        if (gameData.isNullOrBlank()) {
            throw emptyFile(gameFile)
        }
        return gameInfoFromString(gameData)
    }


    /**
     * GDX JSON deserialization does not throw when the file is empty, it just returns `null`.
     *
     * Since we work with non-nullable types, we convert this to an actual exception here to have a nice exception message instead of a NPE.
     */
    private fun emptyFile(gameFile: FileHandle): SerializationException {
        return SerializationException("The file for the game ${gameFile.name()} is empty")
    }

    class CustomLoadResult<T>(
        private val locationAndGameData: Pair<String, T>? = null,
        override val exception: Exception? = null
    ) : ChooseLocationResult {
        override val location: String? get() = locationAndGameData?.first
        val gameData: T? get() = locationAndGameData?.second
    }

    /**
     * Calls the [loadCompleteCallback] on the main thread with the [GameInfo] on success or the [Exception] on error or null in both on cancel.
     *
     * The exception may be [IncompatibleGameInfoVersionException] if the [gameData] was created by a version of this game that is incompatible with the current one.
     */
//    fun loadGameFromCustomLocation(loadCompletionCallback: (CustomLoadResult<logic.RoleSet>) -> Unit) {
//        customFileLocationHelper!!.loadGame { result ->
//            val location = result.location
//            val gameData = result.gameData
//            if (location == null || gameData == null) {
//                loadCompletionCallback(CustomLoadResult(exception = result.exception))
//                return@loadGame
//            }
//
//            try {
//                val gameInfo = gameInfoFromString(gameData)
//                gameInfo.customSaveLocation = location
//                loadCompletionCallback(CustomLoadResult(location to gameInfo))
//            } catch (ex: Exception) {
//                loadCompletionCallback(CustomLoadResult(exception = ex))
//            }
//        }
//    }


    //endregion
    //region Settings

    private fun getGeneralSettingsFile(): FileHandle {
        return files.local(SETTINGS_FILE_NAME)
    }

    fun getGeneralSettings(): QWSettings {
        val settingsFile = getGeneralSettingsFile()
        var settings: QWSettings? = null
        if (settingsFile.exists()) {
            try {
                settings = json().fromJson(QWSettings::class.java, settingsFile)
            } catch (ex: Exception) {
                // I'm not sure of the circumstances,
                // but some people were getting null settings, even though the file existed??? Very odd.
                // ...Json broken or otherwise unreadable is the only possible reason.
                println("Error reading settings file;\n\n$ex")
            }
        }

        return settings ?: QWSettings().apply { isFreshlyCreated = true }
    }

    fun setGeneralSettings(gameSettings: QWSettings) {
        getGeneralSettingsFile().writeString(json().toJson(gameSettings), false)
    }

    companion object {

        var saveZipped = false

        /** Specialized function to access settings before Gdx is initialized.
         *
         * @param base Path to the directory where the file should be - if not set, the OS current directory is used (which is "/" on Android)
         */
        fun getSettingsForPlatformLaunchers(base: String = "."): QWSettings {
            // FileHandle is Gdx, but the class and JsonParser are not dependent on app initialization
            // In fact, at this point Gdx.app or Gdx.files are null but this still works.
            val file = FileHandle(base + File.separator + SETTINGS_FILE_NAME)
            return if (file.exists())
                json().fromJsonFile(
                    QWSettings::class.java,
                    file
                )
            else QWSettings().apply { isFreshlyCreated = true }
        }

        /** @throws IncompatibleGameInfoVersionException if the [gameData] was created by a version of this game that is incompatible with the current one. */
        fun gameInfoFromString(gameData: String): RoleSet {
            val unzippedJson = try {
                Gzip.unzip(gameData)
            } catch (ex: Exception) {
                gameData
            }
            val gameInfo = try {
                json().fromJson(RoleSet::class.java, unzippedJson)
            } catch (ex: Exception) {
                println("Exception while deserializing logic.GameInfo JSON")
                throw(ex)
            }
            return gameInfo
        }

        /** Returns gzipped serialization of [game], optionally gzipped ([forceZip] overrides [saveZipped]) */
        fun gameInfoToString(game: RoleSet, forceZip: Boolean? = null): String {
            val plainJson = json().toJson(game)
            return if (forceZip ?: saveZipped) Gzip.zip(plainJson) else plainJson
        }
    }

    //endregion
    //region Autosave

//    /**
//     * Auto-saves a snapshot of the [gameInfo] in a new thread.
//     */
//    fun requestAutoSave(gameInfo: logic.GameInfo): Job {
//        // The save takes a long time (up to a few seconds on large games!) and we can do it while the player continues his game.
//        // On the other hand if we alter the game data while it's being serialized we could get a concurrent modification exception.
//        // So what we do is we clone all the game data and serialize the clone.
//        return requestAutoSaveUnCloned(gameInfo.clone())
//    }
//
//    /**
//     * In a new thread, auto-saves the [gameInfo] directly - only use this with [logic.GameInfo] objects that are guaranteed not to be changed while the autosave is in progress!
//     */
//    fun requestAutoSaveUnCloned(gameInfo: logic.GameInfo): Job {
//        val job = Concurrency.run("autoSaveUnCloned") {
//            autoSave(gameInfo)
//        }
//        autoSaveJob = job
//        return job
//    }
//
//    fun autoSave(gameInfo: logic.GameInfo) {
//        try {
//            saveGame(gameInfo, AUTOSAVE_FILE_NAME)
//        } catch (oom: OutOfMemoryError) {
//            return  // not much we can do here
//        }
//
//        // keep auto-saves for the last 10 turns for debugging purposes
//        val newAutosaveFilename =
//            SAVE_FILES_FOLDER + File.separator + AUTOSAVE_FILE_NAME + "-${gameInfo.currentPlayer}-${gameInfo.turns}"
//        getSave(AUTOSAVE_FILE_NAME).copyTo(files.local(newAutosaveFilename))
//
//        fun getAutosaves(): Sequence<FileHandle> {
//            return getSaves().filter { it.name().startsWith(AUTOSAVE_FILE_NAME) }
//        }
//        while (getAutosaves().count() > 10) {
//            val saveToDelete = getAutosaves().minByOrNull { it.lastModified() }!!
//            deleteSave(saveToDelete.name())
//        }
//    }
//
//    fun loadLatestAutosave(): logic.GameInfo {
//        try {
//            return loadGameByName(AUTOSAVE_FILE_NAME)
//        } catch (ex: Exception) {
//            // silent fail if we can't read the autosave for any reason - try to load the last autosave by turn number first
//            val autosaves = getSaves().filter { it.name() != AUTOSAVE_FILE_NAME && it.name().startsWith(AUTOSAVE_FILE_NAME) }
//            return loadGameFromFile(autosaves.maxByOrNull { it.lastModified() }!!)
//        }
//    }
//
//    fun autosaveExists(): Boolean {
//        return getSave(AUTOSAVE_FILE_NAME).exists()
//    }

    // endregion
}