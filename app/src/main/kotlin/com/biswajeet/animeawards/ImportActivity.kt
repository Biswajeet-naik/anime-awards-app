package com.biswajeet.animeawards

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar

class ImportActivity : AppCompatActivity() {

    private lateinit var usernameInput: EditText
    private lateinit var yearInput: EditText
    private lateinit var importButton: Button
    private lateinit var fileButton: Button
    private lateinit var statusText: TextView

    private val pickFileRequest = 4711

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_import)

        usernameInput = findViewById(R.id.usernameInput)
        yearInput = findViewById(R.id.yearInput)
        importButton = findViewById(R.id.importGoButton)
        fileButton = findViewById(R.id.chooseFileButton)
        statusText = findViewById(R.id.importStatusText)

        // Remember the username from last time. Year starts as the current year.
        val prefs = getSharedPreferences("anime_prefs", MODE_PRIVATE)
        usernameInput.setText(prefs.getString("mal_username", ""))
        yearInput.setText(Calendar.getInstance().get(Calendar.YEAR).toString())

        importButton.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            if (username.isEmpty()) {
                showStatus("Type your MyAnimeList username first.")
            } else if (!yearIsValid()) {
                showStatus("The year must be a number like 2026, or leave it empty.")
            } else {
                prefs.edit().putString("mal_username", username).apply()
                startUserImport(username, readYear())
            }
        }

        fileButton.setOnClickListener {
            if (!yearIsValid()) {
                showStatus("The year must be a number like 2026, or leave it empty.")
            } else {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                intent.type = "*/*"
                startActivityForResult(intent, pickFileRequest)
            }
        }
    }

    private fun yearIsValid(): Boolean {
        val text = yearInput.text.toString().trim()
        return text.isEmpty() || text.toIntOrNull() != null
    }

    private fun readYear(): Int? = yearInput.text.toString().trim().toIntOrNull()

    private fun showStatus(message: String) {
        statusText.text = message
        statusText.visibility = View.VISIBLE
    }

    private fun setBusy(busy: Boolean) {
        importButton.isEnabled = !busy
        fileButton.isEnabled = !busy
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != pickFileRequest || resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        startFileImport(uri, readYear())
    }

    private fun startUserImport(username: String, year: Int?) {
        setBusy(true)
        showStatus("Starting...")
        MalImport.importUser(
            username,
            year,
            onProgress = { message -> if (!isFinishing) showStatus(message) },
            onDone = { result, error -> handleDone(result, error) }
        )
    }

    private fun startFileImport(uri: Uri, year: Int?) {
        setBusy(true)
        showStatus("Starting...")
        MalImport.importFile(
            contentResolver,
            uri,
            year,
            onProgress = { message -> if (!isFinishing) showStatus(message) },
            onDone = { result, error -> handleDone(result, error) }
        )
    }

    private fun handleDone(result: ImportResult?, error: String?) {
        if (isFinishing) return
        setBusy(false)

        if (result == null) {
            showStatus(error ?: "Something went wrong.")
            return
        }

        // Add only the anime that aren't in the list yet, and save once.
        val current = Storage.getAnime(this).toMutableList()
        val existingIds = current.map { it.id }.toSet()
        val fresh = result.anime.filter { it.id !in existingIds }
        current.addAll(fresh)
        Storage.saveAnime(this, current)

        val alreadyThere = result.anime.size - fresh.size
        val unmatched = result.malTotal - result.anime.size
        val lines = mutableListOf("Done! Added ${fresh.size} new anime.")
        if (alreadyThere > 0) lines.add("$alreadyThere were already in your list.")
        if (unmatched > 0) lines.add("$unmatched couldn't be matched on AniList (skipped).")
        lines.addAll(result.notes)
        lines.add("Go back to see your list.")
        showStatus(lines.joinToString("\n"))
    }
}
