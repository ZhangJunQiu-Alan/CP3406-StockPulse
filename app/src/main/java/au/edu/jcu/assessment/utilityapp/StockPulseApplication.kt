package au.edu.jcu.assessment.utilityapp

import android.app.Application
import au.edu.jcu.assessment.utilityapp.di.AppContainer
import au.edu.jcu.assessment.utilityapp.di.DefaultAppContainer

/** Creates the [AppContainer] once per process so ViewModels can pull dependencies from it. */
class StockPulseApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer()
    }
}
