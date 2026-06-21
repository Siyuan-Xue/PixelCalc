package com.example.pixelcalc

import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

enum class MatrixOperation(val actionLabel: String) {
    MULTIPLY("MUL"),
    INVERSE("INV"),
    DETERMINANT("DET"),
    TRANSPOSE("T"),
}

enum class MatrixSlot {
    A,
    B,
}

data class MatrixDimensions(
    val m: Int = 2,
    val n: Int = 2,
    val p: Int = 2,
)

data class MatrixComputationResult(
    val label: String,
    val rows: Int,
    val columns: Int,
    val entries: List<String>,
    val hasError: Boolean = false,
    val canAdopt: Boolean = false,
)

enum class MatrixAdoptTarget {
    A,
    B,
}

data class MatrixAdoption(
    val dimensions: MatrixDimensions,
    val leftEntries: List<String>,
    val rightEntries: List<String>,
    val focusedSlot: MatrixSlot,
)

object MatrixEngine {
    const val MIN_SIZE = 1
    const val MAX_SIZE = 4
    const val MAX_ENTRY_DIGITS = 4

    val defaultDimensions = MatrixDimensions()

    fun clampSize(value: Int): Int = value.coerceIn(MIN_SIZE, MAX_SIZE)

    fun leftCellCount(dimensions: MatrixDimensions): Int =
        dimensions.m * dimensions.n

    fun rightCellCount(dimensions: MatrixDimensions): Int =
        dimensions.n * dimensions.p

    fun resultCellCount(dimensions: MatrixDimensions): Int =
        dimensions.m * dimensions.p

    fun resizeEntries(entries: List<String>, cellCount: Int): List<String> =
        List(cellCount.coerceAtLeast(0)) { index -> entries.getOrNull(index).orEmpty() }

    fun blankEntries(cellCount: Int): List<String> =
        List(cellCount.coerceAtLeast(0)) { "" }

    fun appendDigit(value: String, digit: String): String {
        if (digit.length != 1 || !digit[0].isDigit()) return value
        if (value.count(Char::isDigit) >= MAX_ENTRY_DIGITS) return value

        return when (value) {
            "" -> digit
            "-" -> "-$digit"
            "0" -> digit
            "-0" -> "-$digit"
            else -> value + digit
        }
    }

    fun toggleSign(value: String): String =
        when {
            value.isBlank() -> "-"
            value == "-" -> ""
            value.startsWith("-") -> value.removePrefix("-")
            else -> "-$value"
        }

    fun backspace(value: String): String =
        value.dropLast(1)

    fun setEntry(entries: List<String>, index: Int, value: String): List<String> =
        entries.mapIndexed { currentIndex, currentValue ->
            if (currentIndex == index) value else currentValue
        }

    fun parseEntries(entries: List<String>, cellCount: Int): List<Double> =
        resizeEntries(entries, cellCount).map { entry -> entry.toDoubleOrNull() ?: 0.0 }

    fun multiply(
        leftEntries: List<String>,
        rightEntries: List<String>,
        dimensions: MatrixDimensions,
    ): MatrixComputationResult {
        val m = clampSize(dimensions.m)
        val n = clampSize(dimensions.n)
        val p = clampSize(dimensions.p)
        val left = parseEntries(leftEntries, m * n)
        val right = parseEntries(rightEntries, n * p)

        val result = List(m * p) { resultIndex ->
            val row = resultIndex / p
            val column = resultIndex % p

            (0 until n).sumOf { inner ->
                left[row * n + inner] * right[inner * p + column]
            }
        }

        return MatrixComputationResult(
            label = "C = A${CalculatorSymbols.MULTIPLY}B",
            rows = m,
            columns = p,
            entries = result.map(::formatValue),
            canAdopt = true,
        )
    }

    fun inverse(
        entries: List<String>,
        rows: Int,
        columns: Int,
        slot: MatrixSlot,
    ): MatrixComputationResult {
        val label = "inv(${slot.name})"
        if (rows != columns) return errorResult(label)

        val matrix = toMatrix(entries, rows, columns)
        val inverse = invert(matrix) ?: return errorResult(label)

        return MatrixComputationResult(
            label = label,
            rows = rows,
            columns = columns,
            entries = inverse.flatten().map(::formatValue),
            canAdopt = true,
        )
    }

    fun determinant(
        entries: List<String>,
        rows: Int,
        columns: Int,
        slot: MatrixSlot,
    ): MatrixComputationResult {
        val label = "det(${slot.name})"
        if (rows != columns) return errorResult(label)

        val matrix = toMatrix(entries, rows, columns)
        val determinant = determinant(matrix)

        return MatrixComputationResult(
            label = label,
            rows = 1,
            columns = 1,
            entries = listOf(formatValue(determinant)),
        )
    }

    fun transpose(
        entries: List<String>,
        rows: Int,
        columns: Int,
        slot: MatrixSlot,
    ): MatrixComputationResult {
        val matrix = toMatrix(entries, rows, columns)
        val result = List(columns) { row ->
            List(rows) { column -> matrix[column][row] }
        }

        return MatrixComputationResult(
            label = "T(${slot.name})",
            rows = columns,
            columns = rows,
            entries = result.flatten().map(::formatValue),
        )
    }

