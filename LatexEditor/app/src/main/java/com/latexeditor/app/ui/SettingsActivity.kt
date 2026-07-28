package com.latexeditor.app.ui

import android.os.Bundle
import android.widget.CheckBox
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.latexeditor.app.R
import com.latexeditor.app.data.Prefs

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        prefs = Prefs(this)

        findViewById<Toolbar>(R.id.toolbar).let {
            setSupportActionBar(it)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            it.setNavigationOnClickListener { finish() }
        }

        val endpointInput = findViewById<EditText>(R.id.inputEndpoint)
        val radioTheme = findViewById<RadioGroup>(R.id.radioTheme)
        val wordWrap = findViewById<CheckBox>(R.id.checkWordWrap)
        val lineNumbers = findViewById<CheckBox>(R.id.checkLineNumbers)
        val autosave = findViewById<CheckBox>(R.id.checkAutosave)

        endpointInput.setText(prefs.compileEndpoint)
        when (prefs.theme) {
            "dark" -> radioTheme.check(R.id.radioDark)
            "high_contrast" -> radioTheme.check(R.id.radioHighContrast)
            else -> radioTheme.check(R.id.radioLight)
        }
        wordWrap.isChecked = prefs.wordWrap
        lineNumbers.isChecked = prefs.lineNumbers
        autosave.isChecked = prefs.autosave

        findViewById<android.widget.Button>(R.id.buttonSave).setOnClickListener {
            prefs.compileEndpoint = endpointInput.text.toString().ifBlank { Prefs.DEFAULT_ENDPOINT }
            prefs.theme = when (radioTheme.checkedRadioButtonId) {
                R.id.radioDark -> "dark"
                R.id.radioHighContrast -> "high_contrast"
                else -> "light"
            }
            prefs.wordWrap = wordWrap.isChecked
            prefs.lineNumbers = lineNumbers.isChecked
            prefs.autosave = autosave.isChecked
            Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
