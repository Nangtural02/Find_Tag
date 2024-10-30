package com.example.ObjectTracking

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.ar.core.TrackingFailureReason
import io.github.sceneview.ar.getDescription
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ARViewModel: ViewModel() {
    private val _VIOtext = MutableStateFlow("empty")
    val VIOtext: StateFlow<String> = _VIOtext

    private val _trackingState = MutableStateFlow<String>("")
    val trackingState: StateFlow<String> = _trackingState
    val isChildNodeEmpty = mutableStateOf(true)

    val targetPosition = MutableStateFlow<Point?>(null)

    fun updateVIOText(text: String) {
        _VIOtext.value = text
    }

    fun updateTrackingFailureReason(context : Context, reason: TrackingFailureReason?) {
         reason?.let {
            _trackingState.value = it.getDescription(context)
        } ?: if (isChildNodeEmpty.value) {
             _trackingState.value = "Move phone more"
        } else {
             _trackingState.value = "VIO Position Good"
        }
    }

}
data class Point(var x: Float = 0f, var y: Float = 0f, var z: Float =0f)
