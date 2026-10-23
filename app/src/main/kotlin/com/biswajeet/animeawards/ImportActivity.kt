package com.biswajeet.animeawards

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ImportActivity : AppCompatActivity() {

    private lateinit var usernameInput: EditText
    private lateinit var importButton: Button
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_import)

        usernameInput = findViewById(R.id.usernameInput)
        importButton = findViewById(R.id.importGoButton)
        statusText = findViewById(R.id.importStatusText)

        // Remember the username from last time.
        val prefs = getSharedPreferences("anime_prefs", MODE_PRIVATE)
        usernameInput.setText(prefs.getString("mal_username", ""))

        importButton.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            if (username.isEmpty()) {
                showStatus("Type your MyAnimeList username first.")
            } else {
                prefs.edit().putString("mal_username", username).apply()
                startImport(username)
            }
        }
    }

    private fun showStatus(message: String) {
        statusText.text = message
        statusText.visibility = View.VISIBLE
    }

    private fun startImport(username: String) {
        importButton.isEnabled = false
        showStatus("Starting...")

        MalImport.importList(
            username,
            onProgress = { message ->
                if (!isFinishing) showStatus(message)
            },
            onDone = { result, error ->
                if (isFinishing) return@importList
                importButton.isEnabled = true

                if (result == null) {
                    showStatus(error ?: "Something went wrong.")
                    return@importList
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
                lines.add("Go back to see your list.")
                showStatus(lines.joinToString("\n"))
            }
        )
    }
}
