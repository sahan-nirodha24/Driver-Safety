package com.example.driversafety

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class signup : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        val btnCreateAccount: MaterialButton = findViewById(R.id.btnCreateAccount)
        val tvSignIn: TextView = findViewById(R.id.tvSignIn)
        val btnBack: ImageView = findViewById(R.id.btnBack)

        btnCreateAccount.setOnClickListener {
            startActivity(Intent(this, login::class.java))
        }

        tvSignIn.setOnClickListener {
            startActivity(Intent(this, login::class.java))
        }

        btnBack.setOnClickListener {
            startActivity(Intent(this, spalsh::class.java))
        }
    }
}