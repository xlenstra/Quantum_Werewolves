package UI.skin


import UI.images.ImageGetter
import UI.Log.debug
import UI.saving.fromJsonFile
import UI.saving.json
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle

object SkinCache : HashMap<String, SkinConfig>() {
    private data class SkinAndMod(val skin: String, val mod: String)
    private val allConfigs = HashMap<SkinAndMod, SkinConfig>()

    fun loadSkinConfigs(consoleMode: Boolean = false){
        allConfigs.clear()
        var skinName = ""

        //load internal Skins
        val fileHandles: Sequence<FileHandle> =
            if (consoleMode) FileHandle("jsons/Skins").list().asSequence()
            else ImageGetter.getAvailableSkins().map { Gdx.files.internal("jsons/Skins/$it.json")}.filter { it.exists() }

        for (configFile in fileHandles){
            skinName = configFile.nameWithoutExtension().removeSuffix("Config")
            try {
                val key = SkinAndMod(skinName, "")
                assert(key !in allConfigs)
                allConfigs[key] = json().fromJsonFile(SkinConfig::class.java, configFile)
                debug("SkinConfig loaded successfully: %s", configFile.name())
            } catch (ex: Exception){
                debug("Exception loading SkinConfig '%s':", configFile.path())
                debug("  %s", ex.localizedMessage)
                debug("  (Source file %s line %s)", ex.stackTrace[0].fileName, ex.stackTrace[0].lineNumber)
            }
        }
    }
}
