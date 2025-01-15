package com.example.find_tag

import android.content.Context
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ar.core.TrackingFailureReason
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.annotations.SerializedName
import io.github.sceneview.ar.getDescription
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private const val maxBufferSize:Int = 1000
private const val minimumVariance:Float = 3f

class ARViewModel: ViewModel() {
    //data to process
    val vioPosition = MutableStateFlow<Point?>(null)
    val targetPosition = MutableStateFlow<Point?>(null)
    var nowDistanceFiltered: MutableState<Float?> = mutableStateOf(null)
    var nowRangingData: MutableState<Float> = mutableFloatStateOf(-1f)
    var rangingList: MutableList<PandD> = mutableListOf()
    var anchorPointList: List<PandD> = emptyList()
    val message : MutableState<String> = mutableStateOf("Move more")
    //
    private var count = 0
    fun blockHandler(blockString: String) {

        try {
            val data = Gson().fromJson(blockString, Data::class.java)
            if(data.results[0].status != "Err"){
                nowRangingData.value = data.results[0].dCm/100f
                rangingList.add(PandD(vioPosition.value ?: Point(), nowRangingData.value))
                if(rangingList.size > maxBufferSize){
                    rangingList.removeAt(0)
                }
                nowDistanceFiltered.value = rangingList.takeLast(10).map{it.distance}.average().toFloat()
                if(getVarianceOfPointList(rangingList.map{it.point})> minimumVariance && rangingList.size > 3 && count >= 20){ //3m 이상 이동했을 때
                    message.value = "OK"
                    count = 0
                    anchorPointList = topThreePandDByKMeans(rangingList)
                    //targetPosition.value = calcBy3Side(anchorPointList)
                    targetPosition.value = refine2DPositionLevenbergMarquardt(anchorPointList.getDistanceList(),anchorPointList.getPointList(), targetPosition.value)
                }else{count += 1}
                viewModelScope.launch { FileManager.writeTextFile("VIO:${vioPosition.value.toString()}, Anchor:${anchorPointList.map{it.point}.toString()}, Target:${targetPosition.value?.changeYandZ().toString()}, AnchorDistance:${anchorPointList.map{it.distance}} , realDistance:${nowDistanceFiltered.value}" + "\n") }

            }
        }catch(e: JsonSyntaxException){
            Log.e("SerialViewModel", "signal error")
        }catch(e: NullPointerException){
            Log.e("SerialViewModel","nullPointer -")
        }
    }
    fun updateVIOPosition(groundX: Float, groundY: Float, elevation: Float){
        vioPosition.value = Point(groundX, groundY, elevation)
    }

    //Describe Camera's State and order to user to get better ar data.
    private val _trackingState = MutableStateFlow<String>("")
    val trackingState: StateFlow<String> = _trackingState
    val isChildNodeEmpty = mutableStateOf(true)
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
data class Result(
    @SerializedName("Addr") val addr: String,
    @SerializedName("Status") val status: String,
    @SerializedName("D_cm") val dCm: Int,
    @SerializedName("LPDoA_deg") val lPDoADeg: Float,
    @SerializedName("LAoA_deg") val lAoADeg: Float,
    @SerializedName("LFoM") val lfom: Int,
    @SerializedName("RAoA_deg") val raDoADeg: Float,
    @SerializedName("CFO_100ppm") val cfo100ppm: Int
)

data class Data(
    @SerializedName("Block") val block: Int,
    @SerializedName("results") val results: List<Result>
)