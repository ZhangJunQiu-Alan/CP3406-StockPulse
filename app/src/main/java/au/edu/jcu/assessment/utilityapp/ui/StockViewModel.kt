package au.edu.jcu.assessment.utilityapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import au.edu.jcu.assessment.utilityapp.StockPulseApplication
import au.edu.jcu.assessment.utilityapp.data.model.AppSettings
import au.edu.jcu.assessment.utilityapp.data.model.ChartRange
import au.edu.jcu.assessment.utilityapp.data.model.DisplayCurrency
import au.edu.jcu.assessment.utilityapp.data.model.RefreshInterval
import au.edu.jcu.assessment.utilityapp.data.model.SortOrder
import au.edu.jcu.assessment.utilityapp.data.model.WatchlistSnapshot
import au.edu.jcu.assessment.utilityapp.data.repository.SettingsRepository
import au.edu.jcu.assessment.utilityapp.data.repository.StockRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

/**
 * Shared by the Utility and Settings screens so that a settings change is reflected on the
 * main screen immediately.
 *
 * Data flow: settings → (network-relevant subset) → periodic fetch → combined with the
 * display-only settings → [uiState]. Polling only runs while the UI is collecting [uiState],
 * so it pauses automatically when the app is in the background or the Settings tab is open.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StockViewModel(
    private val stockRepository: StockRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings

    /** The subset of settings that requires new data from the network when it changes. */
    private data class FetchRequest(
        val symbols: List<String>,
        val chartRange: ChartRange,
        val refreshInterval: RefreshInterval,
    )

    private enum class RefreshTrigger { INITIAL, USER, TIMER }

    private data class FetchState(
        val snapshot: WatchlistSnapshot?,
        val isLoading: Boolean,
        val isUserRefreshing: Boolean,
        val error: LoadError?,
    )

    private val userRefreshRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Kept across fetches so the old data stays visible while new data loads. */
    private var latestSnapshot: WatchlistSnapshot? = null

    val uiState: StateFlow<WatchlistUiState> = combine(
        settings
            .map { FetchRequest(it.watchlist, it.chartRange, it.refreshInterval) }
            .distinctUntilChanged()
            .flatMapLatest(::fetchUpdates),
        settings,
    ) { fetch, currentSettings ->
        WatchlistUiState(
            content = fetch.snapshot?.let { WatchlistUiMapper.toContent(it, currentSettings) },
            isLoading = fetch.isLoading,
            isUserRefreshing = fetch.isUserRefreshing,
            error = fetch.error,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = WatchlistUiState(isLoading = true),
    )

    private val _addSymbolState = MutableStateFlow<AddSymbolState>(AddSymbolState.Idle)
    val addSymbolState: StateFlow<AddSymbolState> = _addSymbolState.asStateFlow()
    private var addSymbolJob: Job? = null

    fun refresh() {
        userRefreshRequests.tryEmit(Unit)
    }

    // region Settings actions

    fun setCurrency(currency: DisplayCurrency) = settingsRepository.update { it.copy(currency = currency) }

    fun setChartRange(range: ChartRange) = settingsRepository.update { it.copy(chartRange = range) }

    fun setSortOrder(order: SortOrder) = settingsRepository.update { it.copy(sortOrder = order) }

    fun setRefreshInterval(interval: RefreshInterval) =
        settingsRepository.update { it.copy(refreshInterval = interval) }

    fun setBigMoveThreshold(percent: Float) = settingsRepository.update {
        it.copy(
            bigMoveThresholdPercent = percent.coerceIn(
                AppSettings.MIN_BIG_MOVE_PERCENT,
                AppSettings.MAX_BIG_MOVE_PERCENT,
            ),
        )
    }

    fun removeSymbol(symbol: String) = settingsRepository.update { it.copy(watchlist = it.watchlist - symbol) }

    /** Validates [input] locally, then confirms with Finnhub that the stock exists before adding it. */
    fun addSymbol(input: String) {
        val symbol = input.trim().uppercase()
        val watchlist = settings.value.watchlist
        val localError = when {
            !SYMBOL_PATTERN.matches(symbol) -> AddSymbolError.INVALID_FORMAT
            symbol in watchlist -> AddSymbolError.ALREADY_ADDED
            watchlist.size >= AppSettings.MAX_WATCHLIST_SIZE -> AddSymbolError.LIST_FULL
            else -> null
        }
        if (localError != null) {
            _addSymbolState.value = AddSymbolState.Rejected(localError)
            return
        }

        addSymbolJob?.cancel()
        addSymbolJob = viewModelScope.launch {
            _addSymbolState.value = AddSymbolState.Checking
            _addSymbolState.value = try {
                val match = stockRepository.findSymbol(symbol)
                if (match == null) {
                    AddSymbolState.Rejected(AddSymbolError.NOT_FOUND)
                } else {
                    settingsRepository.update { current ->
                        val canAdd = match.symbol !in current.watchlist &&
                            current.watchlist.size < AppSettings.MAX_WATCHLIST_SIZE
                        if (canAdd) current.copy(watchlist = current.watchlist + match.symbol) else current
                    }
                    AddSymbolState.Added(match)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AddSymbolState.Rejected(AddSymbolError.NETWORK)
            }
        }
    }

    fun clearAddSymbolState() {
        _addSymbolState.value = AddSymbolState.Idle
    }

    // endregion

    /** Fetches once immediately, then again on every timer tick or user refresh. */
    private fun fetchUpdates(request: FetchRequest): Flow<FetchState> = flow {
        refreshTriggers(request.refreshInterval).collect { trigger ->
            val isUserRefresh = trigger == RefreshTrigger.USER
            emit(FetchState(latestSnapshot, isLoading = true, isUserRefreshing = isUserRefresh, error = null))
            val error = try {
                latestSnapshot = stockRepository.getWatchlistSnapshot(request.symbols, request.chartRange)
                null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                e.toLoadError()
            }
            emit(FetchState(latestSnapshot, isLoading = false, isUserRefreshing = false, error = error))
        }
    }

    /** Conflated so that triggers arriving mid-fetch collapse into a single follow-up fetch. */
    private fun refreshTriggers(interval: RefreshInterval): Flow<RefreshTrigger> = merge(
        flowOf(RefreshTrigger.INITIAL),
        userRefreshRequests.map { RefreshTrigger.USER },
        timer(interval.periodMillis),
    ).conflate()

    private fun timer(periodMillis: Long?): Flow<RefreshTrigger> =
        if (periodMillis == null) {
            emptyFlow()
        } else {
            flow {
                while (true) {
                    delay(periodMillis)
                    emit(RefreshTrigger.TIMER)
                }
            }
        }

    private fun Exception.toLoadError(): LoadError = when (this) {
        is HttpException -> when (code()) {
            401, 403 -> LoadError.INVALID_API_KEY
            429 -> LoadError.RATE_LIMITED
            else -> LoadError.UNKNOWN
        }
        is IOException -> LoadError.NETWORK
        else -> LoadError.UNKNOWN
    }

    companion object {
        /** Keeps polling alive briefly across configuration changes such as rotation. */
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        /** US tickers: a letter followed by letters, digits, '.' or '-' (e.g. BRK.B). */
        private val SYMBOL_PATTERN = Regex("^[A-Z][A-Z0-9.\\-]{0,9}$")

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as StockPulseApplication
                StockViewModel(
                    stockRepository = application.container.stockRepository,
                    settingsRepository = application.container.settingsRepository,
                )
            }
        }
    }
}
