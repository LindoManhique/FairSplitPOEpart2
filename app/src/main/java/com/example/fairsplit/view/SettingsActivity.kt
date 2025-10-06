package com.example.fairsplit.view

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.fairsplit.databinding.ActivitySettingsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class SettingsActivity : AppCompatActivity() {

    private lateinit var b: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(b.root)

        // Prefill from local store
        lifecycleScope.launch {
            setLoading(true)
            try {
                val s = SettingsLocal.load(this@SettingsActivity)
                b.etDisplayName.setText(s.displayName ?: "")
                b.etCurrencyCode.setText(s.currencyCode ?: "")
            } finally {
                setLoading(false)
            }
        }

        // SAVE → toast → go to Groups
        b.btnSave.setOnClickListener {
            lifecycleScope.launch {
                setLoading(true)
                try {
                    val name = b.etDisplayName.text?.toString()?.trim().orEmpty()
                    val code = b.etCurrencyCode.text?.toString()?.trim()
                        ?.uppercase(Locale.ROOT).orEmpty()

                    SettingsLocal.save(this@SettingsActivity, name, code)

                    Toast.makeText(this@SettingsActivity, "Settings saved", Toast.LENGTH_SHORT).show()

                    startActivity(Intent(this@SettingsActivity, GroupsActivity::class.java))
                    finish()
                } finally {
                    setLoading(false)
                }
            }
        }

        // Optional demo fetch (remove if not needed)
        b.btnFetchRates.setOnClickListener {
            lifecycleScope.launch {
                setLoading(true)
                try {
                    val code = b.etCurrencyCode.text?.toString()?.trim()
                        ?.uppercase(Locale.ROOT).orEmpty()
                    val rate = SettingsLocal.fetchRate(code)
                    b.tvRate.text = rate.toString()
                } finally {
                    setLoading(false)
                }
            }
        }
    }

    private fun setLoading(on: Boolean) {
        b.progress.visibility = if (on) View.VISIBLE else View.GONE
        b.btnSave.isEnabled = !on
        b.btnFetchRates.isEnabled = !on
        b.etDisplayName.isEnabled = !on
        b.etCurrencyCode.isEnabled = !on
    }
}

/** Inline local store (so we don't touch other files). */
private object SettingsLocal {
    private const val PREF = "settings"
    private const val KEY_NAME = "display_name"
    private const val KEY_CCY  = "currency_code"

    data class Settings(val displayName: String?, val currencyCode: String?)

    suspend fun load(context: Context): Settings = withContext(Dispatchers.IO) {
        val p = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        Settings(
            displayName = p.getString(KEY_NAME, null),
            currencyCode = p.getString(KEY_CCY, null)
        )
    }

    suspend fun save(context: Context, displayName: String, currencyCode: String) =
        withContext(Dispatchers.IO) {
            val p = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            p.edit()
                .putString(KEY_NAME, displayName)
                .putString(KEY_CCY, currencyCode)
                .apply()
        }

    // Mock rate; swap for real API if you want
    suspend fun fetchRate(currencyCode: String): Double = withContext(Dispatchers.IO) {
        delay(300)
        when (currencyCode.uppercase(Locale.ROOT)) {
            "USD" -> 1.00
            "ZAR" -> 18.50
            "EUR" -> 0.92
            else  -> 1.00
        }
    }
}
