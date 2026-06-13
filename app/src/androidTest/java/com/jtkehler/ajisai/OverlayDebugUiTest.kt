package com.jtkehler.ajisai

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jtkehler.ajisai.overlay.OverlayClient
import com.jtkehler.ajisai.overlay.OverlayDependencies
import com.jtkehler.ajisai.overlay.OverlayPermissionLauncher
import com.jtkehler.ajisai.overlay.OverlayPermissionState
import com.jtkehler.ajisai.overlay.OverlayServiceState
import com.jtkehler.ajisai.overlay.OverlayState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OverlayDebugUiTest {
    private lateinit var fakeClient: FakeOverlayClient
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun setUp() {
        fakeClient = FakeOverlayClient()
        OverlayDependencies.setClientFactoryForTests { _: Context -> fakeClient }
        OverlayDependencies.setPermissionLauncherFactoryForTests { _, onReturn ->
            OverlayPermissionLauncher {
                fakeClient.grantPermission()
                onReturn()
            }
        }
    }

    @After
    fun tearDown() {
        scenario?.close()
        OverlayDependencies.resetForTests()
    }

    @Test
    fun permissionFlowUpdatesUiWithoutOpeningSystemSettings() {
        scenario = ActivityScenario.launch(MainActivity::class.java)

        onView(withId(R.id.overlay_permission_status))
            .perform(scrollTo())
            .check(matches(withText(R.string.overlay_permission_missing)))
        onView(withId(R.id.start_overlay_button)).check(matches(not(isEnabled())))

        onView(withId(R.id.request_overlay_permission_button)).perform(click())

        onView(withId(R.id.overlay_permission_status))
            .check(matches(withText(R.string.overlay_permission_granted)))
        onView(withId(R.id.start_overlay_button)).check(matches(isEnabled()))
    }

    @Test
    fun grantedOverlayCanStartAndStop() {
        fakeClient.grantPermission()
        scenario = ActivityScenario.launch(MainActivity::class.java)

        onView(withId(R.id.start_overlay_button)).perform(scrollTo(), click())
        onView(withId(R.id.overlay_service_status))
            .check(matches(withText(R.string.overlay_service_running)))
        onView(withId(R.id.stop_overlay_button)).check(matches(isEnabled())).perform(click())
        onView(withId(R.id.overlay_service_status))
            .check(matches(withText(R.string.overlay_service_stopped)))
        assertEquals(1, fakeClient.startRequests)
        assertEquals(1, fakeClient.stopRequests)
    }

    private class FakeOverlayClient : OverlayClient {
        private val mutableState = MutableStateFlow(OverlayState())
        override val state: StateFlow<OverlayState> = mutableState
        var startRequests = 0
        var stopRequests = 0

        fun grantPermission() {
            mutableState.value = mutableState.value.copy(
                permission = OverlayPermissionState.GRANTED,
                error = null,
            )
        }

        override fun refreshPermission() = Unit

        override fun startOverlay() {
            startRequests += 1
            mutableState.value = mutableState.value.copy(service = OverlayServiceState.RUNNING)
        }

        override fun stopOverlay() {
            stopRequests += 1
            mutableState.value = mutableState.value.copy(service = OverlayServiceState.STOPPED)
        }
    }
}
