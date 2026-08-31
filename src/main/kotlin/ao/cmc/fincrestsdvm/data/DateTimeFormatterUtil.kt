package ao.cmc.fincrestsdvm.data

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale


object DateTimeFormatterUtil{
    // Portugues de Angola
    private val localAO: Locale = Locale.of("pt", "AO")


    //Ex: 26 ago 2026, 19:53
    private val displayFormatter = DateTimeFormatter
        .ofPattern("dd MM yyyy, HH:mm", localAO)
        .withZone(ZoneId.systemDefault())


    // Ex: 26 de agosto de 2026
    private val longDateFormatter = DateTimeFormatter
        .ofPattern("dd 'de' MMMM 'de' yyyy", localAO)
        .withZone(ZoneId.systemDefault())

    fun formatIsoToDisplay(isoString: String): String {
        return try{
            val instant = Instant.parse(isoString)
            displayFormatter.format(instant)
        } catch (e: Exception) {
           isoString
        }
    }

    fun formatIsoToLongDate(isoString: String): String{
        return try{
            val instant = Instant.parse(isoString)
            longDateFormatter.format(instant)
        } catch (e: Exception) {
            isoString
        }
    }
}