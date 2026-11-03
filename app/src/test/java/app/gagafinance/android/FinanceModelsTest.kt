package app.gagafinance.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FinanceModelsTest {
    @Test
    fun amountsUseExactMinorUnits() {
        assertEquals(125L, Money.parseMinorUnits("1.25"))
        assertEquals(100L, Money.parseMinorUnits("1"))
        assertEquals(10L, Money.parseMinorUnits("0.1"))
        assertEquals("৳ 1,234.56", Money.formatBDT(123456L))
    }

    @Test
    fun invalidAmountsCannotBeSaved() {
        for (input in listOf("", "0", "-1", "0.001", "4e5", "abc", "1000000000")) {
            assertNull("Should reject: " + input, Money.parseMinorUnits(input))
        }
    }

    @Test
    fun totalsCountOnlyEntriesInRequestedPeriod() {
        val entries = listOf(
            FinanceEntry(1, EntryKind.INCOME, "Pay", 10050L, 1500L),
            FinanceEntry(2, EntryKind.EXPENSE, "Lunch", 250L, 1600L),
            FinanceEntry(3, EntryKind.EXPENSE, "Old", 300L, 500L)
        )
        assertEquals(Cashflow(10050L, 250L), FinanceMath.totals(entries, 1000L))
    }

    @Test
    fun categoryCatalogSupportsExpectedValues() {
        assertTrue(FinanceCategories.valid("Food"))
        assertTrue(FinanceCategories.valid("Salary"))
        assertTrue(!FinanceCategories.valid("Secrets"))
    }

    @Test
    fun csvNeutralizesSpreadsheetFormulasAndQuotes() {
        val csv = FinanceCsv.export(listOf(
            FinanceEntry(1, EntryKind.EXPENSE, "=1+2, \"test\"", 199L, 0L)
        ))
        assertTrue(csv.contains("\"'=1+2, \"\"test\"\"\""))
        assertTrue(csv.contains("1.99"))
        assertTrue(csv.startsWith("timestamp_utc,type,description,amount_bdt,category"))
    }
}
