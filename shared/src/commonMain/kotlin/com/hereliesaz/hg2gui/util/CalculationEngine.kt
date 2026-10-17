package com.hereliesaz.hg2gui.util

import kotlin.math.*

object CalculationEngine {

    /**
     * Evaluates a mathematical expression string.
     * Supports +, -, *, /, ^, parentheses, and sqrt, sin, cos, tan (degrees, case-insensitive).
     * A parenthesised function argument binds before `^`: `sqrt(9)^2` is 9, not sqrt(81).
     * Throws [RuntimeException] on malformed input, including a missing `)`.
     */
    fun eval(str: String): Double {
        return object {
            var pos = -1
            var ch = 0

            fun nextChar() {
                ch = if (++pos < str.length) str[pos].code else -1
            }

            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.code) nextChar()
                if (ch == charToEat) {
                    nextChar()
                    return true
                }
                return false
            }

            fun parse(): Double {
                nextChar()
                val x = parseExpression()
                while (ch == ' '.code) nextChar()
                if (pos < str.length) throw RuntimeException("Unexpected: " + ch.toChar())
                return x
            }

            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    if (eat('+'.code)) x += parseTerm()
                    else if (eat('-'.code)) x -= parseTerm()
                    else return x
                }
            }

            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    if (eat('*'.code)) x *= parseFactor()
                    else if (eat('/'.code)) x /= parseFactor()
                    else return x
                }
            }

            fun isLetter(c: Int) = c >= 'a'.code && c <= 'z'.code || c >= 'A'.code && c <= 'Z'.code

            fun parseParenthesised(): Double {
                val x = parseExpression()
                if (!eat(')'.code)) throw RuntimeException("Missing ')'")
                return x
            }

            fun parseFactor(): Double {
                if (eat('+'.code)) return parseFactor()
                if (eat('-'.code)) return -parseFactor()
                var x: Double
                while (ch == ' '.code) nextChar()
                val startPos = pos
                if (eat('('.code)) {
                    x = parseParenthesised()
                } else if (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) {
                    while (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) nextChar()
                    x = str.substring(startPos, pos).toDoubleOrNull()
                        ?: throw RuntimeException("Bad number: " + str.substring(startPos, pos))
                } else if (isLetter(ch)) {
                    while (isLetter(ch)) nextChar()
                    val func = str.substring(startPos, pos).lowercase()
                    // `f(expr)` takes exactly the parenthesised argument, so a following `^`
                    // applies to f's result; a bare `f x` keeps taking one factor.
                    x = if (eat('('.code)) parseParenthesised() else parseFactor()
                    x = when (func) {
                        "sqrt" -> sqrt(x)
                        "sin" -> sin(x.toRad())
                        "cos" -> cos(x.toRad())
                        "tan" -> tan(x.toRad())
                        else -> throw RuntimeException("Unknown function: $func")
                    }
                } else {
                    throw RuntimeException("Unexpected: " + ch.toChar())
                }
                if (eat('^'.code)) x = x.pow(parseFactor())
                return x
            }

            private fun Double.toRad() = this * PI / 180.0
        }.parse()
    }
}
