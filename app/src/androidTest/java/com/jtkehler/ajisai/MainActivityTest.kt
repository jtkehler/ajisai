package com.jtkehler.ajisai

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

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
}
