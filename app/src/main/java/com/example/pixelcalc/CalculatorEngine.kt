package com.example.pixelcalc

import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

object CalculatorSymbols {
    const val CLEAR_ALL = "AC"
    const val MULTIPLY = "\u00D7"
    const val DIVIDE = "\u00F7"
    const val PLUS_MINUS = "\u00B1"
    const val BACKSPACE = "\u232B"
    const val PARENS = "()"
    const val PERCENT = "%"
    const val SQUARE = "x\u00B2"
    const val SQRT = "\u221A"
    const val POWER = "x\u02B8"
    const val PI = "\u03C0"
    const val DOT = "."
    const val INVERSE = "Inv"
    const val NATURAL_LOG = "ln"
    const val COMMON_LOG = "log"
    const val SCI = "SCI"
}

enum class CalculatorOperation(val symbol: String) {
    ADD("+"),
    SUBTRACT("-"),
    MULTIPLY(CalculatorSymbols.MULTIPLY),
    DIVIDE(CalculatorSymbols.DIVIDE),
    POWER(CalculatorSymbols.POWER);

    companion object {
        fun fromSymbol(symbol: String): CalculatorOperation? =
            entries.firstOrNull { it.symbol == symbol }
    }
}

enum class CalculatorUnaryOperation(val label: String) {
    SIN("sin"),
    COS("cos"),
    TAN("tan"),
    SQUARE(CalculatorSymbols.SQUARE),
    SQRT(CalculatorSymbols.SQRT),
    PERCENT(CalculatorSymbols.PERCENT),
    INVERSE(CalculatorSymbols.INVERSE),
    NATURAL_LOG(CalculatorSymbols.NATURAL_LOG),
    COMMON_LOG(CalculatorSymbols.COMMON_LOG);

    companion object {
        fun fromLabel(label: String): CalculatorUnaryOperation? =
            entries.firstOrNull { it.label == label }
    }
}

enum class CalculatorAngleUnit(val label: String) {
    RADIAN("Rad"),
    DEGREE("Deg");

    fun toggle(): CalculatorAngleUnit =
        if (this == RADIAN) DEGREE else RADIAN
}

data class CalculatorState(
    val display: String = "0",
    val currentValue: Double? = 0.0,
    val accumulator: Double? = null,
    val pendingOperation: CalculatorOperation? = null,
    val enteringFreshNumber: Boolean = true,
    val hasError: Boolean = false,
    val justEvaluated: Boolean = false,
    val angleUnit: CalculatorAngleUnit = CalculatorAngleUnit.RADIAN,
    val expressionTokens: List<String> = emptyList(),
)

object CalculatorEngine {
    val initialState = CalculatorState()

    fun press(state: CalculatorState, key: String): CalculatorState {
        if (key == "C" || key == CalculatorSymbols.CLEAR_ALL) {
            return freshState(state)
        }

        return when {
            key == CalculatorSymbols.PARENS -> pressParentheses(state)
            key == CalculatorSymbols.BACKSPACE -> pressBackspace(state)
            key.length == 1 && key[0].isDigit() -> pressDigit(state, key)
            key == CalculatorSymbols.DOT -> pressDecimal(state)
            key == CalculatorSymbols.PLUS_MINUS -> pressPlusMinus(state)
            key == CalculatorSymbols.PI -> pressConstant(state, kotlin.math.PI)
            key == "e" -> pressConstant(state, kotlin.math.E)
            key == CalculatorAngleUnit.RADIAN.label || key == CalculatorAngleUnit.DEGREE.label -> {
                pressAngleUnit(state)
            }
            CalculatorUnaryOperation.fromLabel(key) != null -> pressUnary(state, key)
            CalculatorOperation.fromSymbol(key) != null -> pressOperation(state, key)
            key == "=" -> pressEquals(state)
            else -> state
        }
    }

    fun pressAll(vararg keys: String): CalculatorState =
        keys.fold(initialState) { current, key -> press(current, key) }

    fun formatResult(value: Double): String {
        if (!value.isFinite()) return "ERR"
        val normalized = if (abs(value) < EPSILON) 0.0 else value
        if (abs(normalized - normalized.roundToLong()) < EPSILON) {
            return normalized.roundToLong().toString()
        }

        return String.format(Locale.US, "%.8f", normalized)
            .trimEnd('0')
            .trimEnd('.')
    }

