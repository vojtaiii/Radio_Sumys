package com.example.rdiosum

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Window
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI
import com.example.rdiosum.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // hide fragment titles
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        // setup binding variable
        val binding = DataBindingUtil.setContentView<ActivityMainBinding>(this,
            R.layout.activity_main)

        // find the navigation controller
        val navController = this.findNavController(R.id.myNavHostFragment)
        // link the navigation controller to the app bar
        NavigationUI.setupActionBarWithNavController(this, navController)

        checkPermissions()

    }

    // UP button press
    override fun onSupportNavigateUp(): Boolean {
        val navController = this.findNavController(R.id.myNavHostFragment)
        return navController.navigateUp()
    }

    /**
     * Checks whether app has ALL permissions, otherwise asks for it
     */
    private fun checkPermissions() {
        var hasPermission = true
        val permissionNeeded = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.INTERNET,
                Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_NETWORK_STATE
        )

        for(element in permissionNeeded){
            if (ContextCompat.checkSelfPermission(this, element) != PackageManager.PERMISSION_GRANTED){
                hasPermission = false
            }
        }
        if (!hasPermission) ActivityCompat.requestPermissions(this, permissionNeeded,0)
    }
}