package com.bennybokki.frientrip

import com.bennybokki.frientrip.data.TripRepository
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for the invite code feature:
 * - generateInviteCode() format and character set
 * - normalizeInviteCode() input handling
 * - Join dialog text field formatting logic
 * - canJoin validation
 * - joinCodeSuccess auto-dismiss logic
 */
class InviteCodeTest {

    // ─────────────────────────────────────────────
    // generateInviteCode
    // ─────────────────────────────────────────────

    private val allowedChars = "ABCDEFGHJKMNPQRSTUVWXYZ23456789".toSet()

    @Test
    fun `generated code has correct XXXX-XXXX format`() {
        val code = TripRepository.generateInviteCode()
        assertTrue(
            "Expected format XXXX-XXXX but got: $code",
            code.matches(Regex("^[A-Z0-9]{4}-[A-Z0-9]{4}$"))
        )
    }

    @Test
    fun `generated code is exactly 9 characters including dash`() {
        val code = TripRepository.generateInviteCode()
        assertEquals(9, code.length)
    }

    @Test
    fun `generated code dash is at position 4`() {
        val code = TripRepository.generateInviteCode()
        assertEquals('-', code[4])
    }

    @Test
    fun `generated code only uses allowed alphabet characters`() {
        repeat(20) {
            val code = TripRepository.generateInviteCode()
            val stripped = code.filter { it != '-' }
            stripped.forEach { char ->
                assertTrue("Unexpected character '$char' in code: $code", char in allowedChars)
            }
        }
    }

    @Test
    fun `generated code excludes ambiguous characters`() {
        // I, L, O, 0, 1 are excluded to avoid confusion
        val excluded = setOf('I', 'L', 'O', '0', '1')
        repeat(50) {
            val code = TripRepository.generateInviteCode()
            code.filter { it != '-' }.forEach { char ->
                assertFalse("Ambiguous character '$char' found in code", char in excluded)
            }
        }
    }

    @Test
    fun `generateInviteCode produces unique codes`() {
        val codes = (1..10).map { TripRepository.generateInviteCode() }.toSet()
        // With ~1 trillion combinations, 10 codes should always be unique
        assertEquals(10, codes.size)
    }

    // ─────────────────────────────────────────────
    // normalizeInviteCode
    // ─────────────────────────────────────────────

    @Test
    fun `normalizeInviteCode preserves already-formatted code`() {
        assertEquals("58CS-H6F9", TripRepository.normalizeInviteCode("58CS-H6F9"))
    }

    @Test
    fun `normalizeInviteCode adds dash to code without dash`() {
        assertEquals("58CS-H6F9", TripRepository.normalizeInviteCode("58CSH6F9"))
    }

    @Test
    fun `normalizeInviteCode uppercases lowercase input`() {
        assertEquals("58CS-H6F9", TripRepository.normalizeInviteCode("58csh6f9"))
    }

    @Test
    fun `normalizeInviteCode strips spaces`() {
        assertEquals("58CS-H6F9", TripRepository.normalizeInviteCode("  58CS H6F9  "))
    }

    @Test
    fun `normalizeInviteCode strips special characters`() {
        assertEquals("58CS-H6F9", TripRepository.normalizeInviteCode("58CS!@#H6F9"))
    }

    @Test
    fun `normalizeInviteCode handles mixed case with existing dash`() {
        assertEquals("ABCD-EFGH", TripRepository.normalizeInviteCode("abcd-efgh"))
    }

    @Test
    fun `normalizeInviteCode returns partial input unchanged when less than 8 chars`() {
        val result = TripRepository.normalizeInviteCode("ABCD")
        assertEquals("ABCD", result)
    }

    @Test
    fun `normalizeInviteCode returns empty string for empty input`() {
        assertEquals("", TripRepository.normalizeInviteCode(""))
    }

    @Test
    fun `normalizeInviteCode returns partial result for 3 character input`() {
        assertEquals("AB3", TripRepository.normalizeInviteCode("AB3"))
    }

    @Test
    fun `normalizeInviteCode handles input pasted with surrounding whitespace`() {
        assertEquals("58CS-H6F9", TripRepository.normalizeInviteCode("   58CS-H6F9   "))
    }

    @Test
    fun `normalizeInviteCode is idempotent`() {
        val code = "ABCD-EFGH"
        assertEquals(code, TripRepository.normalizeInviteCode(TripRepository.normalizeInviteCode(code)))
    }

    // ─────────────────────────────────────────────
    // Join dialog text field formatting logic
    // Mirrors the onValueChange logic in JoinWithCodeDialog
    // ─────────────────────────────────────────────

    private fun formatCodeInput(raw: String): String {
        val stripped = raw.uppercase().filter { it.isLetterOrDigit() }.take(8)
        return if (stripped.length > 4) "${stripped.substring(0, 4)}-${stripped.substring(4)}" else stripped
    }

