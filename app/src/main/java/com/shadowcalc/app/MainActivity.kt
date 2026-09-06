package com.shadowcalc.app

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.shadowcalc.app.databinding.ActivityMainBinding
import com.shadowcalc.app.databinding.DialogFirstTimeBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var securityManager: SecurityManager
    private var currentInput = ""
    private var lastValue = 0.0
    private var currentOp = ""
    private var isNewInput = true
    private var wrongPinAttempts = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        securityManager = SecurityManager(this)

        setupCalculator()

        if (securityManager.isFirstTime()) {
            showFirstTimeSetup()
        }
    }

    override fun onResume() {
        super.onResume()
        currentInput = ""
        lastValue = 0.0
        currentOp = ""
        isNewInput = true
        wrongPinAttempts = 0
        updateDisplay()
    }

    private fun setupCalculator() {
        val buttons = mapOf(
            binding.btn0 to "0", binding.btn1 to "1", binding.btn2 to "2", binding.btn3 to "3",
            binding.btn4 to "4", binding.btn5 to "5", binding.btn6 to "6", binding.btn7 to "7",
            binding.btn8 to "8", binding.btn9 to "9", binding.btnDot to "."
        )
        buttons.forEach { (btn, value) ->
            btn.setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                appendNumber(value)
            }
        }
        binding.btnClear.setOnClickListener { it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); clearAll() }
        binding.btnDelete.setOnClickListener { it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); deleteLast() }
        binding.btnPlus.setOnClickListener { it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); setOperation("+") }
        binding.btnMinus.setOnClickListener { it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); setOperation("−") }
        binding.btnMultiply.setOnClickListener { it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); setOperation("×") }
        binding.btnDivide.setOnClickListener { it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); setOperation("÷") }
        binding.btnEquals.setOnClickListener { it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); onEqualsPressed() }
    }

    private fun showFirstTimeSetup() {
        val dialogBinding = DialogFirstTimeBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(this, R.style.DarkAlertDialog)
            .setView(dialogBinding.root)
            .setCancelable(false)
            .create()

        dialogBinding.tvTitle.text = "Welcome"
        dialogBinding.tvSubtitle.text = "Set a 4-digit backup code for calculator data recovery"
        dialogBinding.tvStep.text = "Step 1 of 2"

        var step = 1
        var savedPin = ""

        dialogBinding.btnContinue.setOnClickListener {
            val pin = dialogBinding.etPin.text.toString()
            when {
                pin.length != 4 -> {
                    dialogBinding.etPin.error = "Enter exactly 4 digits"
                }
                step == 1 -> {
                    savedPin = pin
                    dialogBinding.etPin.text?.clear()
                    dialogBinding.etPin.hint = "Re-enter 4-digit code"
                    dialogBinding.tvStep.text = "Step 2 of 2"
                    dialogBinding.tvSubtitle.text = "Confirm your backup code"
                    step = 2
                }
                step == 2 -> {
                    if (pin == savedPin) {
                        securityManager.setPin(savedPin)
                        securityManager.setFirstTimeDone()
                        dialog.dismiss()
                        Toast.makeText(this, "Backup code set successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        dialogBinding.etPin.error = "Codes do not match"
                        dialogBinding.etPin.text?.clear()
                    }
                }
            }
        }

        dialog.show()
    }

    private fun appendNumber(num: String) {
        if (isNewInput) { currentInput = num; isNewInput = false }
        else { if (num == "." && currentInput.contains(".")) return; currentInput += num }
        updateDisplay()
    }

    private fun setOperation(op: String) {
        if (currentInput.isNotEmpty()) { lastValue = currentInput.toDoubleOrNull() ?: 0.0; currentOp = op; isNewInput = true }
        updateDisplay()
    }

    private fun onEqualsPressed() {
        // Check PIN first
        if (securityManager.validatePin(currentInput)) {
            currentInput = ""; updateDisplay()
            launchHome(decoy = false)
            return
        }
        if (securityManager.hasDecoyPin() && securityManager.validateDecoyPin(currentInput)) {
            currentInput = ""; updateDisplay()
            launchHome(decoy = true)
            return
        }

        // Normal calculator operation
        if (currentInput.isEmpty() || currentOp.isEmpty()) return
        val currentVal = currentInput.toDoubleOrNull() ?: 0.0
        val result = when (currentOp) {
            "+" -> lastValue + currentVal
            "−" -> lastValue - currentVal
            "×" -> lastValue * currentVal
            "÷" -> if (currentVal != 0.0) lastValue / currentVal else Double.NaN
            else -> currentVal
        }
        currentInput = if (result.isNaN()) "Error" else if (result == result.toLong().toDouble()) result.toLong().toString() else String.format("%.8f", result).trimEnd('0').trimEnd('.')
        currentOp = ""; isNewInput = true; updateDisplay()
    }

    private fun launchHome(decoy: Boolean) {
        AutoLockManager.getInstance().resetTimer()
        val intent = Intent(this, HomeActivity::class.java)
        intent.putExtra("decoy_mode", decoy)
        startActivity(intent)
    }

    private fun clearAll() { currentInput = ""; lastValue = 0.0; currentOp = ""; isNewInput = true; updateDisplay() }
    private fun deleteLast() { if (currentInput.isNotEmpty()) { currentInput = currentInput.dropLast(1); updateDisplay() } }

    private fun updateDisplay() {
        binding.tvDisplay.text = currentInput.ifEmpty { "0" }
        binding.tvSubDisplay.text = if (currentOp.isNotEmpty()) "$lastValue $currentOp" else ""
    }
}
