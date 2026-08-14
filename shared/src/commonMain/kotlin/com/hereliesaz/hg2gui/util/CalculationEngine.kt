package com.hereliesaz.hg2gui.util

import kotlin.math.*

object CalculationEngine {

    private val calculusPattern = Regex("([+\\-*/^])(\\d+\\.?\\d*)")

    /**
     * Performs sequential calculations based on a string pattern.
     * Example: textCalculus(10.0, "+5*2") -> 30.0
     */
    fun textCalculus(input: Double, text: String): Double {
        var result = input
        calculusPattern.findAll(text).forEach { m ->
            val operator = m.groupValues[1][0]
            val value = m.groupValues[2].toDouble()

            when (operator) {
                '+' -> result += value
                '-' -> result -= value
                '*' -> result *= value
                '/' -> result /= value
                '^' -> result = result.pow(value)
            }
        }
        return result
    }

    /**
     * Evaluates a mathematical expression string.
     * Supports +, -, *, /, ^, sqrt, sin, cos, tan.
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

            fun parseFactor(): Double {
                if (eat('+'.code)) return parseFactor()
                if (eat('-'.code)) return -parseFactor()
                var x: Double
                val startPos = pos
                if (eat('('.code)) {
                    x = parseExpression()
                    eat(')'.code)
                } else if (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) {
                    while (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) nextChar()
                    x = str.substring(startPos, pos).toDouble()
                } else if (ch >= 'a'.code && ch <= 'z'.code) {
                    while (ch >= 'a'.code && ch <= 'z'.code) nextChar()
                    val func = str.substring(startPos, pos)
                    x = parseFactor()
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
