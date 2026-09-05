package com.shadowcalc.app

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
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
    private lateinit var biometricHelper: BiometricHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        securityManager = SecurityManager(this)
        biometricHelper = BiometricHelper(this)
        binding.btnBack.setOnClickListener { finish() }

        refreshUI()

        binding.btnChangePin.setOnClickListener { showChangePinDialog() }
        binding.btnDecoyPin.setOnClickListener { showDecoyPinDialog() }
        binding.btnRecovery.setOnClickListener { showRecoveryDialog() }
        binding.btnForgotPin.setOnClickListener { showForgotPinDialog() }
        binding.btnBiometric.setOnClickListener { toggleBiometric() }
        binding.btnAutoLock.setOnClickListener { showAutoLockDialog() }
        binding.btnPanic.setOnClickListener { togglePanic() }
        binding.btnReset.setOnClickListener { showResetDialog() }
    }

    private fun refreshUI() {
        binding.switchBiometric.isChecked = securityManager.isBiometricEnabled()
        binding.switchPanic.isChecked = securityManager.isPanicEnabled()
        binding.tvAutoLock.text = "${securityManager.getAutoLockMinutes()} min"
        binding.tvDecoyStatus.text = if (securityManager.hasDecoyPin()) "Active" else "Not set"
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

    private fun toggleBiometric() {
        if (!biometricHelper.canAuthenticate()) {
            Toast.makeText(this, "Biometric not available", Toast.LENGTH_SHORT).show()
            binding.switchBiometric.isChecked = false
            return
        }
        val enabled = !securityManager.isBiometricEnabled()
        securityManager.setBiometricEnabled(enabled)
        binding.switchBiometric.isChecked = enabled
        Toast.makeText(this, if (enabled) "Biometric enabled" else "Biometric disabled", Toast.LENGTH_SHORT).show()
    }

    private fun showAutoLockDialog() {
        val minutes = arrayOf("1 min", "2 min", "5 min", "10 min", "15 min", "30 min", "Never")
        val values = arrayOf(1, 2, 5, 10, 15, 30, 0)
        val current = securityManager.getAutoLockMinutes()
        val selected = values.indexOf(current).coerceAtLeast(0)
        AlertDialog.Builder(this, R.style.DarkAlertDialog)
            .setTitle("Auto-Lock Timer")
            .setSingleChoiceItems(minutes, selected) { dialog, which ->
                securityManager.setAutoLockMinutes(values[which])
                refreshUI()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun togglePanic() {
        val enabled = !securityManager.isPanicEnabled()
        securityManager.setPanicEnabled(enabled)
        binding.switchPanic.isChecked = enabled
        Toast.makeText(this, if (enabled) "Panic shake enabled" else "Panic shake disabled", Toast.LENGTH_SHORT).show()
    }

    private fun showResetDialog() {
        AlertDialog.Builder(this, R.style.DarkAlertDialog)
            .setTitle("Reset Everything?")
            .setMessage("This will delete ALL hidden files, notes, passwords, and reset PIN to 1234. Cannot be undone.")
            .setPositiveButton("Reset") { _, _ ->
                securityManager.resetToDefault()
                VaultManager(this, securityManager).emptyTrash()
                listOf("vault", "trash", "notes_v3.enc", "passwords_v3.enc", "intruders").forEach { File(filesDir, it).deleteRecursively() }
                Toast.makeText(this, "App reset complete", Toast.LENGTH_SHORT).show()
                finishAffinity()
            }
            .setNegativeButton("Cancel", null).show()
    }
}
