package com.mobdeve.x21a.manatad.francinne.lakbay

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.mobdeve.x21a.manatad.francinne.lakbay.databinding.ActivitySigninBinding

class SignInActivity : ComponentActivity() {

    private lateinit var binding: ActivitySigninBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySigninBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sharedPreferences: SharedPreferences = getSharedPreferences("LakbaySession", Context.MODE_PRIVATE)
        val isLoggedIn = sharedPreferences.getBoolean("IS_LOGGED_IN", false)

        if (isLoggedIn) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        binding.button.setOnClickListener {
            val email = binding.editTextTextEmailAddress.text.toString().trim()
            val password = binding.editTextNumberPassword.text.toString().trim()

            if (email.isEmpty()) {
                binding.editTextTextEmailAddress.error = "Email address is required"
                binding.editTextTextEmailAddress.requestFocus()
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                binding.editTextNumberPassword.error = "Password is required"
                binding.editTextNumberPassword.requestFocus()
                return@setOnClickListener
            }

            val editor = sharedPreferences.edit()
            editor.putBoolean("IS_LOGGED_IN", true)
            editor.putString("USER_EMAIL", email)
            editor.apply()

            Toast.makeText(this, "Signed in successfully!", Toast.LENGTH_SHORT).show()

            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}