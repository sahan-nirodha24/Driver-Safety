package com.example.driversafety

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.webkit.GeolocationPermissions
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class DirectionsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_directions)

        val destLat = intent.getDoubleExtra("DEST_LAT", 0.0)
        val destLng = intent.getDoubleExtra("DEST_LNG", 0.0)
        val destName = intent.getStringExtra("DEST_NAME") ?: "Destination"

        val tvDestName: TextView = findViewById(R.id.tvDestName)
        tvDestName.text = destName

        val btnBack: ImageView = findViewById(R.id.btnBack)
        btnBack.setOnClickListener { finish() }

        val webView: WebView = findViewById(R.id.directionsWebView)
        webView.settings.javaScriptEnabled = true
        webView.settings.setGeolocationEnabled(true)
        webView.webViewClient = WebViewClient()
        
        webView.webChromeClient = object : WebChromeClient() {
            override fun onGeolocationPermissionsShowPrompt(
                origin: String,
                callback: GeolocationPermissions.Callback
            ) {
                callback.invoke(origin, true, false)
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                val startLoc = getCurrentLocation()
                if (startLoc != null) {
                    Log.d("DirectionsActivity", "Sending points: Start(${startLoc.latitude}, ${startLoc.longitude}) End($destLat, $destLng)")
                    webView.evaluateJavascript(
                        "javascript:drawRoute(${startLoc.latitude}, ${startLoc.longitude}, $destLat, $destLng)",
                        null
                    )
                } else {
                    Log.e("DirectionsActivity", "Current location is NULL")
                    // If current location is null, we can at least center on destination
                    webView.evaluateJavascript("javascript:map.setView([$destLat, $destLng], 15);", null)
                    Toast.makeText(this@DirectionsActivity, "Current location unavailable. Please check GPS.", Toast.LENGTH_LONG).show()
                }
            }
        }

        webView.loadUrl("file:///android_asset/directions.html")
    }

    private fun getCurrentLocation(): Location? {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && 
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return null
        }
        
        // Try to get the last known location from GPS or Network
        return locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) 
            ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
    }
}