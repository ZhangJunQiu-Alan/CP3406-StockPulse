package au.edu.jcu.assessment.utilityapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.edu.jcu.assessment.utilityapp.R
import au.edu.jcu.assessment.utilityapp.data.model.AppSettings
import au.edu.jcu.assessment.utilityapp.data.model.ChartRange
import au.edu.jcu.assessment.utilityapp.data.model.DisplayCurrency
import au.edu.jcu.assessment.utilityapp.data.model.RefreshInterval
import au.edu.jcu.assessment.utilityapp.data.model.SortOrder
import au.edu.jcu.assessment.utilityapp.ui.AddSymbolError
import au.edu.jcu.assessment.utilityapp.ui.AddSymbolState
import au.edu.jcu.assessment.utilityapp.ui.StockViewModel
import au.edu.jcu.assessment.utilityapp.ui.format.StockFormatters
import au.edu.jcu.assessment.utilityapp.ui.theme.StockPulseTheme
import kotlin.math.roundToInt

/** Settings screen: every option here changes what the Utility screen shows. */
@Composable
fun SettingsScreen(
    viewModel: StockViewModel,
    modifier: Modifier = Modifier,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val addSymbolState by viewModel.addSymbolState.collectAsStateWithLifecycle()

    SettingsScreenContent(
        settings = settings,
        addSymbolState = addSymbolState,
        onAddSymbol = viewModel::addSymbol,
        onSymbolInputChanged = viewModel::clearAddSymbolState,
        onRemoveSymbol = viewModel::removeSymbol,
        onCurrencySelected = viewModel::setCurrency,
        onChartRangeSelected = viewModel::setChartRange,
        onSortOrderSelected = viewModel::setSortOrder,
        onThresholdChanged = viewModel::setBigMoveThreshold,
        onRefreshIntervalSelected = viewModel::setRefreshInterval,
        modifier = modifier,
    )
}

@Composable
fun SettingsScreenContent(
    settings: AppSettings,
    addSymbolState: AddSymbolState,
    onAddSymbol: (String) -> Unit,
    onSymbolInputChanged: () -> Unit,
    onRemoveSymbol: (String) -> Unit,
    onCurrencySelected: (DisplayCurrency) -> Unit,
    onChartRangeSelected: (ChartRange) -> Unit,
    onSortOrderSelected: (SortOrder) -> Unit,
    onThresholdChanged: (Float) -> Unit,
    onRefreshIntervalSelected: (RefreshInterval) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        SettingsSection(
            title = stringResource(
                R.string.settings_watchlist_title,
                settings.watchlist.size,
                AppSettings.MAX_WATCHLIST_SIZE,
            ),
            description = stringResource(R.string.settings_watchlist_body),
        ) {
            WatchlistChips(symbols = settings.watchlist, onRemove = onRemoveSymbol)
            AddSymbolField(
                state = addSymbolState,
                onAdd = onAddSymbol,
                onInputChanged = onSymbolInputChanged,
            )
        }
        HorizontalDivider()

        SettingsSection(
            title = stringResource(R.string.settings_currency_title),
            description = stringResource(R.string.settings_currency_body),
        ) {
            SegmentedSelector(
                options = DisplayCurrency.entries,
                selected = settings.currency,
                onSelect = onCurrencySelected,
                label = { it.code },
            )
        }

        SettingsSection(title = stringResource(R.string.settings_chart_title)) {
            SegmentedSelector(
                options = ChartRange.entries,
                selected = settings.chartRange,
                onSelect = onChartRangeSelected,
                label = { it.label },
            )
        }

        SettingsSection(title = stringResource(R.string.settings_sort_title)) {
            SegmentedSelector(
                options = SortOrder.entries,
                selected = settings.sortOrder,
                onSelect = onSortOrderSelected,
                label = { it.label },
            )
        }

        SettingsSection(
            title = stringResource(R.string.settings_alert_title),
            description = stringResource(
                R.string.settings_alert_body,
                StockFormatters.threshold(settings.bigMoveThresholdPercent),
            ),
        ) {
            val min = AppSettings.MIN_BIG_MOVE_PERCENT
            val max = AppSettings.MAX_BIG_MOVE_PERCENT
            Slider(
                value = settings.bigMoveThresholdPercent,
                onValueChange = { onThresholdChanged(it.roundToInt().toFloat()) },
                valueRange = min..max,
                steps = (max - min).toInt() - 1, // one stop per whole percent
            )
        }

        SettingsSection(
            title = stringResource(R.string.settings_refresh_title),
            description = stringResource(R.string.settings_refresh_body),
        ) {
            SegmentedSelector(
                options = RefreshInterval.entries,
                selected = settings.refreshInterval,
                onSelect = onRefreshIntervalSelected,
                label = { it.label },
            )
        }

        Text(
            text = stringResource(R.string.settings_not_persistent),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Consistent title + optional description + controls layout for each setting. */
@Composable
private fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    content: @Composable () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.semantics { heading() },
        )
        if (description != null) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        content()
    }
}

