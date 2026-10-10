package com.devkit.app

import androidx.test.core.app.ActivityScenario
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.devkit.app.ui.MainActivity

/**
 * Smoke test that the app actually launches: the Application, theme, navigation
 * graph, and Compose content all build without throwing. Runs on the JVM via
 * Robolectric so it works in CI without an emulator.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = DevKitApplication::class)
class MainActivityLaunchTest {

    @Test
    fun `application container wires database repository and settings`() {
        val app = org.robolectric.RuntimeEnvironment.getApplication() as DevKitApplication
        assertNotNull(app.repository)
        assertNotNull(app.settings)
        assertNotNull(app.database)
    }

    @Test
    fun `manifest launcher activity is resolvable`() {
        val context = org.robolectric.RuntimeEnvironment.getApplication()
        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN)
            .addCategory(android.content.Intent.CATEGORY_LAUNCHER)
            .setPackage(context.packageName)
        val resolved = context.packageManager.queryIntentActivities(intent, 0)
        assertTrue("launcher activity not registered in the manifest", resolved.isNotEmpty())
    }

    @Test
    fun `main activity launches and reaches resumed state`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> assertNotNull(activity) }
        }
    }
}
