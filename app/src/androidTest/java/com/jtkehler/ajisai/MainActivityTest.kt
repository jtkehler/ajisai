package com.jtkehler.ajisai

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jtkehler.ajisai.dictionary.DictionaryDependencies
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun clearImportedDictionaries() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val metadata = DictionaryDependencies.metadataRepository(context)
        val storage = DictionaryDependencies.storage(context)
        metadata.getAll().forEach { dictionary ->
            storage.delete(dictionary)
            metadata.delete(dictionary.id)
        }
    }

    @Test
    fun mainActivityLaunchesAndRenders() {
        onView(withId(R.id.main_title))
            .check(matches(isDisplayed()))
            .check(matches(withText(R.string.main_title)))
    }

    @Test
    fun settingsEntryPointOpensSettings() {
        onView(withId(R.id.settings_button)).perform(click())

        onView(withId(R.id.settings_title))
            .check(matches(isDisplayed()))
            .check(matches(withText(R.string.settings_title)))
    }

    @Test
    fun settingsNavigatesToDictionaries() {
        onView(withId(R.id.settings_button)).perform(click())
        onView(withId(R.id.dictionaries_button)).perform(click())

        onView(withId(R.id.dictionaries_title))
            .check(matches(isDisplayed()))
            .check(matches(withText(R.string.dictionaries_title)))
        onView(withId(R.id.dictionaries_empty_title))
            .check(matches(isDisplayed()))
            .check(matches(withText(R.string.no_dictionaries_imported)))
        onView(withId(R.id.import_dictionary_button))
            .check(matches(isDisplayed()))
            .check(matches(withText(R.string.import_dictionary)))
    }
}
