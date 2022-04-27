package com.example.licenseplatemanager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class NotificationAdapter(
    private val notifList: ArrayList<NotificationData>,
    private val listener: NotificationAdapter.OnItemClickListener
) : RecyclerView.Adapter<NotificationAdapter.MyViewHolder>() {

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        val plate: TextView = itemView.findViewById(R.id.notifyPlateID)
        val park: TextView = itemView.findViewById(R.id.notifyParkID)
        val dateTime: TextView = itemView.findViewById(R.id.notifyDateTimeID)
        val checkBtn: ImageView = itemView.findViewById(R.id.checkBtnID)

        init {
            checkBtn.setOnClickListener(this)
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
        val itemView = LayoutInflater.from(parent.context).inflate(
            R.layout.notify_item,
            parent, false
        )
        return MyViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val currentNotification = notifList[position]
        holder.plate.text = currentNotification.plate
        holder.park.text = currentNotification.park
        holder.dateTime.text = currentNotification.dateTime
        holder.checkBtn
    }

    override fun getItemCount(): Int {
        return notifList.size
    }
}