    private fun pressDigit(state: CalculatorState, digit: String): CalculatorState {
        val base = if (state.hasError || state.justEvaluated) freshState(state) else state
        val display = when {
            base.enteringFreshNumber -> digit
            base.display == "0" -> digit
            base.display == "-0" -> "-$digit"
            base.display.length >= MAX_DISPLAY_LENGTH -> base.display
            else -> base.display + digit
        }

        return base.copy(
            display = display,
            currentValue = display.toDoubleOrNull(),
            enteringFreshNumber = false,
            hasError = false,
            justEvaluated = false,
        )
    }

    private fun pressDecimal(state: CalculatorState): CalculatorState {
        val base = if (state.hasError || state.justEvaluated) freshState(state) else state
        val display = when {
            base.enteringFreshNumber -> "0."
            base.display.contains(".") -> base.display
            base.display.length >= MAX_DISPLAY_LENGTH -> base.display
            else -> base.display + "."
        }

        return base.copy(
            display = display,
            currentValue = display.toDoubleOrNull() ?: 0.0,
            enteringFreshNumber = false,
            hasError = false,
            justEvaluated = false,
        )
    }

    private fun pressBackspace(state: CalculatorState): CalculatorState {
        if (state.hasError || state.justEvaluated) {
            return freshState(state)
        }

        if (!state.enteringFreshNumber) {
            val display = when {
                state.display.length <= 1 -> "0"
                state.display.length == 2 && state.display.startsWith("-") -> "0"
                else -> state.display.dropLast(1)
            }

            return state.copy(
                display = display,
                currentValue = display.toDoubleOrNull() ?: 0.0,
                enteringFreshNumber = display == "0",
                justEvaluated = false,
            )
        }

        if (state.expressionTokens.isEmpty()) {
            return state.copy(
                display = "0",
                currentValue = 0.0,
                enteringFreshNumber = true,
                hasError = false,
                justEvaluated = false,
            )
        }

        val nextTokens = state.expressionTokens.dropLast(1)
        return state.copy(
            expressionTokens = nextTokens,
            pendingOperation = lastOperation(nextTokens),
            enteringFreshNumber = true,
            justEvaluated = false,
        )
    }

    private fun pressPlusMinus(state: CalculatorState): CalculatorState {
        if (state.hasError) return state
        val display = when {
            state.display == "0" || state.display == "0." -> state.display
            state.display.startsWith("-") -> state.display.removePrefix("-")
            else -> "-${state.display}"
        }
        val currentValue = when {
            display == state.display -> state.currentValue
            state.currentValue != null -> -state.currentValue
            else -> display.toDoubleOrNull()
        }

        return state.copy(
            display = display,
            currentValue = currentValue,
            enteringFreshNumber = false,
            justEvaluated = false,
        )
    }

    private fun pressConstant(state: CalculatorState, value: Double): CalculatorState {
        val base = if (state.hasError || state.justEvaluated) freshState(state) else state

        return base.copy(
            display = formatResult(value),
            currentValue = value,
            enteringFreshNumber = false,
            hasError = false,
            justEvaluated = false,
        )
    }

    private fun pressAngleUnit(state: CalculatorState): CalculatorState =
        state.copy(angleUnit = state.angleUnit.toggle())

    private fun pressParentheses(state: CalculatorState): CalculatorState {
        val base = if (state.hasError || state.justEvaluated) freshState(state) else state
        val tokensWithOperand = appendCurrentOperand(base)
        val canClose = unmatchedOpenCount(tokensWithOperand) > 0 &&
            tokensWithOperand.lastOrNull() != OPEN_PAREN &&
            !tokensWithOperand.lastOrNull().isOperationToken()

        if (canClose) {
            val nextTokens = tokensWithOperand + CLOSE_PAREN
            return base.copy(
                expressionTokens = nextTokens,
                pendingOperation = lastOperation(nextTokens),
                enteringFreshNumber = true,
                justEvaluated = false,
            )
        }

        val needsImplicitMultiply = !base.enteringFreshNumber ||
            base.expressionTokens.lastOrNull() == CLOSE_PAREN
        val nextTokens = if (needsImplicitMultiply) {
            appendCurrentOperand(base) + CalculatorSymbols.MULTIPLY + OPEN_PAREN
        } else {
            base.expressionTokens + OPEN_PAREN
        }

        return base.copy(
            display = "0",
            currentValue = 0.0,
            expressionTokens = nextTokens,
            pendingOperation = null,
            enteringFreshNumber = true,
            justEvaluated = false,
        )
    }

