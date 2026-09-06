package com.shadowcalc.app

import android.os.Bundle
import android.view.WindowManager
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.shadowcalc.app.databinding.ActivitySettingsBinding
import java.io.File

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var securityManager: SecurityManager
    private lateinit var vaultManager: VaultManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        securityManager = SecurityManager(this)
        vaultManager = VaultManager(this, securityManager)
        binding.btnBack.setOnClickListener { finish() }

        refreshUI()
        setupStorageSection()
        setupAutoLock()

        binding.btnChangePin.setOnClickListener { showChangePinDialog() }
        binding.btnDecoyPin.setOnClickListener { showDecoyPinDialog() }
        binding.btnRecovery.setOnClickListener { showRecoveryDialog() }
        binding.btnForgotPin.setOnClickListener { showForgotPinDialog() }
        binding.btnTheme.setOnClickListener { showThemePicker() }
        binding.btnReset.setOnClickListener { showResetDialog() }
    }

    private fun refreshUI() {
        binding.tvDecoyStatus.text = if (securityManager.hasDecoyPin()) "Active" else "Not set"
        val accentName = ThemeManager.getAccentNameList().find { it.first == securityManager.getThemeAccent() }?.second ?: "Neon Green"
        binding.tvTheme.text = accentName
    }

    private fun setupStorageSection() {
        val breakdown = vaultManager.getStorageBreakdown()
        val totalUsed = breakdown.values.sum()
        binding.tvStorageTotal.text = "Used ${formatSize(totalUsed)}"
        binding.tvPhotoSize.text = "Photos: ${formatSize(breakdown["image"] ?: 0)}"
        binding.tvVideoSize.text = "Videos: ${formatSize(breakdown["video"] ?: 0)}"
        binding.tvAudioSize.text = "Audio: ${formatSize(breakdown["audio"] ?: 0)}"
        binding.tvOtherSize.text = "Other: ${formatSize(breakdown["file"] ?: 0)}"
        val percent = ((totalUsed.toFloat() / (10L * 1024 * 1024 * 1024)) * 100).toInt().coerceAtMost(100)
        binding.progressStorage.progress = percent
    }

    private fun setupAutoLock() {
        val minutes = securityManager.getAutoLockMinutes()
        binding.tvAutoLockValue.text = "$minutes min"
        binding.seekAutoLock.progress = minutes
        binding.seekAutoLock.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = progress.coerceAtLeast(1)
                binding.tvAutoLockValue.text = "$value min"
                securityManager.setAutoLockMinutes(value)
                AutoLockManager.getInstance().updateAutoLockMinutes(value)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun formatSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        if (bytes < 1024 * 1024) return "${bytes / 1024} KB"
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
    }

    private fun showChangePinDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_change_pin, null)
        val etOld = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etOldPin)
        val etNew = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etNewPin)
        val etConfirm = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etConfirmPin)
        MaterialAlertDialogBuilder(this, R.style.DarkAlertDialog)
            .setTitle("Change PIN").setView(view)
            .setPositiveButton("Change") { _, _ ->
                val old = etOld.text.toString()
                val newPin = etNew.text.toString()
                val confirm = etConfirm.text.toString()
                when {
                    !securityManager.validatePin(old) -> Toast.makeText(this, "Wrong current PIN", Toast.LENGTH_SHORT).show()
                    newPin.length < 4 -> Toast.makeText(this, "PIN must be 4+ digits", Toast.LENGTH_SHORT).show()
                    newPin != confirm -> Toast.makeText(this, "PINs don't match", Toast.LENGTH_SHORT).show()
                    else -> { securityManager.setPin(newPin); Toast.makeText(this, "PIN changed!", Toast.LENGTH_SHORT).show() }
                }
            }
            .setNegativeButton("Cancel", null).show()
    }

    private fun showDecoyPinDialog() {
        if (securityManager.hasDecoyPin()) {
            AlertDialog.Builder(this, R.style.DarkAlertDialog)
                .setTitle("Decoy PIN")
                .setMessage("Decoy PIN is already set. Remove it?")
                .setPositiveButton("Remove") { _, _ ->
                    securityManager.clearDecoyPin()
                    refreshUI()
                    Toast.makeText(this, "Decoy PIN removed", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Keep", null).show()
            return
        }
        val view = layoutInflater.inflate(R.layout.dialog_change_pin, null)
        val etOld = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etOldPin)
        val etNew = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etNewPin)
        val etConfirm = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etConfirmPin)
        etOld.hint = "Current PIN"
        MaterialAlertDialogBuilder(this, R.style.DarkAlertDialog)
            .setTitle("Set Decoy PIN").setView(view)
            .setPositiveButton("Set") { _, _ ->
                val old = etOld.text.toString()
                val newPin = etNew.text.toString()
                val confirm = etConfirm.text.toString()
                when {
                    !securityManager.validatePin(old) -> Toast.makeText(this, "Wrong current PIN", Toast.LENGTH_SHORT).show()
                    newPin.length < 4 -> Toast.makeText(this, "PIN must be 4+ digits", Toast.LENGTH_SHORT).show()
                    newPin == securityManager.getPin() -> Toast.makeText(this, "Decoy PIN cannot match real PIN", Toast.LENGTH_SHORT).show()
                    newPin != confirm -> Toast.makeText(this, "PINs don't match", Toast.LENGTH_SHORT).show()
                    else -> { securityManager.setDecoyPin(newPin); refreshUI(); Toast.makeText(this, "Decoy PIN set!", Toast.LENGTH_SHORT).show() }
                }
            }
            .setNegativeButton("Cancel", null).show()
    }

    private fun showRecoveryDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_recovery, null)
        val etQ = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etQuestion)
        val etA = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etAnswer)
        if (securityManager.hasRecovery()) etQ.setText(securityManager.getRecoveryQuestion())
        MaterialAlertDialogBuilder(this, R.style.DarkAlertDialog)
            .setTitle("Recovery Setup").setView(view)
            .setPositiveButton("Save") { _, _ ->
                val q = etQ.text.toString(); val a = etA.text.toString()
                if (q.isNotEmpty() && a.isNotEmpty()) { securityManager.setRecovery(q, a); Toast.makeText(this, "Recovery saved!", Toast.LENGTH_SHORT).show() }
            }
            .setNegativeButton("Cancel", null).show()
    }

    private fun showForgotPinDialog() {
        if (!securityManager.hasRecovery()) { Toast.makeText(this, "No recovery set up", Toast.LENGTH_SHORT).show(); return }
        val view = layoutInflater.inflate(R.layout.dialog_forgot_pin, null)
        val etQ = view.findViewById<TextView>(R.id.tvQuestion)
        val etA = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etAnswer)
        etQ.text = securityManager.getRecoveryQuestion()
        MaterialAlertDialogBuilder(this, R.style.DarkAlertDialog)
            .setTitle("Password Recovery").setView(view)
            .setPositiveButton("Verify") { _, _ ->
                if (securityManager.validateRecoveryAnswer(etA.text.toString())) {
                    Toast.makeText(this, "Answer correct! PIN reset to 1234", Toast.LENGTH_LONG).show()
                    securityManager.resetToDefault()
                } else { Toast.makeText(this, "Wrong answer", Toast.LENGTH_SHORT).show() }
            }
            .setNegativeButton("Cancel", null).show()
    }

    private fun showThemePicker() {
        val accents = ThemeManager.getAccentNameList()
        val names = accents.map { it.second }.toTypedArray()
        val current = accents.indexOfFirst { it.first == securityManager.getThemeAccent() }.coerceAtLeast(0)
        AlertDialog.Builder(this, R.style.DarkAlertDialog)
            .setTitle("Choose Accent Color")
            .setSingleChoiceItems(names, current) { dialog, which ->
                securityManager.setThemeAccent(accents[which].first)
                refreshUI()
                Toast.makeText(this, "Theme updated to ${accents[which].second}", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showResetDialog() {
        AlertDialog.Builder(this, R.style.DarkAlertDialog)
            .setTitle("Reset Everything?")
            .setMessage("This will delete ALL hidden files, notes, and reset PIN to 1234. Cannot be undone.")
            .setPositiveButton("Reset") { _, _ ->
                securityManager.resetToDefault()
                vaultManager.emptyTrash()
                listOf("vault", "trash", "notes_v5.enc", "passwords_v5.enc").forEach { File(filesDir, it).deleteRecursively() }
                Toast.makeText(this, "App reset complete", Toast.LENGTH_SHORT).show()
                finishAffinity()
            }
            .setNegativeButton("Cancel", null).show()
    }
}
