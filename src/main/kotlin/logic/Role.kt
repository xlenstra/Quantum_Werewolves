package logic

enum class Team(val displayName: String) {
    VILLAGER("Burger"),
    WEREWOLF("Wolf");
}

enum class Role(val displayName: String) {
    // Team Villager
    GUARDIAN("Beschermengel"),
    HAMSTER_MANIAC("Behamsteraar"),
    VILLAGER("Burger"),
    VILLAGING_VILLAGER("Burgerlijkste Burger"),
    CASANOVA("Casanova"),
    EARLY_BIRD("Earlybird"),
    FRAUDSTER("Frauduer"),
    BLESSED("Gezegende"),
    WITCH("Heks"),
    HUNTER("Jager"),
    APPRENTICE_SEER("Leerlingziener"),
    LYCA("Lycanthroop"),
    OLD_SEER("Oude Ziener"),
    SLUT("Slet"),
    HAMSTER("Weerhamster"),
    SEER("Ziener"),
    
    // Team Wolf
    WEREWOLF("Weerwolf"),
    CURSED("Vervloekte"),
    
    // Other teams or team switchers
    SMALL_WILD_ONE("Kleine Wilde"),
    DEVIL("Duivel"),
    ;
    
    val actions by lazy { mapToActions() }
    val team by lazy {
        when (this) {
            WEREWOLF, CURSED -> Team.WEREWOLF
            else -> Team.VILLAGER
        }
    }

    
    
    private fun mapToActions(): Set<Action> {
        return when (this) {
            GUARDIAN -> setOf(Action.GUARD)
            HAMSTER_MANIAC -> setOf(Action.MAKE_HAMSTER)
            BLESSED -> setOf(Action.BLESSES)
            CASANOVA -> setOf(Action.INVITES)
            WITCH -> setOf(Action.POISONS)
            HUNTER -> setOf(Action.SHOOT)
            OLD_SEER -> setOf(Action.OLDSEE)
            SLUT -> setOf(Action.SLUTS)
            SEER -> setOf(Action.SEE)
            WEREWOLF -> setOf(Action.EAT)
            CURSED -> setOf(Action.CURSES)
            SMALL_WILD_ONE -> setOf(Action.CHOOSE_EXAMPLE)
            DEVIL -> setOf(Action.DEVILS_CHOICE)
            else -> setOf()
        }
    }
    
    fun hasAction(action: Action): Boolean {
        return actions.contains(action)
    }
    
    fun getRoleSeenAs(): Role {
        return when (this) {
            LYCA -> WEREWOLF
            CURSED -> VILLAGER
            else -> this
        }
    }
    
    fun getTeamOldSeenAs(): Team {
        return when (this) {
            LYCA -> Team.WEREWOLF
            CURSED -> Team.VILLAGER
            else -> this.team
        }
    }
    
    fun canBeEaten(): Boolean {
        return this != WEREWOLF
    }
}