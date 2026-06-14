package com.jtkehler.ajisai.ocr

import android.graphics.Bitmap
import android.graphics.Rect
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GoogleLensOcrEngine(
    private val preprocessor: OcrImagePreprocessor = LensImagePreprocessor(),
    private val requestBuilder: LensRequestBuilder = LensRequestBuilder(),
    private val transport: LensTransport = OkHttpLensTransport(),
    private val responseParser: LensResponseParser = LensResponseParser(),
    private val textPostProcessor: OcrTextPostProcessor = JapaneseOcrTextPostProcessor(),
    private val textAssembler: JapaneseOcrTextAssembler = JapaneseOcrTextAssembler(),
    private val debugLogger: OcrDebugLogger = NoOpOcrDebugLogger,
) : OcrEngine {
    override suspend fun recognize(
        image: Bitmap,
        region: Rect?,
        options: OcrOptions,
    ): OcrResult {
        try {
            val processed = withContext(Dispatchers.Default) {
                preprocessor.preprocess(image, region)
            }
            val cropPath = if (options.saveDebugArtifacts) {
                debugLogger.saveCrop(processed)
            } else {
                null
            }
            val response = transport.execute(requestBuilder.build(processed))
            val rawResponsePath = if (options.saveDebugArtifacts) {
                debugLogger.saveRawResponse(response.body)
            } else {
                null
            }
            if (response.statusCode !in 200..299) {
                throw OcrException(
                    OcrErrorType.HTTP,
                    "Google Lens returned HTTP ${response.statusCode}.",
                    httpStatusCode = response.statusCode,
                )
            }
            val lines = textPostProcessor.process(responseParser.parse(response.body).lines)
            if (lines.isEmpty()) {
                throw OcrException(OcrErrorType.NO_TEXT, "No text was found in the OCR box.")
            }
            return OcrResult(
                text = textAssembler.assemble(lines),
                lines = lines,
                providerName = PROVIDER_NAME,
                debugArtifacts = OcrDebugArtifacts(cropPath, rawResponsePath),
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: OcrException) {
            throw error
        } catch (error: Throwable) {
            throw OcrException(OcrErrorType.PARSE, "Google Lens OCR failed.", error)
        }
    }

    private companion object {
        const val PROVIDER_NAME = "Google Lens"
    }
}
