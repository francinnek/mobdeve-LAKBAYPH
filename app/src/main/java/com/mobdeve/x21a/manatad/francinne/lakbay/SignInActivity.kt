package com.mobdeve.x21a.manatad.francinne.lakbay

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.mobdeve.x21a.manatad.francinne.lakbay.databinding.ActivitySigninBinding

class SignInActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySigninBinding
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySigninBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

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

            firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val sharedPreferences = getSharedPreferences("LakbaySession", Context.MODE_PRIVATE)
                        val editor = sharedPreferences.edit()
                        editor.putBoolean("IS_LOGGED_IN", true)
                        editor.putString("USER_EMAIL", email)
                        editor.apply()

                        Toast.makeText(this, "Signed in successfully!", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this, "Authentication Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }
}