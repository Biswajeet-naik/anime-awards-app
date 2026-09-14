package com.biswajeet.animeawards

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val greeting: TextView = findViewById(R.id.greetingText)
        greeting.text = "Hello, ${getAppUserName()}!"
    }

    private fun getAppUserName(): String {
        return "Anime Fan"
    }
}
