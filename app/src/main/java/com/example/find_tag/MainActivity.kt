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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.find_tag.ui.theme.FindTagTheme
import kotlin.math.abs


class MainActivity : ComponentActivity() {
    private val arVIewModel: ARViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SerialManager.initialize(this){blockData -> arVIewModel.blockHandler(blockData)}
        FileManager.setFileTime()
        setContent {
            FindTagTheme {
                Column() {
                    val vioPosition: Point? = arVIewModel.vioPosition.collectAsState().value
                    val targetPosition: Point? = arVIewModel.targetPosition.collectAsState().value
                    val SerialText:String = arVIewModel.nowRangingData.value.toString()
                    val realDistance:Float? = arVIewModel.nowDistanceFiltered.value
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
                            SerialManager.connectSerialDevice()
                        } , content = {
                            Text("Connect")
                        }
                        )
                    }

                    Text(text = "DistanceError: ${abs(vioDistance -  SerialText.toFloat())}")

                    Text(text = arVIewModel.anchorPointList.toString() )
                    Text(text = arVIewModel.message.value)
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
                    Spacer(modifier = Modifier.weight(1f))
                    Box() {
                        ARScreen(arViewModel = arVIewModel, modifier = Modifier.align(Alignment.Center)
                            .height(300.dp)
                            .width(1500.dp)
                        )
                        if(realDistance != null) {
                            Text(text = String.format("%.2f",realDistance)+"m",
                                modifier = Modifier.align(Alignment.Center),
                                fontSize = 30.sp,
                                )
                        }
                    }
                    CoordinatePlane(
                        anchorList = arVIewModel.anchorPointList.getPointList(),
                        pointsList = listOf(arVIewModel.targetPosition.value?:Point()),
                        false,
                        planeModifier = Modifier.fillMaxWidth()
                            .aspectRatio(1f)
                    )
                }
            }
        }
    }
    override fun onResume(){
        super.onResume()
        arVIewModel.isChildNodeEmpty.value = true

    }

    override fun onDestroy() {
        super.onDestroy()
        SerialManager.disconnectSerialDevice()
    }

}