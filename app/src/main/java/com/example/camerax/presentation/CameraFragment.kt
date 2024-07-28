package com.example.camerax.presentation

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import androidx.fragment.app.viewModels
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.camerax.R
import com.example.camerax.databinding.FragmentCameraBinding
import com.example.camerax.utils.Constants.DATE_FORMAT
import com.example.camerax.utils.FlashType
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class CameraFragment:Fragment() ,OnItemClickListener{
    private lateinit var binding: FragmentCameraBinding
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null
    private var cam: Camera?=null

    private val cameraViewModel :CameraViewModel by viewModels()

    private lateinit var flashAdapter: FlashAdapter
    private var imagePreview: Preview? = null


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        cameraExecutor= Executors.newSingleThreadExecutor()

        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                onCameraPermissionGranted()
            } else {
                onCameraPermissionDenied()
            }
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding=FragmentCameraBinding.inflate(inflater,container,false)
        checkAndRequestGranted()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lottieAnimation()
        bindViews()


    }


    private fun bindViews()=binding.apply{

        lottieReverse.setOnClickListener {
            lottieReverse.playAnimation()
            lottieReverse.speed=2f
            cameraViewModel.setReverseCamera()
            val getCameraSelected=cameraViewModel.getReverseCamera()
            setUpCamera(getCameraSelected)

            resetFlashMode()
        }

        imageCaptureButton.setOnClickListener {
            takePhoto()
        }

        flashView(cameraViewModel.getFlashMode())

        handleTouchToFocus()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun handleTouchToFocus(){
        binding.previewImage.setOnTouchListener { view, motionEvent ->
            when(motionEvent.action){
                MotionEvent.ACTION_DOWN -> return@setOnTouchListener true
                MotionEvent.ACTION_UP->{
                    val meteringPointFactory=binding.previewImage.meteringPointFactory
                    val meteringPoint=meteringPointFactory.createPoint(motionEvent.x,motionEvent.y)
                    val action= FocusMeteringAction.Builder(meteringPoint, FocusMeteringAction.FLAG_AF)
                        .setAutoCancelDuration(3, TimeUnit.SECONDS)
                        .build()
                    cam?.let {
                        it.cameraControl.startFocusAndMetering(action)
                        binding.focusCircleView.setCircle(motionEvent.x,motionEvent.y,80f)
                        binding.focusCircleView.visibility=View.VISIBLE
                    }
                    binding.focusCircleView.postDelayed({
                        binding.focusCircleView.visibility=View.GONE
                    },1000)
                    return@setOnTouchListener true
                }
                else-> return@setOnTouchListener false
            }
        }
    }


    private fun checkAndRequestGranted(){
        when{
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                onCameraPermissionGranted()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                showPermissionRationale()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

    }
    private fun onCameraPermissionGranted() {
        setUpCamera(cameraViewModel.getReverseCamera())
    }

    private fun setUpCamera(selfieCameraSelected:Boolean){

        flashView(cameraViewModel.getFlashMode())

        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        val cameraSelect=if(selfieCameraSelected){
            CameraSelector.LENS_FACING_FRONT
        }else{
            CameraSelector.LENS_FACING_BACK
        }
        val cameraSelector = CameraSelector.Builder().requireLensFacing(cameraSelect).build()
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            cameraProvider.unbindAll()

            binding.previewImage.display ?: return@addListener

            imagePreview = Preview.Builder().apply {
                setTargetAspectRatio(AspectRatio.RATIO_16_9)
                setTargetRotation(binding.previewImage.display.rotation)
            }.build()

            imageCapture= ImageCapture.Builder().apply {
                setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                setFlashMode(ImageCapture.FLASH_MODE_AUTO)
            }.build()

            cam=cameraProvider.bindToLifecycle(
                viewLifecycleOwner,
                cameraSelector,
                imagePreview,
                imageCapture)



            binding.previewImage.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            imagePreview?.setSurfaceProvider(binding.previewImage.surfaceProvider)
        },
            ContextCompat.getMainExecutor(requireContext()))
    }

    private fun takePhoto() {
        val name = SimpleDateFormat(DATE_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
            }
        }

        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(requireActivity().contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues)
            .build()
        imageCapture?.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback{
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    binding.previewImage.post {
                        Toast.makeText(requireContext(),"Photo saved${outputFileResults.savedUri}",
                            Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(requireContext(),"Photo could not save: ${exception.localizedMessage}",
                        Toast.LENGTH_SHORT).show()
                }

            }
        )



    }

    private fun showPermissionRationale() {
        AlertDialog.Builder(requireContext())
            .setTitle("Camera Permission Needed")
            .setMessage("This app needs the camera permission to take photos.")
            .setPositiveButton("OK") { _, _ ->
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            .setNegativeButton("Cancel") { _ , _ ->
                onCameraPermissionDenied()
            }
            .show()
    }

    private fun onCameraPermissionDenied() {
        requireActivity().finishAffinity()
    }

    private fun flashView(flashType: FlashType){
        binding.ivFlash.visibility=View.VISIBLE
        binding.rvFlash.visibility=View.GONE
        val flashIcon=getFlashIcon(flashType)
        Glide.with(requireContext())
            .load(flashIcon)
            .into(binding.ivFlash)

        binding.ivFlash.setOnClickListener {
            binding.ivFlash.visibility=View.GONE
            binding.rvFlash.visibility=View.VISIBLE
            setUpRecyclerView(flashType)
        }
        flashModeSetup(flashType)
    }

    private fun flashModeSetup(flashType: FlashType){
        imageCapture?.flashMode=flashType.toCameraFlashMode()

        if ( (cam?.cameraInfo?.hasFlashUnit() == true)){
            val onTorch=if(flashType==FlashType.PERMANENT_FLASH){
                true
            }else{
                false
            }
            cam?.cameraControl?.enableTorch(onTorch)
        }
    }
    private fun FlashType.toCameraFlashMode(): Int {
        return when (this) {
            FlashType.NOT_FLASH -> ImageCapture.FLASH_MODE_OFF
            FlashType.AUTO_FLASH -> ImageCapture.FLASH_MODE_AUTO
            else-> ImageCapture.FLASH_MODE_ON
        }
    }

    private fun setUpRecyclerView(flashModeSelected:FlashType)=binding.rvFlash.apply{

        val flashList=FlashType.values().toMutableList()
        if (cameraViewModel.getReverseCamera()){
            flashList.removeLast()
        }
        flashAdapter= FlashAdapter(flashList,context,this@CameraFragment,flashModeSelected)
        layoutManager= LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL,false)
        adapter=flashAdapter
    }

    private fun resetFlashMode(){
        cameraViewModel.setFlashMode(FlashType.NOT_FLASH)
        flashView(cameraViewModel.getFlashMode())
    }

    private fun lottieAnimation(){
        binding.lottieReverse.setOnClickListener {
            binding.lottieReverse.apply {
                animate()
                playAnimation()
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()

    }



    override fun onFlashClicked(data: FlashType) {
        cameraViewModel.setFlashMode(data)
        binding.apply {
            ivFlash.visibility=View.VISIBLE
            rvFlash.visibility=View.GONE
        }
        flashView(cameraViewModel.getFlashMode())
    }

    val getFlashIcon:(FlashType)->Int={flashType->
        when(flashType){
            FlashType.ONE_TIME_FLASH -> {
                R.drawable.flashwhite
            }
            FlashType.NOT_FLASH -> {
                R.drawable.notflashwhite
            }
            FlashType.AUTO_FLASH -> {
                R.drawable.autoflashwhite
            }
            FlashType.PERMANENT_FLASH ->{
                R.drawable.permanentflashwhite
            }
        }
    }

}