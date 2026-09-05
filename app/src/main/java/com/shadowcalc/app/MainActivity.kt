package com.shadowcalc.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
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
        val numberButtons = listOf(
            binding.btn0, binding.btn1, binding.btn2, binding.btn3,
            binding.btn4, binding.btn5, binding.btn6, binding.btn7,
            binding.btn8, binding.btn9
        )
        numberButtons.forEachIndexed { index, button ->
            button.setOnClickListener { appendNumber(index.toString()) }
        }

        binding.btnDot.setOnClickListener { appendNumber(".") }
        binding.btnClear.setOnClickListener { clearAll() }
        binding.btnDelete.setOnClickListener { deleteLast() }

        binding.btnPlus.setOnClickListener { setOperation("+") }
        binding.btnMinus.setOnClickListener { setOperation("-") }
        binding.btnMultiply.setOnClickListener { setOperation("×") }
        binding.btnDivide.setOnClickListener { setOperation("÷") }

        binding.btnEquals.setOnClickListener { onEqualsPressed() }
    }

    private fun appendNumber(num: String) {
        if (isNewInput) {
            currentInput = num
            isNewInput = false
        } else {
            if (num == "." && currentInput.contains(".")) return
            currentInput += num
        }
        updateDisplay()
    }

    private fun setOperation(op: String) {
        if (currentInput.isNotEmpty()) {
            lastValue = currentInput.toDoubleOrNull() ?: 0.0
            currentOp = op
            isNewInput = true
        }
    }

    private fun onEqualsPressed() {
        // Check for secret PIN first
        if (securityManager.validatePin(currentInput)) {
            openVault()
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

        currentInput = if (result == result.toLong().toDouble()) {
            result.toLong().toString()
        } else {
            String.format("%.8f", result).trimEnd('0').trimEnd('.')
        }
        currentOp = ""
        isNewInput = true
        updateDisplay()
    }

    private fun clearAll() {
        currentInput = ""
        lastValue = 0.0
        currentOp = ""
        isNewInput = true
        updateDisplay()
    }

    private fun deleteLast() {
        if (currentInput.isNotEmpty()) {
            currentInput = currentInput.dropLast(1)
            updateDisplay()
        }
    }

    private fun updateDisplay() {
        binding.tvDisplay.text = currentInput.ifEmpty { "0" }
        val subText = if (currentOp.isNotEmpty()) "$lastValue $currentOp" else ""
        binding.tvSubDisplay.text = subText
    }

    private fun openVault() {
        currentInput = ""
        updateDisplay()
        startActivity(Intent(this, VaultActivity::class.java))
    }
}
