package UI.screens

import logic.GameInfo
import logic.Role
import logic.RoleSet
import QuantumWerewolfGame
import UI.popup.Popup
import UI.util.*
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.unciv.ui.popup.AskTextPopup


class NewGameScreen : PickerScreen() {

    private val optionsTable = GameOptionsColumn()
    private val playerTable = PlayerColumn(this)
    private val rolesTable = RoleColumn(this)

    init {
        scrollPane.setScrollingDisabled(true, true)

        topTable.add("Game Options".toLabel(fontSize = Constants.headingFontSize)).pad(20f, 0f)
        topTable.addSeparatorVertical(Color.BLACK, 1f)
        topTable.add("Select Players".toLabel(fontSize = Constants.headingFontSize)).pad(20f, 0f)
        topTable.addSeparatorVertical(Color.BLACK, 1f)
        topTable.add("Select Roles".toLabel(fontSize = Constants.headingFontSize)).pad(20f, 0f)
        topTable.addSeparator(Color.CLEAR, height = 1f)

        topTable.add(ScrollPane(optionsTable)
            .apply { setOverscroll(false, false) })
            .width(stage.width / 3).top()
        topTable.addSeparatorVertical(Color.CLEAR, 1f)
        topTable.add(ScrollPane(playerTable)
            .apply { setOverscroll(false, false) })
            .width(stage.width / 3).top()
        topTable.addSeparatorVertical(Color.CLEAR, 1f)
        topTable.add(rolesTable)  // No ScrollPane, PlayerPickerTable has its own
            .width(stage.width / 3).top()

        setDefaultCloseAction()

        pick("Start Game")
        rightSideButton.onClick {
            QuantumWerewolfGame.Current.popScreen()
            val roleSet = RoleSet(optionsTable.worldCount, playerTable.players, rolesTable.roles)
            QuantumWerewolfGame.Current.gameInfo = GameInfo(roleSet)
            QuantumWerewolfGame.Current.pushScreen(DayScreen(QuantumWerewolfGame.Current.gameInfo!!))
//            val game = Game()
//            game.players = playerTable.players
//            game.start()
//            game.setScreen<GameScreen>()
        }
    }

    fun tryEnableStartGameButton() {

        @Suppress("SENSELESS_COMPARISON") // Because of the way the game is initialized, these can be null
        if (playerTable == null || rolesTable == null) return
        val enoughPlayers = Constants.minPlayerCount <= playerTable.players.count()
        val enoughRoles = playerTable.players.count() == rolesTable.roles.count()
        setRightSideButtonEnabled(enoughPlayers && enoughRoles)
    }

    fun getPlayers() = playerTable.players
    fun getRoles() = rolesTable.roles
}

private class GameOptionsColumn(): Table() {

    var worldCount = 100

    init {
        pad(10f)
        addWorldSlider()
    }

    private fun addWorldSlider() {
        add("Amount of worlds:".toLabel()).left().expandX()
        val slider = QWSlider(100f, 10000f, 50f, initial = worldCount.toFloat()) {
            worldCount = it.toInt()
        }
        slider.permanentTip = true
        val snapValues = floatArrayOf(100f,250f,500f,1000f,1500f,2500f,5000f,7500f,10000f)
        slider.setSnapToValues(snapValues, 250f)
        add(slider).padTop(10f).row()
    }
}


private class PlayerColumn(private val mainScreen: BaseScreen) : Table() {

    val players = mutableListOf<String>()

    init {
        defaults().pad(10f)
        update()
    }

    /**
     * Updates view of main player table.
     */
    fun update() {
        clear()

        for (player in players) {
            add(getPlayerTable(player)).width(30f).pad(5f).row()
        }
        if (players.size < Constants.maxPlayerCount) {
            val addPlayerButton = "+".toLabel(Color.BLACK, 30)
                .apply { this.setAlignment(Align.center) }
                .surroundWithCircle(50f)
                .onClick {
                    AskTextPopup(
                        mainScreen,
                        label = "Enter player name",
                        validate = { it.isNotBlank() },
                        actionOnOk = { playerName ->
                            players.add(playerName)
                            update()
                        }
                    ).open()
                }
            add(addPlayerButton).pad(10f)
        }
        // Try to enable the start game button if the roleset is valid
        (mainScreen as NewGameScreen).tryEnableStartGameButton()
    }

