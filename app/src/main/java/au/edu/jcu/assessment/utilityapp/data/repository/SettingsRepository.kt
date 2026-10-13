package au.edu.jcu.assessment.utilityapp.data.repository

import au.edu.jcu.assessment.utilityapp.data.model.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Holds the user's current [AppSettings] and notifies observers when they change. */
interface SettingsRepository {
    val settings: StateFlow<AppSettings>
    fun update(transform: (AppSettings) -> AppSettings)
}

/**
 * In-memory implementation: persistence is not required for A1. Because the rest of the app
 * depends only on the interface, a DataStore-backed version could be swapped in later.
 */
class InMemorySettingsRepository(initial: AppSettings = AppSettings()) : SettingsRepository {

    private val _settings = MutableStateFlow(initial)
    override val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    override fun update(transform: (AppSettings) -> AppSettings) = _settings.update(transform)
}
