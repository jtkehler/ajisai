package com.jtkehler.ajisai

import android.app.Activity
import android.net.Uri
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.jtkehler.ajisai.dictionary.DictionaryDependencies
import com.jtkehler.ajisai.dictionary.DictionaryImportRequest
import com.jtkehler.ajisai.dictionary.DictionaryImportResult
import com.jtkehler.ajisai.settings.DictionariesActivity
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RealJmdictImportE2ETest {
    @Test
    fun importsRealJmdictAndRendersPersistedMetadata() {
        requireRealJmdictOptIn()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val archive = File(context.filesDir, REAL_JMDICT_RELATIVE_PATH)
        assertTrue("Missing app-private JMdict fixture at ${archive.absolutePath}", archive.isFile)

        val result = runBlocking {
            withContext(Dispatchers.IO) {
                DictionaryDependencies.importService(context).importDictionary(
                    DictionaryImportRequest(
                        sourceUri = Uri.fromFile(archive).toString(),
                        displayName = archive.name,
                    ),
                )
            }
        }
        assertTrue(
            "Real JMdict import failed: $result",
            result is DictionaryImportResult.Success,
        )

        val dictionaries = DictionaryDependencies.metadataRepository(context).getAll()
        assertTrue("JMdict metadata was not persisted", dictionaries.any { it.title.contains("JMdict", ignoreCase = true) })

        ActivityScenario.launch(DictionariesActivity::class.java).use {
            onView(withId(R.id.dictionary_list_container))
                .check(matches(isDisplayed()))
                .check(matches(hasDescendant(withText(dictionaries.first().title))))
            assertResumedActivity<DictionariesActivity>()
        }
    }
}

@RunWith(AndroidJUnit4::class)
class PostRestartDictionariesRenderE2ETest {
    @Test
    fun reopenedAppKeepsDictionariesActivityVisible() {
        requireRealJmdictOptIn()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dictionaries = DictionaryDependencies.metadataRepository(context).getAll()
        assertTrue("JMdict metadata did not survive restart", dictionaries.isNotEmpty())

        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.settings_button)).perform(click())
            onView(withId(R.id.dictionaries_button)).perform(click())
            onView(withId(R.id.dictionaries_title))
                .check(matches(isDisplayed()))
                .check(matches(withText(R.string.dictionaries_title)))
            onView(withId(R.id.dictionary_list_container))
                .check(matches(hasDescendant(withText(dictionaries.first().title))))
            assertResumedActivity<DictionariesActivity>()
        }
    }
}

private fun requireRealJmdictOptIn() {
    val arguments = InstrumentationRegistry.getArguments()
    assumeTrue(
        "Real JMdict E2E is opt-in; run scripts/e2e-real-jmdict.sh",
        arguments.getString(REAL_JMDICT_ARGUMENT) == "true",
    )
}

private inline fun <reified T : Activity> assertResumedActivity() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    var resumedActivities = emptyList<Activity>()
    instrumentation.runOnMainSync {
        resumedActivities = ActivityLifecycleMonitorRegistry.getInstance()
            .getActivitiesInStage(Stage.RESUMED)
            .toList()
    }
    assertEquals("Expected exactly one resumed activity", 1, resumedActivities.size)
    assertTrue(
        "Expected ${T::class.java.name}, but resumed ${resumedActivities.single()::class.java.name}",
        resumedActivities.single() is T,
    )
}

private const val REAL_JMDICT_ARGUMENT = "ajisaiRealJmdict"
private const val REAL_JMDICT_RELATIVE_PATH = "e2e/JMdict_english_with_examples.zip"
