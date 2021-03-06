package com.emarsys.sample

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.ui.NavigationUI
import com.emarsys.Emarsys
import com.emarsys.mobileengage.api.event.EventHandler
import com.emarsys.sample.extensions.showSnackBar
import com.emarsys.sample.fragments.MobileEngageFragmentTracking
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONObject


class MainActivity : AppCompatActivity(), EventHandler {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navController = Navigation.findNavController(this, R.id.nav_host_fragment)

        setupBottomNavMenu(navController)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            MobileEngageFragmentTracking.REQUEST_LOCATION_PERMISSIONS -> {
                if (grantResults.isNotEmpty() && grantResults[0] === PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        Emarsys.geofence.enable {
                            nav_host_fragment.view?.showSnackBar("Geofence has been Enabled!")
                        }
                    }
                }
                return
            }
        }
    }

    private fun setupBottomNavMenu(navController: NavController) {
        bottom_navigation_view?.let {
            NavigationUI.setupWithNavController(it, navController)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val navController = Navigation.findNavController(this, R.id.nav_host_fragment)
        val navigated = NavigationUI.onNavDestinationSelected(item, navController)
        return navigated || super.onOptionsItemSelected(item)
    }

    override fun handleEvent(context: Context, eventName: String, payload: JSONObject?) {
        Toast.makeText(this, eventName + " - " + payload.toString(), Toast.LENGTH_LONG).show()
    }

}
