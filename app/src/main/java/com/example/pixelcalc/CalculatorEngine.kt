package com.example.pixelcalc

import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

object CalculatorSymbols {
    const val MULTIPLY = "\u00D7"
    const val DIVIDE = "\u00F7"
    const val PLUS_MINUS = "\u00B1"
    const val BACKSPACE = "\u232B"
    const val SQUARE = "x\u00B2"
    const val SQRT = "\u221A"
    const val POWER = "x\u02B8"
    const val PI = "\u03C0"
    const val DOT = "."
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
    SQRT(CalculatorSymbols.SQRT);

    companion object {
        fun fromLabel(label: String): CalculatorUnaryOperation? =
            entries.firstOrNull { it.label == label }
    }
}

data class CalculatorState(
    val display: String = "0",
    val currentValue: Double? = 0.0,
    val accumulator: Double? = null,
    val pendingOperation: CalculatorOperation? = null,
    val enteringFreshNumber: Boolean = true,
    val hasError: Boolean = false,
    val justEvaluated: Boolean = false,
)

object CalculatorEngine {
    val initialState = CalculatorState()

    fun press(state: CalculatorState, key: String): CalculatorState {
        if (key == "C") return initialState

        return when {
            key.length == 1 && key[0].isDigit() -> pressDigit(state, key)
            key == CalculatorSymbols.DOT -> pressDecimal(state)
            key == CalculatorSymbols.PLUS_MINUS -> pressPlusMinus(state)
            key == CalculatorSymbols.PI -> pressConstant(state, kotlin.math.PI)
            key == "e" -> pressConstant(state, kotlin.math.E)
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
        val base = if (state.hasError || state.justEvaluated) initialState else state
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
        val base = if (state.hasError || state.justEvaluated) initialState else state
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
        val base = if (state.hasError || state.justEvaluated) initialState else state

        return base.copy(
            display = formatResult(value),
            currentValue = value,
            enteringFreshNumber = false,
            hasError = false,
            justEvaluated = false,
        )
    }

    private fun pressUnary(state: CalculatorState, label: String): CalculatorState {
        if (state.hasError) return state

        val operation = CalculatorUnaryOperation.fromLabel(label) ?: return state
        val currentValue = readCurrentValue(state) ?: return errorState()
        val result = applyUnaryOperation(currentValue, operation) ?: return errorState()
        val isSecondOperand = state.pendingOperation != null

        return state.copy(
            display = formatResult(result),
            currentValue = result,
            enteringFreshNumber = !isSecondOperand,
            hasError = false,
            justEvaluated = !isSecondOperand,
        )
    }

    private fun pressOperation(state: CalculatorState, symbol: String): CalculatorState {
        if (state.hasError) return state

        val operation = CalculatorOperation.fromSymbol(symbol) ?: return state
        val currentValue = readCurrentValue(state) ?: return errorState()

        if (state.accumulator != null && state.pendingOperation != null && !state.enteringFreshNumber) {
            val result = applyOperation(state.accumulator, currentValue, state.pendingOperation)
                ?: return errorState()

            return state.copy(
                display = formatResult(result),
                currentValue = result,
                accumulator = result,
                pendingOperation = operation,
                enteringFreshNumber = true,
                justEvaluated = false,
            )
        }

        return state.copy(
            accumulator = state.accumulator ?: currentValue,
            pendingOperation = operation,
            enteringFreshNumber = true,
            justEvaluated = false,
        )
    }

    private fun pressEquals(state: CalculatorState): CalculatorState {
        if (state.hasError) return state

        val operation = state.pendingOperation ?: return state.copy(
            enteringFreshNumber = true,
            justEvaluated = true,
        )

        val left = state.accumulator ?: return state
        val right = readCurrentValue(state) ?: return errorState()
        val result = applyOperation(left, right, operation) ?: return errorState()

        return CalculatorState(
            display = formatResult(result),
            currentValue = result,
            accumulator = result,
            enteringFreshNumber = true,
            justEvaluated = true,
        )
    }

    private fun applyOperation(
        left: Double,
        right: Double,
        operation: CalculatorOperation,
    ): Double? {
        val result = when (operation) {
            CalculatorOperation.ADD -> left + right
            CalculatorOperation.SUBTRACT -> left - right
            CalculatorOperation.MULTIPLY -> left * right
            CalculatorOperation.DIVIDE -> if (abs(right) < EPSILON) return null else left / right
            CalculatorOperation.POWER -> left.pow(right)
        }

        return result.takeIf(Double::isFinite)
    }

    private fun applyUnaryOperation(
        value: Double,
        operation: CalculatorUnaryOperation,
    ): Double? {
        val result = when (operation) {
            CalculatorUnaryOperation.SIN -> sin(value)
            CalculatorUnaryOperation.COS -> cos(value)
            CalculatorUnaryOperation.TAN -> {
                if (abs(cos(value)) < EPSILON) return null
                tan(value)
            }
            CalculatorUnaryOperation.SQUARE -> value * value
            CalculatorUnaryOperation.SQRT -> if (value < 0.0) return null else sqrt(value)
        }

        return result.takeIf(Double::isFinite)
    }

    private fun errorState(): CalculatorState =
        CalculatorState(display = "ERR", hasError = true)

    private fun readCurrentValue(state: CalculatorState): Double? =
        state.currentValue ?: state.display.toDoubleOrNull()

    private const val MAX_DISPLAY_LENGTH = 14
    private const val EPSILON = 0.000000001
}
