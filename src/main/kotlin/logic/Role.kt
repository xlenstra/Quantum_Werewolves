package logic

enum class Team(val displayName: String) {
    VILLAGER("Burger"),
    WEREWOLF("Wolf");
}

enum class Role(val displayName: String, val team: Team) {
    VILLAGER("Burger", Team.VILLAGER),
    VILLAGING_VILLAGER("Burgerlijkste Burger", Team.VILLAGER),
    EARLY_BIRD("Earlybird", Team.VILLAGER),
    LYCA("Lycanthroop", Team.VILLAGER),
    GUARDIAN("Beschermengel", Team.VILLAGER),
    SLUT("Slet", Team.VILLAGER),
    HAMSTER("Weerhamster", Team.VILLAGER),
    SEER("Ziener", Team.VILLAGER),
    OLD_SEER("Oude Ziener", Team.VILLAGER),
    APPRENTICE_SEER("Leerlingziener", Team.VILLAGER),
    WITCH("Heks", Team.VILLAGER),
    HUNTER("Jager", Team.VILLAGER),
    WEREWOLF("Weerwolf", Team.WEREWOLF),
    SMALL_WILD_ONE("Kleine Wilde", Team.VILLAGER),
    DEVIL("Duivel", Team.VILLAGER),
    ;

    fun mapToActions(): Set<Action> {
        return when (this) {
            WEREWOLF -> setOf(Action.EAT)
            SEER -> setOf(Action.SEE)
            OLD_SEER -> setOf(Action.OLDSEE)
            GUARDIAN -> setOf(Action.GUARD)
            SLUT -> setOf(Action.SLUTS)
            WITCH -> setOf(Action.POISONS)
            HUNTER -> setOf(Action.SHOOT)
            SMALL_WILD_ONE -> setOf(Action.CHOOSE_EXAMPLE)
            DEVIL -> setOf(Action.DEVILS_CHOICE)
            else -> setOf()
        }
    }
    
    fun getRoleSeenAs(): Role {
        return if (this == LYCA) WEREWOLF
        else this
    } 
}