package io.github.fabiann1809.reader.data.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordPolicyTest {

    @Test
    fun theWeakPasswordThatStartedItAllIsRejected() {
        val failed = PasswordPolicy.failedRules("123456")

        assertEquals(
            listOf(PasswordRule.MIN_LENGTH, PasswordRule.HAS_UPPERCASE, PasswordRule.HAS_LOWERCASE, PasswordRule.HAS_SYMBOL),
            failed,
        )
    }

    @Test
    fun aGoodPasswordPassesEveryRule() {
        assertTrue(PasswordPolicy.isValid("Lectura#2026"))
        assertTrue(PasswordPolicy.failedRules("Lectura#2026").isEmpty())
    }

    @Test
    fun eightCharactersAreTheMinimum() {
        assertFalse(PasswordPolicy.passes(PasswordRule.MIN_LENGTH, "Ab1#xyz"))
        assertTrue(PasswordPolicy.passes(PasswordRule.MIN_LENGTH, "Ab1#xyzw"))
    }

    @Test
    fun eachMissingKindIsReportedAlone() {
        assertEquals(listOf(PasswordRule.HAS_UPPERCASE), PasswordPolicy.failedRules("lectura#2026"))
        assertEquals(listOf(PasswordRule.HAS_LOWERCASE), PasswordPolicy.failedRules("LECTURA#2026"))
        assertEquals(listOf(PasswordRule.HAS_DIGIT), PasswordPolicy.failedRules("Lectura#abcd"))
        assertEquals(listOf(PasswordRule.HAS_SYMBOL), PasswordPolicy.failedRules("Lectura2026"))
    }

    @Test
    fun accentedLettersCountAsLetters() {
        assertTrue(PasswordPolicy.isValid("Ñandú#2026"))
    }

    @Test
    fun aSpaceIsNotASymbol() {
        assertFalse(PasswordPolicy.passes(PasswordRule.HAS_SYMBOL, "Lectura 2026"))
    }

    @Test
    fun anySymbolCounts() {
        listOf("Lectura@2026", "Lectura_2026", "Lectura-2026", "Lectura.2026", "Lectura€2026")
            .forEach { assertTrue(it, PasswordPolicy.isValid(it)) }
    }

    @Test
    fun anEmptyPasswordFailsEverything() {
        assertEquals(PasswordRule.entries, PasswordPolicy.failedRules(""))
    }
}
