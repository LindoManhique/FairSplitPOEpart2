package com.example.fairsplit.view

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.fairsplit.controller.SettingsController
import com.example.fairsplit.databinding.ActivitySettingsBinding
import com.example.fairsplit.model.dto.UserProfile
import com.google.firebase.auth.FirebaseAuth

class SettingsActivity : AppCompatActivity() {
    private lateinit var b: ActivitySettingsBinding
    private lateinit var ctrl: SettingsController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(b.root)

        fun setLoading(on: Boolean) {
            b.progress.visibility = if (on) View.VISIBLE else View.GONE
            b.btnSave.isEnabled = !on
            b.btnRate.isEnabled = !on
            b.etDisplayName.isEnabled = !on
            b.etCurrency.isEnabled = !on
        }

        ctrl = SettingsController { action ->
            runOnUiThread {
                when (action) {
                    is SettingsController.Action.Loading -> setLoading(action.on)
                    is SettingsController.Action.Error ->
                        Toast.makeText(this, action.msg, Toast.LENGTH_SHORT).show()
                    is SettingsController.Action.Loaded -> {
                        b.etDisplayName.setText(action.profile.displayName)
                        b.etCurrency.setText(action.profile.defaultCurrency)
                    }
                    is SettingsController.Action.Rate -> { b.tvRate.text = action.text }
                    is SettingsController.Action.Saved ->
                        Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
                }
            }
        }

        b.btnSave.setOnClickListener {
            val name = b.etDisplayName.text?.toString()?.trim().orEmpty()
            var currency = b.etCurrency.text?.toString()?.trim().orEmpty().uppercase()

            var ok = true
            if (name.isEmpty()) { b.etDisplayName.error = "Display name required"; ok = false }
            if (!currency.matches(Regex("^[A-Z]{3}$"))) {
                b.etCurrency.error = "Use 3-letter code e.g. ZAR"
                ok = false
            }
            if (!ok) return@setOnClickListener

            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            val profile = UserProfile(uid = uid, displayName = name, defaultCurrency = currency)
            ctrl.save(profile)
        }

        b.btnRate.setOnClickListener { ctrl.fetchRateZarToUsd() }
    }

    override fun onResume() {
        super.onResume()
        ctrl.load()
    }
}
