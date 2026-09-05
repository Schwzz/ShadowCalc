package com.shadowcalc.app

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.shadowcalc.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), SensorEventListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var securityManager: SecurityManager
    private lateinit var vaultManager: VaultManager
    private lateinit var intruderManager: IntruderManager
    private lateinit var biometricHelper: BiometricHelper
    private lateinit var sensorManager: SensorManager
    private var currentInput = ""
    private var lastValue = 0.0
    private var currentOp = ""
    private var isNewInput = true
    private var wrongAttempts = 0
    private var lastShakeTime = 0L
    private val handler = Handler(Looper.getMainLooper())
    private var autoLockRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        securityManager = SecurityManager(this)
        vaultManager = VaultManager(this, securityManager)
        intruderManager = IntruderManager(this)
        biometricHelper = BiometricHelper(this)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        setupCalculator()
        setupBiometric()
        setupShakeDetector()
    }

    override fun onResume() {
        super.onResume()
        currentInput = ""
        lastValue = 0.0
        currentOp = ""
        isNewInput = true
        wrongAttempts = 0
        updateDisplay()
        if (securityManager.isPanicEnabled()) {
            sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        autoLockRunnable?.let { handler.removeCallbacks(it) }
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

    private fun setupBiometric() {
        if (securityManager.isBiometricEnabled() && biometricHelper.canAuthenticate()) {
            binding.btnBiometric.visibility = android.view.View.VISIBLE
            binding.btnBiometric.setOnClickListener {
                biometricHelper.showBiometricPrompt(this,
                    onSuccess = { launchHome(decoy = false) },
                    onError = {}
                )
            }
        } else {
            binding.btnBiometric.visibility = android.view.View.GONE
        }
    }

    private fun setupShakeDetector() {
        // Handled in onSensorChanged
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
        // Check real PIN
        if (securityManager.validatePin(currentInput)) {
            currentInput = ""; updateDisplay()
            launchHome(decoy = false)
            return
        }
        // Check decoy PIN
        if (securityManager.hasDecoyPin() && securityManager.validateDecoyPin(currentInput)) {
            currentInput = ""; updateDisplay()
            launchHome(decoy = true)
            return
        }

        wrongAttempts++
        if (wrongAttempts >= 3) {
            intruderManager.onWrongPinAttempt(vaultManager)
            wrongAttempts = 0
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

    private fun launchHome(decoy: Boolean) {
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

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val acceleration = kotlin.math.sqrt(x * x + y * y + z * z)
        val now = System.currentTimeMillis()
        if (acceleration > 20 && now - lastShakeTime > 2000) {
            lastShakeTime = now
            finishAffinity()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
