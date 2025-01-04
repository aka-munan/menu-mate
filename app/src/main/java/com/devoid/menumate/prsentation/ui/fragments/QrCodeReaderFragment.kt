package com.devoid.menumate.prsentation.ui.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.hardware.camera2.params.SessionConfiguration.SESSION_REGULAR
import android.media.ImageReader
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.devoid.menumate.R
import com.devoid.menumate.data.remote.TableManager
import com.devoid.menumate.databinding.QrReaderLayoutBinding
import com.devoid.menumate.domain.model.TableInfo
import com.devoid.menumate.prsentation.state.RestaurantUiState
import com.devoid.menumate.prsentation.ui.KitchenDashboardActivity
import com.devoid.menumate.prsentation.ui.RestaurantSetupActivity
import com.devoid.menumate.prsentation.viewmodel.RestaurantSetupViewModel
import com.devoid.menumate.prsentation.viewmodel.showLogoutDialog
import com.devoid.menumate.utils.toLuminanceSource
import com.google.android.material.snackbar.Snackbar
import com.google.zxing.BinaryBitmap
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import dagger.Lazy
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class QrCodeReaderFragment : Fragment(), ActivityCompat.OnRequestPermissionsResultCallback {
    private lateinit var activityResultLauncher: ActivityResultLauncher<String>

    @Inject
    lateinit var tableManager: Lazy<TableManager>
    private val viewModel: RestaurantSetupViewModel by viewModels()
    private lateinit var imageReader: ImageReader
    private lateinit var cameraManager: CameraManager
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private val qrCodeReader = QRCodeReader()

    private val TAG = this::class.simpleName

    private lateinit var binding: QrReaderLayoutBinding

    private val imageWith = 1080
    private val imageHeight = 1080

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = QrReaderLayoutBinding.inflate(layoutInflater, container, false)
        observeViewModel()
        init()
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                    if (checkCameraPermission()) {
                        startCamera()
                    } else {
                        activityResultLauncher.launch(Manifest.permission.CAMERA)
                    }
            }
        }
        return binding.root
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.restaurantUiState.collect { state ->
                    when (state) {
                        RestaurantUiState.Loading -> {
                            binding.toolbar.menu.findItem(R.id.my_orders)?.isVisible = false
                            binding.toolbar.menu.findItem(R.id.register_rest)?.isVisible = false
                            binding.toolbar.menu.findItem(R.id.restaurant_dash)?.isVisible = false
                        }

                        is RestaurantUiState.LocalRestaurant -> {
                            binding.toolbar.menu.findItem(R.id.register_rest)?.isVisible = true
                        }

                        is RestaurantUiState.RemoteRestaurant -> {
                            binding.toolbar.menu.findItem(R.id.restaurant_dash)?.isVisible = true
                        }
                    }
                }
            }
        }
    }

    private fun init() {
        requireActivity().onBackPressedDispatcher.addCallback(this){

        }
        binding.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.restaurant_dash -> {
                    requireActivity().startActivity(
                        Intent(
                            requireContext(),
                            KitchenDashboardActivity::class.java
                        )
                    )
                }

                R.id.register_rest -> {
                    requireActivity().startActivity(
                        Intent(
                            requireContext(),
                            RestaurantSetupActivity::class.java
                        )
                    )
                }

                R.id.log_out -> {
                    showLogoutDialog(requireActivity() as AppCompatActivity)
                }
            }
            true
        }
        binding.continueAsTestUserBtn.setOnClickListener {
            stopCamera()
            tableManager.get().initialize(TableInfo("cPnkeQPZlEhNc9CsDDR3r9lObtp1", 1))
            requireActivity().supportFragmentManager.beginTransaction()
                .remove(this)
                .commit()
        }
        activityResultLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    startCamera()
                } else {
                    val rootView: View =
                        requireActivity().window.decorView.findViewById(android.R.id.content)
                    Snackbar.make(rootView, "Camera permission denied", Snackbar.LENGTH_INDEFINITE)
                        .setAction("open settings") {
                            val settingIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            val uri = Uri.fromParts("package", requireActivity().packageName, null)
                            settingIntent.data = uri
                            requireActivity().startActivity(settingIntent)
                        }
                        .setAnchorView(binding.scanInstructions)
                        .show()
                }
            }
        setUpImageReader()
    }

    private fun setUpImageReader() {
        imageReader = ImageReader.newInstance(imageWith, imageHeight, ImageFormat.YUV_420_888, 1)
        binding.cameraPreview.holder.setFixedSize(imageWith, imageHeight)
        imageReader.setOnImageAvailableListener({
            val image = it.acquireNextImage()
            image?.let {
                val source = image.toLuminanceSource()
                analyzeImage(source) { uri ->
                    Log.i("TAG", "init: $uri")
                    stopCamera()
                    initializeTableManager(uri)
                }
                image.close()
            }
        }, Handler(Looper.getMainLooper()))
        cameraManager = requireContext().getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    private fun initializeTableManager(uri: Uri) {
        val restaurantId = uri.getQueryParameter("id")!!
        val tableNo = uri.getQueryParameter("table")!!
        tableManager.get().initialize(TableInfo(restaurantId, tableNo.toInt()))
        requireActivity().supportFragmentManager.beginTransaction()
            .remove(this)
            .commit()
    }

    private fun stopCamera() {
        captureSession?.let { session ->
            session.stopRepeating()
            session.abortCaptures()
            session.close()
            captureSession = null
        }
        imageReader.close()
    }

    private fun analyzeImage(source: PlanarYUVLuminanceSource, onSuccess: (Uri) -> Unit) {
        try {
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            val result = qrCodeReader.decode(binaryBitmap)
            result?.let {
                onSuccess(Uri.parse(result.text))
            }
        } catch (e: Exception) {
            if (e is NotFoundException)
                return
            Log.e(TAG, "analyzeBitmap: ", e)
        }
    }

    private fun checkCameraPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

    }

    @SuppressLint("MissingPermission")
    private fun startCamera() {
        val cameraId = cameraManager.cameraIdList.first { id ->
            val characteristics = cameraManager.getCameraCharacteristics(id)
            characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
        }

        cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                cameraDevice = camera
                createCameraPreview()
            }

            override fun onDisconnected(camera: CameraDevice) {
                cameraDevice?.close()
            }

            override fun onError(camera: CameraDevice, error: Int) {
                cameraDevice?.close()
                cameraDevice = null
            }
        }, null)
    }


    private fun createCameraPreview() {
        val surface = binding.cameraPreview.holder.surface
        val previewRequestBuilder : CaptureRequest.Builder?
        try {
             previewRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        }catch (e:Exception){
            Log.e(TAG, "createCameraPreview: ",e )
            return
        }
        previewRequestBuilder?.addTarget(surface)
        previewRequestBuilder?.addTarget(imageReader.surface)
        val sessionConfiguration = SessionConfiguration(
            SESSION_REGULAR,
            listOf(OutputConfiguration(surface), OutputConfiguration(imageReader.surface)),
            ContextCompat.getMainExecutor(requireContext()),
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    captureSession = session
                    previewRequestBuilder?.let { builder ->
                        builder.set(
                            CaptureRequest.CONTROL_AF_MODE,
                            CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                        )
                        session.setRepeatingRequest(builder.build(), null, null)
                    }
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {

                }
            }
        )
        cameraDevice?.createCaptureSession(sessionConfiguration)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopCamera()
    }

}