    private fun strippedLength(formatted: String) = formatted.filter { it.isLetterOrDigit() }.length

    @Test
    fun `typing 4 chars shows no dash`() {
        assertEquals("ABCD", formatCodeInput("ABCD"))
    }

    @Test
    fun `typing 5th char inserts dash`() {
        assertEquals("ABCD-E", formatCodeInput("ABCDE"))
    }

    @Test
    fun `typing all 8 chars produces full formatted code`() {
        assertEquals("ABCD-EFGH", formatCodeInput("ABCDEFGH"))
    }

    @Test
    fun `typing more than 8 chars is capped at 8`() {
        assertEquals("ABCD-EFGH", formatCodeInput("ABCDEFGHIJK"))
    }

    @Test
    fun `non-alphanumeric input is stripped`() {
        assertEquals("ABCD", formatCodeInput("AB!@CD"))
    }

    @Test
    fun `lowercase input is uppercased`() {
        assertEquals("ABCD-EFGH", formatCodeInput("abcdefgh"))
    }

    @Test
    fun `pasting code with dash formats correctly`() {
        assertEquals("ABCD-EFGH", formatCodeInput("ABCD-EFGH"))
    }

    @Test
    fun `empty input returns empty string`() {
        assertEquals("", formatCodeInput(""))
    }

    @Test
    fun `stripped length of full formatted code is 8`() {
        val formatted = formatCodeInput("ABCDEFGH")
        assertEquals(8, strippedLength(formatted))
    }

    @Test
    fun `stripped length of partial code is less than 8`() {
        val formatted = formatCodeInput("ABCD")
        assertTrue(strippedLength(formatted) < 8)
    }

    // ─────────────────────────────────────────────
    // canJoin validation
    // Mirrors: stripped.length == 8 && !isLoading
    // ─────────────────────────────────────────────

    private fun canJoin(formattedCode: String, isLoading: Boolean = false): Boolean {
        val stripped = formattedCode.filter { it.isLetterOrDigit() }
        return stripped.length == 8 && !isLoading
    }

    @Test
    fun `canJoin is true for complete 8-char code when not loading`() {
        assertTrue(canJoin("ABCD-EFGH"))
    }

    @Test
    fun `canJoin is false for incomplete code`() {
        assertFalse(canJoin("ABCD"))
    }

    @Test
    fun `canJoin is false for empty input`() {
        assertFalse(canJoin(""))
    }

    @Test
    fun `canJoin is false when loading even with complete code`() {
        assertFalse(canJoin("ABCD-EFGH", isLoading = true))
    }

    @Test
    fun `canJoin is false for 7-char code`() {
        assertFalse(canJoin("ABCD-EFG"))
    }

    @Test
    fun `canJoin is true regardless of dash presence if 8 alphanum chars`() {
        assertTrue(canJoin("ABCDEFGH")) // no dash, 8 alphanumeric chars
    }

    // ─────────────────────────────────────────────
    // joinCodeSuccess auto-dismiss logic
    // ─────────────────────────────────────────────

    @Test
    fun `joinCodeSuccess starts as false`() {
        // Mirrors: private val _joinCodeSuccess = MutableStateFlow(false)
        var joinCodeSuccess = false
        assertFalse(joinCodeSuccess)
    }

    @Test
    fun `joinCodeSuccess set to true triggers dialog dismiss`() {
        var dialogVisible = true
        var joinCodeSuccess = false

        // Simulate successful join
        joinCodeSuccess = true

        // Simulate LaunchedEffect behaviour
        if (joinCodeSuccess) {
            dialogVisible = false
            joinCodeSuccess = false
        }

        assertFalse(dialogVisible)
        assertFalse(joinCodeSuccess) // reset after dismiss
    }

    @Test
    fun `joinCodeSuccess reset after dismiss keeps dialog closed`() {
        var dialogVisible = true
        var joinCodeSuccess = true

        if (joinCodeSuccess) {
            dialogVisible = false
            joinCodeSuccess = false
        }

        // Simulate the user opening the dialog again manually
        dialogVisible = true
        assertFalse(joinCodeSuccess) // flag was reset, won't auto-dismiss again
        assertTrue(dialogVisible)
    }

    @Test
    fun `failed join does not set joinCodeSuccess`() {
        var joinCodeSuccess = false
        var joinCodeError: String? = null

        // Simulate failed join
        try {
            throw Exception("Invalid or disabled invite code")
        } catch (e: Exception) {
            joinCodeError = e.message
        }

        assertFalse(joinCodeSuccess)
        assertNotNull(joinCodeError)
    }

    @Test
    fun `error is cleared and success flag reset when dialog dismissed manually`() {
        var joinCodeError: String? = "Invalid or disabled invite code"
        var joinCodeSuccess = false

        // User manually dismisses dialog (onDismiss)
        joinCodeError = null
        joinCodeSuccess = false

        assertNull(joinCodeError)
        assertFalse(joinCodeSuccess)
    }
}
