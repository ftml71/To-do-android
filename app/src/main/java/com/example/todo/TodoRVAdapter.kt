package com.example.todo

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Color
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.todo.data.TodoEntity
import com.example.todo.databinding.FragmentMydayBinding
import com.example.todo.databinding.SingleColumnBinding
import com.example.todo.fragments.MydayFragment

class TodoRVAdapter: RecyclerView.Adapter<TodoRVAdapter.TodoViewHolder>() {

    private var DataList = emptyList<TodoEntity>()
    var _binding : SingleColumnBinding? = null
    val binding get() =  _binding!!
    private lateinit var context:Context

    class TodoViewHolder(itemView:View):RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
        _binding = SingleColumnBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        context = parent.context
        return TodoViewHolder(binding.root)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        val currentItem = DataList[position]
        val star_icon: ImageView = binding.starIconColumn

        binding.checkboxColumn.text = currentItem.title
        if (currentItem.important==true){
            binding.starIconColumn.imageTintList = (ColorStateList.valueOf(ContextCompat.getColor(context,R.color.blue)))
        }

        //set onclick listener on text and checkbox
        binding.starIconColumn.setOnClickListener {
            var star:Boolean = false
            if(currentItem.important==true){
                star = false
                binding.starIconColumn.imageTintList = null

            }
            else{
                star = true
                binding.starIconColumn.imageTintList = (ColorStateList.valueOf(ContextCompat.getColor(context,R.color.blue)))
            }
        }

    }

    override fun getItemCount(): Int {
        return DataList.size
    }

    class TodoDiffCallback(var oldList:List<TodoEntity>,var newList: List<TodoEntity>):DiffUtil.Callback(){
        override fun getOldListSize(): Int {
            return oldList.size
        }

        override fun getNewListSize(): Int {
            return newList.size
        }

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return (oldList.get(oldItemPosition).id == newList.get(newItemPosition).id)
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return (oldList.get(oldItemPosition).equals(newList.get(newItemPosition)))
        }

    }

    @SuppressLint("NotifyDataSetChanged")
    fun NotifyChanges(DataList:List<TodoEntity>){
        val oldList = this.DataList
        val diffResult: DiffUtil.DiffResult = DiffUtil.calculateDiff(
            TodoDiffCallback(oldList,DataList)
        )
        this.DataList = DataList
        //notifyDataSetChanged()
        diffResult.dispatchUpdatesTo(this)
    }
    fun ColumnAdded(Data: TodoEntity){
        val mutableDataList:MutableList<TodoEntity> = DataList.toMutableList()
        mutableDataList.add(0,Data)
        this.DataList = mutableDataList.toList()
//        DataList.toMutableList().add(0,Data)
        notifyItemInserted(0)
    }
}