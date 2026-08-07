package com.mobdeve.x21a.manatad.francinne.lakbay

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mobdeve.x21a.manatad.francinne.lakbay.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.ivBackBtn.setOnClickListener {
            finish()
        }

        binding.tvFaqs.setOnClickListener {
            Toast.makeText(this, "FAQs selected", Toast.LENGTH_SHORT).show()
        }

        binding.tvPrivacyPolicy.setOnClickListener {
            Toast.makeText(this, "Privacy Policy selected", Toast.LENGTH_SHORT).show()
        }

        binding.tvTermsOfService.setOnClickListener {
            Toast.makeText(this, "Terms of Service selected", Toast.LENGTH_SHORT).show()
        }

        binding.btnLogout.setOnClickListener {
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
            val sharedPreferences = getSharedPreferences("LakbaySession", MODE_PRIVATE)
            sharedPreferences.edit().clear().apply()

            val intent = Intent(this, OnLaunchActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}