    private fun pressUnary(state: CalculatorState, label: String): CalculatorState {
        if (state.hasError) return state

        val operation = CalculatorUnaryOperation.fromLabel(label) ?: return state
        val currentValue = readCurrentValue(state) ?: return errorState(state)
        val result = applyUnaryOperation(currentValue, operation, state.angleUnit) ?: return errorState(state)

        return state.copy(
            display = formatResult(result),
            currentValue = result,
            enteringFreshNumber = false,
            hasError = false,
            justEvaluated = false,
        )
    }

    private fun pressOperation(state: CalculatorState, symbol: String): CalculatorState {
        if (state.hasError) return state

        val operation = CalculatorOperation.fromSymbol(symbol) ?: return state
        val base = if (state.justEvaluated) {
            state.copy(expressionTokens = emptyList(), enteringFreshNumber = false, justEvaluated = false)
        } else {
            state
        }
        val tokensWithOperand = appendCurrentOperand(base)
        val lastToken = tokensWithOperand.lastOrNull()
        val nextTokens = when {
            tokensWithOperand.isEmpty() && operation == CalculatorOperation.SUBTRACT -> {
                listOf("0", operation.symbol)
            }
            tokensWithOperand.isEmpty() -> {
                return base.copy(pendingOperation = operation, enteringFreshNumber = true)
            }
            lastToken.isOperationToken() -> {
                tokensWithOperand.dropLast(1) + operation.symbol
            }
            lastToken == OPEN_PAREN && operation == CalculatorOperation.SUBTRACT -> {
                tokensWithOperand + "0" + operation.symbol
            }
            lastToken == OPEN_PAREN -> {
                return base
            }
            else -> {
                tokensWithOperand + operation.symbol
            }
        }

        return base.copy(
            expressionTokens = nextTokens,
            pendingOperation = operation,
            accumulator = null,
            enteringFreshNumber = true,
            justEvaluated = false,
        )
    }

    private fun pressEquals(state: CalculatorState): CalculatorState {
        if (state.hasError) return state

        val tokens = tokensForEvaluation(state) ?: return errorState(state)
        if (tokens.isEmpty()) {
            return state.copy(enteringFreshNumber = true, justEvaluated = true)
        }

        val result = ExpressionParser(tokens).parse() ?: return errorState(state)
        return CalculatorState(
            display = formatResult(result),
            currentValue = result,
            accumulator = result,
            enteringFreshNumber = true,
            justEvaluated = true,
            angleUnit = state.angleUnit,
        )
    }

    private fun applyUnaryOperation(
        value: Double,
        operation: CalculatorUnaryOperation,
        angleUnit: CalculatorAngleUnit,
    ): Double? {
        val angleValue = when (angleUnit) {
            CalculatorAngleUnit.RADIAN -> value
            CalculatorAngleUnit.DEGREE -> Math.toRadians(value)
        }
        val result = when (operation) {
            CalculatorUnaryOperation.SIN -> sin(angleValue)
            CalculatorUnaryOperation.COS -> cos(angleValue)
            CalculatorUnaryOperation.TAN -> {
                if (abs(cos(angleValue)) < EPSILON) return null
                tan(angleValue)
            }
            CalculatorUnaryOperation.SQUARE -> value * value
            CalculatorUnaryOperation.SQRT -> if (value < 0.0) return null else sqrt(value)
            CalculatorUnaryOperation.PERCENT -> value / 100.0
            CalculatorUnaryOperation.INVERSE -> if (abs(value) < EPSILON) return null else 1.0 / value
            CalculatorUnaryOperation.NATURAL_LOG -> if (value <= 0.0) return null else ln(value)
            CalculatorUnaryOperation.COMMON_LOG -> if (value <= 0.0) return null else log10(value)
        }

        return result.takeIf(Double::isFinite)
    }

