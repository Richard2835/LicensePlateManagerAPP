package com.example.licenseplatemanager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class LicPlateAdapter(
    private val plateList: ArrayList<LicPlateData>, private val listener: LicPlateAdapter.OnItemClickListener
): RecyclerView.Adapter<LicPlateAdapter.MyViewHolder>() {

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener{
        val plateText : TextView = itemView.findViewById(R.id.plateTextID)
        val dateText : TextView = itemView.findViewById(R.id.dateID)
        val deletePlateBtn : ImageView = itemView.findViewById(R.id.deleteBtnID)

        init {
            deletePlateBtn.setOnClickListener(this)
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
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.plate_item,
            parent,false)
        return MyViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val currentData = plateList[position]
        holder.plateText.text = currentData.name
        holder.dateText.text = currentData.date
        holder.deletePlateBtn
    }

    override fun getItemCount(): Int {
        return plateList.size
    }
}