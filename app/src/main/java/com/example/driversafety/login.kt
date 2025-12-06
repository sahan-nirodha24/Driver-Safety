package com.example.driversafety

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class login : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val btnLogin: MaterialButton = findViewById(R.id.btnLogIn)
        val tvForgotPassword: TextView = findViewById(R.id.tvForgotPassword)
        val btnBack: ImageView = findViewById(R.id.btnBack)

        btnLogin.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, forget_password::class.java))
        }

        btnBack.setOnClickListener {
            startActivity(Intent(this, spalsh::class.java))
        }
    }
}