package com.shadowcalc.app

import android.content.Intent
import android.os.Bundle
import android.view.HapticFeedbackConstants
import androidx.appcompat.app.AppCompatActivity
import com.shadowcalc.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var securityManager: SecurityManager
    private var currentInput = ""
    private var lastValue = 0.0
    private var currentOp = ""
    private var isNewInput = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        securityManager = SecurityManager(this)
        setupCalculator()
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
        binding.btnMinus.setOnClickListener { it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); setOperation("-") }
        binding.btnMultiply.setOnClickListener { it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); setOperation("×") }
        binding.btnDivide.setOnClickListener { it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); setOperation("÷") }
        binding.btnEquals.setOnClickListener { it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); onEqualsPressed() }
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
        if (securityManager.validatePin(currentInput)) {
            currentInput = ""; updateDisplay()
            startActivity(Intent(this, HomeActivity::class.java))
            return
        }
        if (currentInput.isEmpty() || currentOp.isEmpty()) return
        val currentVal = currentInput.toDoubleOrNull() ?: 0.0
        val result = when (currentOp) {
            "+" -> lastValue + currentVal
            "-" -> lastValue - currentVal
            "×" -> lastValue * currentVal
            "÷" -> if (currentVal != 0.0) lastValue / currentVal else Double.NaN
            else -> currentVal
        }
        currentInput = if (result.isNaN()) "Error" else if (result == result.toLong().toDouble()) result.toLong().toString() else String.format("%.8f", result).trimEnd('0').trimEnd('.')
        currentOp = ""; isNewInput = true; updateDisplay()
    }
    private fun clearAll() { currentInput = ""; lastValue = 0.0; currentOp = ""; isNewInput = true; updateDisplay() }
    private fun deleteLast() { if (currentInput.isNotEmpty()) { currentInput = currentInput.dropLast(1); updateDisplay() } }
    private fun updateDisplay() {
        binding.tvDisplay.text = currentInput.ifEmpty { "0" }
        binding.tvSubDisplay.text = if (currentOp.isNotEmpty()) "$lastValue $currentOp" else ""
    }
}