    fun formatValue(value: Double): String {
        val normalized = if (abs(value) < EPSILON) 0.0 else value
        if (abs(normalized - normalized.roundToLong()) < EPSILON) {
            return normalized.roundToLong().toString()
        }

        return String.format(Locale.US, "%.4f", normalized)
            .trimEnd('0')
            .trimEnd('.')
    }

    fun adoptResult(
        result: MatrixComputationResult,
        target: MatrixAdoptTarget,
    ): MatrixAdoption? {
        if (!result.canAdopt || result.hasError || result.entries.isEmpty()) return null
        val rows = clampSize(result.rows)
        val columns = clampSize(result.columns)
        if (rows != result.rows || columns != result.columns) return null
        val entries = resizeEntries(result.entries, rows * columns)

        return when (target) {
            MatrixAdoptTarget.A -> {
                val dimensions = MatrixDimensions(m = rows, n = columns, p = columns)
                MatrixAdoption(
                    dimensions = dimensions,
                    leftEntries = entries,
                    rightEntries = blankEntries(rightCellCount(dimensions)),
                    focusedSlot = MatrixSlot.B,
                )
            }
            MatrixAdoptTarget.B -> {
                val dimensions = MatrixDimensions(m = rows, n = rows, p = columns)
                MatrixAdoption(
                    dimensions = dimensions,
                    leftEntries = blankEntries(leftCellCount(dimensions)),
                    rightEntries = entries,
                    focusedSlot = MatrixSlot.A,
                )
            }
        }
    }

    fun nextFocus(
        slot: MatrixSlot,
        index: Int,
        dimensions: MatrixDimensions,
    ): Pair<MatrixSlot, Int> {
        val currentCount = if (slot == MatrixSlot.A) {
            leftCellCount(dimensions)
        } else {
            rightCellCount(dimensions)
        }

        if (index + 1 < currentCount) return slot to (index + 1)
        return if (slot == MatrixSlot.A) {
            MatrixSlot.B to 0
        } else {
            MatrixSlot.A to 0
        }
    }

    fun coerceFocus(
        slot: MatrixSlot,
        index: Int,
        dimensions: MatrixDimensions,
    ): Pair<MatrixSlot, Int> {
        val count = if (slot == MatrixSlot.A) {
            leftCellCount(dimensions)
        } else {
            rightCellCount(dimensions)
        }

        return slot to index.coerceIn(0, (count - 1).coerceAtLeast(0))
    }

    private fun toMatrix(entries: List<String>, rows: Int, columns: Int): List<List<Double>> {
        val values = parseEntries(entries, rows * columns)
        return List(rows) { row ->
            List(columns) { column -> values[row * columns + column] }
        }
    }

    private fun determinant(source: List<List<Double>>): Double {
        val size = source.size
        val matrix = source.map { it.toMutableList() }.toMutableList()
        var determinant = 1.0
        var sign = 1.0

        for (column in 0 until size) {
            val pivotRow = (column until size).maxBy { row -> abs(matrix[row][column]) }
            if (abs(matrix[pivotRow][column]) < EPSILON) return 0.0

            if (pivotRow != column) {
                val temp = matrix[column]
                matrix[column] = matrix[pivotRow]
                matrix[pivotRow] = temp
                sign *= -1.0
            }

            val pivot = matrix[column][column]
            determinant *= pivot

            for (row in column + 1 until size) {
                val factor = matrix[row][column] / pivot
                for (innerColumn in column until size) {
                    matrix[row][innerColumn] -= factor * matrix[column][innerColumn]
                }
            }
        }

        return determinant * sign
    }

    private fun invert(source: List<List<Double>>): List<List<Double>>? {
        val size = source.size
        val augmented = List(size) { row ->
            MutableList(size * 2) { column ->
                when {
                    column < size -> source[row][column]
                    column - size == row -> 1.0
                    else -> 0.0
                }
            }
        }.toMutableList()

        for (column in 0 until size) {
            val pivotRow = (column until size).maxBy { row -> abs(augmented[row][column]) }
            if (abs(augmented[pivotRow][column]) < EPSILON) return null

            if (pivotRow != column) {
                val temp = augmented[column]
                augmented[column] = augmented[pivotRow]
                augmented[pivotRow] = temp
            }

            val pivot = augmented[column][column]
            for (innerColumn in 0 until size * 2) {
                augmented[column][innerColumn] /= pivot
            }

            for (row in 0 until size) {
                if (row == column) continue

                val factor = augmented[row][column]
                for (innerColumn in 0 until size * 2) {
                    augmented[row][innerColumn] -= factor * augmented[column][innerColumn]
                }
            }
        }

        return List(size) { row ->
            List(size) { column -> augmented[row][column + size] }
        }
    }

    private fun errorResult(label: String): MatrixComputationResult =
        MatrixComputationResult(
            label = label,
            rows = 1,
            columns = 1,
            entries = listOf("ERR"),
            hasError = true,
        )

    private const val EPSILON = 0.000000001
}
