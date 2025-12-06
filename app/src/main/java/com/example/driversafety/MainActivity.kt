package com.example.driversafety

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.driversafety.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var fragmentManager: FragmentManager
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomNavigation.background = null
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when(item.itemId){
                R.id.bottom_dashboard -> openFragment(Dashboard())
                R.id.bottom_profile -> openFragment(Profile())
                R.id.bottom_diagnosis -> openFragment(Diagnosis())
                R.id.bottom_smart_assistant -> openFragment(SmartAssistant())
                R.id.bottom_rest_stops -> openFragment(RestStops())

            }
            true
        }
        fragmentManager = supportFragmentManager
        openFragment(Dashboard())

    }

    private fun openFragment(fragment: Fragment){
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.fragment_container, fragment)
        fragmentTransaction.commit()

    }
}