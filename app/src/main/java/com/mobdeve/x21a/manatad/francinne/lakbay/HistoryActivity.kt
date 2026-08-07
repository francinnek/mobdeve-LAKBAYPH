package com.mobdeve.x21a.manatad.francinne.lakbay

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.mobdeve.x21a.manatad.francinne.lakbay.databinding.ActivityHistoryBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.ivBackBtn.setOnClickListener {
            finish()
        }

        binding.rvHistory.layoutManager = LinearLayoutManager(this)
        database = AppDatabase.getDatabase(this)

        loadCommuteHistory()
    }

    private fun loadCommuteHistory() {
        lifecycleScope.launch(Dispatchers.IO) {
            val historyDao = database.historyDao()
            val savedHistory = historyDao.getAllHistory()

            withContext(Dispatchers.Main) {
                val adapter = HistoryAdapter(savedHistory)
                binding.rvHistory.adapter = adapter
            }
        }
    }
}