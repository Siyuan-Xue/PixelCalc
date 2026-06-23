package com.example.pixelcalc

import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun additionProducesExpectedResult() {
        assertEquals("5", calculate("2", "+", "3", "="))
    }

    @Test
    fun subtractionProducesExpectedResult() {
        assertEquals("3", calculate("8", "-", "5", "="))
    }

    @Test
    fun multiplicationProducesExpectedResult() {
        assertEquals("42", calculate("6", CalculatorSymbols.MULTIPLY, "7", "="))
    }

    @Test
    fun divisionProducesExpectedResult() {
        assertEquals("4", calculate("8", CalculatorSymbols.DIVIDE, "2", "="))
    }

    @Test
    fun divisionByZeroShowsError() {
        assertEquals("ERR", calculate("1", CalculatorSymbols.DIVIDE, "0", "="))
    }

    @Test
    fun clearResetsDisplay() {
        assertEquals("0", calculate("9", "+", "1", "C"))
    }

    @Test
    fun allClearLabelResetsDisplay() {
        assertEquals("0", calculate("9", "+", "1", CalculatorSymbols.CLEAR_ALL))
    }

    @Test
    fun multiplicationTakesPrecedenceOverAddition() {
        assertEquals(
            "14",
            calculate("2", "+", "3", CalculatorSymbols.MULTIPLY, "4", "="),
        )
    }

    @Test
    fun parenthesesOverrideOperatorPrecedence() {
        assertEquals(
            "20",
            calculate(
                "2",
                CalculatorSymbols.MULTIPLY,
                CalculatorSymbols.PARENS,
                "3",
                "+",
                "7",
                CalculatorSymbols.PARENS,
                "=",
            ),
        )
    }

    @Test
    fun percentScalesCurrentValue() {
        assertEquals("1", calculate("1", "0", "0", CalculatorSymbols.PERCENT))
    }

    @Test
    fun backspaceDeletesCurrentInput() {
        assertEquals("12", calculate("1", "2", "3", CalculatorSymbols.BACKSPACE))
    }

    @Test
    fun sineUsesRadiansByDefault() {
        assertEquals("0", calculate(CalculatorSymbols.PI, "sin"))
    }

    @Test
    fun cosineUsesRadiansByDefault() {
        assertEquals("-1", calculate(CalculatorSymbols.PI, "cos"))
    }

    @Test
    fun tangentUsesRadiansByDefault() {
        assertEquals("0", calculate(CalculatorSymbols.PI, "tan"))
    }

    @Test
    fun squareRootProducesExpectedResult() {
        assertEquals("3", calculate("9", CalculatorSymbols.SQRT, "="))
    }

    @Test
    fun squareProducesExpectedResult() {
        assertEquals("25", calculate("5", CalculatorSymbols.SQUARE))
    }

    @Test
    fun inverseAndLogarithmsProduceExpectedResults() {
        assertEquals("0.25", calculate("4", CalculatorSymbols.INVERSE))
        assertEquals("1", calculate("e", CalculatorSymbols.NATURAL_LOG))
        assertEquals("2", calculate("1", "0", "0", CalculatorSymbols.COMMON_LOG))
    }

    @Test
    fun degreeModeConvertsTrigonometry() {
        assertEquals("1", calculate(CalculatorAngleUnit.RADIAN.label, "9", "0", "sin"))
    }

    @Test
    fun powerProducesExpectedResult() {
        assertEquals("8", calculate("2", CalculatorSymbols.POWER, "3", "="))
    }

    @Test
    fun constantsCanParticipateInOperations() {
        assertEquals("4.14159265", calculate(CalculatorSymbols.PI, "+", "1", "="))
        assertEquals("3.71828183", calculate("e", "+", "1", "="))
    }

    @Test
    fun decimalAndSignToggleWorkTogether() {
        assertEquals("-2.5", calculate("2", CalculatorSymbols.DOT, "5", CalculatorSymbols.PLUS_MINUS))
    }

    @Test
    fun multipliesTwoByTwoMatrices() {
        val result = MatrixEngine.multiply(
            leftEntries = listOf("1", "2", "3", "4"),
            rightEntries = listOf("5", "6", "7", "8"),
            dimensions = MatrixDimensions(m = 2, n = 2, p = 2),
        )

        assertEquals(listOf("19", "22", "43", "50"), result.entries)
        assertEquals(true, result.canAdopt)
    }

    @Test
    fun multipliesRectangularMatrices() {
        val result = MatrixEngine.multiply(
            leftEntries = listOf("1", "2", "3", "4", "5", "6"),
            rightEntries = listOf("7", "8", "9", "10", "11", "12"),
            dimensions = MatrixDimensions(m = 2, n = 3, p = 2),
        )

        assertEquals(listOf("58", "64", "139", "154"), result.entries)
    }

    @Test
    fun matrixMultiplicationSupportsNegativeValues() {
        val result = MatrixEngine.multiply(
            leftEntries = listOf("-1", "2", "3", "-4"),
            rightEntries = listOf("5", "-6", "7", "8"),
            dimensions = MatrixDimensions(m = 2, n = 2, p = 2),
        )

        assertEquals(listOf("9", "22", "-13", "-50"), result.entries)
    }

    @Test
    fun blankMatrixEntriesAreTreatedAsZero() {
        val result = MatrixEngine.multiply(
            leftEntries = listOf("1", "", "", "4"),
            rightEntries = listOf("", "2", "3", ""),
            dimensions = MatrixDimensions(m = 2, n = 2, p = 2),
        )

        assertEquals(listOf("0", "2", "12", "0"), result.entries)
    }

    @Test
    fun invertsTwoByTwoMatrix() {
        val result = MatrixEngine.inverse(
            entries = listOf("4", "7", "2", "6"),
            rows = 2,
            columns = 2,
            slot = MatrixSlot.A,
        )

        assertEquals(listOf("0.6", "-0.7", "-0.2", "0.4"), result.entries)
        assertEquals(true, result.canAdopt)
    }

    @Test
    fun determinantOfTwoByTwoMatrix() {
        val result = MatrixEngine.determinant(
            entries = listOf("1", "2", "3", "4"),
            rows = 2,
            columns = 2,
            slot = MatrixSlot.A,
        )

        assertEquals(listOf("-2"), result.entries)
        assertEquals(false, result.canAdopt)
    }

    @Test
    fun determinantOfThreeByThreeMatrix() {
        val result = MatrixEngine.determinant(
            entries = listOf("6", "1", "1", "4", "-2", "5", "2", "8", "7"),
            rows = 3,
            columns = 3,
            slot = MatrixSlot.B,
        )

        assertEquals(listOf("-306"), result.entries)
    }

    @Test
    fun inverseRejectsNonSquareMatrix() {
        val result = MatrixEngine.inverse(
            entries = listOf("1", "2", "3", "4", "5", "6"),
            rows = 2,
            columns = 3,
            slot = MatrixSlot.A,
        )

        assertEquals(true, result.hasError)
        assertEquals(listOf("ERR"), result.entries)
    }

    @Test
    fun determinantRejectsNonSquareMatrix() {
        val result = MatrixEngine.determinant(
            entries = listOf("1", "2", "3", "4", "5", "6"),
            rows = 2,
            columns = 3,
            slot = MatrixSlot.A,
        )

        assertEquals(true, result.hasError)
        assertEquals(listOf("ERR"), result.entries)
    }

    @Test
    fun inverseRejectsSingularMatrix() {
        val result = MatrixEngine.inverse(
            entries = listOf("1", "2", "2", "4"),
            rows = 2,
            columns = 2,
            slot = MatrixSlot.A,
        )

        assertEquals(true, result.hasError)
        assertEquals(listOf("ERR"), result.entries)
    }

    @Test
    fun transposesRectangularMatrix() {
        val result = MatrixEngine.transpose(
            entries = listOf("1", "2", "3", "4", "5", "6"),
            rows = 2,
            columns = 3,
            slot = MatrixSlot.B,
        )

        assertEquals(3, result.rows)
        assertEquals(2, result.columns)
        assertEquals(listOf("1", "4", "2", "5", "3", "6"), result.entries)
    }

    @Test
    fun multiplicationResultCanBeAdoptedToLeftMatrix() {
        val result = MatrixEngine.multiply(
            leftEntries = listOf("1", "2", "3", "4"),
            rightEntries = listOf("5", "6", "7", "8"),
            dimensions = MatrixDimensions(m = 2, n = 2, p = 2),
        )
        val adoption = MatrixEngine.adoptResult(result, MatrixAdoptTarget.A)

        requireNotNull(adoption)
        assertEquals(MatrixDimensions(m = 2, n = 2, p = 2), adoption.dimensions)
        assertEquals(listOf("19", "22", "43", "50"), adoption.leftEntries)
        assertEquals(listOf("", "", "", ""), adoption.rightEntries)
        assertEquals(MatrixSlot.B, adoption.focusedSlot)
    }

    @Test
    fun multiplicationResultCanBeAdoptedToRightMatrix() {
        val result = MatrixEngine.multiply(
            leftEntries = listOf("1", "2", "3", "4"),
            rightEntries = listOf("5", "6", "7", "8"),
            dimensions = MatrixDimensions(m = 2, n = 2, p = 2),
        )
        val adoption = MatrixEngine.adoptResult(result, MatrixAdoptTarget.B)

        requireNotNull(adoption)
        assertEquals(MatrixDimensions(m = 2, n = 2, p = 2), adoption.dimensions)
        assertEquals(listOf("", "", "", ""), adoption.leftEntries)
        assertEquals(listOf("19", "22", "43", "50"), adoption.rightEntries)
        assertEquals(MatrixSlot.A, adoption.focusedSlot)
    }

    @Test
    fun inverseResultCanBeAdoptedForFollowUpMultiplication() {
        val result = MatrixEngine.inverse(
            entries = listOf("4", "7", "2", "6"),
            rows = 2,
            columns = 2,
            slot = MatrixSlot.A,
        )
        val adoption = MatrixEngine.adoptResult(result, MatrixAdoptTarget.A)

        requireNotNull(adoption)
        assertEquals(listOf("0.6", "-0.7", "-0.2", "0.4"), adoption.leftEntries)
        assertEquals(listOf("", "", "", ""), adoption.rightEntries)
    }

    @Test
    fun determinantAndErrorResultsCannotBeAdopted() {
        val determinant = MatrixEngine.determinant(
            entries = listOf("1", "2", "3", "4"),
            rows = 2,
            columns = 2,
            slot = MatrixSlot.A,
        )
        val error = MatrixEngine.inverse(
            entries = listOf("1", "2", "3", "4", "5", "6"),
            rows = 2,
            columns = 3,
            slot = MatrixSlot.A,
        )

        assertEquals(null, MatrixEngine.adoptResult(determinant, MatrixAdoptTarget.A))
        assertEquals(null, MatrixEngine.adoptResult(error, MatrixAdoptTarget.B))
    }

    @Test
    fun resizingEntriesCropsAndPadsForNewDimensions() {
        assertEquals(
            listOf("1", "2", "3"),
            MatrixEngine.resizeEntries(listOf("1", "2", "3", "4"), 3),
        )
        assertEquals(
            listOf("1", "2", "", ""),
            MatrixEngine.resizeEntries(listOf("1", "2"), 4),
        )
    }

    private fun calculate(vararg keys: String): String {
        return CalculatorEngine.pressAll(*keys).display
    }
}
