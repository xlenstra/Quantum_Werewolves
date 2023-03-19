package UI.screens

import logic.GameInfo
import UI.util.TabbedPager
import com.badlogic.gdx.graphics.Color

class DayScreen(gameInfo: GameInfo) : PickerScreen() {
    // 50 normal button height + 2*10 topTable padding + 2 Separator + 2*5 centerTable padding
    // Since a resize recreates this screen this should be fine as a val
    private val centerAreaHeight = stage.height - 82f


    private val tabbedPager = TabbedPager(
        stage.width, stage.width,
        centerAreaHeight, centerAreaHeight,
        separatorColor = Color.WHITE
    )

    init {
        closeButton.isVisible = false

        tabbedPager.setFillParent(true)
        topTable.add(tabbedPager).grow()
        tabbedPager.addPage(
            "Overview",
            PeriodOverviewScreen(gameInfo, this, true)
        )
        tabbedPager.addPage(
            "Percentage",
            PercentageOverview(gameInfo, this)
        )
        tabbedPager.selectPage(0)
    }
}