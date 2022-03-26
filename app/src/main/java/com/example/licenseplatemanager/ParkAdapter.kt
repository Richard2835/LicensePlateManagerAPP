package com.example.licenseplatemanager

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.view.menu.ActionMenuItemView
import androidx.recyclerview.widget.RecyclerView

class ParkAdapter(
    private val parkList: ArrayList<ParkData>, private val listener: OnItemClickListener
    ) : RecyclerView.Adapter<ParkAdapter.MyViewHolder>() {

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
    View.OnClickListener {

        val parkName : TextView = itemView.findViewById(R.id.parkID)
        val plateNum : TextView = itemView.findViewById(R.id.numPlateID)

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(p0: View?) {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                listener.onItemClick(position)
            }
        }
    }

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.park_item,
        parent,false)
        return MyViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val currentData = parkList[position]
        holder.parkName.text = currentData.parkName
        holder.plateNum.text = currentData.plateNum
    }

    override fun getItemCount(): Int {
        return parkList.size
    }

}