package com.shadowcalc.app

import android.os.Bundle
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        securityManager = SecurityManager(this)
        binding.btnBack.setOnClickListener { finish() }

        binding.btnChangePin.setOnClickListener {
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

        binding.btnRecovery.setOnClickListener {
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

        binding.btnForgotPin.setOnClickListener {
            if (!securityManager.hasRecovery()) { Toast.makeText(this, "No recovery set up", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
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

        binding.btnReset.setOnClickListener {
            AlertDialog.Builder(this, R.style.DarkAlertDialog)
                .setTitle("Reset Everything?")
                .setMessage("This will delete ALL hidden files, notes, and reset PIN to 1234. Cannot be undone.")
                .setPositiveButton("Reset") { _, _ ->
                    securityManager.resetToDefault()
                    VaultManager(this).emptyTrash()
                    listOf("vault", "trash", "notes.enc").forEach { File(filesDir, it).deleteRecursively() }
                    Toast.makeText(this, "App reset complete", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .setNegativeButton("Cancel", null).show()
        }
    }
}