/** A reusable single-choice row of Material 3 segmented buttons for any enum-like option. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> SegmentedSelector(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
            ) {
                Text(label(option), maxLines = 1)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WatchlistChips(
    symbols: List<String>,
    onRemove: (String) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        symbols.forEach { symbol ->
            InputChip(
                selected = false,
                onClick = { onRemove(symbol) },
                label = { Text(symbol) },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.settings_remove_symbol, symbol),
                        modifier = Modifier.size(InputChipDefaults.IconSize),
                    )
                },
            )
        }
    }
}

@Composable
private fun AddSymbolField(
    state: AddSymbolState,
    onAdd: (String) -> Unit,
    onInputChanged: () -> Unit,
) {
    var input by rememberSaveable { mutableStateOf("") }

    // Clear the field once a symbol has been added successfully.
    LaunchedEffect(state) {
        if (state is AddSymbolState.Added) input = ""
    }

    val submit = { if (input.isNotBlank()) onAdd(input) }
    val isChecking = state is AddSymbolState.Checking

    OutlinedTextField(
        value = input,
        onValueChange = {
            // Don't rewrite the text here (e.g. uppercase()): changing it while the keyboard is
            // still composing makes fast typing drop characters. It is shown in capitals by
            // [UppercaseTransformation] and normalised by the ViewModel instead.
            input = it
            onInputChanged()
        },
        visualTransformation = UppercaseTransformation,
        label = { Text(stringResource(R.string.settings_add_symbol_label)) },
        placeholder = { Text(stringResource(R.string.settings_add_symbol_placeholder)) },
        singleLine = true,
        isError = state is AddSymbolState.Rejected,
        supportingText = addSymbolSupportingText(state)?.let { message -> { Text(message) } },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters,
            autoCorrectEnabled = false,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { submit() }),
        trailingIcon = {
            if (isChecking) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                IconButton(onClick = submit, enabled = input.isNotBlank()) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.settings_add_symbol_action))
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Displays text in capitals without changing the underlying value (same length, so identity offsets). */
private object UppercaseTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString) =
        TransformedText(AnnotatedString(text.text.uppercase()), OffsetMapping.Identity)
}

@Composable
private fun addSymbolSupportingText(state: AddSymbolState): String? = when (state) {
    AddSymbolState.Idle -> null
    AddSymbolState.Checking -> stringResource(R.string.settings_add_checking)
    is AddSymbolState.Added ->
        stringResource(R.string.settings_add_success, state.match.symbol, state.match.companyName)
    is AddSymbolState.Rejected -> stringResource(
        when (state.reason) {
            AddSymbolError.INVALID_FORMAT -> R.string.settings_add_invalid
            AddSymbolError.ALREADY_ADDED -> R.string.settings_add_duplicate
            AddSymbolError.LIST_FULL -> R.string.settings_add_full
            AddSymbolError.NOT_FOUND -> R.string.settings_add_not_found
            AddSymbolError.NETWORK -> R.string.settings_add_network
        }
    )
}

@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun SettingsScreenPreview() {
    StockPulseTheme {
        SettingsScreenContent(
            settings = AppSettings(),
            addSymbolState = AddSymbolState.Idle,
            onAddSymbol = {},
            onSymbolInputChanged = {},
            onRemoveSymbol = {},
            onCurrencySelected = {},
            onChartRangeSelected = {},
            onSortOrderSelected = {},
            onThresholdChanged = {},
            onRefreshIntervalSelected = {},
        )
    }
}
