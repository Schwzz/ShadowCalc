package com.shadowcalc.app

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import java.nio.ByteBuffer

class IntruderManager(private val context: Context) {
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null
    private var handlerThread: HandlerThread? = null
    private var handler: Handler? = null
    private var attemptCount = 0

    fun onWrongPinAttempt(vaultManager: VaultManager) {
        attemptCount++
        if (attemptCount >= 3) {
            attemptCount = 0
            captureIntruder(vaultManager)
        }
    }

    private fun captureIntruder(vaultManager: VaultManager) {
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val frontCamera = cameraManager.cameraIdList.find { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT
            } ?: return

            handlerThread = HandlerThread("IntruderCam").apply { start() }
            handler = Handler(handlerThread!!.looper)

            imageReader = ImageReader.newInstance(640, 480, ImageFormat.JPEG, 1).apply {
                setOnImageAvailableListener({ reader ->
                    val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
                    val buffer: ByteBuffer = image.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)
                    image.close()
                    vaultManager.saveIntruderPhoto(bytes)
                    closeCamera()
                }, handler)
            }

            cameraManager.openCamera(frontCamera, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    createCaptureSession()
                }
                override fun onDisconnected(camera: CameraDevice) { closeCamera() }
                override fun onError(camera: CameraDevice, error: Int) { closeCamera() }
            }, handler)
        } catch (e: SecurityException) { /* No camera permission */ }
        catch (e: Exception) { e.printStackTrace() }
    }

    private fun createCaptureSession() {
        val surfaces = listOf(imageReader!!.surface)
        cameraDevice?.createCaptureSession(surfaces, object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                captureSession = session
                val request = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                    addTarget(imageReader!!.surface)
                }.build()
                session.capture(request, null, handler)
            }
            override fun onConfigureFailed(session: CameraCaptureSession) { closeCamera() }
        }, handler)
    }

    private fun closeCamera() {
        captureSession?.close()
        cameraDevice?.close()
        imageReader?.close()
        handlerThread?.quitSafely()
        captureSession = null
        cameraDevice = null
        imageReader = null
        handlerThread = null
        handler = null
    }
}
