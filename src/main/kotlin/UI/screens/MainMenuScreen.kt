package UI.screens

import UI.images.ImageGetter
import UI.saving.AutoSave
import UI.saving.LoadGameScreen
import UI.util.*
import UI.util.BaseScreen.Companion.skinStrings
import UI.util.widgets.AutoScrollPane
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Table

/** Create one **Main Menu Button** including onClick/key binding
 *  @param text      The text to display on the button
 *  @param icon      The path of the icon to display on the button
 *  @param key       Optional key binding (limited to Char subset of [KeyCharAndCode], which is OK for the main menu)
 *  @param function  logic.Action to invoke when the button is activated
 */
private fun getMenuButton(
    text: String,
    icon: String,
    function: () -> Unit
): Table {
    val table = Table().pad(15f, 30f, 15f, 30f)
    table.background = skinStrings.getUiBackground(
        "MainMenuScreen/MenuButton",
        skinStrings.roundedEdgeRectangleShape,
        skinStrings.skinConfig.baseColor
    )
    table.add(ImageGetter.getImage(icon)).size(50f).padRight(30f)
    table.add(text.toLabel().setFontSize(30)).minWidth(200f)

    table.touchable = Touchable.enabled
    table.onActivation(function)

    table.pack()
    return table
}

class MainMenuScreen() : BaseScreen() {

    init {
        val column1 = Table().apply { defaults().pad(10f).fillX() }
        val column2 = Table().apply { defaults().pad(10f).fillX() }

        if (game.files.autosaveExists() || game.gameInfo != null) {
            val resumeTable = getMenuButton("Resume", "OtherIcons/Resume")
            { resumeGame() }
            column2.add(resumeTable).row()
        }

//        val quickstartTable = getMenuButton("Quickstart", "OtherIcons/Quickstart")
//        { }
//        column1.add(quickstartTable).row()

        val newGameButton = getMenuButton("Start new game", "OtherIcons/New")
        { game.pushScreen(NewGameScreen()) }
        column2.add(newGameButton).row()

        if (game.files.getSaves().any()) {
            val loadGameTable = getMenuButton("Load game", "OtherIcons/Load")
            { game.pushScreen(LoadGameScreen()) }
            column2.add(loadGameTable).row()
        }

//        val optionsTable = getMenuButton("Options", "OtherIcons/Options")
//        {  }
//        column2.add(optionsTable).row()


        val table = Table().apply { defaults().pad(10f) }
        table.add(column1)
        table.add(column2)
        table.pack()

        val scrollPane = AutoScrollPane(table)
        scrollPane.setFillParent(true)
        stage.addActor(scrollPane)
        table.center(scrollPane)

    }

    private fun resumeGame() {
        if (game.gameScreen != null) {
            game.resetToGameScreen()
//            curWorldScreen.popups.filterIsInstance(WorldScreenMenuPopup::class.java).forEach(Popup::close)
        } else {
            AutoSave.autoLoadGame(this)
        }
    }
}