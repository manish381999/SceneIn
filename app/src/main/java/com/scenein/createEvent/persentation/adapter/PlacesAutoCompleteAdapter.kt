package com.scenein.createEvent.persentation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.scenein.R

class PlacesAutoCompleteAdapter(
    private val onPlaceClick: (AutocompletePrediction) -> Unit
) : RecyclerView.Adapter<PlacesAutoCompleteAdapter.PredictionViewHolder>() {

    private var predictions: List<AutocompletePrediction> = listOf()

    fun setPredictions(predictions: List<AutocompletePrediction>) {
        this.predictions = predictions
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PredictionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_place_prediction, parent, false)
        return PredictionViewHolder(view)
    }

    override fun onBindViewHolder(holder: PredictionViewHolder, position: Int) {
        val prediction = predictions[position]
        holder.bind(prediction)
    }

    override fun getItemCount(): Int = predictions.size

    inner class PredictionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val primaryTextView: TextView = itemView.findViewById(R.id.tvPrimaryText)
        private val secondaryTextView: TextView = itemView.findViewById(R.id.tvSecondaryText)

        fun bind(prediction: AutocompletePrediction) {
            primaryTextView.text = prediction.getPrimaryText(null)
            secondaryTextView.text = prediction.getSecondaryText(null)
            itemView.setOnClickListener { onPlaceClick(prediction) }
        }
    }
}