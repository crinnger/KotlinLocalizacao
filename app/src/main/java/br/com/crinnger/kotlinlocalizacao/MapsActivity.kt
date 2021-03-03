package br.com.crinnger.kotlinlocalizacao

import android.Manifest
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.Address
import android.location.Geocoder
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import java.util.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var lastLocation :Location

    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private var locationUpdateState = false

    companion object{
        private const val PERMISION_LOCATION_REQUEST_CODE=1001
        private const val REQUEST_CHECK_SETTINGS = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        fusedLocationClient=LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback(){
            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)
                lastLocation=p0.lastLocation
                placeMarkerOnMap(LatLng(lastLocation.latitude,lastLocation.longitude))
            }
        }

        createLocationRequest()
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        // Add a marker in Sydney and move the camera
        /**val meuLugar = LatLng(40.73,-73.99)
        mMap.addMarker(MarkerOptions().position(meuLugar).title("Marker in Minha Cidade"))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(meuLugar))
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(meuLugar,12.0F))**/

        mMap.getUiSettings().setZoomControlsEnabled(true)
        mMap.setOnMarkerClickListener(this)

        setLocation()
    }

    private fun setLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISION_LOCATION_REQUEST_CODE
            )
            return
        }
        mMap.isMyLocationEnabled=true
        mMap.mapType = GoogleMap.MAP_TYPE_HYBRID

        fusedLocationClient.lastLocation.addOnSuccessListener(this){ location ->
            if(location!=null){
                lastLocation = location
                val currentLatLng = LatLng(location.latitude,location.longitude)
                placeMarkerOnMap(currentLatLng)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng,12f))
            }
        }
    }

    override fun onMarkerClick(p0: Marker?): Boolean {
        return false
    }

    // colocar uma marca no mapa com icone custom
    private fun placeMarkerOnMap(location:LatLng){
        val markerOptions = MarkerOptions().position(location)
        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(resources,R.mipmap.ic_user_location)))

        val title = getAdress(location)
        markerOptions.title(title)
        mMap.addMarker(markerOptions)
    }

    // obtem o endereco do location
    private fun getAdress(location: LatLng): String{
        val geocoder:Geocoder
        val addresses:List<Address>
        geocoder= Geocoder(this, Locale.getDefault())

        addresses = geocoder.getFromLocation(location.latitude,location.longitude,1)
        val address =addresses[0].getAddressLine(0)
        val city = addresses[0].locality
        val state = addresses[0].adminArea
        val country = addresses[0].countryName
        val postalCode = addresses[0].postalCode
        return address
    }

    private fun startLocationUpdate(){
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISION_LOCATION_REQUEST_CODE
            )
            return
        }

        fusedLocationClient.requestLocationUpdates(locationRequest,locationCallback,null)
    }

    private fun createLocationRequest(){
        locationRequest= LocationRequest()
        locationRequest.interval=10000
        locationRequest.fastestInterval=5000
        locationRequest.priority=LocationRequest.PRIORITY_HIGH_ACCURACY

        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

        val client = LocationServices.getSettingsClient(this)
        val task = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            locationUpdateState=true
            startLocationUpdate()
        }

        task.addOnFailureListener { e->
            if(e is ResolvableApiException){
                try {
                    e.startResolutionForResult(this@MapsActivity, REQUEST_CHECK_SETTINGS)
                } catch(sendEx : IntentSender.SendIntentException){

                }

            }
        }
    }

    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        locationUpdateState=false
    }

    override fun onResume() {
        super.onResume()
        if(!locationUpdateState){
            startLocationUpdate()
        }
    }
}