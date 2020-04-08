package com.gtiagomaia.camerax

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.*
import android.media.Image
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.lifecycle.LifecycleOwner
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity(), LifecycleOwner {



    // This is an arbitrary number we are using to keep track of the permission
    // request. Where an app has multiple context for requesting permission,
    // this can help differentiate the different contexts.
    private val REQUEST_CODE_PERMISSIONS = 10
    // This is an array of all the permission specified in the manifest.
    private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)





    private lateinit var previewView: PreviewView
    private lateinit var imageView: ImageView
    private lateinit var cameraProvider:ProcessCameraProvider
    private var imageCapture: ImageCapture? = null
    private val executor = Executors.newSingleThreadExecutor()




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

         previewView = findViewById(R.id.view_finder)
        imageView = findViewById(R.id.imageview)


        // Request camera permissions
        if (allPermissionsGranted()) {
            previewView.post { startCamera() }
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }


        findViewById<Button>(R.id.btn_capture_image).apply {
            this.setOnClickListener {
                imageCapture?.takePicture(executor,
                    object : ImageCapture.OnImageCapturedCallback() {
                        @SuppressLint("UnsafeExperimentalUsageError")
                        override fun onCaptureSuccess(image: ImageProxy) {


                            //
//                            val rotatedBitmap = bitmapHelper.rotateImage(
//                                bitmapHelper.imageToBitmap(image = image.image!!),
//                                image.imageInfo.rotationDegrees.toFloat()
//                            )


                            runOnUiThread {
                               // imageView.setImageBitmap(image.image!!.toBitmap())
                                if(executor.isTerminated || executor.isTerminated) return@runOnUiThread
                                imageView.apply {
                                    image.image?.let {
                                        it.toBitmap()?.let { bitmap ->
                                            imageView.setImageBitmap(bitmap)
                                            cameraProvider.unbindAll()
                                        }
                                    }
                                }
                            }


                            Toast.makeText(context, "capture success", Toast.LENGTH_SHORT).show()
                            super.onCaptureSuccess(image)
                        }

                        override fun onError(exception: ImageCaptureException) {

                            Toast.makeText(context, "image save failed", Toast.LENGTH_SHORT).show()
                            startCamera()
                            super.onError(exception)

                        }
                    })
            }
        }

        Toast.makeText(this, "camerax oncreate success", Toast.LENGTH_SHORT).show()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)


        cameraProviderFuture.addListener(Runnable {
            // Camera provider is now guaranteed to be available
            cameraProvider = cameraProviderFuture.get()


            // Set up the preview use case to display camera preview.
            val preview = Preview.Builder().build()

            // Set up the capture use case to allow users to take photos.
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            // Choose the camera by requiring a lens facing
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()

            // Attach use cases to the camera with the same lifecycle owner
            val camera = cameraProvider.bindToLifecycle(
                this as LifecycleOwner, cameraSelector, preview, imageCapture)

            // Connect the preview use case to the previewView
            preview.setSurfaceProvider(
               // previewView.createSurfaceProvider(camera.cameraInfo))
                previewView.createSurfaceProvider(camera.cameraInfo))

        }, ContextCompat.getMainExecutor(this))

    }


    fun Image.toBitmap(): Bitmap? {
        val bytes = null
        var bitmap:Bitmap? = null
        try {
            val buffer = planes[0].buffer
            buffer.rewind()
            val bytes = ByteArray(buffer.capacity())
            buffer.get(bytes)
            bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }catch (e: IllegalStateException){
            print(e)
        }
//        val buffer = planes[0].buffer
//        buffer.rewind()
//        val bytes = ByteArray(buffer.capacity())
//        buffer.get(bytes)
        return bitmap
    }


    fun Image.toBitmapUsingYUV(): Bitmap {
        val yBuffer = planes[0].buffer // Y
        val uBuffer = planes[1].buffer // U
        val vBuffer = planes[2].buffer // V

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        //U and V are swapped
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, this.width, this.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 50, out)
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }
//
//    private fun startCamera() {
//
//
//
//        // Build the viewfinder use case
//        val preview = Preview.Builder()
//            .setTargetResolution(Size(640, 480))
//            .build()
//
//        // Every time the viewfinder is updated, recompute layout
//        preview.setOnPreviewOutputUpdateListener {
//
//            // To update the SurfaceTexture, we have to remove it and re-add it
//            val parent = viewFinder.parent as ViewGroup
//            parent.removeView(viewFinder)
//            parent.addView(viewFinder, 0)
//
//            viewFinder.surfaceTexture = it.surfaceTexture
//            updateTransform()
//        }
//
//        // Bind use cases to lifecycle
//        // If Android Studio complains about "this" being not a LifecycleOwner
//        // try rebuilding the project or updating the appcompat dependency to
//        // version 1.1.0 or higher.
//        CameraX.bindToLifecycle(this, preview)
//    }
//
//
//    private fun updateTransform() {
//        val matrix = Matrix()
//
//        // Compute the center of the view finder
//        val centerX = viewFinder.width / 2f
//        val centerY = viewFinder.height / 2f
//
//        // Correct preview output to account for display rotation
//        val rotationDegrees = when(viewFinder.display.rotation) {
//            Surface.ROTATION_0 -> 0
//            Surface.ROTATION_90 -> 90
//            Surface.ROTATION_180 -> 180
//            Surface.ROTATION_270 -> 270
//            else -> return
//        }
//        matrix.postRotate(-rotationDegrees.toFloat(), centerX, centerY)
//
//        // Finally, apply transformations to our TextureView
//        viewFinder.setTransform(matrix)
//    }
    /**
     * Process result from permission request dialog box, has the request
     * been granted? If yes, start Camera. Otherwise display a toast
     */
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                previewView.post { startCamera() }
            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    /**
     * Check if all permission specified in the manifest have been granted
     */
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }
}