    private fun appendCurrentOperand(state: CalculatorState): List<String> {
        if (state.enteringFreshNumber || state.hasError) return state.expressionTokens
        val value = state.currentValue ?: state.display.toDoubleOrNull() ?: return state.expressionTokens
        return state.expressionTokens + value.toString()
    }

    private fun tokensForEvaluation(state: CalculatorState): List<String>? {
        var tokens = appendCurrentOperand(state)
        if (tokens.isEmpty()) return tokens
        if (tokens.last().isOperationToken() || tokens.last() == OPEN_PAREN) return null

        var depth = 0
        tokens.forEach { token ->
            when (token) {
                OPEN_PAREN -> depth += 1
                CLOSE_PAREN -> {
                    depth -= 1
                    if (depth < 0) return null
                }
            }
        }
        tokens = tokens + List(depth) { CLOSE_PAREN }
        return tokens
    }

    private fun unmatchedOpenCount(tokens: List<String>): Int {
        var depth = 0
        tokens.forEach { token ->
            when (token) {
                OPEN_PAREN -> depth += 1
                CLOSE_PAREN -> depth = (depth - 1).coerceAtLeast(0)
            }
        }
        return depth
    }

    private fun lastOperation(tokens: List<String>): CalculatorOperation? =
        tokens.asReversed().firstNotNullOfOrNull(CalculatorOperation::fromSymbol)

    private fun String?.isOperationToken(): Boolean =
        this != null && CalculatorOperation.fromSymbol(this) != null

    private fun freshState(state: CalculatorState): CalculatorState =
        initialState.copy(angleUnit = state.angleUnit)

    private fun errorState(state: CalculatorState): CalculatorState =
        CalculatorState(display = "ERR", hasError = true, angleUnit = state.angleUnit)

    private fun readCurrentValue(state: CalculatorState): Double? =
        state.currentValue ?: state.display.toDoubleOrNull()

    private class ExpressionParser(
        private val tokens: List<String>,
    ) {
        private var position = 0

        fun parse(): Double? {
            val result = parseExpression() ?: return null
            if (position != tokens.size) return null
            return result.takeIf(Double::isFinite)
        }

        private fun parseExpression(): Double? {
            var result = parseTerm() ?: return null
            while (true) {
                result = when {
                    match("+") -> result + (parseTerm() ?: return null)
                    match("-") -> result - (parseTerm() ?: return null)
                    else -> return result
                }
            }
        }

        private fun parseTerm(): Double? {
            var result = parsePower() ?: return null
            while (true) {
                result = when {
                    match(CalculatorSymbols.MULTIPLY) -> result * (parsePower() ?: return null)
                    match(CalculatorSymbols.DIVIDE) -> {
                        val divisor = parsePower() ?: return null
                        if (abs(divisor) < EPSILON) return null
                        result / divisor
                    }
                    else -> return result
                }
            }
        }

        private fun parsePower(): Double? {
            val base = parseUnary() ?: return null
            if (!match(CalculatorSymbols.POWER)) return base
            val exponent = parsePower() ?: return null
            return base.pow(exponent).takeIf(Double::isFinite)
        }

        private fun parseUnary(): Double? =
            when {
                match("+") -> parseUnary()
                match("-") -> parseUnary()?.unaryMinus()
                else -> parsePrimary()
            }

        private fun parsePrimary(): Double? {
            if (match(OPEN_PAREN)) {
                val result = parseExpression() ?: return null
                if (!match(CLOSE_PAREN)) return null
                return result
            }

            val token = tokens.getOrNull(position) ?: return null
            val value = token.toDoubleOrNull() ?: return null
            position += 1
            return value
        }

        private fun match(token: String): Boolean {
            if (tokens.getOrNull(position) != token) return false
            position += 1
            return true
        }
    }

    private const val OPEN_PAREN = "("
    private const val CLOSE_PAREN = ")"
    private const val MAX_DISPLAY_LENGTH = 14
    private const val EPSILON = 0.000000001
}
