import kotlinx.serialization.Serializable



enum class Actions {
    EAT,
    SEE,
    LYNCH,
}

@Serializable
data class Action(
    val performer: String? = null, // Who preforms the action?
    val action: Actions, // What action?
    val target: String? = null, // Who is the target of the action?
    val targetRole: Role? = null // What role does the target have?
)