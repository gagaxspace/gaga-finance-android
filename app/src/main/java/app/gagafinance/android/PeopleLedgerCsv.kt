package app.gagafinance.android

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Explicit user-triggered export, not automatic cloud synchronization or backup. */
object PeopleLedgerCsv {
    fun export(people: List<LedgerPerson>, events: List<DebtEvent>): String {
        val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val byPerson = events.groupBy { it.personId }
        return buildString {
            appendLine("person_id,name,nickname_or_note,timestamp_utc,event,amount_bdt,event_note")
            for (person in people.sortedBy { it.id }) {
                val history = byPerson[person.id].orEmpty()
                    .sortedWith(compareBy<DebtEvent> { it.createdAt }.thenBy { it.id })
                if (history.isEmpty()) {
                    appendLine(
                        person.id.toString() + "," + cell(person.name) + "," +
                            cell(person.note) + ",,,,"
                    )
                } else {
                    for (event in history) {
                        append(person.id)
                        append(',')
                        append(cell(person.name))
                        append(',')
                        append(cell(person.note))
                        append(',')
                        append(timestamp.format(Date(event.createdAt)))
                        append(',')
                        append(cell(event.kind.label))
                        append(',')
                        append(Money.amountForCsv(event.minorUnits))
                        append(',')
                        append(cell(event.note))
                        append('\n')
                    }
                }
            }
        }
    }

    /** Quote CSV fields and neutralize formulas when opened in spreadsheet software. */
    private fun cell(raw: String): String {
        val cleaned = raw.replace("\r", " ").replace("\n", " ").trim()
        val safe = if (cleaned.startsWith("=") || cleaned.startsWith("+") ||
            cleaned.startsWith("-") || cleaned.startsWith("@")) {
            "'" + cleaned
        } else cleaned
        return "\"" + safe.replace("\"", "\"\"") + "\""
    }
}
