package com.likhibi.keyboard

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.AdapterView
import com.google.android.material.switchmaterial.SwitchMaterial
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import android.widget.LinearLayout

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var lblHaptic: TextView
    private lateinit var seekHaptic: SeekBar

    private lateinit var fontSpinner: Spinner
    private lateinit var switchSound: SwitchMaterial
    private lateinit var lblSoundVolume: TextView
    private lateinit var seekSoundVolume: SeekBar

    companion object {
        private const val RC_PICK_WALLPAPER = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        prefs = getSharedPreferences("likhibi_keyboard_prefs", Context.MODE_PRIVATE)

        findViewById<Button>(R.id.btn_enable_keyboard).setOnClickListener {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }

        findViewById<Button>(R.id.btn_select_keyboard).setOnClickListener {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showInputMethodPicker()
        }

        // Theme Selection via Spinner
        val themeSpinner: Spinner = findViewById(R.id.spinner_theme)
        val themeOptions = arrayOf("Likhibi's Light", "Likhibi's Dark", "Slate Grey", "Navy Blue", "Earth Tone", "Custom Photo")
        val themeValues = arrayOf("theme_classic_light", "theme_oled_black", "theme_slate_grey", "theme_navy_blue", "theme_earth_tone", "theme_custom_wallpaper")
        val themeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, themeOptions)
        themeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        themeSpinner.adapter = themeAdapter

        val currentTheme = prefs.getString("selected_theme", "theme_oled_black") ?: "theme_oled_black"
        themeSpinner.setSelection(themeValues.indexOf(currentTheme).coerceAtLeast(0))
        var isInitialSelection = true

        themeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!isInitialSelection) performHapticClick()
                if (themeValues[position] == "theme_custom_wallpaper" && !isInitialSelection) {
                    val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
                    startActivityForResult(intent, RC_PICK_WALLPAPER)
                }
                isInitialSelection = false
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Font Selection
        fontSpinner = findViewById(R.id.spinner_font)
        val fontOptions = arrayOf("Modern Clean (Default)", "Elegant Light", "Bold Impact", "Classic Serif", "Playful Casual", "Monospace Typewriter")
        val fontValues = arrayOf("sans-serif", "sans-serif-light", "sans-serif-black", "serif", "casual", "monospace")
        val fontAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, fontOptions)
        fontAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        fontSpinner.adapter = fontAdapter

        val currentFont = prefs.getString("selected_font", "sans-serif") ?: "sans-serif"
        val fontIndex = fontValues.indexOf(currentFont).coerceAtLeast(0)
        fontSpinner.setSelection(fontIndex)

        fontSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                performHapticClick()
                prefs.edit().putString("selected_font", fontValues[position]).apply()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Sound Feedback
        switchSound = findViewById(R.id.switch_sound)
        lblSoundVolume = findViewById(R.id.lbl_sound_volume)
        seekSoundVolume = findViewById(R.id.seek_sound_volume)

        val soundEnabled = prefs.getBoolean("sound_enabled", true)
        switchSound.isChecked = soundEnabled
        seekSoundVolume.isEnabled = soundEnabled

        val currentVolume = (prefs.getFloat("sound_volume", 0.5f) * 100).toInt()
        seekSoundVolume.progress = currentVolume
        lblSoundVolume.text = "Volume: ${currentVolume}%"

        switchSound.setOnCheckedChangeListener { _, isChecked ->
            performHapticClick()
            prefs.edit().putBoolean("sound_enabled", isChecked).apply()
            seekSoundVolume.isEnabled = isChecked
        }

        seekSoundVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                lblSoundVolume.text = "Volume: ${progress}%"
                prefs.edit().putFloat("sound_volume", progress / 100f).apply()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Haptic Intensity
        lblHaptic = findViewById(R.id.lbl_haptic)
        seekHaptic = findViewById(R.id.seek_haptic)

        val activeHaptic = prefs.getInt("haptic_strength", 10)
        seekHaptic.progress = activeHaptic
        lblHaptic.text = "Vibration strength: ${activeHaptic}ms"

        seekHaptic.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                lblHaptic.text = "Vibration strength: ${progress}ms"
                prefs.edit().putInt("haptic_strength", progress).apply()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                triggerTactileSample(seekHaptic.progress)
            }
        })

        // Clipboard Manager
        findViewById<LinearLayout>(R.id.btn_clear_clipboard).setOnClickListener {
            performHapticClick()
            prefs.edit().putString("clipboard_history", "").apply()
            Toast.makeText(this, "Clipboard history cleared!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun performHapticClick() {
        val hapticStrength = prefs.getInt("haptic_strength", 10)
        triggerTactileSample(hapticStrength)
    }

    private fun triggerTactileSample(durationMs: Int) {
        if (durationMs <= 0) return
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(durationMs.toLong(), 200))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(durationMs.toLong())
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_PICK_WALLPAPER && resultCode == RESULT_OK && data != null) {
            val uri: Uri = data.data ?: return
            try {
                val inputStream: InputStream? = contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val wallpaperFile = File(filesDir, "custom_wallpaper.jpg")
                    val outputStream = FileOutputStream(wallpaperFile)
                    inputStream.copyTo(outputStream)
                    inputStream.close()
                    outputStream.close()

                    prefs.edit().putString("selected_theme", "theme_custom_wallpaper").apply()
                    findViewById<Spinner>(R.id.spinner_theme).setSelection(5) // Index 5 is Custom Photo

                    Toast.makeText(this, "Custom photo applied successfully!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to load custom photo: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
