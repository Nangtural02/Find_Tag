package com.example.find_tag

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.find_tag.ui.theme.ObjectTrackingTheme
import com.example.find_tag.ui.theme.SerialViewModel
import kotlin.math.abs


class MainActivity : ComponentActivity() {
    private val arVIewModel: ARViewModel by viewModels()
    private val serialViewmodel by viewModels<SerialViewModel>{
        SerialViewModelFactory(application, arVIewModel)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ObjectTrackingTheme {
                Column() {
                    val VIOtext:String = arVIewModel.vioText.collectAsState().value
                    val vioPosition: Point? = arVIewModel.vioPosition.collectAsState().value
                    val targetPosition: Point? = arVIewModel.targetPosition.collectAsState().value
                    val SerialText:String = serialViewmodel.nowRangingData.value.toString()
                    val realDistance:Float? = serialViewmodel.nowDistanceFiltered.value
                    val coroutineScope = rememberCoroutineScope()
                    val context = LocalContext.current
                    var vioDistance:Float = 0f
                    Box(modifier = Modifier){
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = vioPosition.toString())
                            targetPosition?.let {
                                vioDistance = vioPosition?.getDistance(it) ?: 0f
                            }
                            Text(text = "VIOdistance: ${vioDistance}")
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Absolute.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically

                    ){
                        Text(text = "target: ${targetPosition.toString()} || d:${SerialText}m")
                        TextButton(onClick =
                        {
                            serialViewmodel.connectSerialDevice(context = context)
                        } , content = {
                            Text("Connect")
                        }
                        )
                    }

                    Text(text = "DistanceError: ${abs(vioDistance -  SerialText.toFloat())}")

                    Text(text = serialViewmodel.anchorPointList.toString() )
                    /*
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween

                    ) {
                        TextButton(onClick =
                        {
                            arVIewModel.targetPosition.value = Point(3f, 1f, 3f)
                        }, content = {
                            Text("make Something")
                        }
                        )
                        TextButton(onClick =
                        {
                            arVIewModel.targetPosition.value = arVIewModel.targetPosition.value?.move(1f,0f,1f) ?: Point(3f,1f,3f)
                        }, content = {Text("move SomeWhere")}
                        )

                    }
                    */
                    Spacer(modifier = Modifier.height(30.dp))
                    Box() {
                        ARScreen(arViewModel = arVIewModel, modifier = Modifier.align(Alignment.Center))
                        if(realDistance != null) {
                            Text(text = String.format("%.2f",realDistance)+"m",
                                modifier = Modifier.align(Alignment.Center),
                                fontSize = 30.sp,
                                )
                        }
                    }
                }
            }
        }
    }
    override fun onResume(){
        super.onResume()
        arVIewModel.isChildNodeEmpty.value = true

    }


}