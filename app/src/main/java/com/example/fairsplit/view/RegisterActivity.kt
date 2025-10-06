package com.example.fairsplit.view

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.fairsplit.controller.AuthController
import com.example.fairsplit.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var ctrl: AuthController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fun setLoading(on: Boolean) {
            binding.progress.visibility = if (on) View.VISIBLE else View.GONE
            binding.btnRegister.isEnabled = !on
            binding.tvGoLogin.isEnabled = !on
            binding.etName.isEnabled = !on
            binding.etEmail.isEnabled = !on
            binding.etPassword.isEnabled = !on
        }

        ctrl = AuthController { action ->
            runOnUiThread {
                when (action) {
                    is AuthController.Action.Loading -> setLoading(action.on)
                    is AuthController.Action.Error ->
                        Toast.makeText(this, action.msg, Toast.LENGTH_SHORT).show()
                    is AuthController.Action.LoginSuccess -> {
                        startActivity(Intent(this, GroupsActivity::class.java))
                        finish()
                    }
                }
            }
        }

        binding.btnRegister.setOnClickListener {
            val name = binding.etName.text?.toString()?.trim().orEmpty()
            val email = binding.etEmail.text?.toString()?.trim().orEmpty()
            val pass  = binding.etPassword.text?.toString()?.trim().orEmpty()

            var ok = true
            if (name.isEmpty()) { binding.etName.error = "Display name required"; ok = false }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.etEmail.error = "Enter a valid email"; ok = false
            }
            if (pass.length < 6) { binding.etPassword.error = "Password min 6 chars"; ok = false }
            if (!ok) return@setOnClickListener

            ctrl.register(email, pass, name)
        }

        // Back to login
        binding.tvGoLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
