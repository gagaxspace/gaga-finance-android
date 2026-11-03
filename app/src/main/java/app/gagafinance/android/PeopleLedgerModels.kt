package app.gagafinance.android

/** Local notebook data only; never a bank-confirmed debt or money transfer. */
data class LedgerPerson(
    val id: Long,
    val name: String,
    val note: String,
    val createdAt: Long
)

enum class DebtEventKind(val label: String) {
    LENT("I lent"),
    BORROWED("I borrowed"),
    REPAID_TO_ME("Received repayment"),
    I_REPAID("I repaid")
}

data class DebtEvent(
    val id: Long,
    val personId: Long,
    val kind: DebtEventKind,
    val minorUnits: Long,
    val note: String,
    val createdAt: Long
)

data class DebtBalance(
    val lentOutstanding: Long = 0L,
    val borrowedOutstanding: Long = 0L,
    val activityCount: Int = 0
) {
    val status: String
        get() = when {
            activityCount == 0 -> "No activity"
            lentOutstanding == 0L && borrowedOutstanding == 0L -> "Settled"
            else -> "Active"
        }
}

object DebtMath {
    fun balance(events: List<DebtEvent>): DebtBalance {
        var lent = 0L
        var borrowed = 0L
        // Process in ledger order, including records that share the same millisecond.
        for (event in events.sortedWith(compareBy<DebtEvent> { it.createdAt }.thenBy { it.id })) {
            require(event.minorUnits > 0L) { "Amount must be positive" }
            when (event.kind) {
                DebtEventKind.LENT -> lent = Math.addExact(lent, event.minorUnits)
                DebtEventKind.BORROWED -> borrowed = Math.addExact(borrowed, event.minorUnits)
                DebtEventKind.REPAID_TO_ME -> {
                    require(event.minorUnits <= lent) { "Repayment exceeds amount lent" }
                    lent = Math.subtractExact(lent, event.minorUnits)
                }
                DebtEventKind.I_REPAID -> {
                    require(event.minorUnits <= borrowed) { "Repayment exceeds amount borrowed" }
                    borrowed = Math.subtractExact(borrowed, event.minorUnits)
                }
            }
        }
        return DebtBalance(lent, borrowed, events.size)
    }

    fun canAdd(current: DebtBalance, kind: DebtEventKind, amount: Long): Boolean =
        amount > 0L && when (kind) {
            DebtEventKind.LENT, DebtEventKind.BORROWED -> true
            DebtEventKind.REPAID_TO_ME -> amount <= current.lentOutstanding
            DebtEventKind.I_REPAID -> amount <= current.borrowedOutstanding
        }

    fun total(peopleBalances: Collection<DebtBalance>): DebtBalance =
        peopleBalances.fold(DebtBalance()) { sum, balance ->
            DebtBalance(
                lentOutstanding = Math.addExact(sum.lentOutstanding, balance.lentOutstanding),
                borrowedOutstanding = Math.addExact(sum.borrowedOutstanding, balance.borrowedOutstanding),
                activityCount = Math.addExact(sum.activityCount, balance.activityCount)
            )
        }
}
