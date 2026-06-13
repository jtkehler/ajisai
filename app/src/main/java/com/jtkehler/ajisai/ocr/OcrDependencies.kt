package com.jtkehler.ajisai.ocr

import android.content.Context
import com.jtkehler.ajisai.capture.CaptureDependencies
import com.jtkehler.ajisai.ocrbox.OcrBoxDependencies
import kotlinx.coroutines.CoroutineScope

object OcrDependencies {
    private val productionEngineFactory: (Context) -> OcrEngine = { context ->
        GoogleLensOcrEngine(debugLogger = FileOcrDebugLogger(context.applicationContext))
    }
    private val productionRunnerFactory: (Context, CoroutineScope) -> OcrRunner = { context, scope ->
        DefaultOcrRunner(
            scope = scope,
            captureClient = CaptureDependencies.client(context),
            ocrBoxRepository = OcrBoxDependencies.repository(context),
            cropper = OcrBoxDependencies.cropper(),
            ocrEngine = engine(context),
            optionsProvider = {
                OcrOptions(
                    saveDebugArtifacts = context.getSharedPreferences(
                        PREFERENCES_NAME,
                        Context.MODE_PRIVATE,
                    ).getBoolean(KEY_SAVE_DEBUG_ARTIFACTS, false),
                )
            },
        )
    }

    @Volatile
    private var engineFactory = productionEngineFactory

    @Volatile
    private var runnerFactory = productionRunnerFactory

    fun engine(context: Context): OcrEngine = engineFactory(context.applicationContext)

    fun runner(context: Context, scope: CoroutineScope): OcrRunner =
        runnerFactory(context.applicationContext, scope)

    fun setEngineFactoryForTests(factory: (Context) -> OcrEngine) {
        engineFactory = factory
    }

    fun setRunnerFactoryForTests(factory: (Context, CoroutineScope) -> OcrRunner) {
        runnerFactory = factory
    }

    fun setDebugSavingEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SAVE_DEBUG_ARTIFACTS, enabled)
            .apply()
    }

    fun resetForTests() {
        engineFactory = productionEngineFactory
        runnerFactory = productionRunnerFactory
    }

    private const val PREFERENCES_NAME = "ocr"
    private const val KEY_SAVE_DEBUG_ARTIFACTS = "save_debug_artifacts"
}
