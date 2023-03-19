package UI.popup
// Adapted from Unciv, the civ-V clone, by yairm210

import UI.util.BaseScreen
import UI.util.toLabel
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle
import com.badlogic.gdx.utils.Align

/** Variant of [Popup] pre-populated with one label, plus confirm and cancel buttons
 * @param question The text for the label
 * @param confirmText The text for the "Confirm" button
 * @param isConfirmPositive If the action to be performed is positive or not (i.e. buy = positive, delete = negative), default false
 * @param action A lambda to execute when "Yes" is chosen
 * @param screen The parent screen - see [Popup.screen]. Optional, defaults to the current [WorldScreen][com.unciv.ui.worldscreen.WorldScreen]
 * @param restoreDefault A lambda to execute when "No" is chosen
 */
open class ConfirmPopup(
    stageToShowOn: Stage,
    question: String,
    confirmText: String,
    isConfirmPositive: Boolean = false,
    restoreDefault: () -> Unit = {},
    action: () -> Unit
) : Popup(stageToShowOn) {

    constructor(
        screen: BaseScreen,
        question: String,
        confirmText: String,
        isConfirmPositive: Boolean = false,
        restoreDefault: () -> Unit = {},
        action: () -> Unit
    ) : this(screen.stage, question, confirmText, isConfirmPositive, restoreDefault, action)

    /** The [Label][com.badlogic.gdx.scenes.scene2d.ui.Label] created for parameter `question` for optional layout tweaking */
    private val promptLabel = question.toLabel()

    init {
        promptLabel.setAlignment(Align.center)
        add(promptLabel).colspan(2).row()
        addCloseButton("Cancel", action = restoreDefault)
        val confirmStyleName = if (isConfirmPositive) "positive" else "negative"
        val confirmStyle = BaseScreen.skin.get(confirmStyleName, TextButtonStyle::class.java)
        addOKButton(confirmText, confirmStyle, action = action)
        equalizeLastTwoButtonWidths()
    }
}
