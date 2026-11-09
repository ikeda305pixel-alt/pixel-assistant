package com.antigravity.pixelassistant

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.antigravity.pixelassistant.ai.FreeTierQuotaManager
import com.google.android.material.switchmaterial.SwitchMaterial

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var quotaManager: FreeTierQuotaManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("pixel_assistant_prefs", Context.MODE_PRIVATE)
        quotaManager = FreeTierQuotaManager(this)

        val editGeminiKey = findViewById<EditText>(R.id.editGeminiApiKey)
        val textFreeTierStatus = findViewById<TextView>(R.id.textFreeTierStatus)
        val switchAutoSend = findViewById<SwitchMaterial>(R.id.switchAutoSend)
        val switchNetflixSubs = findViewById<SwitchMaterial>(R.id.switchNetflixSubs)
        val switchNetflixAdMute = findViewById<SwitchMaterial>(R.id.switchNetflixAdMute)
        val btnSave = findViewById<Button>(R.id.btnSaveSettings)
        val btnAccessibility = findViewById<Button>(R.id.btnOpenAccessibility)
        val btnOverlay = findViewById<Button>(R.id.btnOpenOverlayPermission)

        // Load saved preferences
        editGeminiKey.setText(prefs.getString("gemini_api_key", ""))
        switchAutoSend.isChecked = prefs.getBoolean("auto_send", false) // Safe default: FALSE
        switchNetflixSubs.isChecked = prefs.getBoolean("netflix_subs", true)
        switchNetflixAdMute.isChecked = prefs.getBoolean("netflix_ad_mute", true)

        updateFreeTierDisplay(textFreeTierStatus)

        // Navigation shortcuts for permissions
        btnAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        btnOverlay.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(this)) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "重ねて表示は既に許可されています", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnSave.setOnClickListener {
            prefs.edit()
                .putString("gemini_api_key", editGeminiKey.text.toString().trim())
                .putBoolean("auto_send", switchAutoSend.isChecked)
                .putBoolean("netflix_subs", switchNetflixSubs.isChecked)
                .putBoolean("netflix_ad_mute", switchNetflixAdMute.isChecked)
                .apply()

            Toast.makeText(this, "設定を保存しました", Toast.LENGTH_SHORT).show()
            updateFreeTierDisplay(textFreeTierStatus)
        }
    }

    private fun updateFreeTierDisplay(textView: TextView) {
        val usage = quotaManager.getDailyUsage()
        textView.text = getString(R.string.status_free_tier, usage)
    }

    override fun onResume() {
        super.onResume()
        val textFreeTierStatus = findViewById<TextView>(R.id.textFreeTierStatus)
        updateFreeTierDisplay(textFreeTierStatus)
    }
}
