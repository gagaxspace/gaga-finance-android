package app.gagafinance.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ObligationsModelsTest {
    @Test
    fun parsesStrictDateWithoutThrowing() {
        assertNull(FinanceDates.parseDate("2026-02-30"))
        assertNull(FinanceDates.parseDate("2026/12/01"))
        assertNull(FinanceDates.parseDate("abc"))
        val valid = FinanceDates.parseDate("2026-12-31")
        assertTrue(valid != null)
        assertEquals("2026-12-31", FinanceDates.formatDate(valid!!))
    }

    @Test
    fun repaymentCannotExceedPrincipalAndSettlesLoan() {
        val due = FinanceDates.parseDate("2026-12-31")!!
        val loan = LoanContract(1L, 10L, DebtEventKind.LENT, 50_000L, 0, due, "", due - 86400000)
        assertEquals(50_000L, ObligationMath.loanRemaining(loan, emptyList()))
        val payment = LoanRepayment(1L, 1L, 50_000L, due)
        assertEquals(0L, ObligationMath.loanRemaining(loan, listOf(payment)))
        assertEquals("Settled", ObligationMath.status(loan, listOf(payment), due + 999999))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsOverRepayment() {
        val loan = LoanContract(1L, 1L, DebtEventKind.BORROWED, 100L, 0, 0L, "", 0L)
        ObligationMath.loanRemaining(loan, listOf(LoanRepayment(1L, 1L, 101L, 1L)))
    }

    @Test
    fun billsMarkPaidAndOverdueCorrectly() {
        val now = FinanceDates.parseDate("2026-11-10")!!
        val bill = BillReminder(1L, "Rent", 80_000L, FinanceDates.parseDate("2026-11-01")!!,
            null, "", now)
        assertEquals("Overdue", ObligationMath.billStatus(bill, now))
        assertEquals("Paid", ObligationMath.billStatus(bill.copy(paidAt = now), now))
        assertEquals("Pending", ObligationMath.billStatus(bill.copy(dueAt = FinanceDates.parseDate("2026-11-11")!!), now))
    }

    @Test
    fun simpleInterestIsOnlyEstimate() {
        val loan = LoanContract(1L, 1L, DebtEventKind.LENT,
            365_000L, 1000, 365L * 86_400_000L, "", 0L)
        assertEquals(36_500L, ObligationMath.estimatedInterest(loan))
    }
}
