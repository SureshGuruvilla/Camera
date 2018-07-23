package com.example.admin.camera

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

import java.sql.Timestamp

import android.app.Activity
import android.app.AlertDialog
import android.app.AlertDialog.Builder
import android.content.ContentValues
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.hardware.Camera.ErrorCallback
import android.hardware.Camera.Parameters
import android.hardware.Camera.PictureCallback
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.MediaStore.Images
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceHolder.Callback
import android.view.SurfaceView
import android.view.View
import android.view.View.OnClickListener
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import java.util.*

class ActivityMain : Activity(), Callback, OnClickListener {

    private var surfaceView: SurfaceView? = null
    private var surfaceHolder: SurfaceHolder? = null
    private var camera: Camera? = null
    private var flipCamera: Button? = null
    private var flashCameraButton: Button? = null
    private var captureImage: Button? = null
    private var cameraId: Int = 0
    private var flashmode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // camera surface view created
        cameraId = CameraInfo.CAMERA_FACING_BACK
        flipCamera = findViewById<View>(R.id.flipCamera) as Button
        flashCameraButton = findViewById<View>(R.id.flash) as Button
        captureImage = findViewById<View>(R.id.captureImage) as Button
        surfaceView = findViewById<View>(R.id.surfaceView) as SurfaceView
        surfaceHolder = surfaceView!!.holder
        surfaceHolder!!.addCallback(this)
        flipCamera!!.setOnClickListener(this)
        captureImage!!.setOnClickListener(this)
        flashCameraButton!!.setOnClickListener(this)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        if (Camera.getNumberOfCameras() > 1) {
            flipCamera!!.visibility = View.VISIBLE
        }
        if (!baseContext.packageManager.hasSystemFeature(
                        PackageManager.FEATURE_CAMERA_FLASH)) {
            flashCameraButton!!.visibility = View.GONE
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        if (!openCamera(CameraInfo.CAMERA_FACING_BACK)) {
            alertCameraDialog()
        }

    }

