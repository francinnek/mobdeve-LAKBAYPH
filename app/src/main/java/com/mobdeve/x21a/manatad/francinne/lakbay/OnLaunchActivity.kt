package com.mobdeve.x21a.manatad.francinne.lakbay

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.mobdeve.x21a.manatad.francinne.lakbay.databinding.ActivityOnlaunchBinding

class OnLaunchActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnlaunchBinding

    private var pendingIntent: Intent? = null

    private val requestLocationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                pendingIntent?.let {
                    startActivity(it)
                }
            }
            pendingIntent = null
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPreferences = getSharedPreferences("LakbaySession", Context.MODE_PRIVATE)
        val isLoggedIn = sharedPreferences.getBoolean("IS_LOGGED_IN", false)

        if (isLoggedIn) {
            navigateWithLocationPermission(
                Intent(this, MainActivity::class.java)
            )
            finish()
            return
        }

        binding = ActivityOnlaunchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSignIn.setOnClickListener {
            navigateWithLocationPermission(
                Intent(this, SignInActivity::class.java)
            )
        }

        binding.btnSignUp.setOnClickListener {
            navigateWithLocationPermission(
                Intent(this, SignUpActivity::class.java)
            )
        }
    }

    private fun navigateWithLocationPermission(intent: Intent) {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startActivity(intent)
        } else {
            pendingIntent = intent
            requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
}