package UI.popup

import UI.util.*
import UI.util.widgets.AutoScrollPane
import UI.util.widgets.QWStage
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Cell
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.badlogic.gdx.utils.Align

/**
 * Base class for all Popups, i.e. Tables that get rendered in the middle of a screen and on top of everything else
 */
@Suppress("MemberVisibilityCanBePrivate")
open class Popup(
    val stageToShowOn: Stage,
    scrollable: Boolean = true
): Table(BaseScreen.skin) {

    constructor(screen: BaseScreen) : this(screen.stage)

    // This exists to differentiate the actual popup (the inner table)
    // from the 'screen blocking' part of the popup (which covers the entire screen)
    val innerTable = Table(BaseScreen.skin)

    val showListeners = mutableListOf<() -> Unit>()
    val closeListeners = mutableListOf<() -> Unit>()

    init {
        // Set actor name for debugging
        name = javaClass.simpleName

        background = BaseScreen.skinStrings.getUiBackground(
            "General/Popup/Background",
            tintColor = Color.GRAY.cpy().apply { a=.5f })
        innerTable.background = BaseScreen.skinStrings.getUiBackground(
            "General/Popup/InnerTable",
            tintColor = BaseScreen.skinStrings.skinConfig.baseColor.darken(0.5f)
        )

        innerTable.pad(20f)
        innerTable.defaults().pad(5f)

        super.add(if (scrollable) AutoScrollPane(innerTable, BaseScreen.skin) else innerTable)

        this.isVisible = false
        touchable = Touchable.enabled // don't allow clicking behind
        this.setFillParent(true)
    }

    /**
     * Displays the Popup on the screen. If another popup is already open, this one will display after the other has
     * closed. Use [force] = true if you want to open this popup above the other one anyway.
     */
    fun open(force: Boolean = false) {
        stageToShowOn.addActor(this)
        innerTable.pack()
        pack()
        center(stageToShowOn)
        fitContentIntoVisibleArea((stageToShowOn as QWStage).lastKnownVisibleArea)
        if (force || !stageToShowOn.hasOpenPopups()) {
            show()
        }
    }

    private fun fitContentIntoVisibleArea(visibleArea: Rectangle) {
        padLeft(visibleArea.x)
        padBottom(visibleArea.y)
        padRight(stageToShowOn.width - visibleArea.x - visibleArea.width)
        padTop(stageToShowOn.height - visibleArea.y - visibleArea.height)
        invalidate()
    }

    /** Subroutine for [open] handles only visibility */
    private fun show() {
        this.isVisible = true
        for (listener in showListeners) listener()
    }

    /**
     * Close this popup and - if any other popups are pending - display the next one.
     */
    open fun close() {
        for (listener in closeListeners) listener()
        remove()
        val nextPopup = stageToShowOn.actors.firstOrNull { it is Popup }
        if (nextPopup != null) (nextPopup as Popup).show()
    }

    /* All additions to the popup are to the inner table - we shouldn't care that there's an inner table at all */
    final override fun <T : Actor?> add(actor: T): Cell<T> = innerTable.add(actor)
    override fun row(): Cell<Actor> = innerTable.row()
    override fun defaults(): Cell<Actor> = innerTable.defaults()
    fun addSeparator() = innerTable.addSeparator()

    /**
     * Adds a [caption][text] label: A label with word wrap enabled over half the stage width.
     * Will be larger than normal text if the [size] parameter is set to > [Constants.defaultFontSize].
     * @param text The caption text.
     * @param size The font size for the label.
     */
    fun addGoodSizedLabel(text: String, size:Int=Constants.defaultFontSize): Cell<Label> {
        val label = text.toLabel(fontSize = size)
        label.wrap = true
        label.setAlignment(Align.center)
        return add(label).width(stageToShowOn.width / 2)
    }

    /**
     * Adds a [TextButton].
     * @param text The button's caption.
     * @param key Associate a key with this button's action.
     * @param action A lambda to be executed when the button is clicked.
     * @return The new [Cell]
     */
    fun addButton(text: String, style: TextButtonStyle? = null, action: () -> Unit): Cell<TextButton> {
        val button = text.toTextButton(style)
        button.onActivation { action() }
        return add(button)
    }

    /**
     * Adds a [TextButton] that closes the popup, with [BACK][KeyCharAndCode.BACK] already mapped.
     * @param text The button's caption, defaults to "Close".
     * @param additionalKey An additional key that should act like a click.
     * @param action A lambda to be executed after closing the popup when the button is clicked.
     * @return The new [Cell]
     */
    fun addCloseButton(
        text: String = Constants.close,
        style: TextButtonStyle? = null,
        action: (()->Unit)? = null
    ): Cell<TextButton> {
        val cell = addButton(text, style) { close(); if(action!=null) action() }
        return cell
    }

    /**
     * Adds a [TextButton] that can close the popup, with [RETURN][KeyCharAndCode.RETURN] already mapped.
     * @param text The button's caption, defaults to "OK".
     * @param additionalKey An additional key that should act like a click.
     * @param validate Function that should return true when the popup can be closed and `action` can be run.
     * When this function returns false, nothing happens.
     * @param action A lambda to be executed after closing the popup when the button is clicked.
     * @return The new [Cell]
     */
    fun addOKButton(
        text: String = Constants.OK,
        style: TextButtonStyle? = null,
        validate: (() -> Boolean) = { true },
        action: (() -> Unit),
    ): Cell<TextButton> {
        val cell = addButton(text, style) {
            if (validate()) {
                close()
                action()
            }
        }
        return cell
    }

    /**
     * The last two additions ***must*** be buttons.
     * Make their width equal by setting minWidth of one cell to actor width of the other.
     */
    fun equalizeLastTwoButtonWidths() {
        val n = innerTable.cells.size
        if (n < 2) throw UnsupportedOperationException()
        val cell1 = innerTable.cells[n-2]
        val cell2 = innerTable.cells[n-1]
        if (cell1.actor !is Button || cell2.actor !is Button) {
            println("Last two cells are not buttons: ${cell1.actor} and ${cell2.actor}")
            throw UnsupportedOperationException()
        }
        cell1.minWidth(cell2.actor.width).uniformX()
        cell2.minWidth(cell1.actor.width).uniformX()
    }

    /**
     * Reuse this popup as an error/info popup with a new message.
     * Removes everything from the popup to replace it with the message
     * and a close button if requested
     */
    fun reuseWith(newText: String, withCloseButton: Boolean = false) {
        innerTable.clear()
        addGoodSizedLabel(newText)
        if (withCloseButton) {
            row()
            addCloseButton()
        }
    }

    /**
     * Sets or retrieves the [Actor] that currently has keyboard focus.
     *
     * Setting focus on a [TextField] will select all contained text unless a
     * [FocusListener][com.badlogic.gdx.scenes.scene2d.utils.FocusListener] cancels the event.
     */
    var keyboardFocus: Actor?
        get() = stageToShowOn.keyboardFocus
        set(value) {
            if (stageToShowOn.setKeyboardFocus(value))
                (value as? TextField)?.selectAll()
        }
}


/** @return A [List] of currently active or pending [Popup] screens. */
val BaseScreen.popups
    get() = stage.popups
private val Stage.popups: List<Popup>
    get() = actors.filterIsInstance<Popup>()

/** @return The currently active [Popup] or [null] if none. */
val BaseScreen.activePopup: Popup?
    get() = popups.lastOrNull { it.isVisible }

/**
 * Checks if there are visible [Popup]s.
 * @return `true` if any were found.
 */
fun BaseScreen.hasOpenPopups(): Boolean = stage.hasOpenPopups()
private fun Stage.hasOpenPopups(): Boolean = actors.any { it is Popup && it.isVisible }

/**
 * Counts number of visible[Popup]s.
 *
 * Used for key dispatcher precedence.
 */
private fun Stage.countOpenPopups() = actors.count { it is Popup && it.isVisible }

/** Closes all [Popup]s. */
fun BaseScreen.closeAllPopups() = popups.forEach { it.close() }
