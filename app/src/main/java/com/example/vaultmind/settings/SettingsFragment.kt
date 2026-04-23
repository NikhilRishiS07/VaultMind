package com.example.vaultmind.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.content.Context
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.button.MaterialButton
import com.example.vaultmind.R

class SettingsFragment : Fragment() {
    private val prefs by lazy {
        requireContext().getSharedPreferences("vaultmind_settings", Context.MODE_PRIVATE)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_settings, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val biometricSwitch = view.findViewById<MaterialSwitch>(R.id.biometricSwitch)
        val darkModeSwitch = view.findViewById<MaterialSwitch>(R.id.darkModeSwitch)
        val aiSwitch = view.findViewById<MaterialSwitch>(R.id.aiSwitch)

        biometricSwitch.isChecked = prefs.getBoolean("biometric", true)
        darkModeSwitch.isChecked = prefs.getBoolean("dark_mode", true)
        aiSwitch.isChecked = prefs.getBoolean("ai", true)

        biometricSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("biometric", checked).apply()
            Toast.makeText(requireContext(), if (checked) "Biometric enabled" else "Biometric disabled", Toast.LENGTH_SHORT).show()
        }

        darkModeSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("dark_mode", checked).apply()
            Toast.makeText(requireContext(), if (checked) "Dark mode on" else "Dark mode off", Toast.LENGTH_SHORT).show()
        }

        aiSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("ai", checked).apply()
            Toast.makeText(requireContext(), if (checked) "AI enabled" else "AI disabled", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<MaterialButton>(R.id.autoLock15Button).setOnClickListener { setAutoLock(15) }
        view.findViewById<MaterialButton>(R.id.autoLock30Button).setOnClickListener { setAutoLock(30) }
        view.findViewById<MaterialButton>(R.id.autoLock60Button).setOnClickListener { setAutoLock(60) }

        view.findViewById<MaterialButton>(R.id.changePinButton).setOnClickListener {
            Toast.makeText(requireContext(), "Change PIN flow will be wired next", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<MaterialButton>(R.id.exportBackupButton).setOnClickListener {
            Toast.makeText(requireContext(), "Backup export placeholder ready", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setAutoLock(minutes: Int) {
        prefs.edit().putInt("auto_lock_minutes", minutes).apply()
        Toast.makeText(requireContext(), "Auto lock set to $minutes minutes", Toast.LENGTH_SHORT).show()
    }
}