package UI.util.widgets

import UI.util.BaseScreen
import UI.util.addSeparatorVertical
import UI.util.onClick
import UI.util.toTextButton
import UI.util.widgets.AutoScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Table

open class HorizontalTabber(
    private val mainScreen: BaseScreen,
    var tabs: List<String>,
    private val contentGenerator: (String, Float) -> Table,
) : Table() {
    
    private val optionTable = Table()
    private val informationTable = Table()
    private val informationTableWidth = mainScreen.stage.width/3f*2f-2f
    
    init {
        update()
    }
    
    fun update(pickOption: String? = null) {
        clear()
        optionTable.clear()
        informationTable.clear()
        add(AutoScrollPane(optionTable)).width(mainScreen.stage.width/3f-2f).padBottom(10f).padTop(10f).growY()
        addSeparatorVertical()
        add((informationTable)).width(informationTableWidth).padBottom(10f).padTop(10f).growY().row()
        if (pickOption != null) {
            setInformationTable(pickOption)
        }
        setOptionTable()
    }

    private fun setOptionTable() {
        for (tab in tabs) {
            val tabButton = tab.toTextButton()
            tabButton.onClick {
                setInformationTable(tab)
            }
            optionTable.add(tabButton).pad(10f).row()
        }
    }
    
    private fun setInformationTable(option: String) {
        informationTable.clear()
        informationTable.add(AutoScrollPane(contentGenerator(option, informationTableWidth))).top().grow()
    }
}