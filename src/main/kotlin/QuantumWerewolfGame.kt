// Adapted from Unciv, the civ-V clone, by yairm210

import UI.Log
import UI.debug
import UI.images.ImageGetter
import UI.screens.MainMenuScreen
import UI.skin.SkinCache
import UI.util.*
import UI.popup.ConfirmPopup
import UI.wrapCrashHandlingUnit
import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.utils.Align
import logic.GameInfo
import java.io.PrintWriter
import kotlin.coroutines.cancellation.CancellationException

class QuantumWerewolfGame(val parameters: QWParameters) : Game() {
    var fontImplementation = parameters.fontImplementation
    private val customSaveLocationHelper = parameters.customFileLocationHelper

    var gameInfo: GameInfo? = null
    lateinit var settings: QWSettings
    lateinit var files: QuantumFiles

    /** A wrapped render() method that crashes to [CrashScreen] on a unhandled exception or error. */
    private val wrappedCrashHandlingRender = { super.render() }.wrapCrashHandlingUnit()

    var isInitialized = false

    /** A wrapped render() method that crashes to [CrashScreen] on a unhandled exception or error. */
    // Stored here because I imagine that might be slightly faster than allocating for a new lambda every time, and the render loop is possibly one of the only places where that could have a significant impact.

//    val translations = Translations()

    val screenStack = ArrayDeque<BaseScreen>()

    override fun create() {
        isInitialized = false // this could be on reload, therefore we need to keep setting this to false
        Gdx.input.setCatchKey(Input.Keys.BACK, true)
        Current = this
        files = QuantumFiles(Gdx.files, customSaveLocationHelper)

        // If this takes too long players, especially with older phones, get ANR problems.
        // Whatever needs graphics needs to be done on the main thread,
        // So it's basically a long set of deferred actions.

        /** When we recreate the GL context for whatever reason (say - we moved to a split screen on Android),
         * ALL objects that were related to the old context - need to be recreated.
         * So far we have:
         * - All textures (hence the texture atlas)
         * - SpriteBatch (hence BaseScreen uses a new SpriteBatch for each screen)
         * - Skin (hence BaseScreen.setSkin())
         * - Font (hence Fonts.resetFont() inside setSkin())
         */
        settings = files.getGeneralSettings() // needed for the screen
        setAsRootScreen(GameStartScreen())  // NOT dependent on any atlas or skin

        ImageGetter.resetAtlases()
        ImageGetter.loadAtlasses()

        Gdx.graphics.isContinuousRendering = settings.continuousRendering

        Concurrency.run("LoadJSON") {
            SkinCache.loadSkinConfigs()

            // Loading available fonts can take a long time on Android phones.
            // Therefore we initialize the lazy parameters in the font implementation, while we're in another thread, to avoid ANRs on main thread
            fontImplementation?.getCharPixmap('S')

            // This stuff needs to run on the main thread because it needs the GL context
            launchOnGLThread {
                BaseScreen.setSkin() // needs to come AFTER the Texture reset, since the buttons depend on it and after loadSkinConfigs to be able to use the SkinConfig

//                ImageGetter.ruleset = vanillaRuleset // so that we can enter the map editor without having to load a game first


                setAsRootScreen(MainMenuScreen(this@QuantumWerewolfGame))
//                when {
//                    settings.isFreshlyCreated -> setAsRootScreen(LanguagePickerScreen())
//                    deepLinkedMultiplayerGame == null ->
//                    else -> tryLoadDeepLinkedGame()
//                }

                isInitialized = true
            }
        }
    }

    /**
     * Loads a game, [disposing][BaseScreen.dispose] all screens.
     *
     * Initializes the state of all important modules.
     *
     * Automatically runs on the appropriate thread.
     *
     * Sets the returned `WorldScreen` as the only active screen.
     */
    suspend fun loadGame(newGameInfo: GameInfo)/*: WorldScreen = withThreadPoolContext toplevel@*/{
        val prevGameInfo = gameInfo
        gameInfo = newGameInfo



//        val isLoadingSameGame = worldScreen != null && prevGameInfo != null && prevGameInfo.gameId == newGameInfo.gameId
//        val worldScreenRestoreState = if (isLoadingSameGame) worldScreen!!.getRestoreState() else null
//
//        lateinit var loadingScreen: LoadingScreen

//        withGLContext {
//            // this is not merged with the below GL context block so that our loading screen gets a chance to show - otherwise
//            // we do it all in one swoop on the same thread and the application just "freezes" without loading screen for the duration.
//            loadingScreen = LoadingScreen(getScreen())
//            setScreen(loadingScreen)
//        }

//        return@toplevel withGLContext {
//            for (screen in screenStack) screen.dispose()
//            screenStack.clear()
//
//            worldScreen = null // This allows the GC to collect our old WorldScreen, otherwise we keep two WorldScreens in memory.
//            val newWorldScreen = WorldScreen(newGameInfo, newGameInfo.getPlayerToViewAs(), worldScreenRestoreState)
//            worldScreen = newWorldScreen
//
//            val moreThanOnePlayer = newGameInfo.civilizations.count { it.playerType == PlayerType.Human } > 1
//            val isSingleplayer = !newGameInfo.gameParameters.isOnlineMultiplayer
//            val screenToShow = if (moreThanOnePlayer && isSingleplayer) {
//                PlayerReadyScreen(newWorldScreen)
//            } else {
//                newWorldScreen
//            }
//
//            screenStack.addLast(screenToShow)
//            setScreen(screenToShow)
//            loadingScreen.dispose()
//
//            return@withGLContext newWorldScreen
//        }
    }

