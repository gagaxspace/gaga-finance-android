package app.gagafinance.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PeopleLedgerModelsTest {
    private fun event(
        id: Long, person: Long, kind: DebtEventKind,
        amount: Long, time: Long = id
    ) = DebtEvent(id, person, kind, amount, "", time)

    @Test
    fun tracksLentBorrowedAndPartialRepaymentsIndependently() {
        val events = listOf(
            event(1, 10, DebtEventKind.LENT, 300_000L),
            event(2, 10, DebtEventKind.BORROWED, 500_000L),
            event(3, 10, DebtEventKind.REPAID_TO_ME, 100_000L),
            event(4, 10, DebtEventKind.I_REPAID, 200_000L)
        )
        assertEquals(DebtBalance(200_000L, 300_000L, 4), DebtMath.balance(events))
        assertEquals("৳ 2,000.00", Money.formatBDT(DebtMath.balance(events).lentOutstanding))
        assertEquals("Active", DebtMath.balance(events).status)
    }

    @Test
    fun fullyRepaidPersonIsSettledAndEmptyPersonHasNoActivity() {
        assertEquals("No activity", DebtMath.balance(emptyList()).status)
        val events = listOf(
            event(1, 1, DebtEventKind.LENT, 350L),
            event(2, 1, DebtEventKind.REPAID_TO_ME, 350L)
        )
        assertEquals(DebtBalance(0L, 0L, 2), DebtMath.balance(events))
        assertEquals("Settled", DebtMath.balance(events).status)
    }

    @Test
    fun repaymentsCannotExceedCorrectOutstandingSide() {
        val balance = DebtBalance(lentOutstanding = 300L, borrowedOutstanding = 500L)
        assertTrue(DebtMath.canAdd(balance, DebtEventKind.REPAID_TO_ME, 300L))
        assertFalse(DebtMath.canAdd(balance, DebtEventKind.REPAID_TO_ME, 301L))
        assertTrue(DebtMath.canAdd(balance, DebtEventKind.I_REPAID, 500L))
        assertFalse(DebtMath.canAdd(balance, DebtEventKind.I_REPAID, 501L))
        assertFalse(DebtMath.canAdd(balance, DebtEventKind.LENT, 0L))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsRepaymentBeforeLoanEvenWhenInputIsOutOfOrder() {
        DebtMath.balance(listOf(
            event(2, 1, DebtEventKind.LENT, 100L, 200L),
            event(1, 1, DebtEventKind.REPAID_TO_ME, 50L, 100L)
        ))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsOverpaymentInLedgerHistory() {
        DebtMath.balance(listOf(
            event(1, 1, DebtEventKind.BORROWED, 100L),
            event(2, 1, DebtEventKind.I_REPAID, 101L)
        ))
    }

    @Test
    fun peopleLedgerExportIncludesEmptyPeopleAndGuardsFormulas() {
        val csv = PeopleLedgerCsv.export(
            listOf(
                LedgerPerson(1, "=HYPERLINK(1)", "Friend", 0L),
                LedgerPerson(2, "Person with no entries", "", 0L)
            ),
            listOf(
                DebtEvent(7, 1, DebtEventKind.LENT, 300_000L, "@formula", 1_000L)
            )
        )
        assertTrue(csv.startsWith("person_id,name,nickname_or_note,timestamp_utc,event,amount_bdt,event_note"))
        assertTrue(csv.contains("\"'=HYPERLINK(1)\""))
        assertTrue(csv.contains("\"'@formula\""))
        assertTrue(csv.contains(",3000.00,"))
        assertTrue(csv.contains("Person with no entries"))
    }

    @Test
    fun aggregatesMultiplePeopleWithoutCombiningOppositeDebts() {
        val totals = DebtMath.total(
            listOf(
                DebtBalance(300_000L, 500_000L, 2),
                DebtBalance(300_000L, 0L, 1)
            )
        )
        assertEquals(600_000L, totals.lentOutstanding)
        assertEquals(500_000L, totals.borrowedOutstanding)
    }
}
