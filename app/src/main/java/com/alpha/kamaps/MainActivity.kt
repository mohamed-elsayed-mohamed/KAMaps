package com.alpha.kamaps

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.alpha.kamaps.databinding.ActivityMainBinding
import com.esri.arcgisruntime.ArcGISRuntimeEnvironment
import com.esri.arcgisruntime.mapping.ArcGISMap
import com.esri.arcgisruntime.mapping.BasemapStyle
import com.esri.arcgisruntime.mapping.Viewpoint
import com.esri.arcgisruntime.mapping.view.LocationDisplay
import com.esri.arcgisruntime.mapping.view.MapView

class MainActivity : AppCompatActivity() {

    private val binding: ActivityMainBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val maMap: MapView by lazy { binding.maMap }

    private val locationDisplay: LocationDisplay by lazy { maMap.locationDisplay }
    private var defaultPosition: Viewpoint = Viewpoint(26.8448466, 26.3846369, 144000.0)

    private val locationManager: LocationManager by lazy { this.getSystemService(Context.LOCATION_SERVICE) as LocationManager }
    private val receiver: GpsStatusReceiver = GpsStatusReceiver()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupMap()

        registerReceiver(receiver, IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION))

        binding.fbCurrentLocation.setOnClickListener {
            checkPermissionAndNavigate()
        }
    }

    private fun setupMap(){
        ArcGISRuntimeEnvironment.setApiKey(BuildConfig.API_KEY)
        maMap.map = ArcGISMap(BasemapStyle.ARCGIS_IMAGERY)
        maMap.setViewpoint(defaultPosition)
        locationDisplay.isShowPingAnimation = true
        checkPermissionAndNavigate()
    }

    override fun onPause() {
        maMap.pause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        maMap.resume()
    }

    override fun onDestroy() {
        maMap.dispose()
        unregisterReceiver(receiver)
        super.onDestroy()
    }

    private fun checkPermissionAndNavigate() {
        val reqPermissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        val permissionCheckFineLocation =
            ContextCompat.checkSelfPermission(this@MainActivity, reqPermissions[0]) ==
                    PackageManager.PERMISSION_GRANTED

        val permissionCheckCoarseLocation =
            ContextCompat.checkSelfPermission(this@MainActivity, reqPermissions[1]) ==
                    PackageManager.PERMISSION_GRANTED

        if (!(permissionCheckFineLocation && permissionCheckCoarseLocation)) {
            ActivityCompat.requestPermissions(this@MainActivity, reqPermissions, REQUEST_CODE)
        } else {
            checkGPS()
            navigate()
        }
    }

    private fun checkGPS(){
        if(!locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            val alertDialog =
                builder.setMessage(getString(R.string.noGPSMsg))
                    .setPositiveButton(
                        getString(R.string.yes)
                    ) { _, _ -> startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }
                    .setNegativeButton(
                        getString(R.string.no)
                    ) { dialog, _ -> dialog.cancel() }.create()

            alertDialog.show()
        }
    }

    private fun navigate(){
        locationDisplay.autoPanMode = LocationDisplay.AutoPanMode.RECENTER
        locationDisplay.startAsync()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    checkPermissionAndNavigate()
                else
                    Toast.makeText(this, getString(R.string.denyPermission), Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val REQUEST_CODE = 101
    }

    private inner class GpsStatusReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if(locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
                navigate()
        }
    }
}