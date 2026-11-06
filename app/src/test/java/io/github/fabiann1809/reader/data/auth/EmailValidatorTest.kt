package io.github.fabiann1809.reader.data.auth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmailValidatorTest {

    @Test
    fun usualAddressesAreAccepted() {
        listOf("ana@example.com", "ana.perez@mail.example.co", "ana+libros@example.org", "a1@x-y.io", "  ana@example.com  ")
            .forEach { assertTrue(it, EmailValidator.isValid(it)) }
    }

    @Test
    fun addressesThatCannotWorkAreRejected() {
        listOf(
            "", "ana", "ana@", "@example.com", "ana@example", "ana@.com", "ana@example.", "ana@@example.com",
            "ana perez@example.com", "ana@exa mple.com", "ana..perez@example.com", ".ana@example.com",
            "ana.@example.com", "ana@example.c", "ana@-example.com", "ana@example..com",
        ).forEach { assertFalse(it, EmailValidator.isValid(it)) }
    }

    @Test
    fun anAbsurdlyLongAddressIsRejected() {
        assertFalse(EmailValidator.isValid("a".repeat(250) + "@example.com"))
    }
}
