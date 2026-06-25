package com.example.pixelcalc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pixelcalc.ui.theme.PixelAmber
import com.example.pixelcalc.ui.theme.PixelButtonAlt
import com.example.pixelcalc.ui.theme.PixelButtonSurface
import com.example.pixelcalc.ui.theme.PixelCalcTheme
import com.example.pixelcalc.ui.theme.PixelClay
import com.example.pixelcalc.ui.theme.PixelClayDark
import com.example.pixelcalc.ui.theme.PixelError
import com.example.pixelcalc.ui.theme.PixelInk
import com.example.pixelcalc.ui.theme.PixelLine
import com.example.pixelcalc.ui.theme.PixelPanel
import com.example.pixelcalc.ui.theme.PixelPaper
import com.example.pixelcalc.ui.theme.PixelSage

private enum class AppMode {
    STANDARD,
    MATRIX,
}

private const val PixelCreditLabel = "CODEX \u00D7 XUE"
private const val AdoptResultToLeftLabel = "C\u2192A"
private const val AdoptResultToRightLabel = "C\u2192B"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PixelCalcTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    PixelCalcApp()
                }
            }
        }
    }
}

@Composable
fun PixelCalcApp(modifier: Modifier = Modifier) {
    var modeName by rememberSaveable { mutableStateOf(AppMode.STANDARD.name) }
    val mode = AppMode.valueOf(modeName)

    var standardDisplay by rememberSaveable { mutableStateOf(CalculatorEngine.initialState.display) }
    var standardCurrentValue by rememberSaveable { mutableStateOf(CalculatorEngine.initialState.currentValue) }
    var standardAccumulator by rememberSaveable { mutableStateOf<Double?>(null) }
    var standardPendingOperation by rememberSaveable { mutableStateOf<String?>(null) }
    var standardEnteringFreshNumber by rememberSaveable {
        mutableStateOf(CalculatorEngine.initialState.enteringFreshNumber)
    }
    var standardHasError by rememberSaveable { mutableStateOf(CalculatorEngine.initialState.hasError) }
    var standardJustEvaluated by rememberSaveable {
        mutableStateOf(CalculatorEngine.initialState.justEvaluated)
    }
    var standardAngleUnitName by rememberSaveable {
        mutableStateOf(CalculatorEngine.initialState.angleUnit.name)
    }
    var standardExpressionTokens by rememberSaveable {
        mutableStateOf(CalculatorEngine.initialState.expressionTokens)
    }

    var matrixM by rememberSaveable { mutableStateOf(MatrixEngine.defaultDimensions.m) }
    var matrixN by rememberSaveable { mutableStateOf(MatrixEngine.defaultDimensions.n) }
    var matrixP by rememberSaveable { mutableStateOf(MatrixEngine.defaultDimensions.p) }
    var scienceOpen by rememberSaveable { mutableStateOf(false) }
    var focusedMatrixName by rememberSaveable { mutableStateOf(MatrixSlot.A.name) }
    var focusedMatrixIndex by rememberSaveable { mutableStateOf(0) }
    var leftEntries by rememberSaveable {
        mutableStateOf(MatrixEngine.blankEntries(MatrixEngine.leftCellCount(MatrixEngine.defaultDimensions)))
    }
    var rightEntries by rememberSaveable {
        mutableStateOf(MatrixEngine.blankEntries(MatrixEngine.rightCellCount(MatrixEngine.defaultDimensions)))
    }
    var resultEntries by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var resultLabel by rememberSaveable {
        mutableStateOf("C = A${CalculatorSymbols.MULTIPLY}B")
    }
    var resultRows by rememberSaveable { mutableStateOf(MatrixEngine.defaultDimensions.m) }
    var resultColumns by rememberSaveable { mutableStateOf(MatrixEngine.defaultDimensions.p) }
    var resultHasError by rememberSaveable { mutableStateOf(false) }
    var resultCanAdopt by rememberSaveable { mutableStateOf(false) }

    val standardState = CalculatorState(
        display = standardDisplay,
        currentValue = standardCurrentValue,
        accumulator = standardAccumulator,
        pendingOperation = standardPendingOperation?.let(CalculatorOperation::fromSymbol),
        enteringFreshNumber = standardEnteringFreshNumber,
        hasError = standardHasError,
        justEvaluated = standardJustEvaluated,
        angleUnit = CalculatorAngleUnit.valueOf(standardAngleUnitName),
        expressionTokens = standardExpressionTokens,
    )

    val matrixDimensions = MatrixDimensions(matrixM, matrixN, matrixP)
    val focusedMatrix = MatrixSlot.valueOf(focusedMatrixName)

    fun commitStandard(nextState: CalculatorState) {
        standardDisplay = nextState.display
        standardCurrentValue = nextState.currentValue
        standardAccumulator = nextState.accumulator
        standardPendingOperation = nextState.pendingOperation?.symbol
        standardEnteringFreshNumber = nextState.enteringFreshNumber
        standardHasError = nextState.hasError
        standardJustEvaluated = nextState.justEvaluated
        standardAngleUnitName = nextState.angleUnit.name
        standardExpressionTokens = nextState.expressionTokens
    }

    fun resetMatrix() {
        val default = MatrixEngine.defaultDimensions
        matrixM = default.m
        matrixN = default.n
        matrixP = default.p
        focusedMatrixName = MatrixSlot.A.name
        focusedMatrixIndex = 0
        leftEntries = MatrixEngine.blankEntries(MatrixEngine.leftCellCount(default))
        rightEntries = MatrixEngine.blankEntries(MatrixEngine.rightCellCount(default))
        resultEntries = emptyList()
        resultLabel = "C = A${CalculatorSymbols.MULTIPLY}B"
        resultRows = default.m
        resultColumns = default.p
        resultHasError = false
        resultCanAdopt = false
    }

    fun clearMatrixResult(dimensions: MatrixDimensions = matrixDimensions) {
        resultEntries = emptyList()
        resultLabel = "C = A${CalculatorSymbols.MULTIPLY}B"
        resultRows = dimensions.m
        resultColumns = dimensions.p
        resultHasError = false
        resultCanAdopt = false
    }

    fun commitMatrixResult(result: MatrixComputationResult) {
        resultEntries = result.entries
        resultLabel = result.label
        resultRows = result.rows
        resultColumns = result.columns
        resultHasError = result.hasError
        resultCanAdopt = result.canAdopt
    }

    fun commitDimensions(nextDimensions: MatrixDimensions) {
        val safeDimensions = MatrixDimensions(
            m = MatrixEngine.clampSize(nextDimensions.m),
            n = MatrixEngine.clampSize(nextDimensions.n),
            p = MatrixEngine.clampSize(nextDimensions.p),
        )
        matrixM = safeDimensions.m
        matrixN = safeDimensions.n
        matrixP = safeDimensions.p
        leftEntries = MatrixEngine.resizeEntries(leftEntries, MatrixEngine.leftCellCount(safeDimensions))
        rightEntries = MatrixEngine.resizeEntries(rightEntries, MatrixEngine.rightCellCount(safeDimensions))
        val coercedFocus = MatrixEngine.coerceFocus(
            focusedMatrix,
            focusedMatrixIndex,
            safeDimensions,
        )
        focusedMatrixName = coercedFocus.first.name
        focusedMatrixIndex = coercedFocus.second
        clearMatrixResult(safeDimensions)
    }

    fun moveToNextMatrixCell() {
        val nextFocus = MatrixEngine.nextFocus(
            MatrixSlot.valueOf(focusedMatrixName),
            focusedMatrixIndex,
            matrixDimensions,
        )
        focusedMatrixName = nextFocus.first.name
        focusedMatrixIndex = nextFocus.second
    }

    fun updateFocusedMatrixCell(transform: (String) -> String) {
        val currentSlot = MatrixSlot.valueOf(focusedMatrixName)
        if (currentSlot == MatrixSlot.A) {
            val current = leftEntries.getOrNull(focusedMatrixIndex).orEmpty()
            leftEntries = MatrixEngine.setEntry(leftEntries, focusedMatrixIndex, transform(current))
        } else {
            val current = rightEntries.getOrNull(focusedMatrixIndex).orEmpty()
            rightEntries = MatrixEngine.setEntry(rightEntries, focusedMatrixIndex, transform(current))
        }
        clearMatrixResult()
    }

    fun handleMatrixOperation(operation: MatrixOperation) {
        val focusedSlot = MatrixSlot.valueOf(focusedMatrixName)
        val focusedEntries = if (focusedSlot == MatrixSlot.A) leftEntries else rightEntries
        val focusedRows = if (focusedSlot == MatrixSlot.A) matrixDimensions.m else matrixDimensions.n
        val focusedColumns = if (focusedSlot == MatrixSlot.A) matrixDimensions.n else matrixDimensions.p

        val result = when (operation) {
            MatrixOperation.MULTIPLY -> MatrixEngine.multiply(
                leftEntries,
                rightEntries,
                matrixDimensions,
            )
            MatrixOperation.INVERSE -> MatrixEngine.inverse(
                focusedEntries,
                focusedRows,
                focusedColumns,
                focusedSlot,
            )
            MatrixOperation.DETERMINANT -> MatrixEngine.determinant(
                focusedEntries,
                focusedRows,
                focusedColumns,
                focusedSlot,
            )
            MatrixOperation.TRANSPOSE -> MatrixEngine.transpose(
                focusedEntries,
                focusedRows,
                focusedColumns,
                focusedSlot,
            )
        }

        commitMatrixResult(result)
    }

    fun adoptMatrixResult(target: MatrixAdoptTarget) {
        val currentResult = MatrixComputationResult(
            label = resultLabel,
            rows = resultRows,
            columns = resultColumns,
            entries = resultEntries,
            hasError = resultHasError,
            canAdopt = resultCanAdopt,
        )
        val adoption = MatrixEngine.adoptResult(currentResult, target) ?: return
        matrixM = adoption.dimensions.m
        matrixN = adoption.dimensions.n
        matrixP = adoption.dimensions.p
        leftEntries = adoption.leftEntries
        rightEntries = adoption.rightEntries
        focusedMatrixName = adoption.focusedSlot.name
        focusedMatrixIndex = 0
        clearMatrixResult(adoption.dimensions)
    }

    fun handleMatrixKey(key: String) {
        when {
            key.length == 1 && key[0].isDigit() -> {
                var shouldAdvance = false
                updateFocusedMatrixCell { current ->
                    val next = MatrixEngine.appendDigit(current, key)
                    shouldAdvance = next != current &&
                        next.count { character -> character.isDigit() } >= MatrixEngine.MAX_ENTRY_DIGITS
                    next
                }
                if (shouldAdvance) moveToNextMatrixCell()
            }

            key == "NEXT" -> moveToNextMatrixCell()
            key == "A/B" -> {
                focusedMatrixName = if (MatrixSlot.valueOf(focusedMatrixName) == MatrixSlot.A) {
                    MatrixSlot.B.name
                } else {
                    MatrixSlot.A.name
                }
                val coercedFocus = MatrixEngine.coerceFocus(
                    MatrixSlot.valueOf(focusedMatrixName),
                    focusedMatrixIndex,
                    matrixDimensions,
                )
                focusedMatrixIndex = coercedFocus.second
            }

            key == CalculatorSymbols.PLUS_MINUS -> updateFocusedMatrixCell(MatrixEngine::toggleSign)
            key == CalculatorSymbols.BACKSPACE -> updateFocusedMatrixCell(MatrixEngine::backspace)
            key == "C" -> resetMatrix()
            key == "=" -> handleMatrixOperation(MatrixOperation.MULTIPLY)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PixelInk)
            .systemBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PixelCalcHeader(
            status = if (mode == AppMode.STANDARD) {
                standardState.pendingOperation?.symbol ?: PixelCreditLabel
            } else {
                "MAT ${matrixM}x${matrixN}/${matrixN}x${matrixP}"
            },
            modifier = Modifier.fillMaxWidth(),
        )
        PixelModeSwitch(
            mode = mode,
            onModeChange = { modeName = it.name },
            modifier = Modifier.fillMaxWidth(),
        )
        if (mode == AppMode.STANDARD) {
            StandardCalculatorScreen(
                state = standardState,
                onPress = { key -> commitStandard(CalculatorEngine.press(standardState, key)) },
                scienceOpen = scienceOpen,
                onToggleScience = { scienceOpen = !scienceOpen },
                modifier = Modifier.weight(1f),
            )
        } else {
            MatrixCalculatorScreen(
                dimensions = matrixDimensions,
                leftEntries = leftEntries,
                rightEntries = rightEntries,
                resultEntries = resultEntries,
                focusedMatrix = focusedMatrix,
                focusedIndex = focusedMatrixIndex,
                resultLabel = resultLabel,
                resultRows = resultRows,
                resultColumns = resultColumns,
                resultHasError = resultHasError,
                resultCanAdopt = resultCanAdopt,
                onDimensionChange = ::commitDimensions,
                onCellFocus = { slot, index ->
                    focusedMatrixName = slot.name
                    focusedMatrixIndex = index
                },
                onOperation = ::handleMatrixOperation,
                onAdoptResult = ::adoptMatrixResult,
                onKeyPress = ::handleMatrixKey,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StandardCalculatorScreen(
    state: CalculatorState,
    onPress: (String) -> Unit,
    scienceOpen: Boolean,
    onToggleScience: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PixelDisplay(
            value = state.display,
            hasError = state.hasError,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 112.dp),
        )
        PixelKey(
            label = CalculatorSymbols.SCI,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 42.dp),
            onClick = onToggleScience,
        )
        StandardKeyGrid(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            scienceOpen = scienceOpen,
            angleUnit = state.angleUnit,
            onPress = onPress,
        )
    }
}

@Composable
private fun MatrixCalculatorScreen(
    dimensions: MatrixDimensions,
    leftEntries: List<String>,
    rightEntries: List<String>,
    resultEntries: List<String>,
    focusedMatrix: MatrixSlot,
    focusedIndex: Int,
    resultLabel: String,
    resultRows: Int,
    resultColumns: Int,
    resultHasError: Boolean,
    resultCanAdopt: Boolean,
    onDimensionChange: (MatrixDimensions) -> Unit,
    onCellFocus: (MatrixSlot, Int) -> Unit,
    onOperation: (MatrixOperation) -> Unit,
    onAdoptResult: (MatrixAdoptTarget) -> Unit,
    onKeyPress: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MatrixDimensionControls(
            dimensions = dimensions,
            onChange = onDimensionChange,
            modifier = Modifier.fillMaxWidth(),
        )
        MatrixOperationBar(
            onOperation = onOperation,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.05f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MatrixGrid(
                title = "A ${dimensions.m}x${dimensions.n}",
                rows = dimensions.m,
                columns = dimensions.n,
                entries = leftEntries,
                activeIndex = if (focusedMatrix == MatrixSlot.A) focusedIndex else null,
                onCellFocus = { index -> onCellFocus(MatrixSlot.A, index) },
                modifier = Modifier.weight(1f),
            )
            MatrixGrid(
                title = "B ${dimensions.n}x${dimensions.p}",
                rows = dimensions.n,
                columns = dimensions.p,
                entries = rightEntries,
                activeIndex = if (focusedMatrix == MatrixSlot.B) focusedIndex else null,
                onCellFocus = { index -> onCellFocus(MatrixSlot.B, index) },
                modifier = Modifier.weight(1f),
            )
        }
        MatrixResultPanel(
            label = resultLabel,
            rows = resultRows,
            columns = resultColumns,
            entries = resultEntries,
            hasError = resultHasError,
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.62f),
        )
        MatrixAdoptControls(
            canAdopt = resultCanAdopt,
            onAdoptResult = onAdoptResult,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 34.dp),
        )
        MatrixKeyGrid(
            onPress = onKeyPress,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )
    }
}

