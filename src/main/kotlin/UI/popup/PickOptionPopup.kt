package UI.popup

import UI.util.BaseScreen
import UI.util.toLabel

class PickOptionPopup(
    mainScreen: BaseScreen,
    optionList: List<String>,
    title: String,
    private val onOptionSelect: (String) -> Unit
) : Popup(mainScreen) {

    init {
        add(title.toLabel()).pad(5f).row()
        addSeparator().pad(10f)
        for (option in optionList) {
            addSelectPlayerButton(option)
        }
        addCloseButton()
    }

    private fun addSelectPlayerButton(playerName: String) {
        addButton(playerName) {
            onOptionSelect(playerName)
            close()
        }.fillX().row()
    }
}