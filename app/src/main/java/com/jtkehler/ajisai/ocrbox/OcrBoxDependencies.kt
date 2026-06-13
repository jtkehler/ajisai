package com.jtkehler.ajisai.ocrbox

import android.content.Context

object OcrBoxDependencies {
    private val productionRepositoryFactory: (Context) -> OcrBoxRepository = { context ->
        SharedPreferencesOcrBoxRepository(
            context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE),
        )
    }
    private val productionCropperFactory: () -> OcrFrameCropper = ::BitmapOcrFrameCropper

    @Volatile
    private var repositoryFactory = productionRepositoryFactory

    @Volatile
    private var cropperFactory = productionCropperFactory

    fun repository(context: Context): OcrBoxRepository = repositoryFactory(context.applicationContext)

    fun cropper(): OcrFrameCropper = cropperFactory()

    fun setRepositoryFactoryForTests(factory: (Context) -> OcrBoxRepository) {
        repositoryFactory = factory
    }

    fun setCropperFactoryForTests(factory: () -> OcrFrameCropper) {
        cropperFactory = factory
    }

    fun resetForTests() {
        repositoryFactory = productionRepositoryFactory
        cropperFactory = productionCropperFactory
    }

    private const val PREFERENCES_NAME = "ocr_boxes"
}
