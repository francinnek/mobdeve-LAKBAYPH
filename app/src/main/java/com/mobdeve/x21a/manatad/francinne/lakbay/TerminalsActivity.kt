package com.mobdeve.x21a.manatad.francinne.lakbay

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.mobdeve.x21a.manatad.francinne.lakbay.databinding.ActivityTerminalsBinding
import java.util.concurrent.Executors

class TerminalsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTerminalsBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: TerminalAdapter
    private val executorService = Executors.newSingleThreadExecutor()

    private var masterTerminalList = listOf<Terminal>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTerminalsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()

        binding.ivBackBtn.setOnClickListener {
            finish()
        }

        binding.rvTerminals.layoutManager = LinearLayoutManager(this)
        adapter = TerminalAdapter(emptyList())
        binding.rvTerminals.adapter = adapter

        adapter.setOnReportClickListener { terminal ->
            showCongestionReportDialog(terminal)
        }

        setupSearchFilter()
        fetchTerminalsWithGtfsFallback()
    }

    private fun setupSearchFilter() {
        binding.etSearchTerminal.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterTerminals(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterTerminals(query: String) {
        if (query.isBlank()) {
            adapter.updateData(masterTerminalList)
        } else {
            val filtered = masterTerminalList.filter {
                it.name.contains(query, ignoreCase = true)
            }
            adapter.updateData(filtered)
        }
    }

    private fun fetchTerminalsWithGtfsFallback() {
        db.collection("Terminals")
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful && task.result != null && !task.result!!.isEmpty) {
                    val terminals = mutableListOf<Terminal>()
                    for (document in task.result!!) {
                        val terminal = document.toObject(Terminal::class.java)
                        terminal.id = document.id
                        terminals.add(terminal)
                    }
                    masterTerminalList = terminals
                    filterTerminals(binding.etSearchTerminal.text.toString())
                } else {
                    loadTerminalsFromGtfs()
                }
            }
    }

    private fun loadTerminalsFromGtfs() {
        executorService.execute {
            try {
                val stopsRawId = resources.getIdentifier("stops", "raw", packageName)
                if (stopsRawId != 0) {
                    val inputStream = resources.openRawResource(stopsRawId)
                    val parser = GtfsParser()
                    val gtfsStops = parser.parseStops(inputStream)

                    masterTerminalList = gtfsStops.map { stop ->
                        Terminal(
                            id = stop.stopId,
                            name = stop.stopName,
                            congestion = "Unknown"
                        )
                    }

                    runOnUiThread {
                        filterTerminals(binding.etSearchTerminal.text.toString())
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showCongestionReportDialog(terminal: Terminal) {
        val options = arrayOf("Empty", "Moderate", "Heavy")

        AlertDialog.Builder(this)
            .setTitle("Report Congestion for ${terminal.name}")
            .setItems(options) { _, which ->
                val selectedCongestion = options[which]
                updateTerminalCongestion(terminal.id, selectedCongestion)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateTerminalCongestion(terminalId: String, congestionLevel: String) {
        val data = hashMapOf<String, Any>(
            "congestion" to congestionLevel
        )

        db.collection("Terminals").document(terminalId)
            .set(data, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(this, "Congestion updated!", Toast.LENGTH_SHORT).show()
                fetchTerminalsWithGtfsFallback()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error updating document", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        executorService.shutdown()
    }
}