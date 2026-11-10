package app.gagafinance.android

/** Local notebook data only; never a bank-confirmed debt or money transfer. */
data class LedgerPerson(
    val id: Long,
    val name: String,
    val note: String,
    val createdAt: Long,
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val relation: String = ""
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


/** Gross activity and outstanding balances are displayed separately. */
data class PeopleTotals(
    val people: Int,
    val active: Int,
    val totalLent: Long,
    val totalBorrowed: Long,
    val receivedBack: Long,
    val paidBack: Long,
    val lentOutstanding: Long,
    val borrowedOutstanding: Long
) {
    val netOutstanding: Long get() = Math.subtractExact(lentOutstanding, borrowedOutstanding)
}

object PeopleTotalsMath {
    fun calculate(people: List<LedgerPerson>, events: List<DebtEvent>): PeopleTotals {
        var totalLent = 0L
        var totalBorrowed = 0L
        var receivedBack = 0L
        var paidBack = 0L
        for (event in events) {
            when (event.kind) {
                DebtEventKind.LENT -> totalLent = Math.addExact(totalLent, event.minorUnits)
                DebtEventKind.BORROWED -> totalBorrowed = Math.addExact(totalBorrowed, event.minorUnits)
                DebtEventKind.REPAID_TO_ME -> receivedBack = Math.addExact(receivedBack, event.minorUnits)
                DebtEventKind.I_REPAID -> paidBack = Math.addExact(paidBack, event.minorUnits)
            }
        }
        val balanceByPerson = events.groupBy { it.personId }
        val active = people.count {
            val balance = DebtMath.balance(balanceByPerson[it.id].orEmpty())
            balance.lentOutstanding > 0L || balance.borrowedOutstanding > 0L
        }
        val netLent = Math.subtractExact(totalLent, receivedBack)
        val netBorrowed = Math.subtractExact(totalBorrowed, paidBack)
        return PeopleTotals(
            people.size, active, totalLent, totalBorrowed,
            receivedBack, paidBack, netLent, netBorrowed
        )
    }
}
