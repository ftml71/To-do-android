package com.example.todo.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.opengl.Visibility
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.GeneratedAdapter
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.todo.R
import com.example.todo.SwipeGesture
import com.example.todo.TodoRVAdapter
import com.example.todo.TodoViewModel
import com.example.todo.data.TodoEntity
import com.example.todo.databinding.FragmentMydayBinding
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*


class MydayFragment : Fragment(),TodoRVAdapter.RVInterface {

    var _binding:FragmentMydayBinding? = null
    val binding get() = _binding!!
    private val mViewModel: TodoViewModel by activityViewModels()
    val args: MydayFragmentArgs by navArgs()
    private val rotateAnim: Animation by lazy { AnimationUtils.loadAnimation(context,R.anim.rotate_ninty) }
    private val rotateAnimAnti: Animation by lazy { AnimationUtils.loadAnimation(context,R.anim.rotate_ninty_anti) }

    // Getting Date-Time
    @RequiresApi(Build.VERSION_CODES.O)
    val dateToday = LocalDateTime.now()
    @RequiresApi(Build.VERSION_CODES.O)
    val day = LocalDate.now().dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.US)

    @RequiresApi(Build.VERSION_CODES.O)
    val formatter = DateTimeFormatter.ofPattern("MMMM dd")
    @RequiresApi(Build.VERSION_CODES.O)
    val formatted = dateToday.format(formatter)

    val dayToShow: String = "${day},${formatted}"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentMydayBinding.inflate(layoutInflater,container,false)
        val view =  binding.root
        //mViewModel = ViewModelProvider(this).get(TodoViewModel::class.java)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        // First RV
        val recyclerViewIncomp = binding.recyclerViewIncomplete
        val adapter_incomp = TodoRVAdapter(this)
        binding.dateTimeExpanded.text = dayToShow

        //SWIPE GESTURE for rv1
        val swipeGesture = object :SwipeGesture(){

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                when(direction){
                    ItemTouchHelper.LEFT->{
                        val data = mViewModel.listData().value?.get(viewHolder.adapterPosition)
                        mViewModel.removeTodo(data!!)
                    }
                }
            }
        }

        //First RV
        recyclerViewIncomp.adapter = adapter_incomp
        recyclerViewIncomp.layoutManager = LinearLayoutManager(context)
        recyclerViewIncomp.setHasFixedSize(false)
        ItemTouchHelper(swipeGesture).attachToRecyclerView(recyclerViewIncomp)

        //SWIPE Gesture for rv2
        val swipeGestureTwo = object :SwipeGesture(){

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                when(direction){
                    ItemTouchHelper.LEFT->{
                        val data = mViewModel.listCompleted().value?.get(viewHolder.adapterPosition)
                        mViewModel.removeTodo(data!!)
                    }
                }
            }
        }


        // Second RV
        val recyclerViewComp = binding.recyclerViewComplete
        val adapter_comp = TodoRVAdapter(this)
        recyclerViewComp.adapter = adapter_comp
        recyclerViewComp.layoutManager = LinearLayoutManager(context)
        recyclerViewComp.setHasFixedSize(false)
        ItemTouchHelper(swipeGestureTwo).attachToRecyclerView(recyclerViewComp)

        mViewModel.listData().observe(viewLifecycleOwner, Observer {
            binding.progressBar.isVisible = true
            adapter_incomp.NotifyChanges(it)
            binding.progressBar.isVisible = false

        })

        mViewModel.listCompleted().observe(viewLifecycleOwner, Observer {
            adapter_comp.NotifyChanges(it)
        })

        binding.completedText.setOnClickListener{
            val completed_recycler = binding.recyclerViewComplete
            if (completed_recycler.isVisible){
                binding.completedIcon.animation = rotateAnimAnti
                completed_recycler.isVisible = false
            }
            else{
                binding.completedIcon.animation = rotateAnim
                completed_recycler.isVisible = true
            }
            //completed_recycler.isVisible = !completed_recycler.isVisible
        }

        binding.floatingActionButton.setOnClickListener {
            val action = MydayFragmentDirections.actionMydayFragmentToAddFragment()
            Navigation.findNavController(view).navigate(action)
        }
        binding.navigateBack.setOnClickListener {
            val action = MydayFragmentDirections.actionMydayFragmentToFrontpage()

            Navigation.findNavController(view).navigate(action)

        }

    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCheckboxClick(data:TodoEntity) {
        //val columnData = mViewModel.listData().value?.get(position)
        if (data?.completed == false){
            //Toast.makeText(context,position.toString(),Toast.LENGTH_SHORT).show()
            val newData = TodoEntity(data.id,data.title,data.important,true)
            mViewModel.updateTodo(newData)

        }
        else{
            val newData = TodoEntity(data.id,data.title,data.important,false)
            mViewModel.updateTodo(newData)
        }
    }

    override fun onStarClick(position: Int) {
        val columnData = mViewModel.listData().value?.get(position)
        if (columnData?.important == true){
            val newData = TodoEntity(columnData.id,columnData.title,false,columnData.completed)
            mViewModel.updateTodo(newData)

        }else{
            val newData = TodoEntity(columnData?.id!!,columnData.title,true,columnData.completed)
            mViewModel.updateTodo(newData)
        }
    }

}