package com.jtkehler.ajisai.ocr

data class LensConfig(
    val endpoint: String = DEFAULT_ENDPOINT,
    val apiKey: String = DEFAULT_API_KEY,
    val userAgent: String = DEFAULT_USER_AGENT,
    val language: String = "ja",
    val region: String = "US",
    val timeZone: String = "America/Los_Angeles",
) {
    companion object {
        const val DEFAULT_ENDPOINT = "https://lensfrontend-pa.googleapis.com/v1/crupload"

        // Public Chromium Lens key used by compatible open-source clients. Keep overrideable.
        const val DEFAULT_API_KEY = "AIzaSyDr2UxVnv_U85AbhhY8XSHSIavUW0DC-sY"
        const val DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    }
}
