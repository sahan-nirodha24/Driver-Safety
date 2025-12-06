package com.example.driversafety

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class forget_password : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forget_password)

        val btnBackToLogin: MaterialButton = findViewById(R.id.btnBackToLogin)
        val btnBack: ImageView = findViewById(R.id.btnBack)

        btnBackToLogin.setOnClickListener {
            startActivity(Intent(this, login::class.java))
        }

        btnBack.setOnClickListener {
            startActivity(Intent(this, login::class.java))
        }
    }
}