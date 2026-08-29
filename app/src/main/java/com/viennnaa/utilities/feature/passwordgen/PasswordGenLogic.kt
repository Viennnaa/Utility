package com.viennnaa.utilities.feature.passwordgen

import java.security.SecureRandom
import java.util.Random
import kotlin.math.ln

/**
 * Password generation.
 *
 * The default source is [SecureRandom], not `kotlin.random.Random`: the ordinary
 * generator is a predictable PRNG, and a password made from a predictable
 * sequence is not a secret. Tests pass their own seeded source, which is the only
 * reason the parameter exists.
 */

const val MIN_LENGTH = 4
const val MAX_LENGTH = 64
const val DEFAULT_LENGTH = 16

/** A group of characters a password can draw from. */
enum class CharacterSet(val characters: String) {
    LOWERCASE("abcdefghijklmnopqrstuvwxyz"),
    UPPERCASE("ABCDEFGHIJKLMNOPQRSTUVWXYZ"),
    DIGITS("0123456789"),

    // Deliberately no quotes or backslashes: they are the characters most likely
    // to be mangled by a shell, a CSV export, or a badly written input field.
    SYMBOLS("!#%&()*+,-./:;<=>?@[]^_{|}~"),
}

/** What the user asked for. [sets] empty means nothing can be generated. */
data class PasswordSpec(
    val length: Int = DEFAULT_LENGTH,
    val sets: Set<CharacterSet> = setOf(
        CharacterSet.LOWERCASE,
        CharacterSet.UPPERCASE,
        CharacterSet.DIGITS,
    ),
)

/** Clamps a length coming from a slider or restored state. */
fun clampLength(length: Int): Int = length.coerceIn(MIN_LENGTH, MAX_LENGTH)

/** Every character [spec] allows, in a stable order. */
fun pool(spec: PasswordSpec): String =
    CharacterSet.entries.filter { it in spec.sets }.joinToString("") { it.characters }

/**
 * Generates a password matching [spec], or null when no character set is chosen.
 *
 * Every chosen set is guaranteed to appear at least once — a password that asks
 * for digits and happens to contain none would be rejected by the very form the
 * user is filling in. The guaranteed characters are placed first and then the
 * whole thing is shuffled, so their positions do not leak the set order.
 *
 * If the requested length is shorter than the number of chosen sets, the length
 * wins and some sets will be missing: the user asked for that length explicitly.
 */
fun generatePassword(spec: PasswordSpec, random: Random = SecureRandom()): String? {
    val chosen = CharacterSet.entries.filter { it in spec.sets }
    if (chosen.isEmpty()) return null

    val length = clampLength(spec.length)
    val all = chosen.joinToString("") { it.characters }

    val characters = ArrayList<Char>(length)
    for (set in chosen) {
        if (characters.size == length) break
        characters.add(set.characters[random.nextInt(set.characters.length)])
    }
    while (characters.size < length) {
        characters.add(all[random.nextInt(all.length)])
    }

    // Fisher-Yates, so the guaranteed characters do not sit in a known order.
    for (index in characters.indices.reversed()) {
        val swap = random.nextInt(index + 1)
        val held = characters[index]
        characters[index] = characters[swap]
        characters[swap] = held
    }
    return characters.joinToString("")
}

/**
 * Rough strength in bits: how many yes/no questions an attacker who knows the
 * settings would need. Assumes each position is independent, which is what the
 * generator actually does.
 */
fun entropyBits(spec: PasswordSpec): Double {
    val size = pool(spec).length
    if (size <= 1) return 0.0
    return clampLength(spec.length) * (ln(size.toDouble()) / ln(2.0))
}

/** Coarse buckets for the strength label. */
enum class Strength { WEAK, FAIR, STRONG, EXCELLENT }

fun strengthOf(spec: PasswordSpec): Strength = when {
    entropyBits(spec) < 40 -> Strength.WEAK
    entropyBits(spec) < 60 -> Strength.FAIR
    entropyBits(spec) < 90 -> Strength.STRONG
    else -> Strength.EXCELLENT
}
