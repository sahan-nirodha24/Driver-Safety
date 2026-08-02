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
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
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

    private lateinit var btnCoffeeShops: TextView
    private lateinit var btnRestaurants: TextView
    private lateinit var btnGasStations: TextView

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
        
        btnCoffeeShops = view.findViewById(R.id.btnCoffeeShops)
        btnRestaurants = view.findViewById(R.id.btnRestaurants)
        btnGasStations = view.findViewById(R.id.btnGasStations)

        setupWebView()

        val etSearch: EditText = view.findViewById(R.id.etSearch)
        val btnClearSearch: ImageView = view.findViewById(R.id.btnClearSearch)

        etSearch.addTextChangedListener { text ->
            btnClearSearch.visibility = if (text.isNullOrEmpty()) View.GONE else View.VISIBLE
        }

        btnClearSearch.setOnClickListener {
            etSearch.text.clear()
            selectedLat = 6.9271
            selectedLng = 79.8612
            resetChips()
            mapWebView.evaluateJavascript("javascript:clearMap()", null)
            mapWebView.evaluateJavascript("javascript:updateTheme(false)", null)
            Toast.makeText(requireContext(), "Search cleared", Toast.LENGTH_SHORT).show()
        }

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

        btnCoffeeShops.setOnClickListener {
            setActiveChip(btnCoffeeShops)
            fetchAndShowNearby("amenity=cafe", "marker-orange")
        }
        btnRestaurants.setOnClickListener {
            setActiveChip(btnRestaurants)
            fetchAndShowNearby("amenity=restaurant", "marker-red")
        }
        btnGasStations.setOnClickListener {
            setActiveChip(btnGasStations)
            fetchAndShowNearby("amenity=fuel", "marker-green")
        }
    }

    private fun setActiveChip(activeChip: TextView) {
        resetChips()
        activeChip.setBackgroundResource(R.drawable.bg_surface_rounded)
        activeChip.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#1380ec"))
    }

    private fun resetChips() {
        val chips = listOf(btnCoffeeShops, btnRestaurants, btnGasStations)
        chips.forEach { chip ->
            chip.setBackgroundResource(R.drawable.bg_surface_rounded)
            chip.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#233648"))
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
                            Toast.makeText(requireContext(), "Location updated: $query", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    activity?.runOnUiThread { 
                        if (isAdded) {
                            mapProgressBar.visibility = View.GONE
                            Toast.makeText(requireContext(), "Location not found", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                activity?.runOnUiThread { 
                    if (isAdded) {
                        mapProgressBar.visibility = View.GONE
                        Toast.makeText(requireContext(), "Search error: Check connection", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun fetchAndShowNearby(category: String, markerClass: String) {
        if (!isAdded) return
        mapProgressBar.visibility = View.VISIBLE
        thread {
            try {
                activity?.runOnUiThread { Toast.makeText(requireContext(), "Preparing Overpass request...", Toast.LENGTH_SHORT).show() }
                
                // Refined Overpass query: radius 5km, max 100 results, including centers for ways/relations
                val query = "[out:json][timeout:30];(node(around:5000,$selectedLat,$selectedLng)[$category];way(around:5000,$selectedLat,$selectedLng)[$category];rel(around:5000,$selectedLat,$selectedLng)[$category];);out 100 center;"
                Log.d("RestStops", "Query: $query")

                val userAgent = "DriverSafetyApp/1.2 (Android; Testing; RetryLogic)"
                var response = ""
                
                val endpoints = listOf(
                    "https://overpass-api.de/api/interpreter",
                    "https://lz4.overpass-api.de/api/interpreter",
                    "https://z.overpass-api.de/api/interpreter",
                    "https://overpass.kumi.systems/api/interpreter",
                    "https://overpass.osm.ch/api/interpreter"
                )

                try {
                    val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
                    val postData = "data=$encodedQuery"
                    
                    for (baseUrl in endpoints) {
                        try {
                            val url = URL(baseUrl)
                            Log.d("RestStops", "Attempting fetch from: $baseUrl")
                            activity?.runOnUiThread { Toast.makeText(requireContext(), "Trying server: ${baseUrl.substringAfter("://").substringBefore("/")}", Toast.LENGTH_SHORT).show() }
                            
                            val connection = url.openConnection() as java.net.HttpURLConnection
                            connection.requestMethod = "POST"
                            connection.setRequestProperty("User-Agent", userAgent)
                            connection.setRequestProperty("Accept", "application/json")
                            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                            connection.doOutput = true
                            connection.connectTimeout = 10000 // 10s connect
                            connection.readTimeout = 20000    // 20s read
                            
                            connection.outputStream.use { it.write(postData.toByteArray()) }
                            
                            val responseCode = connection.responseCode
                            Log.d("RestStops", "Response Code from $baseUrl: $responseCode")
                            
                            if (responseCode == 200) {
                                response = connection.inputStream.bufferedReader().use { it.readText() }
                                if (response.isNotEmpty()) {
                                    Log.d("RestStops", "Success from $baseUrl")
                                    break // Success! Exit the loop
                                }
                            } else {
                                Log.e("RestStops", "Server $baseUrl returned $responseCode")
                            }
                        } catch (e: Exception) {
                            Log.e("RestStops", "Failed to fetch from $baseUrl: ${e.message}")
                            // Continue to next endpoint
                        }
                    }
                } catch (e: Exception) {
                    Log.e("RestStops", "Fatal encoding error", e)
                }

                if (response.isEmpty()) {
                    activity?.runOnUiThread {
                        if (isAdded) {
                            mapProgressBar.visibility = View.GONE
                            Toast.makeText(requireContext(), "Failed to fetch map data. Service may be down.", Toast.LENGTH_LONG).show()
                        }
                    }
                    return@thread
                }

                Log.d("RestStops", "Response Length: ${response.length}")
                activity?.runOnUiThread { Toast.makeText(requireContext(), "Parsing JSON response...", Toast.LENGTH_SHORT).show() }
                
                val json = JSONObject(response)
                val elements = json.getJSONArray("elements")
                val placesJson = JSONArray()

                for (i in 0 until elements.length()) {
                    val obj = elements.getJSONObject(i)
                    // Nodes have lat/lon directly. Ways/Relations have them in "center" object if using "out center"
                    val pLat = if (obj.has("lat")) {
                        obj.getDouble("lat")
                    } else {
                        obj.optJSONObject("center")?.optDouble("lat") ?: continue
                    }
                    
                    val pLng = if (obj.has("lon")) {
                        obj.getDouble("lon")
                    } else {
                        obj.optJSONObject("center")?.optDouble("lon") ?: continue
                    }
                    
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
                            Toast.makeText(requireContext(), "No results found in this area", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(requireContext(), "Success: Found ${placesJson.length()} locations", Toast.LENGTH_SHORT).show()
                        }
                        
                        Log.d("RestStops", "Sending ${placesJson.length()} places to WebView")
                        mapWebView.evaluateJavascript("javascript:updateTheme(true)", null)
                        val base64Data = Base64.encodeToString(placesJson.toString().toByteArray(), Base64.NO_WRAP)
                        mapWebView.evaluateJavascript("javascript:addNearbyMarkersBase64('$base64Data', '$markerClass')", null)
                    }
                }
            } catch (e: Exception) {
                Log.e("RestStops", "Processing error", e)
                activity?.runOnUiThread { 
                    if (isAdded) {
                        mapProgressBar.visibility = View.GONE
                        Toast.makeText(requireContext(), "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }
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