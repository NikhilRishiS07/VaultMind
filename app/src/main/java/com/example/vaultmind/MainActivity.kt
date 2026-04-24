package com.example.vaultmind

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import eightbitlab.com.blurview.BlurView
import eightbitlab.com.blurview.RenderEffectBlur

class MainActivity : AppCompatActivity() {
    private lateinit var navController: NavController
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var bottomNavBlur: BlurView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        bottomNav = findViewById(R.id.bottom_nav)
        bottomNavBlur = findViewById(R.id.bottom_nav_blur)

        val contentView = window.decorView.findViewById<android.view.ViewGroup>(android.R.id.content)
        val windowBackground = window.decorView.background ?: ColorDrawable(Color.TRANSPARENT)
        bottomNavBlur
            .setupWith(contentView, RenderEffectBlur())
            .setFrameClearDrawable(windowBackground)
            .setBlurRadius(18f)
            .setBlurAutoUpdate(true)

        bottomNav.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val showBottomBar = destination.id != R.id.noteEditorFragment
            bottomNav.isVisible = showBottomBar
            bottomNavBlur.isVisible = showBottomBar
        }
    }
}