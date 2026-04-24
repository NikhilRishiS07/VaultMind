package com.example.vaultmind

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.textfield.TextInputEditText
import eightbitlab.com.blurview.BlurView
import eightbitlab.com.blurview.RenderEffectBlur

class MainActivity : AppCompatActivity() {
    private lateinit var navController: NavController
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var bottomNavBlur: BlurView
    private val lockManager by lazy { AppGraph.lockManager(this) }
    private var lockDialog: AlertDialog? = null

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

        showUnlockIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        showUnlockIfNeeded()
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) {
            lockManager.markLocked()
        }
    }

    private fun showUnlockIfNeeded() {
        if (!lockManager.isLocked() || (lockDialog?.isShowing == true)) return

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_app_unlock, null)
        val usernameInput = dialogView.findViewById<TextInputEditText>(R.id.unlockUsernameInput)
        val passwordInput = dialogView.findViewById<TextInputEditText>(R.id.unlockPasswordInput)
        val errorText = dialogView.findViewById<TextView>(R.id.unlockErrorText)
        val loginButton = dialogView.findViewById<MaterialButton>(R.id.unlockLoginButton)
        val biometricButton = dialogView.findViewById<MaterialButton>(R.id.unlockBiometricButton)
        val loginCircle = dialogView.findViewById<View>(R.id.unlockLoginCircle)
        val biometricCircle = dialogView.findViewById<View>(R.id.unlockBiometricCircle)

        usernameInput.setText(lockManager.tempUsername())
        errorText.visibility = android.view.View.GONE

        applyAnimatedButtonBehavior(loginButton, loginCircle)
        applyAnimatedButtonBehavior(biometricButton, biometricCircle)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        fun unlockWithTransition() {
            dialogView.animate()
                .alpha(0f)
                .scaleX(0.96f)
                .scaleY(0.96f)
                .translationY(-24f)
                .setDuration(280)
                .setInterpolator(FastOutSlowInInterpolator())
                .withEndAction {
                    dialog.dismiss()
                    lockDialog = null
                    val content = window.decorView.findViewById<ViewGroup>(android.R.id.content)
                    content.scaleX = 0.985f
                    content.scaleY = 0.985f
                    content.alpha = 0.92f
                    content.animate()
                        .alpha(1f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(220)
                        .setInterpolator(FastOutSlowInInterpolator())
                        .start()
                }
                .start()
        }

        loginButton.setOnClickListener {
            val username = usernameInput.text?.toString().orEmpty().trim()
            val password = passwordInput.text?.toString().orEmpty()

            if (username.isBlank()) {
                errorText.text = "Enter your username"
                errorText.visibility = android.view.View.VISIBLE
                Toast.makeText(this, "Please enter your username", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.isBlank()) {
                errorText.text = "Enter your password"
                errorText.visibility = android.view.View.VISIBLE
                Toast.makeText(this, "Please enter your password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (lockManager.verifyCredentials(username, password)) {
                errorText.visibility = android.view.View.GONE
                unlockWithTransition()
            } else {
                errorText.text = "Invalid credentials"
                errorText.visibility = android.view.View.VISIBLE
                Toast.makeText(this, "Invalid username or password", Toast.LENGTH_SHORT).show()
            }
        }

        biometricButton.setOnClickListener {
            Toast.makeText(this, "Biometric unlock is not configured yet", Toast.LENGTH_SHORT).show()
        }

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
        dialog.window?.attributes = dialog.window?.attributes?.apply {
            blurBehindRadius = 120
            dimAmount = 1.0f
        }
        dialog.show()
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        lockDialog = dialog
        dialog.setOnDismissListener { lockDialog = null }
    }

    private fun applyAnimatedButtonBehavior(button: MaterialButton, circle: View) {
        button.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    view.animate().scaleX(0.95f).scaleY(0.95f).setDuration(120).start()
                    circle.scaleX = 1f
                    circle.scaleY = 1f
                    circle.alpha = 0.18f
                    circle.animate().scaleX(11f).scaleY(11f).alpha(0f).setDuration(650).start()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> view.animate().scaleX(1f).scaleY(1f).setDuration(180).start()
            }
            false
        }
    }
}