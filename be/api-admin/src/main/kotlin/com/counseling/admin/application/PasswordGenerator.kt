package com.counseling.admin.application

import java.security.SecureRandom

object PasswordGenerator {
    private val UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private val LOWER = "abcdefghijklmnopqrstuvwxyz"
    private val DIGITS = "0123456789"
    private val SPECIAL = "!@#\$%&*"
    private val ALL = UPPER + LOWER + DIGITS + SPECIAL
    private val random = SecureRandom()

    fun generate(length: Int = 10): String {
        val password = StringBuilder(length)
        password.append(UPPER[random.nextInt(UPPER.length)])
        password.append(LOWER[random.nextInt(LOWER.length)])
        password.append(DIGITS[random.nextInt(DIGITS.length)])
        password.append(SPECIAL[random.nextInt(SPECIAL.length)])
        repeat(length - 4) {
            password.append(ALL[random.nextInt(ALL.length)])
        }
        return password.toList().shuffled(random).joinToString("")
    }
}