@Composable
private fun PixelCalcHeader(
    status: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "PIXELCALC",
            color = PixelPaper,
            fontFamily = FontFamily.Monospace,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.sp,
        )
        Text(
            text = status,
            color = if (status == PixelCreditLabel) PixelSage else PixelAmber,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.sp,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PixelModeSwitch(
    mode: AppMode,
    onModeChange: (AppMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(PixelButtonSurface, RectangleShape)
            .border(BorderStroke(2.dp, PixelLine), RectangleShape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        AppMode.entries.forEach { tabMode ->
            val selected = tabMode == mode
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(if (selected) PixelClay else PixelButtonAlt, RectangleShape)
                    .border(
                        BorderStroke(1.dp, if (selected) PixelAmber else PixelLine),
                        RectangleShape,
                    )
                    .clickable { onModeChange(tabMode) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (tabMode == AppMode.STANDARD) "STD" else "MAT",
                    color = if (selected) PixelInk else PixelPaper,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.sp,
                )
            }
        }
    }
}

@Composable
private fun PixelDisplay(
    value: String,
    hasError: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(PixelPanel, RectangleShape)
            .border(BorderStroke(2.dp, PixelLine), RectangleShape)
            .padding(horizontal = 18.dp, vertical = 20.dp),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Text(
            text = value,
            color = if (hasError) PixelError else PixelInk,
            fontFamily = FontFamily.Monospace,
            fontSize = 46.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun MatrixDimensionControls(
    dimensions: MatrixDimensions,
    onChange: (MatrixDimensions) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DimensionStepper(
            value = dimensions.m,
            onMinus = { onChange(dimensions.copy(m = dimensions.m - 1)) },
            onPlus = { onChange(dimensions.copy(m = dimensions.m + 1)) },
            modifier = Modifier.weight(1f),
        )
        DimensionStepper(
            value = dimensions.n,
            onMinus = { onChange(dimensions.copy(n = dimensions.n - 1)) },
            onPlus = { onChange(dimensions.copy(n = dimensions.n + 1)) },
            modifier = Modifier.weight(1f),
        )
        DimensionStepper(
            value = dimensions.p,
            onMinus = { onChange(dimensions.copy(p = dimensions.p - 1)) },
            onPlus = { onChange(dimensions.copy(p = dimensions.p + 1)) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MatrixOperationBar(
    onOperation: (MatrixOperation) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(PixelButtonSurface, RectangleShape)
            .border(BorderStroke(2.dp, PixelLine), RectangleShape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        listOf(
            MatrixOperation.INVERSE,
            MatrixOperation.DETERMINANT,
        ).forEach { operation ->
            MiniPixelButton(
                label = operation.actionLabel,
                onClick = { onOperation(operation) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MatrixAdoptControls(
    canAdopt: Boolean,
    onAdoptResult: (MatrixAdoptTarget) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(PixelButtonSurface, RectangleShape)
            .border(BorderStroke(2.dp, PixelLine), RectangleShape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        MiniPixelButton(
            label = AdoptResultToLeftLabel,
            onClick = { onAdoptResult(MatrixAdoptTarget.A) },
            modifier = Modifier.weight(1f),
            enabled = canAdopt,
        )
        MiniPixelButton(
            label = AdoptResultToRightLabel,
            onClick = { onAdoptResult(MatrixAdoptTarget.B) },
            modifier = Modifier.weight(1f),
            enabled = canAdopt,
        )
    }
}

@Composable
private fun DimensionStepper(
    value: Int,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(PixelButtonSurface, RectangleShape)
            .border(BorderStroke(2.dp, PixelLine), RectangleShape)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        MiniPixelButton("-", onMinus, Modifier.weight(0.75f))
        Text(
            text = value.toString(),
            color = PixelPaper,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        MiniPixelButton("+", onPlus, Modifier.weight(0.75f))
    }
}

@Composable
private fun MiniPixelButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .background(if (enabled) PixelButtonAlt else PixelButtonSurface, RectangleShape)
            .border(BorderStroke(1.dp, if (enabled) PixelLine else PixelSage), RectangleShape)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (enabled) PixelAmber else PixelSage,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.sp,
        )
    }
}

@Composable
private fun MatrixGrid(
    title: String,
    rows: Int,
    columns: Int,
    entries: List<String>,
    activeIndex: Int?,
    onCellFocus: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = title,
            color = PixelSage,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.sp,
            maxLines = 1,
        )
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            val safeRows = rows.coerceAtLeast(1)
            val safeColumns = columns.coerceAtLeast(1)
            val gap = 4.dp
            val widthForCells = maxWidth - gap * (safeColumns - 1).toFloat()
            val heightForCells = maxHeight - gap * (safeRows - 1).toFloat()
            val cellByWidth = widthForCells / safeColumns.toFloat()
            val cellByHeight = heightForCells / safeRows.toFloat()
            val cellSize = if (cellByWidth < cellByHeight) cellByWidth else cellByHeight

            Column(
                verticalArrangement = Arrangement.spacedBy(gap),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                repeat(safeRows) { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(gap),
                    ) {
                        repeat(safeColumns) { column ->
                            val index = row * safeColumns + column
                            MatrixCell(
                                value = entries.getOrNull(index).orEmpty(),
                                active = activeIndex == index,
                                onClick = { onCellFocus(index) },
                                modifier = Modifier.size(cellSize),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MatrixCell(
    value: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(if (active) PixelClay else PixelButtonSurface, RectangleShape)
            .border(
                BorderStroke(2.dp, if (active) PixelAmber else PixelLine),
                RectangleShape,
            )
            .clickable(onClick = onClick)
            .padding(2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = value.ifBlank { "0" },
            color = if (value.isBlank()) PixelSage else if (active) PixelInk else PixelPaper,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun MatrixResultPanel(
    label: String,
    rows: Int,
    columns: Int,
    entries: List<String>,
    hasError: Boolean,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .background(PixelPanel, RectangleShape)
            .border(BorderStroke(2.dp, PixelLine), RectangleShape)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            color = if (hasError) PixelError else PixelInk,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.sp,
        )
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            val density = LocalDensity.current
            val viewportHeightPx = with(density) { maxHeight.toPx() }
            val contentHeightPx = viewportHeightPx + scrollState.maxValue
            val showScrollbar = scrollState.maxValue > 0 && contentHeightPx > 0f
            val scrollbarPadding = if (showScrollbar) 8.dp else 0.dp
            val thumbHeight = if (showScrollbar) {
                with(density) {
                    ((viewportHeightPx * viewportHeightPx) / contentHeightPx)
                        .toDp()
                        .coerceAtLeast(16.dp)
                }
            } else {
                0.dp
            }
            val thumbOffset = if (showScrollbar) {
                val trackHeightPx = with(density) { (maxHeight - thumbHeight).toPx() }
                val offsetPx = trackHeightPx * scrollState.value / scrollState.maxValue
                with(density) { offsetPx.toDp() }
            } else {
                0.dp
            }

            ResultGrid(
                rows = rows,
                columns = columns,
                entries = entries,
                hasError = hasError,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = scrollbarPadding)
                    .verticalScroll(scrollState),
            )
            if (showScrollbar) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .width(2.dp)
                        .background(PixelLine, RectangleShape),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(y = thumbOffset)
                        .height(thumbHeight)
                        .width(2.dp)
                        .background(PixelClayDark, RectangleShape),
                )
            }
        }
    }
}

@Composable
private fun ResultGrid(
    rows: Int,
    columns: Int,
    entries: List<String>,
    hasError: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        repeat(rows) { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                repeat(columns) { column ->
                    val index = row * columns + column
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(PixelInk, RectangleShape)
                            .border(BorderStroke(1.dp, PixelLine), RectangleShape)
                            .padding(vertical = 5.dp, horizontal = 2.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = entries.getOrNull(index) ?: "-",
                            color = when {
                                hasError -> PixelError
                                entries.isEmpty() -> PixelSage
                                else -> PixelPaper
                            },
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StandardKeyGrid(
    onPress: (String) -> Unit,
    scienceOpen: Boolean,
    angleUnit: CalculatorAngleUnit,
    modifier: Modifier = Modifier,
) {
    val scienceRows = listOf(
        listOf(
            CalculatorSymbols.SQRT,
            CalculatorSymbols.PI,
            CalculatorSymbols.POWER,
            CalculatorSymbols.SQUARE,
        ),
        listOf(angleUnit.label, "sin", "cos", "tan"),
        listOf(
            CalculatorSymbols.INVERSE,
            "e",
            CalculatorSymbols.NATURAL_LOG,
            CalculatorSymbols.COMMON_LOG,
        ),
    )
    val standardRows = listOf(
        listOf(
            CalculatorSymbols.CLEAR_ALL,
            CalculatorSymbols.PARENS,
            CalculatorSymbols.PERCENT,
            CalculatorSymbols.DIVIDE,
        ),
        listOf("7", "8", "9", CalculatorSymbols.MULTIPLY),
        listOf("4", "5", "6", "-"),
        listOf("1", "2", "3", "+"),
        listOf("0", CalculatorSymbols.DOT, CalculatorSymbols.BACKSPACE, "="),
    )
    val rows = if (scienceOpen) scienceRows + standardRows else standardRows

    PixelKeyRows(rows = rows, onPress = onPress, modifier = modifier)
}

@Composable
private fun MatrixKeyGrid(
    onPress: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = listOf(
        listOf("7", "8", "9", "NEXT"),
        listOf("4", "5", "6", CalculatorSymbols.PLUS_MINUS),
        listOf("1", "2", "3", CalculatorSymbols.BACKSPACE),
        listOf("C", "0", "A/B", "="),
    )

    PixelKeyRows(rows = rows, onPress = onPress, modifier = modifier)
}

@Composable
private fun PixelKeyRows(
    rows: List<List<String>>,
    onPress: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { key ->
                    PixelKey(
                        label = key,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        onClick = { onPress(key) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PixelKey(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val isOperator = label == CalculatorSymbols.SCI || label in setOf(
        "+",
        "-",
        CalculatorSymbols.MULTIPLY,
        CalculatorSymbols.DIVIDE,
        CalculatorSymbols.PARENS,
        CalculatorSymbols.PERCENT,
        CalculatorSymbols.POWER,
        CalculatorSymbols.SQUARE,
        CalculatorSymbols.SQRT,
        CalculatorSymbols.INVERSE,
        CalculatorSymbols.NATURAL_LOG,
        CalculatorSymbols.COMMON_LOG,
        CalculatorSymbols.PI,
        CalculatorSymbols.DOT,
        "e",
        "sin",
        "cos",
        "tan",
        CalculatorAngleUnit.RADIAN.label,
        CalculatorAngleUnit.DEGREE.label,
        "NEXT",
        "A/B",
        CalculatorSymbols.PLUS_MINUS,
        CalculatorSymbols.BACKSPACE,
    )
    val isCommit = label == "="
    val isClear = label == "C" || label == CalculatorSymbols.CLEAR_ALL
    val background = when {
        isCommit -> PixelClay
        isOperator -> PixelButtonAlt
        isClear -> PixelClayDark
        else -> PixelButtonSurface
    }
    val contentColor = when {
        isCommit -> PixelInk
        isClear -> PixelPaper
        isOperator -> PixelAmber
        else -> PixelPaper
    }

    Box(
        modifier = modifier
            .background(background, RectangleShape)
            .border(BorderStroke(2.dp, keyBorderColor(label)), RectangleShape)
            .clickable(onClick = onClick)
            .padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = contentColor,
            fontFamily = FontFamily.Monospace,
            fontSize = when {
                label.length > 3 -> 13.sp
                label.length > 1 -> 18.sp
                else -> 26.sp
            },
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun keyBorderColor(label: String): Color =
    when {
        label == CalculatorSymbols.SCI -> PixelClay
        label == "=" -> PixelAmber
        label == "C" || label == CalculatorSymbols.CLEAR_ALL -> PixelError
        label in setOf(
            "+",
            "-",
            CalculatorSymbols.MULTIPLY,
            CalculatorSymbols.DIVIDE,
            CalculatorSymbols.PARENS,
            CalculatorSymbols.PERCENT,
            CalculatorSymbols.POWER,
            CalculatorSymbols.SQUARE,
            CalculatorSymbols.SQRT,
            CalculatorSymbols.INVERSE,
            CalculatorSymbols.NATURAL_LOG,
            CalculatorSymbols.COMMON_LOG,
            CalculatorSymbols.PI,
            CalculatorSymbols.DOT,
            "e",
            "sin",
            "cos",
            "tan",
            CalculatorAngleUnit.RADIAN.label,
            CalculatorAngleUnit.DEGREE.label,
            "NEXT",
            "A/B",
            CalculatorSymbols.PLUS_MINUS,
            CalculatorSymbols.BACKSPACE,
        ) -> PixelClay
        else -> PixelLine
    }

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun PixelCalcPreview() {
    PixelCalcTheme {
        PixelCalcApp()
    }
}
