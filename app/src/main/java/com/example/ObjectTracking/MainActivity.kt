package com.example.ObjectTracking

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
import com.example.ObjectTracking.ui.theme.SerialViewModel
import com.example.ObjectTracking.ui.theme.ObjectTrackingTheme


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
                    val VIOtext:String = arVIewModel.VIOtext.collectAsState().value
                    val SerialText:String = serialViewmodel.blockString.collectAsState().value
                    val coroutineScope = rememberCoroutineScope()
                    val context = LocalContext.current
                    Box(modifier = Modifier){
                        Text(text = VIOtext)
                    }
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Absolute.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically

                    ){

                        Text(text = SerialText)
                        TextButton(onClick =
                        {
                            serialViewmodel.connectSerialDevice(context = context)
                        } , content = {
                            Text("Connect")
                        }
                        )
                    }
                    TextButton(onClick =
                    {
                        arVIewModel.targetPosition.value = Point(3f,1f,3f)
                    } , content = {
                        Text("make Something")
                    }
                    )
                    Spacer(modifier = Modifier.height(30.dp))
                    ARScreen(arViewModel = arVIewModel, modifier = Modifier)
                }
            }
        }
    }
    override fun onResume(){
        super.onResume()
        arVIewModel.isChildNodeEmpty.value = true

    }


}