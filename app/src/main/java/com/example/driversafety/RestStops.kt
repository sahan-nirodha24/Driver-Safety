package com.example.driversafety

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import kotlin.concurrent.thread

class RestStops : Fragment() {

    private lateinit var mapWebView: WebView
    private lateinit var mapProgressBar: ProgressBar
    private var selectedLat: Double = 6.9271
    private var selectedLng: Double = 79.8612

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_rest_stops, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mapWebView = view.findViewById(R.id.mapWebView)
        mapProgressBar = view.findViewById(R.id.mapProgressBar)
        setupWebView()

        val etSearch: EditText = view.findViewById(R.id.etSearch)
        etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = etSearch.text.toString()
                if (query.isNotEmpty()) {
                    geocodeAndSearch(query)
                }
                true
            } else {
                false
            }
        }

        view.findViewById<TextView>(R.id.btnCoffeeShops).setOnClickListener {
            fetchAndShowNearby("amenity=cafe", "marker-orange")
        }
        view.findViewById<TextView>(R.id.btnRestaurants).setOnClickListener {
            fetchAndShowNearby("amenity=restaurant", "marker-red")
        }
        view.findViewById<TextView>(R.id.btnGasStations).setOnClickListener {
            fetchAndShowNearby("amenity=fuel", "marker-green")
        }
    }

    private fun setupWebView() {
        mapWebView.settings.javaScriptEnabled = true
        mapWebView.addJavascriptInterface(MapBridge(), "Android")
        
        mapWebView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                Log.d("MapJS", consoleMessage?.message() ?: "")
                return true
            }
        }
        
        mapWebView.webViewClient = WebViewClient()
        mapWebView.loadUrl("file:///android_asset/map.html")
    }

    private fun geocodeAndSearch(query: String) {
        if (!isAdded) return
        mapProgressBar.visibility = View.VISIBLE
        thread {
            try {
                val url = URL("https://nominatim.openstreetmap.org/search?q=${java.net.URLEncoder.encode(query, "UTF-8")}&format=json&limit=1")
                val connection = url.openConnection()
                connection.setRequestProperty("User-Agent", "DriverSafetyApp/1.0")
                connection.connectTimeout = 5000
                connection.readTimeout = 10000
                
                val response = connection.getInputStream().bufferedReader().use { it.readText() }
                val json = JSONArray(response)
                if (json.length() > 0) {
                    val obj = json.getJSONObject(0)
                    val lat = obj.getDouble("lat")
                    val lon = obj.getDouble("lon")
                    
                    selectedLat = lat
                    selectedLng = lon

                    activity?.runOnUiThread {
                        if (isAdded) {
                            mapProgressBar.visibility = View.GONE
                            mapWebView.evaluateJavascript("javascript:setCenter($lat, $lon)", null)
                        }
                    }
                } else {
                    activity?.runOnUiThread { if (isAdded) mapProgressBar.visibility = View.GONE }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                activity?.runOnUiThread { if (isAdded) mapProgressBar.visibility = View.GONE }
            }
        }
    }

    private fun fetchAndShowNearby(category: String, markerClass: String) {
        if (!isAdded) return
        mapProgressBar.visibility = View.VISIBLE
        thread {
            try {
                // radius 5km for more results
                val query = "[out:json][timeout:30];(node(around:5000,$selectedLat,$selectedLng)[$category];way(around:5000,$selectedLat,$selectedLng)[$category];rel(around:5000,$selectedLat,$selectedLng)[$category];);out center 50;"
                
                // Using a standard browser User-Agent
                val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                
                var response = ""
                try {
                    val url = URL("https://overpass-api.de/api/interpreter?data=${java.net.URLEncoder.encode(query, "UTF-8")}")
                    val connection = url.openConnection() as java.net.HttpURLConnection
                    connection.setRequestProperty("User-Agent", userAgent)
                    connection.connectTimeout = 15000
                    connection.readTimeout = 30000
                    
                    if (connection.responseCode == 200) {
                        response = connection.inputStream.bufferedReader().use { it.readText() }
                    } else {
                        Log.e("RestStops", "Primary server error: ${connection.responseCode}")
                        // Try backup server
                        val backupUrl = URL("https://overpass.kumi.systems/api/interpreter?data=${java.net.URLEncoder.encode(query, "UTF-8")}")
                        val backupConn = backupUrl.openConnection() as java.net.HttpURLConnection
                        backupConn.setRequestProperty("User-Agent", userAgent)
                        backupConn.connectTimeout = 15000
                        backupConn.readTimeout = 30000
                        if (backupConn.responseCode == 200) {
                            response = backupConn.inputStream.bufferedReader().use { it.readText() }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("RestStops", "Network exception", e)
                }

                if (response.isEmpty()) {
                    activity?.runOnUiThread {
                        if (isAdded) {
                            mapProgressBar.visibility = View.GONE
                            Toast.makeText(requireContext(), "Map service temporarily unavailable", Toast.LENGTH_SHORT).show()
                        }
                    }
                    return@thread
                }

                Log.d("RestStops", "Data received: ${response.length} chars")
                val json = JSONObject(response)
                val elements = json.getJSONArray("elements")

                val placesJson = JSONArray()
                for (i in 0 until elements.length()) {
                    val obj = elements.getJSONObject(i)
                    val pLat = if (obj.has("lat")) obj.getDouble("lat") else obj.optJSONObject("center")?.optDouble("lat") ?: continue
                    val pLng = if (obj.has("lon")) obj.getDouble("lon") else obj.optJSONObject("center")?.optDouble("lon") ?: continue
                    
                    val tags = obj.optJSONObject("tags")
                    val name = tags?.optString("name") ?: "Unnamed Place"
                    val street = tags?.optString("addr:street") ?: ""
                    val city = tags?.optString("addr:city") ?: ""
                    val address = if (street.isNotEmpty()) "$street, $city" else "Nearby Location"
                    
                    val place = JSONObject()
                    place.put("name", name)
                    place.put("address", address)
                    place.put("lat", pLat)
                    place.put("lng", pLng)
                    placesJson.put(place)
                }

                activity?.runOnUiThread {
                    if (isAdded) {
                        mapProgressBar.visibility = View.GONE
                        if (placesJson.length() == 0) {
                            Toast.makeText(requireContext(), "No places found within 5km", Toast.LENGTH_SHORT).show()
                        }
                        val base64Data = Base64.encodeToString(placesJson.toString().toByteArray(), Base64.NO_WRAP)
                        mapWebView.evaluateJavascript("javascript:addNearbyMarkersBase64('$base64Data', '$markerClass')", null)
                    }
                }
            } catch (e: Exception) {
                Log.e("RestStops", "Parsing error", e)
                activity?.runOnUiThread { 
                    if (isAdded) {
                        mapProgressBar.visibility = View.GONE
                        Toast.makeText(requireContext(), "Error updating map", Toast.LENGTH_SHORT).show()
                    }
                }
            } finally {
                activity?.runOnUiThread { if (isAdded) mapProgressBar.visibility = View.GONE }
            }
        }
    }

    inner class MapBridge {
        @JavascriptInterface
        fun onLocationSelected(lat: Double, lng: Double) {
            selectedLat = lat
            selectedLng = lng
            activity?.runOnUiThread {
                Toast.makeText(requireContext(), "Location Selected", Toast.LENGTH_SHORT).show()
            }
        }

        @JavascriptInterface
        fun navigateTo(lat: Double, lng: Double, name: String) {
            activity?.runOnUiThread {
                val intent = Intent(requireContext(), DirectionsActivity::class.java)
                intent.putExtra("DEST_LAT", lat)
                intent.putExtra("DEST_LNG", lng)
                intent.putExtra("DEST_NAME", name)
                startActivity(intent)
            }
        }
    }
}