package app.gagafinance.android

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupPasswordPolicyTest {
    @Test
    fun requiresAtLeastTwelveCharactersAndBothLettersAndNumbers() {
        assertFalse(BackupPasswordPolicy.valid("short12"))
        assertFalse(BackupPasswordPolicy.valid("onlyletterslong"))
        assertFalse(BackupPasswordPolicy.valid("1234567891234567"))
        assertTrue(BackupPasswordPolicy.valid("MyFinanceBackup2026"))
    }

    @Test
    fun rejectsOverlongBackupPasswords() {
        assertFalse(BackupPasswordPolicy.valid("Ab1" + "x".repeat(128)))
    }
}
