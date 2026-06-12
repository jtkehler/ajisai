package com.jtkehler.ajisai

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.jtkehler.ajisai.settings.DictionariesActivity

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        findViewById<MaterialButton>(R.id.dictionaries_button).setOnClickListener {
            startActivity(Intent(this, DictionariesActivity::class.java))
        }
    }
}
