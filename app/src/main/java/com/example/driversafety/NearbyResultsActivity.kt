package com.example.driversafety

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject
import java.net.URL
import kotlin.concurrent.thread

data class Place(val name: String, val address: String, val type: String, val lat: Double, val lng: Double)

class NearbyResultsActivity : AppCompatActivity() {

    private lateinit var rvPlaces: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvTitle: TextView
    private val placesList = mutableListOf<Place>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nearby_results)

        rvPlaces = findViewById(R.id.rvPlaces)
        progressBar = findViewById(R.id.progressBar)
        tvTitle = findViewById(R.id.tvTitle)

        val btnBack: ImageView = findViewById(R.id.btnBack)
        btnBack.setOnClickListener { finish() }

        val lat = intent.getDoubleExtra("LAT", 0.0)
        val lng = intent.getDoubleExtra("LNG", 0.0)
        val category = intent.getStringExtra("CATEGORY") ?: "amenity=cafe"
        val title = intent.getStringExtra("TITLE") ?: "Nearby"

        tvTitle.text = title
        rvPlaces.layoutManager = LinearLayoutManager(this)
        rvPlaces.adapter = PlaceAdapter(placesList) { place ->
            val intent = Intent(this, DirectionsActivity::class.java)
            intent.putExtra("DEST_LAT", place.lat)
            intent.putExtra("DEST_LNG", place.lng)
            intent.putExtra("DEST_NAME", place.name)
            startActivity(intent)
        }

        fetchNearbyPlaces(lat, lng, category)
    }

    private fun fetchNearbyPlaces(lat: Double, lng: Double, category: String) {
        progressBar.visibility = View.VISIBLE
        thread {
            try {
                // Improved Overpass API Query: node, way, and relation with 10km radius
                val query = "[out:json];(node(around:10000,$lat,$lng)[$category];way(around:10000,$lat,$lng)[$category];rel(around:10000,$lat,$lng)[$category];);out center;"
                val url = URL("https://overpass-api.de/api/interpreter?data=${java.net.URLEncoder.encode(query, "UTF-8")}")
                val connection = url.openConnection()
                connection.setRequestProperty("User-Agent", "DriverSafetyApp/1.0")
                val response = connection.getInputStream().bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val elements = json.getJSONArray("elements")

                placesList.clear()
                for (i in 0 until elements.length()) {
                    val obj = elements.getJSONObject(i)
                    
                    // Handle coordinates for nodes and center coordinates for ways/relations
                    val pLat = if (obj.has("lat")) obj.getDouble("lat") else obj.getJSONObject("center").getDouble("lat")
                    val pLng = if (obj.has("lon")) obj.getDouble("lon") else obj.getJSONObject("center").getDouble("lon")
                    
                    val tags = obj.optJSONObject("tags")
                    val name = tags?.optString("name") ?: "Unnamed Place"
                    val street = tags?.optString("addr:street") ?: ""
                    val city = tags?.optString("addr:city") ?: ""
                    val address = if (street.isNotEmpty()) "$street, $city" else "Nearby Location"
                    
                    placesList.add(Place(name, address, category, pLat, pLng))
                }

                runOnUiThread {
                    progressBar.visibility = View.GONE
                    rvPlaces.adapter?.notifyDataSetChanged()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    progressBar.visibility = View.GONE
                }
            }
        }
    }

    class PlaceAdapter(
        private val places: List<Place>,
        private val onItemClick: (Place) -> Unit
    ) : RecyclerView.Adapter<PlaceAdapter.ViewHolder>() {
        
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tvName)
            val tvAddress: TextView = view.findViewById(R.id.tvAddress)
            val ivIcon: ImageView = view.findViewById(R.id.ivIcon)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_place, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val place = places[position]
            holder.tvName.text = place.name
            holder.tvAddress.text = place.address
            
            val iconRes = when {
                place.type.contains("cafe") -> R.drawable.cafe
                place.type.contains("restaurant") -> R.drawable.dinner
                place.type.contains("fuel") -> R.drawable.fuel
                else -> R.drawable.cafe
            }
            holder.ivIcon.setImageResource(iconRes)
            
            holder.itemView.setOnClickListener {
                onItemClick(place)
            }
        }

        override fun getItemCount() = places.size
    }
}