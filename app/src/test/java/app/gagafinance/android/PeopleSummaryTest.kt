package app.gagafinance.android

import org.junit.Assert.assertEquals
import org.junit.Test

class PeopleSummaryTest {
    @Test
    fun separatesGrossRepaymentsAndOutstandingTotals() {
        val people = listOf(
            LedgerPerson(1L, "A", "", 0L),
            LedgerPerson(2L, "B", "", 0L),
            LedgerPerson(3L, "C", "", 0L)
        )
        val events = listOf(
            DebtEvent(1L, 1L, DebtEventKind.LENT, 300_000L, "", 1L),
            DebtEvent(2L, 1L, DebtEventKind.BORROWED, 500_000L, "", 2L),
            DebtEvent(3L, 1L, DebtEventKind.I_REPAID, 198_000L, "", 3L),
            DebtEvent(4L, 2L, DebtEventKind.LENT, 50_000L, "", 4L),
            DebtEvent(5L, 2L, DebtEventKind.REPAID_TO_ME, 50_000L, "", 5L)
        )
        val totals = PeopleTotalsMath.calculate(people, events)
        assertEquals(3, totals.people)
        assertEquals(1, totals.active)
        assertEquals(350_000L, totals.totalLent)
        assertEquals(500_000L, totals.totalBorrowed)
        assertEquals(50_000L, totals.receivedBack)
        assertEquals(198_000L, totals.paidBack)
        assertEquals(300_000L, totals.lentOutstanding)
        assertEquals(302_000L, totals.borrowedOutstanding)
        assertEquals(-2_000L, totals.netOutstanding)
    }
}
