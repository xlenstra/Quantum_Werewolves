package UI.util

import logic.Action
import logic.Role
import logic.Team
import java.text.SimpleDateFormat
import java.util.*


/**
 * Standardize date formatting so dates are presented in a consistent style and all decisions
 * to change date handling are encapsulated here
 */
object UncivDateFormat {
    private val standardFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

    /** Format a date to ISO format with minutes */
    fun Date.formatDate(): String = standardFormat.format(this)
    // Previously also used:
    //val updateString = "{Updated}: " +DateFormat.getDateInstance(DateFormat.SHORT).format(date)

    // Everything under java.time is from Java 8 onwards, meaning older phones that use Java 7 won't be able to handle it :/
    // So we're forced to use ancient Java 6 classes instead of the newer and nicer LocalDateTime.parse :(
    // Direct solution from https://stackoverflow.com/questions/2201925/converting-iso-8601-compliant-string-to-java-util-date

    @Suppress("SpellCheckingInspection")
    private val utcFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)

    /** Parse an UTC date as passed by online API's
     * example: `"2021-04-11T14:43:33Z".parseDate()`
     */
    fun String.parseDate(): Date = utcFormat.parse(this)
}

fun String.toRole() = Role.valueOf(this)
fun String.toTeam() = Team.valueOf(this)
fun String.toAction() = Action.valueOf(this)