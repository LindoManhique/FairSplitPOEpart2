package com.example.fairsplit.view

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.fairsplit.controller.AuthController
import com.example.fairsplit.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var ctrl: AuthController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fun setLoading(on: Boolean) {
            binding.progress.visibility = if (on) View.VISIBLE else View.GONE
            binding.btnLogin.isEnabled = !on
            binding.btnGoRegister.isEnabled = !on
            binding.tvForgot.isEnabled = !on
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

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text?.toString()?.trim().orEmpty()
            val pass  = binding.etPassword.text?.toString()?.trim().orEmpty()

            var ok = true
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.etEmail.error = "Enter a valid email"
                ok = false
            }
            if (pass.length < 6) {
                binding.etPassword.error = "Password min 6 chars"
                ok = false
            }
            if (!ok) return@setOnClickListener

            ctrl.login(email, pass)
        }

        binding.btnGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // Forgot password
        binding.tvForgot.setOnClickListener {
            val email = binding.etEmail.text?.toString()?.trim().orEmpty()
            if (Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                FirebaseAuth.getInstance()
                    .sendPasswordResetEmail(email)
                    .addOnCompleteListener { t ->
                        val msg = if (t.isSuccessful)
                            "Password reset email sent."
                        else
                            t.exception?.localizedMessage ?: "Could not send reset email."
                        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                    }
            } else {
                binding.etEmail.error = "Enter your email to reset"
            }
        }
    }
}
