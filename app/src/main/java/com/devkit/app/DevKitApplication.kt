package com.devkit.app

import android.app.Application
import com.devkit.app.data.local.DevKitDatabase
import com.devkit.app.data.prefs.SettingsRepository
import com.devkit.app.data.repo.DevKitRepository

/**
 * Manual dependency container. Kept intentionally small: the app has one
 * database, one settings store and one repository, so a DI framework would add
 * size and startup cost without benefit.
 */
class DevKitApplication : Application() {

    val database: DevKitDatabase by lazy { DevKitDatabase.get(this) }
    val repository: DevKitRepository by lazy { DevKitRepository(database) }
    val settings: SettingsRepository by lazy { SettingsRepository(this) }
}
