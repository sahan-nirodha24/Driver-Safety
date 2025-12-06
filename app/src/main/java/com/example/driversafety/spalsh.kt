package com.example.driversafety

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class spalsh : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spalsh)

        val btnGetStarted: MaterialButton = findViewById(R.id.btnGetStarted)
        val btnSkip: MaterialButton = findViewById(R.id.btnSkip)

        btnGetStarted.setOnClickListener {
            startActivity(Intent(this, signup::class.java))
        }

        btnSkip.setOnClickListener {
            startActivity(Intent(this, login::class.java))
        }
    }
}