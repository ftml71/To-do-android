package com.example.todo.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PorterDuff
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.format.DateUtils
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.activityViewModels
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.AppBarConfiguration
import com.example.todo.R
import com.example.todo.TodoViewModel
import com.example.todo.alarms.AlarmReceiver
import com.example.todo.alarms.AlarmService
import com.example.todo.data.TodoEntity
import com.example.todo.databinding.FragmentDetailsBinding
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class DetailsFragment : Fragment() {

    private var _binding: FragmentDetailsBinding? = null
    private val binding get() = _binding!!
    private val mViewModel: TodoViewModel by activityViewModels()
    private val args:DetailsFragmentArgs by navArgs()
    private var bmp: Bitmap? = null
    private lateinit var calendar:Calendar
    private var alarmTime:Int? = null
    private var dueDate:String? = null
    private var alarmRepeatSelected:Boolean = false
    private val buttonPress: Animation by lazy { AnimationUtils.loadAnimation(context,R.anim.button_press) }

    val timeNow = SimpleDateFormat("yyyyMMdd_HHmmss",Locale.getDefault()).format(System.currentTimeMillis())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentDetailsBinding.inflate(layoutInflater,container,false)
        val view =  binding.root

        return view
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val entity = args.dataEntity!!
        bmp = entity.bitmap
        var imp = entity.important
        alarmTime = entity.alarmTime
        dueDate = entity.dueDate
        var formattedDate:String? = null
        var dateObject:Date? = null
        if (dueDate!=null) {
            val format = SimpleDateFormat("dd/MM/yyyy")
            dateObject = format.parse(dueDate!!)
            formattedDate = getFormattedDate(dateObject!!)
        }

        if (alarmTime!=null){
            val hrs:Int = alarmTime!!/100
            val min:Int = alarmTime!!%100

            binding.setAlarmIcon.apply {
                text = String.format("%02d:%02d",hrs,min)
                setTextColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(),R.color.deep_blue)))
                val drawables = compoundDrawables
                drawables.forEach {
                    it?.setTint(ContextCompat.getColor(requireContext(),R.color.deep_blue))
                }
            }
        }

        if (dueDate!=null){
            binding.setDueIcon.text = formattedDate
            if (Date().after(dateObject)){
                binding.setDueIcon.
                    setTextColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.red)))
            }
        }

        binding.setAlarmIcon.setOnClickListener {
            it.startAnimation(buttonPress)
            showClockPicker()
        }

        binding.setDueIcon.setOnClickListener {
            it.startAnimation(buttonPress)
            showDatePicker()
        }

        activity?.findViewById<BottomAppBar>(R.id.bottom_appbar)?.visibility = View.GONE
        activity?.findViewById<FloatingActionButton>(R.id.floatingActionButtonMain)?.visibility = View.GONE

        binding.detailedItemTitle.setText( args.dataEntity?.title.toString())
        binding.detailedDescription.setText(args.dataEntity?.description)
        binding.datetimeDetails.text = "Last Updated: ${entity.dateTime}"

        if(args.dataEntity?.important == true){
            binding.importantSign.imageTintList = (ColorStateList.valueOf(ContextCompat.getColor(
                requireContext(), R.color.red
            )))
        }
        if(args.dataEntity?.completed==true){
            binding.checkbox.isChecked = true
        }

        //Picks Image From Ext. Storage
        val imageAction = registerForActivityResult(
            ActivityResultContracts.GetContent(),
            ActivityResultCallback { uri->

                binding.image1.setImageURI(uri)
                val stream = activity?.contentResolver?.openInputStream(uri)
                bmp = BitmapFactory.decodeStream(stream)
                //saveImage(bmp)
            }
        )

        binding.galleryIcon.setOnClickListener {
            it.startAnimation(buttonPress)
            if (bmp==null) {
                requestPermission()
                imageAction.launch("image/*")
                //bmp = binding.image1.drawable.toBitmap(250, 250)
                binding.imageCard.visibility = View.VISIBLE
                //binding.image1.setImageBitmap(bmp)
            }else{
                Toast.makeText(context,"Image already exist, Remove to add new one",Toast.LENGTH_SHORT).show()
            }
        }

        // checks for existing bitmap
        if (bmp!=null){
            Log.d("BitmapDetails",bmp.toString())
            binding.apply {
                image1.setImageBitmap(bmp)
                image1.invalidate()
                imageCard.visibility = View.VISIBLE
            }
        }else{
            binding.imageCard.visibility = View.GONE
        }

        //Picks Image From Camera
        val getImageFromCamera = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            ActivityResultCallback {
                if (it.data!=null && it.resultCode==RESULT_OK){
                    val bundle = it.data!!.extras
                    val bitmap:Bitmap = bundle?.get("data") as Bitmap
                    binding.image1.setImageBitmap(bitmap)
                    bmp = bitmap
                    binding.imageCard.visibility = View.VISIBLE
                }
            }
        )

        binding.cameraIcon.setOnClickListener {
            it.startAnimation(buttonPress)
            if(bmp==null) {
                requestPermission()
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                getImageFromCamera.launch(intent)
            }else{
                Toast.makeText(context,"Image Already Exists",Toast.LENGTH_SHORT).show()
            }
        }

        binding.image1.setOnClickListener {
            //TODO: Popup with large preview (Create a fragment if needed)
        }

        binding.removeImage.setOnClickListener {
            it.startAnimation(buttonPress)
            bmp = null
            binding.imageCard.visibility = View.GONE
        }
        binding.importantSign.apply {
            setOnClickListener {
                it.startAnimation(buttonPress)
                if (entity.important) {
                    imp = false
                    this.colorFilter = null
                } else {
                    imp = true
                    this.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.red))
                }
            }
            if (imp){
                this.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.red))
            }
        }

        binding.updateTickButton.setOnClickListener {
            it.startAnimation(buttonPress)
            val title:String = binding.detailedItemTitle.text.toString()
            val description:String = binding.detailedDescription.text.toString()
            //val importance:Boolean = args.dataEntity!!.important
            val check: Boolean = binding.checkbox.isChecked
            var requestCode:Int? = entity.requestCode
            //Add more later on (links,images,importance,etc)

            val updatedUnit:TodoEntity = TodoEntity(entity.id,title,description,imp,check,entity.groupName,entity.dateTime,entity.nestedTodo,bmp,requestCode,alarmTime,entity.dueDate)
            mViewModel.updateTodo(updatedUnit)

            val action = DetailsFragmentDirections.actionDetailsFragmentToMydayFragment()
            Navigation.findNavController(view).navigate(action)


            val intent:Intent = Intent(context, AlarmReceiver::class.java)
            intent.putExtra("title",entity.title)

            if (alarmRepeatSelected) {
                if (requestCode!=null) {
                    Log.d("Alarm REQUEST GOT", requestCode.toString())
                    AlarmService(requireContext()).setExactAlarm(
                        calendar.timeInMillis,
                        requestCode,
                        intent
                    )
                }else{
                    //Creating new requestCode
                    requestCode = entity.id
                    AlarmService(requireContext()).setExactAlarm(
                        calendar.timeInMillis,
                        requestCode,
                        intent
                    )
                }
            }
        }

        binding.navigateBack.setOnClickListener {
            Navigation.findNavController(it).navigateUp()
        }

    }

