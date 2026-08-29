package com.viennnaa.utilities.feature.passwordgen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.asJavaRandom

class PasswordGenLogicTest {

    private fun seeded(seed: Int = 1) = kotlin.random.Random(seed).asJavaRandom()

    @Test
    fun `generates the requested length`() {
        for (length in listOf(MIN_LENGTH, 8, 16, 32, MAX_LENGTH)) {
            val password = generatePassword(PasswordSpec(length = length), seeded())!!
            assertEquals(length, password.length)
        }
    }

    @Test
    fun `uses only characters from the chosen sets`() {
        val spec = PasswordSpec(length = 40, sets = setOf(CharacterSet.DIGITS, CharacterSet.LOWERCASE))
        val allowed = (CharacterSet.DIGITS.characters + CharacterSet.LOWERCASE.characters).toSet()
        repeat(50) { seed ->
            val password = generatePassword(spec, seeded(seed))!!
            password.forEach { assertTrue("$it not allowed", it in allowed) }
        }
    }

    @Test
    fun `includes at least one character from every chosen set`() {
        val spec = PasswordSpec(length = 12, sets = CharacterSet.entries.toSet())
        repeat(100) { seed ->
            val password = generatePassword(spec, seeded(seed))!!
            for (set in CharacterSet.entries) {
                assertTrue(
                    "seed $seed produced $password with no ${set.name}",
                    password.any { it in set.characters },
                )
            }
        }
    }

    @Test
    fun `returns null when no character set is chosen`() {
        assertNull(generatePassword(PasswordSpec(sets = emptySet()), seeded()))
    }

    @Test
    fun `a length shorter than the set count still honours the length`() {
        // The user asked for four characters; they get four, not one per set.
        val spec = PasswordSpec(length = MIN_LENGTH, sets = CharacterSet.entries.toSet())
        val password = generatePassword(spec, seeded())!!
        assertEquals(MIN_LENGTH, password.length)
    }

    @Test
    fun `an out of range length is clamped`() {
        assertEquals(MIN_LENGTH, generatePassword(PasswordSpec(length = 1), seeded())!!.length)
        assertEquals(MAX_LENGTH, generatePassword(PasswordSpec(length = 500), seeded())!!.length)
        assertEquals(MIN_LENGTH, clampLength(-4))
        assertEquals(MAX_LENGTH, clampLength(999))
    }

    @Test
    fun `successive passwords differ`() {
        val random = seeded()
        val spec = PasswordSpec(length = 20)
        val generated = (1..50).map { generatePassword(spec, random) }.toSet()
        assertTrue("only ${generated.size} distinct passwords", generated.size > 40)
    }

    @Test
    fun `the guaranteed characters are not always in set order`() {
        // Without the shuffle the first characters would always be lower, upper,
        // digit, symbol in that order.
        val spec = PasswordSpec(length = 16, sets = CharacterSet.entries.toSet())
        val firsts = (1..60).map { generatePassword(spec, seeded(it))!!.first() }.toSet()
        val allLowercase = firsts.all { it in CharacterSet.LOWERCASE.characters }
        assertTrue("first character was always lowercase", !allLowercase)
    }

    @Test
    fun `pool contains every chosen set and nothing else`() {
        val spec = PasswordSpec(sets = setOf(CharacterSet.DIGITS))
        assertEquals(CharacterSet.DIGITS.characters, pool(spec))
    }

    @Test
    fun `entropy rises with length and with more sets`() {
        val short = PasswordSpec(length = 8, sets = setOf(CharacterSet.LOWERCASE))
        val longer = PasswordSpec(length = 16, sets = setOf(CharacterSet.LOWERCASE))
        val wider = PasswordSpec(length = 8, sets = CharacterSet.entries.toSet())
        assertTrue(entropyBits(longer) > entropyBits(short))
        assertTrue(entropyBits(wider) > entropyBits(short))
    }

    @Test
    fun `entropy is zero when nothing is chosen`() {
        assertEquals(0.0, entropyBits(PasswordSpec(sets = emptySet())), 0.0)
    }

    @Test
    fun `strength buckets move in the right direction`() {
        val weak = PasswordSpec(length = 4, sets = setOf(CharacterSet.DIGITS))
        val strong = PasswordSpec(length = 32, sets = CharacterSet.entries.toSet())
        assertEquals(Strength.WEAK, strengthOf(weak))
        assertEquals(Strength.EXCELLENT, strengthOf(strong))
        assertNotNull(strengthOf(PasswordSpec()))
    }

    @Test
    fun `symbols avoid quotes and backslashes`() {
        // Those are the characters most likely to be mangled downstream.
        val risky = listOf('"', '\'', '\\', '`')
        risky.forEach { assertTrue("$it should not be offered", it !in CharacterSet.SYMBOLS.characters) }
    }
}