    /**
     * Creates [Table] for single player containing clickable
     * and "-" remove player button.*
     * @param player for which [Table] is generated
     * @return [Table] containing the all the elements
     */
    private fun getPlayerTable(player: String): Table {
        val playerTable = Table()
        playerTable.pad(5f)
        playerTable.background = BaseScreen.skinStrings.getUiBackground(
            "NewGameScreen/PlayerPickerTable/PlayerTable",
            tintColor = BaseScreen.skinStrings.skinConfig.baseColor.darken(0.8f)
        )

        val playerTypeTextButton = player.toTextButton()
        playerTypeTextButton.onClick {
            AskTextPopup(
                mainScreen,
                label = "Edit player name",
                defaultText = player,
                validate = { it.isNotBlank() && it !in players },
                actionOnOk = { playerName ->
                    players.replaceAll { if (it == player) playerName else it }
                    update()
                }
            ).open()
        }
        add(playerTypeTextButton).pad(5f).fillX()
        add("-".toLabel(Color.BLACK, 30).apply { this.setAlignment(Align.center) }
            .surroundWithCircle(40f)
            .onClick {
                players.remove(player)
                update()
            }).pad(5f).right().row()

        return playerTable
    }
}

private class RoleColumn(private val mainScreen: BaseScreen) : Table() {

    val roles = mutableListOf<Role>()
    val civBlocksWidth = mainScreen.stage.width / 3

    init {
        defaults().pad(10f)
        update()
    }

    /**
     * Updates view of main player table.
     */
    fun update() {
        clear()

        for (role in roles) {
            add(getRoleTable(role)).width(civBlocksWidth).pad(5f).row()
        }
        if (roles.size < Constants.maxPlayerCount) {
            val addRoleButton = "+".toLabel(Color.BLACK, 30)
                .apply { this.setAlignment(Align.center) }
                .surroundWithCircle(50f)
                .onClick {
                    SelectRolesPopup(mainScreen) {
                        roles.add(it)
                        update()
                    }.open()
                }
            add(addRoleButton).pad(10f)
        }
        // Try to enable start game button if there are enough players and roles
        (mainScreen as? NewGameScreen)?.tryEnableStartGameButton()
    }

    /**
     * Creates [Table] for single player containing clickable
     * a "-" remove player button.*
     * @param role for which [Table] is generated
     * @return [Table] containing the all the elements
     */
    private fun getRoleTable(role: Role): Table {
        val roleTable = Table()
//        playerTable.pad(5f)
        roleTable.background = BaseScreen.skinStrings.getUiBackground(
            "NewGameScreen/PlayerPickerTable/PlayerTable",
            tintColor = BaseScreen.skinStrings.skinConfig.baseColor.darken(0.8f)
        )

        val nationTable = getNationTable(role)
        roleTable.add(nationTable).fillX()

        roleTable.add("-".toLabel(Color.BLACK, 30).apply { this.setAlignment(Align.center) }
            .surroundWithCircle(40f)
            .onClick {
                roles.remove(role)
                update()
            }
        ).pad(5f).right()

        return roleTable
    }

    /**
     * Creates clickable icon and nation name for some [Player]
     * as a [Table]. Clicking creates [popupNationPicker] to choose new nation.
     * @param player [Player] for which generated
     * @return [Table] containing nation icon and name
     */
    private fun getNationTable(role: Role): Table {
        val nationTable = Table()
        nationTable.add(role.displayName.toLabel()).pad(5f)
        return nationTable
    }
}

private class SelectRolesPopup(
    previousScreen: BaseScreen,
    actionOnSelect: (role: Role) -> Unit
) : Popup(previousScreen) {
    init {
        innerTable.add("Select a role".toLabel(fontSize = 30)).pad(10f).row()
        innerTable.addSeparator()
        for (role in Role.values()) {
            val selectRoleButton = role.displayName.toTextButton()
            selectRoleButton.onClick {
                actionOnSelect(role)
                close()
            }
            innerTable.add(selectRoleButton).fillX().pad(5f).row()
        }
    }
}




