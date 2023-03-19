package UI.util
// Adapted from Unciv, the civ-V clone, by yairm210

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import java.text.Collator
import java.util.*

data class WindowState (val width: Int = 900, val height: Int = 600)

enum class ScreenSize(val virtualWidth:Float, val virtualHeight:Float){
    Tiny(750f,500f),
    Small(900f,600f),
    Medium(1050f,700f),
    Large(1200f,800f),
    Huge(1500f,1000f),
    Enormous(1920f,1080f)
}


class QWSettings {
    var language: String = "English"
    @Transient
    var locale: Locale? = null
    var screenSize: ScreenSize = ScreenSize.Enormous

    var skin: String = Constants.defaultSkin

    var continuousRendering = false
    var windowState = WindowState()
    var isFreshlyCreated = false

//    var androidCutout: Boolean = false

//    var allowAndroidPortrait = false    // Opt-in to allow Unciv to follow a screen rotation to portrait

    /** Saves the last successful new game's setup */
//    var lastGameSetup: GameSetupInfo? = null

    var fontFamily: String = Fonts.DEFAULT_FONT_FAMILY
    var fontSizeMultiplier: Float = 1f


    fun save() {
        if (!isFreshlyCreated && Gdx.app?.type == Application.ApplicationType.Desktop) {
            windowState = WindowState(Gdx.graphics.width, Gdx.graphics.height)
        }
        QuantumWerewolfGame.Current.files.setGeneralSettings(this)
    }

//    fun updateLocaleFromLanguage() {
//        val bannedCharacters = listOf(' ', '_', '-', '(', ')') // Things not to have in enum names
//        val languageName = language.filterNot { it in bannedCharacters }
//        locale = try {
//            val code = LocaleCode.valueOf(languageName)
//            Locale(code.language, code.country)
//        } catch (e: Exception) {
//            Locale.getDefault()
//        }
//    }

    fun getCurrentLocale(): Locale {
//        if (locale == null)
//            updateLocaleFromLanguage()
        return locale!!
    }
//
    fun getCollatorFromLocale(): Collator {
        return Collator.getInstance(getCurrentLocale())
    }
}
