package com.example.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class checkInActivity : AppCompatActivity() {

    private lateinit var locationClient: FusedLocationProviderClient

    companion object {
        private const val MY_PERMISSIONS_REQUEST_LOCATION = 1
    }

    @SuppressLint("MissingInflatedId", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_check_in)
        val auth = FirebaseAuth.getInstance()
        val currentTimeZone = TimeZone.getDefault()
        val dateFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        dateFormat.timeZone = currentTimeZone
        val currentTime = dateFormat.format(Date())
        val currentTimeTextView = findViewById<TextView>(R.id.currentTimeTextView)
        val btn = findViewById<Button>(R.id.authButton)
        currentTimeTextView.text = "Current Time: $currentTime"

        locationClient = LocationServices.getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                MY_PERMISSIONS_REQUEST_LOCATION
            )
        } else {
            val id = auth.currentUser?.uid
            fetchLocationAndSaveAttendance(id, "Check-In")
        }

        btn.setOnClickListener {
            val id = auth.currentUser?.uid
            fetchLocationAndSaveAttendance(id, "Check-in")
        }
    }

    private fun fetchLocationAndSaveAttendance(uid: String?, activityType: String) {
        fetchLocation { location ->
            if (location != null) {
                // Call saveAttendance with the fetched location
                saveAttendance(uid, activityType, location)
            } else {
                showToast("Location is null")
            }
        }
    }

    private fun fetchLocation(locationCallback: (location: String?) -> Unit) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        val latitude = location.latitude
                        val longitude = location.longitude
                        val locationString = "$latitude, $longitude"
                        locationCallback.invoke(locationString)
                    } else {
                        locationCallback.invoke(null)
                    }
                }
                .addOnFailureListener {
                    showToast("Failed to fetch location")
                    locationCallback.invoke(null)
                }
        } else {
            showToast("Location permissions not granted")
            locationCallback.invoke(null)
        }
    }

    private fun saveAttendance(uid: String?, activityType: String, location: String?) {
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val db = FirebaseFirestore.getInstance()

        val attendanceDocumentRef = db.collection("employeeAttendance")
            .document(uid ?: "")
            .collection(currentDate)
            .document("attendance")

        attendanceDocumentRef.get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val documentSnapshot = task.result
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        val attendanceDetails = hashMapOf(
                            "Check-In Time" to FieldValue.serverTimestamp(),
                            "Check-In Location" to location,
                            "Check-Out Time" to null,
                            "Check-Out Location" to null
                        )

                        attendanceDocumentRef.update(attendanceDetails)
                            .addOnSuccessListener {
                                Toast.makeText(
                                    this,
                                    "$activityType successful",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    this,
                                    "Failed to record $activityType details",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    } else {
                        // Document does not exist, create a new one
                        val attendanceDetails = hashMapOf(
                            "Check-In Time" to FieldValue.serverTimestamp(),
                            "Check-In Location" to location,
                            "Check-Out Time" to null,
                            "Check-Out Location" to null
                        )

                        attendanceDocumentRef.set(attendanceDetails)
                            .addOnSuccessListener {
                                Toast.makeText(
                                    this,
                                    "$activityType successful",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    this,
                                    "Failed to record $activityType details",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                } else {
                    showToast("Failed to check attendance document")
                }
            }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == MY_PERMISSIONS_REQUEST_LOCATION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val auth = FirebaseAuth.getInstance()
                val id = auth.currentUser?.uid
                fetchLocationAndSaveAttendance(id, "Check-IN")
            } else {
                showToast("Location permission denied")
            }
        }
    }
}


