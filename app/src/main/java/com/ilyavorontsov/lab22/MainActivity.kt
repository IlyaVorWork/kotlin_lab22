package com.ilyavorontsov.lab22

import android.Manifest
import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.MutableLiveData
import java.lang.Math.toRadians
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var locationManager: LocationManager

    private val currentLatitude: MutableLiveData<Double> = MutableLiveData()
    private val currentLongitude: MutableLiveData<Double> = MutableLiveData()
    private val distanceBetweenLocations: MutableLiveData<Double> = MutableLiveData()

    private val destinationLatitude: MutableLiveData<Double> = MutableLiveData()
    private val destinationLongitude: MutableLiveData<Double> = MutableLiveData()

    private val earthRadius = 6371000

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            currentLatitude.value = location.latitude
            currentLongitude.value = location.longitude

            if (isFirstRun) {
                if (ActivityCompat.checkSelfPermission(
                        this@MainActivity,
                        ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this@MainActivity,
                        ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestResult.launch(ACCESS_FINE_LOCATION)
                    requestResult.launch(ACCESS_COARSE_LOCATION)
                }
                generateDestinationPoint()
                isFirstRun = false
            }
        }
    }

    private lateinit var tvGameStatus: TextView
    private lateinit var tvCurrentDistance: TextView
    private lateinit var btnSetDestination: Button
    private lateinit var btnOpenSettings: Button

    private var isGameRunning: Boolean = false
    private var isFirstRun: Boolean = true

    private val requestResult = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            trackLocation()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tvGameStatus = findViewById(R.id.tvGameStatus)
        tvCurrentDistance = findViewById(R.id.tvCurrentDistance)
        btnSetDestination = findViewById(R.id.btnSetDestination)
        btnOpenSettings = findViewById(R.id.btnOpenSettings)

        btnOpenSettings.setOnClickListener {
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:${packageName}")
            )
            startActivity(intent)
        }

        btnSetDestination.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestResult.launch(ACCESS_FINE_LOCATION)
                requestResult.launch(ACCESS_COARSE_LOCATION)
            }

            generateDestinationPoint()
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestResult.launch(ACCESS_FINE_LOCATION)
            requestResult.launch(ACCESS_COARSE_LOCATION)
        } else {
            trackLocation()
        }


        currentLongitude.observe(this) {
            calculateDistance()
        }

        destinationLongitude.observe(this) {
            calculateDistance()
        }

        distanceBetweenLocations.observe(this) {
            Log.d("SET", "onCreate: ${distanceBetweenLocations.value} ${isGameRunning}")
            if (isGameRunning) {
                if (distanceBetweenLocations.value!! < 100) {
                    tvGameStatus.text = getString(R.string.destination_found)
                    tvGameStatus.setTextColor(getColor(R.color.green))
                    isGameRunning = false
                }
                tvCurrentDistance.text =
                    String.format(getString(R.string.current_distance), "%.2f".format(distanceBetweenLocations.value))
            }
        }
    }

    private fun generateDestinationPoint() {
        Log.d("GENERATE", "generateDestinationPoint: ")
        val angle: Double = Random.nextDouble() * 2 * Math.PI
        val distance: Double = Random.nextDouble() * 0.02

        isGameRunning = true
        destinationLatitude.value = currentLatitude.value?.plus(distance * sin(angle))
        destinationLongitude.value = currentLongitude.value?.plus(distance * cos(angle))
        tvGameStatus.text = getString(R.string.destination_set)
        tvGameStatus.setTextColor(getColor(R.color.blue))
    }

    private fun calculateDistance() {
        if (destinationLatitude.value != null && destinationLongitude.value != null) {
            distanceBetweenLocations.value = earthRadius * acos(
                sin(toRadians(currentLatitude.value!!)) * sin(toRadians(destinationLatitude.value!!)) + cos(
                    toRadians(
                        currentLatitude.value!!
                    )
                ) * cos(
                    toRadians(destinationLatitude.value!!)
                ) * cos(toRadians(currentLongitude.value!!) - toRadians(destinationLongitude.value!!))
            )
        }
    }

    private fun trackLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER, 1000, 10f, locationListener
        )
        locationManager.requestLocationUpdates(
            LocationManager.NETWORK_PROVIDER, 1000, 10f, locationListener
        )
    }
}