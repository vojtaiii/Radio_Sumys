package com.example.rdiosum

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Window
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

    }

    // UP button press
    override fun onSupportNavigateUp(): Boolean {
        val navController = this.findNavController(R.id.myNavHostFragment)
        return navController.navigateUp()
    }
}