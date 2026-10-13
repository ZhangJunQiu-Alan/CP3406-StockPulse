package au.edu.jcu.assessment.utilityapp.ui

import au.edu.jcu.assessment.utilityapp.data.model.AppSettings
import au.edu.jcu.assessment.utilityapp.data.model.DisplayCurrency
import au.edu.jcu.assessment.utilityapp.data.model.RefreshInterval
import au.edu.jcu.assessment.utilityapp.data.model.SymbolMatch
import au.edu.jcu.assessment.utilityapp.data.repository.InMemorySettingsRepository
import au.edu.jcu.assessment.utilityapp.fakes.FakeStockRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StockViewModelTest {

    private val repository = FakeStockRepository()
    // Manual refresh keeps the test deterministic: no timer is running in the background.
    private val settingsRepository = InMemorySettingsRepository(
        AppSettings(
            watchlist = listOf("AAPL", "TSLA"),
            currency = DisplayCurrency.USD,
            refreshInterval = RefreshInterval.MANUAL,
        )
    )
    private lateinit var viewModel: StockViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = StockViewModel(repository, settingsRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** uiState only fetches while someone is collecting it, like the real screen does. */
    private fun TestScope.startCollecting() {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
    }

    @Test
    fun initialLoad_showsWatchlist() = runTest {
        startCollecting()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(listOf("AAPL", "TSLA"), state.content?.rows?.map { it.symbol })
    }

    @Test
    fun displayOnlySettings_doNotTriggerNetworkRequests() = runTest {
        startCollecting()
        assertEquals(1, repository.snapshotRequests)

        viewModel.setCurrency(DisplayCurrency.SGD)
        viewModel.setBigMoveThreshold(1f)

        assertEquals(1, repository.snapshotRequests)
        assertEquals(125.0, viewModel.uiState.value.content!!.rows.first().price, 1e-9)
    }

    @Test
    fun refresh_requestsNewData() = runTest {
        startCollecting()

        viewModel.refresh()

        assertEquals(2, repository.snapshotRequests)
    }

    @Test
    fun failedRefresh_keepsLastGoodDataAndReportsError() = runTest {
        startCollecting()
        repository.shouldFail = true

        viewModel.refresh()

        val state = viewModel.uiState.value
        assertEquals(LoadError.NETWORK, state.error)
        assertNotNull(state.content)
    }

    @Test
    fun failedFirstLoad_hasNoContent() = runTest {
        repository.shouldFail = true
        startCollecting()

        assertNull(viewModel.uiState.value.content)
        assertEquals(LoadError.NETWORK, viewModel.uiState.value.error)
    }

    @Test
    fun addSymbol_validSymbolIsAddedAfterLookup() = runTest {
        viewModel.addSymbol(" amzn ")

        assertEquals(AddSymbolState.Added(SymbolMatch("AMZN", "Amazon.com Inc")), viewModel.addSymbolState.value)
        assertEquals(listOf("AAPL", "TSLA", "AMZN"), viewModel.settings.value.watchlist)
    }

    @Test
    fun addSymbol_rejectsInvalidDuplicateAndUnknownSymbols() = runTest {
        viewModel.addSymbol("12$")
        assertEquals(AddSymbolState.Rejected(AddSymbolError.INVALID_FORMAT), viewModel.addSymbolState.value)

        viewModel.addSymbol("aapl")
        assertEquals(AddSymbolState.Rejected(AddSymbolError.ALREADY_ADDED), viewModel.addSymbolState.value)

        viewModel.addSymbol("ZZZZQ")
        assertEquals(AddSymbolState.Rejected(AddSymbolError.NOT_FOUND), viewModel.addSymbolState.value)
        assertEquals(listOf("AAPL", "TSLA"), viewModel.settings.value.watchlist)
    }

    @Test
    fun addSymbol_rejectsWhenWatchlistFull() = runTest {
        settingsRepository.update {
            it.copy(watchlist = (1..AppSettings.MAX_WATCHLIST_SIZE).map { n -> "S$n" })
        }

        viewModel.addSymbol("AMZN")

        assertEquals(AddSymbolState.Rejected(AddSymbolError.LIST_FULL), viewModel.addSymbolState.value)
    }
}