    private fun openCamera(id: Int): Boolean {
        var result = false
        cameraId = id
        releaseCamera()
        try {
            camera = Camera.open(cameraId)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (camera != null) {
            try {
                setUpCamera(camera!!)


                camera!!.setErrorCallback { error, camera -> }
                camera!!.setPreviewDisplay(surfaceHolder)
                camera!!.startPreview()
                val params = camera!!.parameters
                // start face detection only *after* preview has started
                if (params.maxNumDetectedFaces > 0) {
                    // camera supports face detection, so can start it:
                    camera!!.startFaceDetection()
                }
                result = true
            } catch (e: IOException) {
                e.printStackTrace()
                result = false
                releaseCamera()
            }

        }
        return result
    }

    private fun setUpCamera(c: Camera) {
        val info = Camera.CameraInfo()
        Camera.getCameraInfo(cameraId, info)
        rotation = windowManager.defaultDisplay.rotation
        var degree = 0
        when (rotation) {
            Surface.ROTATION_0 -> degree = 0
            Surface.ROTATION_90 -> degree = 90
            Surface.ROTATION_180 -> degree = 180
            Surface.ROTATION_270 -> degree = 270

            else -> {
            }
        }

        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            // frontFacing
            rotation = (info.orientation + degree) % 330
            rotation = (360 - rotation) % 360
        } else {
            // Back-facing
            rotation = (info.orientation - degree + 360) % 360
        }
        c.setDisplayOrientation(rotation)
        val params = c.parameters

        showFlashButton(params)

        val focusModes = params.supportedFlashModes
        if (focusModes != null) {
            if (focusModes
                            .contains(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                params.flashMode = Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
            }
        }

        params.setRotation(rotation)
    }

    private fun showFlashButton(params: Parameters) {
        val showFlash = (packageManager.hasSystemFeature(
                PackageManager.FEATURE_CAMERA_FLASH) && params.flashMode != null
                && params.supportedFlashModes != null
                && params.supportedFocusModes.size > 1)

        flashCameraButton!!.visibility = if (showFlash)
            View.VISIBLE
        else
            View.INVISIBLE

    }

    private fun releaseCamera() {
        try {
            if (camera != null) {
                camera!!.setPreviewCallback(null)
                camera!!.setErrorCallback(null)
                camera!!.stopPreview()
                camera!!.release()
                camera = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("error", e.toString())
            camera = null
        }

    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int,
                                height: Int) {

    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {

    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.flash -> flashOnButton()
            R.id.flipCamera -> flipCamera()
            R.id.captureImage -> takeImage()

            else -> {
            }
        }
    }

    private fun takeImage() {
        camera!!.takePicture(null, null, object : PictureCallback {

            private var imageFile: File? = null

            override fun onPictureTaken(data: ByteArray, camera: Camera) {
                try {
                    // convert byte array into bitmap
                    var loadedImage: Bitmap? = null
                    var rotatedBitmap: Bitmap? = null
                    loadedImage = BitmapFactory.decodeByteArray(data, 0,
                            data.size)

                    //                     rotate Image
                    val rotateMatrix = Matrix()

                    //////////
                    //
                    //
                    val rotation1: Int
                    val info = CameraInfo()
                    if (info.facing == 1) {
                        // frontFacing
                        rotation1 = 270
                    } else {
                        rotation1 = rotation
                    }


                    //
                    ////////////

                    rotateMatrix.postRotate(rotation1.toFloat())
                    Toast.makeText(this@ActivityMain, "rotation  $rotation1", Toast.LENGTH_LONG).show()
                    rotatedBitmap = Bitmap.createBitmap(loadedImage!!, 0, 0,
                            loadedImage.width, loadedImage.height,
                            rotateMatrix, true)
                    val state = Environment.getExternalStorageState()
                    var folder: File? = null
                    if (state.equals(Environment.MEDIA_MOUNTED)) {
                        folder = File(Environment
                                .getExternalStorageDirectory(), "/MYCAMERAAPP")
                    } else {
                        folder = File(Environment
                                .getExternalStorageDirectory(), "/MYCAMERAAPP")
                    }

                    var success = true
                    if (!folder.exists()) {
                        success = folder.mkdir()
                    }
                    if (success) {
                        val date = Date()
                        imageFile = File(folder.absolutePath
                                + File.separator
                                + Timestamp(date.time).toString()
                                + "Image.jpg")

                        imageFile!!.createNewFile()
                    } else {
                        Toast.makeText(baseContext, "Image Not saved",
                                Toast.LENGTH_SHORT).show()
                        return
                    }
                    val ostream = ByteArrayOutputStream()
                    rotatedBitmap!!.compress(CompressFormat.JPEG, 100, ostream)
                    val fout = FileOutputStream(imageFile!!)
                    fout.write(ostream.toByteArray())
                    fout.close()
                    val values = ContentValues()
                    values.put(Images.Media.DATE_TAKEN,
                            System.currentTimeMillis())
                    values.put(Images.Media.MIME_TYPE, "image/jpeg")
                    values.put(MediaStore.MediaColumns.DATA,
                            imageFile!!.absolutePath)
                    this@ActivityMain.contentResolver.insert(
                            Images.Media.EXTERNAL_CONTENT_URI, values)
                } catch (e: Exception) {
                    Toast.makeText(this@ActivityMain, "Picture not taken\n" + e.toString(), Toast.LENGTH_SHORT).show()
                }

            }
        })
    }

    private fun flipCamera() {
        val id = if (cameraId == CameraInfo.CAMERA_FACING_BACK)
            CameraInfo.CAMERA_FACING_FRONT
        else
            CameraInfo.CAMERA_FACING_BACK
        if (!openCamera(id)) {
            alertCameraDialog()
        }
    }

    private fun alertCameraDialog() {
        val dialog = createAlert(this@ActivityMain,
                "Camera info", "error to open camera")
        dialog.setNegativeButton("OK") { dialog, which -> dialog.cancel() }

        dialog.show()
    }

    private fun createAlert(context: Context, title: String?, message: String): Builder {
        val dialog = AlertDialog.Builder(
                ContextThemeWrapper(context,
                        android.R.style.Theme_Holo_Light_Dialog))
        dialog.setIcon(R.drawable.ic_launcher_background)
        if (title != null)
            dialog.setTitle(title)
        else
            dialog.setTitle("Information")
        dialog.setMessage(message)
        dialog.setCancelable(false)
        return dialog
    }

    private fun flashOnButton() {
        if (camera != null) {
            try {
                val param = camera!!.parameters
                param.flashMode = if (!flashmode)
                    Parameters.FLASH_MODE_TORCH
                else
                    Parameters.FLASH_MODE_OFF
                camera!!.parameters = param
                flashmode = !flashmode
            } catch (e: Exception) {
                // TODO: handle exception
            }

        }
    }

    companion object {
        private var rotation: Int = 0
    }
}