//    private fun saveImage(bmp:Bitmap?) {
//        val path:File = Environment.getStorageDirectory()
//    }

    private fun showDatePicker() {

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select Date")
            .setTheme(R.style.ThemeOverlay_App_DatePicker)
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build()

        datePicker.show(parentFragmentManager,"DatePicker")

        datePicker.addOnPositiveButtonClickListener {
            val privateCalendar = Calendar.getInstance()
            privateCalendar.time = Date(it)
            val day = String.format("%02d/%02d/%d",privateCalendar.get(Calendar.DAY_OF_MONTH),privateCalendar.get(Calendar.MONTH),privateCalendar.get(Calendar.YEAR))
            dueDate = day
            binding.setDueIcon.apply {
                text = day
                setTextColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.deep_blue)))
                //compoundDrawables[0].setTint(resources.getColor(R.color.deep_blue))
            }
        }
    }

    private fun showClockPicker() {
        val entity = args.dataEntity
        val alarmTimeInit = entity?.alarmTime
        val currentTime = Calendar.getInstance()
        var hours = currentTime.get(Calendar.HOUR_OF_DAY)
        var minutes = currentTime.get(Calendar.MINUTE)

        if (alarmTimeInit!=null){
            hours =alarmTimeInit.toInt()/100
            minutes =alarmTimeInit.toInt()%100
        }

        val timePicker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_12H)
            .setHour(hours)
            .setMinute(minutes)
            .setTitleText("Select Time")
            .setTheme(R.style.Theme_App_Timepicker)
            .build()

        timePicker.show(parentFragmentManager,"TimePicker")

        timePicker.addOnPositiveButtonClickListener {
            val hrs = timePicker.hour
            val min = timePicker.minute

            calendar = Calendar.getInstance()
            calendar[Calendar.HOUR_OF_DAY] = hrs
            calendar[Calendar.MINUTE] = min
            calendar[Calendar.SECOND] = 0
            calendar[Calendar.MILLISECOND] = 0


            binding.setAlarmIcon.text = String.format("%02d:%02d PM",hrs,min)
            alarmRepeatSelected = true
            alarmTime = hrs*100+min

        }
    }

    private fun hasStorageReadPermission() = ActivityCompat.checkSelfPermission(requireContext(),Manifest.permission.READ_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED
    private fun hasStorageWritePermission() = ActivityCompat.checkSelfPermission(requireContext(),Manifest.permission.WRITE_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED
    private fun hasCameraPermission() = ActivityCompat.checkSelfPermission(requireContext(),Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED
    private fun requestPermission(){
        val permissionList = mutableListOf<String>()
        if(!hasStorageReadPermission()){
            permissionList.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (!hasStorageWritePermission()){
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        if (!hasCameraPermission()){
            permissionList.add(Manifest.permission.CAMERA)
        }

        if (permissionList.isNotEmpty()){
            ActivityCompat.requestPermissions(requireActivity(),permissionList.toTypedArray(),0)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode==0 && grantResults.isNotEmpty()){
            grantResults.forEach {
                if (it==PackageManager.PERMISSION_GRANTED){
                    Log.d("PermissionsRequest","Permission Granted")
                }
            }
        }
    }

    private fun getFormattedDate(d: Date): String {
        val today:Boolean = DateUtils.isToday(d.time)
        val tomorrow:Boolean = DateUtils.isToday(d.time - DateUtils.DAY_IN_MILLIS)
        val yesterday:Boolean = DateUtils.isToday(d.time + DateUtils.DAY_IN_MILLIS)

        return when {
            today -> {
                "Today"
            }
            tomorrow -> {
                "Tomorrow"
            }
            yesterday -> {
                "Yesterday"
            }
            else -> {
                val formatter = SimpleDateFormat("dd/MM/YYYY")
                formatter.format(d)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}