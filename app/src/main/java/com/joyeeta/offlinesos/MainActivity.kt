package com.joyeeta.offlinesos

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.telephony.SmsManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class MainActivity : ComponentActivity() {

    // =========================================================
    // LOCATION
    // =========================================================

    private lateinit var locationManager: LocationManager

    private var currentLocation by mutableStateOf<Location?>(null)
    private var gpsReady by mutableStateOf(false)

    // =========================================================
    // SMS
    // =========================================================

    private var emergencyNumber by mutableStateOf("")
    private var sosStatus by mutableStateOf("SOS CREATED")

    // =========================================================
    // NEARBY CONNECTIONS
    // =========================================================

    private val serviceId = "com.joyeeta.offlinesos"

    private val strategy = Strategy.P2P_CLUSTER

    private lateinit var connectionsClient: ConnectionsClient

    private var nearbyStarted = false

    private val connectedEndpoints = mutableSetOf<String>()
    private val connectingEndpoints = mutableSetOf<String>()

    private var connectedPhoneCount by mutableStateOf(0)

    private var nearbyStatus by mutableStateOf(
        "Checking Nearby permissions..."
    )

    // =========================================================
    // SETTINGS
    // =========================================================

    private var showSettings by mutableStateOf(false)

    // =========================================================
    // LOCATION PERMISSION
    // =========================================================

    private val locationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            val fineGranted =
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true

            val coarseGranted =
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (fineGranted || coarseGranted) {

                startGps()

                // IMPORTANT:
                // Request Nearby permissions only after
                // Location permission has been processed.
                requestNearbyPermissions()

            } else {

                gpsReady = false

                nearbyStatus =
                    "Location permission is required."
            }
        }

    // =========================================================
    // NEARBY PERMISSION
    // =========================================================

    private val nearbyPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {

            if (areNearbyPermissionsGranted()) {

                nearbyStatus =
                    "Nearby permissions granted. Starting..."

                startNearby()

            } else {

                nearbyStatus =
                    buildNearbyPermissionStatus()
            }
        }

    // =========================================================
    // SMS PERMISSION
    // =========================================================

    private val smsPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {

                sendSOS()

            } else {

                Toast.makeText(
                    this,
                    "SMS permission is required.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    // =========================================================
    // LOCATION LISTENER
    // =========================================================

    private val locationListener =
        object : LocationListener {

            override fun onLocationChanged(
                location: Location
            ) {

                currentLocation = location
                gpsReady = true
            }

            override fun onProviderEnabled(
                provider: String
            ) {

                if (provider == LocationManager.GPS_PROVIDER) {
                    gpsReady = true
                }
            }

            override fun onProviderDisabled(
                provider: String
            ) {

                if (provider == LocationManager.GPS_PROVIDER) {
                    gpsReady = false
                }
            }
        }

    // =========================================================
    // ON CREATE
    // =========================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        locationManager =
            getSystemService(LOCATION_SERVICE)
                    as LocationManager

        connectionsClient =
            Nearby.getConnectionsClient(this)

        loadEmergencyNumber()

        setContent {
            OfflineSOSScreen()
        }

        // Correct permission sequence.
        if (hasLocationPermission()) {

            startGps()
            requestNearbyPermissions()

        } else {

            requestLocationPermission()
        }
    }

    // =========================================================
    // LOCATION PERMISSION CHECK
    // =========================================================

    private fun hasLocationPermission(): Boolean {

        val fine =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        val coarse =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        return fine || coarse
    }

    private fun requestLocationPermission() {

        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    // =========================================================
    // GPS
    // =========================================================

    private fun startGps() {

        if (!hasLocationPermission()) {
            return
        }

        try {

            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000L,
                1f,
                locationListener
            )

            val lastLocation =
                locationManager.getLastKnownLocation(
                    LocationManager.GPS_PROVIDER
                )

            if (lastLocation != null) {

                currentLocation = lastLocation
                gpsReady = true
            }

        } catch (_: SecurityException) {

            gpsReady = false
        }
    }

    // =========================================================
    // NEARBY PERMISSIONS
    // =========================================================

    private fun getNearbyPermissions(): Array<String> {

        val permissions =
            mutableListOf<String>()

        // Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            permissions.add(
                Manifest.permission.BLUETOOTH_SCAN
            )

            permissions.add(
                Manifest.permission.BLUETOOTH_CONNECT
            )

            permissions.add(
                Manifest.permission.BLUETOOTH_ADVERTISE
            )
        }

        // Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            permissions.add(
                Manifest.permission.NEARBY_WIFI_DEVICES
            )
        }

        // Android 17 / API 37+
        if (Build.VERSION.SDK_INT >= 37) {

            permissions.add(
                Manifest.permission.ACCESS_LOCAL_NETWORK
            )
        }

        return permissions.toTypedArray()
    }

    private fun getMissingNearbyPermissions(): List<String> {

        return getNearbyPermissions().filter { permission ->

            ContextCompat.checkSelfPermission(
                this,
                permission
            ) != PackageManager.PERMISSION_GRANTED
        }
    }

    private fun areNearbyPermissionsGranted(): Boolean {

        return getMissingNearbyPermissions().isEmpty()
    }

    private fun buildNearbyPermissionStatus(): String {

        val missing =
            getMissingNearbyPermissions()

        if (missing.isEmpty()) {

            return "Nearby permissions granted."
        }

        val names =
            mutableListOf<String>()

        missing.forEach { permission ->

            when (permission) {

                Manifest.permission.BLUETOOTH_SCAN ->
                    names.add("Bluetooth Scan")

                Manifest.permission.BLUETOOTH_CONNECT ->
                    names.add("Bluetooth Connect")

                Manifest.permission.BLUETOOTH_ADVERTISE ->
                    names.add("Bluetooth Advertise")

                Manifest.permission.NEARBY_WIFI_DEVICES ->
                    names.add("Nearby Wi-Fi Devices")

                Manifest.permission.ACCESS_LOCAL_NETWORK ->
                    names.add("Local Network")
            }
        }

        return "Permission required: ${names.joinToString(", ")}"
    }

    private fun requestNearbyPermissions() {

        val missing =
            getMissingNearbyPermissions()

        if (missing.isEmpty()) {

            nearbyStatus =
                "Nearby permissions granted. Starting..."

            startNearby()

        } else {

            nearbyStatus =
                "Requesting Nearby permissions..."

            nearbyPermissionLauncher.launch(
                missing.toTypedArray()
            )
        }
    }

    // =========================================================
    // START NEARBY
    // =========================================================

    private fun startNearby() {

        if (nearbyStarted) {
            return
        }

        if (!areNearbyPermissionsGranted()) {

            nearbyStatus =
                buildNearbyPermissionStatus()

            return
        }

        nearbyStarted = true

        connectedEndpoints.clear()
        connectingEndpoints.clear()

        connectedPhoneCount = 0

        nearbyStatus =
            "Searching for nearby OfflineSOS phones..."

        startAdvertising()
        startDiscovery()
    }

    // =========================================================
    // ADVERTISING
    // =========================================================

    private fun startAdvertising() {

        try {

            val options =
                AdvertisingOptions.Builder()
                    .setStrategy(strategy)
                    .build()

            connectionsClient
                .startAdvertising(
                    "OfflineSOS",
                    serviceId,
                    connectionLifecycleCallback,
                    options
                )
                .addOnSuccessListener {

                    nearbyStatus =
                        "Advertising. Searching for nearby phones..."
                }
                .addOnFailureListener { exception ->

                    nearbyStatus =
                        "Advertising failed: ${exception.message}"

                    nearbyStarted = false
                }

        } catch (exception: SecurityException) {

            nearbyStatus =
                "Advertising permission denied: ${exception.message}"

            nearbyStarted = false
        }
    }

    // =========================================================
    // DISCOVERY
    // =========================================================

    private fun startDiscovery() {

        try {

            val options =
                DiscoveryOptions.Builder()
                    .setStrategy(strategy)
                    .build()

            connectionsClient
                .startDiscovery(
                    serviceId,
                    endpointDiscoveryCallback,
                    options
                )
                .addOnSuccessListener {

                    nearbyStatus =
                        "Searching for nearby OfflineSOS phones..."
                }
                .addOnFailureListener { exception ->

                    nearbyStatus =
                        "Discovery failed: ${exception.message}"

                    nearbyStarted = false
                }

        } catch (exception: SecurityException) {

            nearbyStatus =
                "Discovery permission denied: ${exception.message}"

            nearbyStarted = false
        }
    }

    // =========================================================
    // ENDPOINT DISCOVERY CALLBACK
    // =========================================================

    private val endpointDiscoveryCallback =
        object : EndpointDiscoveryCallback() {

            override fun onEndpointFound(
                endpointId: String,
                info: DiscoveredEndpointInfo
            ) {

                if (
                    connectedEndpoints.contains(endpointId) ||
                    connectingEndpoints.contains(endpointId)
                ) {
                    return
                }

                connectingEndpoints.add(endpointId)

                nearbyStatus =
                    "Nearby OfflineSOS phone found. Connecting..."

                try {

                    connectionsClient
                        .requestConnection(
                            "OfflineSOS",
                            endpointId,
                            connectionLifecycleCallback
                        )
                        .addOnSuccessListener {

                            nearbyStatus =
                                "Connection request sent..."
                        }
                        .addOnFailureListener { exception ->

                            connectingEndpoints.remove(
                                endpointId
                            )

                            nearbyStatus =
                                "Connection request failed: ${exception.message}"
                        }

                } catch (exception: SecurityException) {

                    connectingEndpoints.remove(
                        endpointId
                    )

                    nearbyStatus =
                        "Bluetooth connection permission denied."
                }
            }

            override fun onEndpointLost(
                endpointId: String
            ) {

                connectingEndpoints.remove(
                    endpointId
                )

                connectedEndpoints.remove(
                    endpointId
                )

                connectedPhoneCount =
                    connectedEndpoints.size
            }
        }

    // =========================================================
    // CONNECTION CALLBACK
    // =========================================================

    private val connectionLifecycleCallback =
        object : ConnectionLifecycleCallback() {

            override fun onConnectionInitiated(
                endpointId: String,
                connectionInfo: ConnectionInfo
            ) {

                try {

                    connectionsClient.acceptConnection(
                        endpointId,
                        payloadCallback
                    )

                } catch (exception: SecurityException) {

                    nearbyStatus =
                        "Unable to accept Nearby connection."
                }
            }

            override fun onConnectionResult(
                endpointId: String,
                result: ConnectionResolution
            ) {

                connectingEndpoints.remove(
                    endpointId
                )

                if (
                    result.status.statusCode ==
                    ConnectionsStatusCodes.STATUS_OK
                ) {

                    connectedEndpoints.add(
                        endpointId
                    )

                    connectedPhoneCount =
                        connectedEndpoints.size

                    nearbyStatus =
                        "Phone-to-phone communication active."

                } else {

                    connectedEndpoints.remove(
                        endpointId
                    )

                    connectedPhoneCount =
                        connectedEndpoints.size

                    nearbyStatus =
                        "Connection failed: " +
                                result.status.statusMessage
                }
            }

            override fun onDisconnected(
                endpointId: String
            ) {

                connectingEndpoints.remove(
                    endpointId
                )

                connectedEndpoints.remove(
                    endpointId
                )

                connectedPhoneCount =
                    connectedEndpoints.size

                if (connectedPhoneCount == 0) {

                    nearbyStatus =
                        "Waiting for nearby OfflineSOS phones..."
                }
            }
        }

    // =========================================================
    // PAYLOAD CALLBACK
    // =========================================================

    private val payloadCallback =
        object : PayloadCallback() {

            override fun onPayloadReceived(
                endpointId: String,
                payload: Payload
            ) {

                val bytes =
                    payload.asBytes()
                        ?: return

                val message =
                    String(
                        bytes,
                        Charsets.UTF_8
                    )

                handleNearbyMessage(
                    message
                )
            }

            override fun onPayloadTransferUpdate(
                endpointId: String,
                update: PayloadTransferUpdate
            ) {
                // Small SOS messages do not require
                // transfer progress handling.
            }
        }

    // =========================================================
    // HANDLE RECEIVED SOS
    // =========================================================

    private fun handleNearbyMessage(
        message: String
    ) {

        runOnUiThread {

            sosStatus =
                "SOS RECEIVED FROM NEARBY PHONE"

            Toast.makeText(
                this,
                "Nearby SOS received",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // =========================================================
    // SEND SOS
    // =========================================================

    private fun sendSOS() {

        if (emergencyNumber.length != 10) {

            Toast.makeText(
                this,
                "Enter a valid 10-digit emergency number.",
                Toast.LENGTH_LONG
            ).show()

            showSettings = true

            return
        }

        if (!hasLocationPermission()) {

            requestLocationPermission()

            return
        }

        if (!hasSmsPermission()) {

            smsPermissionLauncher.launch(
                Manifest.permission.SEND_SMS
            )

            return
        }

        val location =
            currentLocation

        if (location == null) {

            Toast.makeText(
                this,
                "GPS location is not ready.",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        val incidentId =
            UUID.randomUUID()
                .toString()
                .replace("-", "")
                .take(8)
                .uppercase()

        val timestamp =
            SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                Locale.getDefault()
            ).format(Date())

        val battery =
            getBatteryLevel()

        val message =
            """
            🚨 OFFLINESOS EMERGENCY ALERT 🚨
            
            Incident ID: $incidentId
            
            Time: $timestamp
            
            Location:
            Latitude: ${location.latitude}
            Longitude: ${location.longitude}
            Accuracy: ${location.accuracy} meters
            
            Battery: $battery%
            
            Emergency assistance required.
            """.trimIndent()

        // Send to connected OfflineSOS phones.
        sendNearbySOS(message)

        // Also send directly through cellular SMS.
        sendSOSBySMS(message)

        sosStatus =
            "SOS SENT"
    }

    // =========================================================
    // SMS
    // =========================================================

    private fun hasSmsPermission(): Boolean {

        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun sendSOSBySMS(
        message: String
    ) {

        try {

            val smsManager =
                SmsManager.getDefault()

            val parts =
                smsManager.divideMessage(
                    message
                )

            if (parts.size > 1) {

                smsManager.sendMultipartTextMessage(
                    emergencyNumber,
                    null,
                    parts,
                    null,
                    null
                )

            } else {

                smsManager.sendTextMessage(
                    emergencyNumber,
                    null,
                    message,
                    null,
                    null
                )
            }

            Toast.makeText(
                this,
                "SOS SMS sent.",
                Toast.LENGTH_LONG
            ).show()

        } catch (exception: Exception) {

            Toast.makeText(
                this,
                "SMS failed: ${exception.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // =========================================================
    // SEND THROUGH NEARBY
    // =========================================================

    private fun sendNearbySOS(
        message: String
    ) {

        if (connectedEndpoints.isEmpty()) {
            return
        }

        val payload = Payload.fromBytes(
            message.toByteArray(Charsets.UTF_8)
        )

        try {

            connectionsClient.sendPayload(
                connectedEndpoints.toList(),
                payload
            )

        } catch (exception: Exception) {

            Toast.makeText(
                this,
                "Nearby SOS failed: ${exception.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // =========================================================
    // BATTERY
    // =========================================================

    private fun getBatteryLevel(): Int {

        val batteryManager =
            getSystemService(
                BATTERY_SERVICE
            ) as android.os.BatteryManager

        return batteryManager.getIntProperty(
            android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY
        )
    }

    // =========================================================
    // EMERGENCY NUMBER
    // =========================================================

    private fun loadEmergencyNumber() {

        val preferences =
            getSharedPreferences(
                "OfflineSOS",
                MODE_PRIVATE
            )

        emergencyNumber =
            preferences.getString(
                "emergency_number",
                ""
            ) ?: ""
    }

    private fun saveEmergencyNumber(
        number: String
    ) {

        emergencyNumber = number

        getSharedPreferences(
            "OfflineSOS",
            MODE_PRIVATE
        )
            .edit()
            .putString(
                "emergency_number",
                number
            )
            .apply()
    }

    // =========================================================
    // CLEANUP
    // =========================================================

    override fun onDestroy() {

        try {

            locationManager.removeUpdates(
                locationListener
            )

        } catch (_: Exception) {
        }

        try {

            connectionsClient.stopAdvertising()
            connectionsClient.stopDiscovery()
            connectionsClient.stopAllEndpoints()

        } catch (_: Exception) {
        }

        connectedEndpoints.clear()
        connectingEndpoints.clear()

        connectedPhoneCount = 0
        nearbyStarted = false

        super.onDestroy()
    }

    // =========================================================
    // USER INTERFACE
    // =========================================================

    @Composable
    private fun OfflineSOSScreen() {

        MaterialTheme {

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFF090D15)
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    // -------------------------------------------------
                    // HEADER
                    // -------------------------------------------------

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.SpaceBetween,
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Text(
                            text = "OfflineSOS",
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedButton(
                            onClick = {
                                showSettings = true
                            }
                        ) {

                            Text("SETTINGS")
                        }
                    }

                    Spacer(
                        modifier = Modifier.height(30.dp)
                    )

                    // -------------------------------------------------
                    // TITLE
                    // -------------------------------------------------

                    Text(
                        text = "OFFLINE SOS",
                        color = Color.White,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Emergency Communication System",
                        color = Color.White,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(
                        modifier = Modifier.height(28.dp)
                    )

                    // -------------------------------------------------
                    // GPS CARD
                    // -------------------------------------------------

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor =
                                Color(0xFF171D28)
                        ),
                        shape =
                            RoundedCornerShape(22.dp)
                    ) {

                        Column(
                            modifier =
                                Modifier.padding(22.dp)
                        ) {

                            Text(
                                text =
                                    if (gpsReady)
                                        "📍 GPS: READY"
                                    else
                                        "📍 GPS: WAITING",
                                color =
                                    if (gpsReady)
                                        Color(0xFF52E889)
                                    else
                                        Color.Yellow,
                                fontSize = 22.sp,
                                fontWeight =
                                    FontWeight.Bold
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(10.dp)
                            )

                            Text(
                                text =
                                    currentLocation?.let {
                                        "Accuracy: ${
                                            String.format(
                                                "%.1f",
                                                it.accuracy
                                            )
                                        } meters"
                                    } ?: "Waiting for GPS...",
                                color = Color.White,
                                fontSize = 17.sp
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(10.dp)
                            )

                            Text(
                                text =
                                    "📡 SMS: Cellular network",
                                color = Color.White,
                                fontSize = 17.sp
                            )

                            Text(
                                text =
                                    "Internet/Wi-Fi is not required for SMS.",
                                color = Color.LightGray,
                                fontSize = 15.sp
                            )
                        }
                    }

                    Spacer(
                        modifier =
                            Modifier.height(20.dp)
                    )

                    // -------------------------------------------------
                    // OFFLINE NETWORK CARD
                    // -------------------------------------------------

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor =
                                Color(0xFF171D28)
                        ),
                        shape =
                            RoundedCornerShape(22.dp)
                    ) {

                        Column(
                            modifier =
                                Modifier.padding(22.dp)
                        ) {

                            Text(
                                text =
                                    "📡 OFFLINE NETWORK",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight =
                                    FontWeight.Bold
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(10.dp)
                            )

                            Text(
                                text =
                                    "Connected phones: $connectedPhoneCount",
                                color =
                                    Color(0xFF52E889),
                                fontSize = 19.sp,
                                fontWeight =
                                    FontWeight.Bold
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(10.dp)
                            )

                            Text(
                                text = nearbyStatus,
                                color = Color.White,
                                fontSize = 16.sp
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(15.dp)
                            )

                            if (!areNearbyPermissionsGranted()) {

                                Button(
                                    onClick = {
                                        requestNearbyPermissions()
                                    }
                                ) {

                                    Text(
                                        "GRANT NEARBY PERMISSIONS"
                                    )
                                }

                            } else {

                                Text(
                                    text =
                                        if (
                                            connectedPhoneCount > 0
                                        ) {
                                            "Phone-to-phone communication active"
                                        } else {
                                            "Waiting for nearby OfflineSOS phones..."
                                        },
                                    color =
                                        Color.LightGray,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }

                    Spacer(
                        modifier =
                            Modifier.height(25.dp)
                    )

                    // -------------------------------------------------
                    // SOS BUTTON
                    // -------------------------------------------------

                    Button(
                        onClick = {

                            if (!hasSmsPermission()) {

                                smsPermissionLauncher.launch(
                                    Manifest.permission.SEND_SMS
                                )

                            } else {

                                sendSOS()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(125.dp),
                        shape =
                            RoundedCornerShape(60.dp),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor =
                                    Color(0xFFFF3838)
                            )
                    ) {

                        Text(
                            text = "🚨  SEND SOS",
                            color = Color.White,
                            fontSize = 27.sp,
                            fontWeight =
                                FontWeight.Bold
                        )
                    }

                    Spacer(
                        modifier =
                            Modifier.height(18.dp)
                    )

                    Text(
                        text =
                            "Press SEND SOS only during an emergency.",
                        color = Color.LightGray,
                        fontSize = 15.sp,
                        textAlign =
                            TextAlign.Center
                    )

                    Spacer(
                        modifier =
                            Modifier.height(20.dp)
                    )

                    // -------------------------------------------------
                    // STATUS
                    // -------------------------------------------------

                    Card(
                        modifier =
                            Modifier.fillMaxWidth(),
                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    Color(0xFF171D28)
                            ),
                        shape =
                            RoundedCornerShape(20.dp)
                    ) {

                        Text(
                            text =
                                "🚨  $sosStatus",
                            modifier =
                                Modifier.padding(22.dp),
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight =
                                FontWeight.Bold
                        )
                    }
                }

                // -----------------------------------------------------
                // SETTINGS DIALOG
                // -----------------------------------------------------

                if (showSettings) {

                    AlertDialog(
                        onDismissRequest = {
                            showSettings = false
                        },

                        title = {
                            Text(
                                "Emergency Settings"
                            )
                        },

                        text = {

                            Column {

                                Text(
                                    "Enter emergency SMS number:"
                                )

                                Spacer(
                                    modifier =
                                        Modifier.height(10.dp)
                                )

                                OutlinedTextField(
                                    value =
                                        emergencyNumber,
                                    onValueChange = { value ->

                                        if (
                                            value.all {
                                                it.isDigit()
                                            } &&
                                            value.length <= 10
                                        ) {

                                            emergencyNumber =
                                                value
                                        }
                                    },
                                    label = {
                                        Text(
                                            "10-digit number"
                                        )
                                    },
                                    singleLine = true
                                )
                            }
                        },

                        confirmButton = {

                            TextButton(
                                onClick = {

                                    if (
                                        emergencyNumber.length == 10
                                    ) {

                                        saveEmergencyNumber(
                                            emergencyNumber
                                        )

                                        showSettings = false

                                        Toast.makeText(
                                            this@MainActivity,
                                            "Emergency number saved.",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                    } else {

                                        Toast.makeText(
                                            this@MainActivity,
                                            "Enter a valid 10-digit number.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            ) {

                                Text("SAVE")
                            }
                        },

                        dismissButton = {

                            TextButton(
                                onClick = {
                                    showSettings = false
                                }
                            ) {

                                Text("CANCEL")
                            }
                        }
                    )
                }
            }
        }
    }
}