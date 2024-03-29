package cz.sumys.rdiosum.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Window
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI
import com.google.android.material.bottomnavigation.BottomNavigationView
import cz.sumys.rdiosum.R
import cz.sumys.rdiosum.databinding.ActivityMainBinding
import cz.sumys.rdiosum.utilities.BackgroundSumysService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import android.content.pm.PackageInfo
import android.util.Base64
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException


class MainActivity : AppCompatActivity() {
    val log: Logger = LoggerFactory.getLogger(MainActivity::class.java)
    private lateinit var notificationManager: NotificationManagerCompat
    private lateinit var toolBar: ActionBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // hide fragment titles
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        // setup binding variable
        val binding = DataBindingUtil.setContentView<ActivityMainBinding>(this,
                R.layout.activity_main)

        // setup bottom action bar
        toolBar = supportActionBar!!
        val bottomNavigation: BottomNavigationView = binding.bottomNavigation

        // find the navigation controller
        val navController = this.findNavController(R.id.myNavHostFragment)
        // link the navigation controller to the app three dots bar
        NavigationUI.setupActionBarWithNavController(this, navController)
        // link the navigation controller with the bottom navigation bar
        NavigationUI.setupWithNavController(bottomNavigation, navController)

        // setup notifications manager
        notificationManager = NotificationManagerCompat.from(this)

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
        if (!hasPermission) ActivityCompat.requestPermissions(this, permissionNeeded, 0)
    }

    /**
     * Hide notification about playing content and unregister the receiver
     */
    override fun onDestroy() {
        notificationManager.cancel(1)
        // stop the streaming service if god forbid still playing
        // this stop the streaming service
        val sumysIntent = Intent(applicationContext, BackgroundSumysService::class.java)
        applicationContext?.stopService(sumysIntent)
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(Intent("NOTIFICATION_DISMISSED"))

        log.debug("Activity destroyed")
        super.onDestroy()
    }
}