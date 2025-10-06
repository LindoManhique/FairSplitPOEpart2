package com.example.fairsplit.view

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.fairsplit.controller.RatesController
import com.example.fairsplit.controller.SettingsController
import com.example.fairsplit.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var b: ActivitySettingsBinding
    private lateinit var settingsCtrl: SettingsController
    private lateinit var ratesCtrl: RatesController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(b.root)

        // Title
        b.tvTitle.text = "Settings"

        fun setLoading(on: Boolean) {
            b.progress.visibility = if (on) View.VISIBLE else View.GONE
            b.btnSave.isEnabled = !on
            b.btnFetch.isEnabled = !on
            b.etDisplayName.isEnabled = !on
            b.etCurrency.isEnabled = !on
        }

        // Settings controller
        settingsCtrl = SettingsController(ui = { action ->
            runOnUiThread {
                when (action) {
                    is SettingsController.Action.Loading -> setLoading(action.on)
                    is SettingsController.Action.Error ->
                        Toast.makeText(this, action.msg, Toast.LENGTH_SHORT).show()
                    is SettingsController.Action.Saved -> {
                        // no-op; we navigate optimistically on Save
                    }
                    is SettingsController.Action.Prefilled -> {
                        // <-- NO 'profile' variable here; use the action values
                        b.etDisplayName.setText(action.name)
                        b.etCurrency.setText(action.currency)
                    }
                }
            }
        })

        // Rates controller
        ratesCtrl = RatesController(ui = { action ->
            runOnUiThread {
                when (action) {
                    is RatesController.Action.Loading -> setLoading(action.on)
                    is RatesController.Action.Error ->
                        Toast.makeText(this, action.msg, Toast.LENGTH_SHORT).show()
                    is RatesController.Action.Result -> {
                        b.tvRate.text = "1 ${action.base} = ${action.rate} ${action.target}"
                        Toast.makeText(this, "Rate updated", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })

        // SAVE — optimistic navigate back to Groups
        b.btnSave.setOnClickListener {
            val name = b.etDisplayName.text?.toString()?.trim().orEmpty()
            val currency = b.etCurrency.text?.toString()?.trim()?.uppercase().orEmpty()

            var ok = true
            if (name.isEmpty()) { b.etDisplayName.error = "Enter a display name"; ok = false }
            if (currency.length != 3) { b.etCurrency.error = "Use a 3-letter code (e.g., ZAR)"; ok = false }
            if (!ok) return@setOnClickListener

            // IMPORTANT: positional args — do NOT use a named arg 'currency' or 'currencyCode'
            settingsCtrl.saveProfile(name, currency)

            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
            startActivity(
                Intent(this, GroupsActivity::class.java)
                    .putExtra("displayNameOverride", name)
            )
            finish()
        }

        // FETCH rate (no hang)
        b.btnFetch.setOnClickListener {
            val base = b.etCurrency.text?.toString()?.trim()?.uppercase().orEmpty()
            if (base.length != 3) {
                b.etCurrency.error = "Use a 3-letter code (e.g., ZAR)"
                return@setOnClickListener
            }
            ratesCtrl.fetch(base, "USD")
        }
    }

    override fun onStart() {
        super.onStart()
        settingsCtrl.loadProfile()
    }
}