    /**
     * @throws UnsupportedOperationException Use pushScreen or replaceCurrentScreen instead
     */
    @Deprecated("Never use this, it's only here because it's part of the gdx.Game interface.", ReplaceWith("pushScreen"))
    override fun setScreen(screen: Screen) {
        throw UnsupportedOperationException("Use pushScreen or replaceCurrentScreen instead")
    }

    override fun getScreen(): BaseScreen? {
        val curScreen = super.getScreen()
        return if (curScreen == null) { null } else { curScreen as BaseScreen }
    }

    private fun setScreen(newScreen: BaseScreen) {
        debug("Setting new screen: %s, screenStack: %s", newScreen, screenStack)
        Gdx.input.inputProcessor = newScreen.stage
        super.setScreen(newScreen) // This can set the screen to the policy picker or tech picker screen, so the input processor must be set before
        Gdx.graphics.requestRendering()
    }

    /** Removes & [disposes][BaseScreen.dispose] all currently active screens in the [screenStack] and sets the given screen as the only screen. */
    private fun setAsRootScreen(root: BaseScreen) {
        for (screen in screenStack) screen.dispose()
        screenStack.clear()
        screenStack.addLast(root)
        setScreen(root)
    }
    /** Adds a screen to be displayed instead of the current screen, with an option to go back to the previous screen by calling [popScreen] */
    fun pushScreen(newScreen: BaseScreen) {
        screenStack.addLast(newScreen)
        setScreen(newScreen)
    }

    /**
     * Pops the currently displayed screen off the screen stack and shows the previous screen.
     *
     * If there is no other screen than the current, will ask the user to quit the game and return null.
     *
     * Automatically [disposes][BaseScreen.dispose] the old screen.
     *
     * @return the new screen
     */
    fun popScreen(): BaseScreen? {
        if (screenStack.size == 1) {
            ConfirmPopup(
                screen = screenStack.last(),
                question = "Do you want to exit the game?",
                confirmText = "Exit",
                action = { Gdx.app.exit() }
            ).open(force = true)
            return null
        }
        val oldScreen = screenStack.removeLast()
        val newScreen = screenStack.last()
        setScreen(newScreen)
        newScreen.resume()
        oldScreen.dispose()
        return newScreen
    }

    /** Replaces the current screen with a new one. Automatically [disposes][BaseScreen.dispose] the old screen. */
    fun replaceCurrentScreen(newScreen: BaseScreen) {
        val oldScreen = screenStack.removeLast()
        screenStack.addLast(newScreen)
        setScreen(newScreen)
        oldScreen.dispose()
    }


    // This is ALWAYS called after create() on Android - google "Android life cycle"
    override fun resume() {
        super.resume()
        if (!isInitialized) return // The stuff from Create() is still happening, so the main screen will load eventually
    }

    override fun pause() {
        val curGameInfo = gameInfo
        super.pause()
    }

    override fun resize(width: Int, height: Int) {
        screen.resize(width, height)
    }

    override fun render() = wrappedCrashHandlingRender()

    override fun dispose() {
        Gdx.input.inputProcessor = null // don't allow ANRs when shutting down, that's silly

        settings.save()
        Concurrency.stopThreadPools()

        // On desktop this should only be this one and "DestroyJavaVM"
        logRunningThreads()

        System.exit(0)
    }

    private fun logRunningThreads() {
        val numThreads = Thread.activeCount()
        val threadList = Array(numThreads) { _ -> Thread() }
        Thread.enumerate(threadList)
        threadList.filter { it !== Thread.currentThread() && it.name != "DestroyJavaVM" }.forEach {
            debug("Thread %s still running in UncivGame.dispose().", it.name)
        }
    }

    /** Handles an uncaught exception or error. First attempts a platform-specific handler, and if that didn't handle the exception or error, brings the game to a [CrashScreen]. */
    fun handleUncaughtThrowable(ex: Throwable) {
        if (ex is CancellationException) {
            return // kotlin coroutines use this for control flow... so we can just ignore them.
        }
        Log.error("Uncaught throwable", ex)
        try {
            PrintWriter(files.fileWriter("lasterror.txt")).use {
                ex.printStackTrace(it)
            }
        } catch (ex: Exception) {
            // ignore
        }
//        if (platformSpecificHelper?.handleUncaughtThrowable(ex) == true) return
//        Gdx.app.postRunnable {
//            setAsRootScreen(CrashScreen(ex))
//        }
    }

//    fun goToMainMenu(): MainMenuScreen {
//        val curGameInfo = gameInfo
//        if (curGameInfo != null) {
//            files.requestAutoSaveUnCloned(curGameInfo) // Can save gameInfo directly because the user can't modify it on the MainMenuScreen
//        }
//        val mainMenuScreen = MainMenuScreen()
//        pushScreen(mainMenuScreen)
//        return mainMenuScreen
//    }

    companion object {
        lateinit var Current: QuantumWerewolfGame
        fun isCurrentInitialized() = this::Current.isInitialized
    }
}

private class GameStartScreen : BaseScreen() {
    init {
        val logoImage = ImageGetter.getExternalImage("banner.png")
        logoImage.center(stage)
        logoImage.setOrigin(Align.center)
        logoImage.color = Color.WHITE.cpy().apply { a = 0f }
        logoImage.addAction(Actions.alpha(1f, 0.3f))
        stage.addActor(logoImage)
    }
}
