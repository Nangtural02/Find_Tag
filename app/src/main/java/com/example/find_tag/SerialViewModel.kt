package com.example.find_tag
//
//import android.app.Application
//import android.app.PendingIntent
//import android.content.BroadcastReceiver
//import android.content.Context
//import android.content.Intent
//import android.content.IntentFilter
//import android.hardware.usb.UsbDevice
//import android.hardware.usb.UsbDeviceConnection
//import android.hardware.usb.UsbManager
//import android.os.Build
//import android.os.Environment
//import android.util.Log
//import androidx.compose.runtime.MutableState
//import androidx.compose.runtime.mutableFloatStateOf
//import androidx.compose.runtime.mutableStateOf
//import androidx.lifecycle.AndroidViewModel
//import androidx.lifecycle.viewModelScope
//import com.example.find_tag.ARViewModel
//import com.example.find_tag.Point
//import com.example.find_tag.calcBy3Side
//import com.example.find_tag.getVarianceOfPointList
//import com.example.find_tag.toString
//import com.example.find_tag.topThreePandDByKMeans
//import com.google.gson.Gson
//import com.google.gson.JsonSyntaxException
//import com.google.gson.annotations.SerializedName
//import com.hoho.android.usbserial.BuildConfig
//import com.hoho.android.usbserial.driver.CdcAcmSerialDriver
//import com.hoho.android.usbserial.driver.UsbSerialDriver
//import com.hoho.android.usbserial.driver.UsbSerialPort
//import com.hoho.android.usbserial.util.SerialInputOutputManager
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.cancel
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.flow.update
//import kotlinx.coroutines.launch
//import java.io.IOException
//
//
//
//
//
//class SerialViewModel(application: Application, private val arViewModel: ARViewModel): AndroidViewModel(application), SerialInputOutputManager.Listener {
//
//    //data to Update
//    var nowDistanceFiltered: MutableState<Float?> = mutableStateOf(null)
//    var nowRangingData: MutableState<Float> = mutableFloatStateOf(-1f)
//    var rangingList: MutableList<PandD> = mutableListOf()
//    var anchorPointList: List<PandD> = emptyList()
//    //
//    var baudRate = 115200
//    var count:Int = 0
//
//    //parsing custom data format for DWM3001CDK, which is "{ID(int), BlockNum(int), Distance(float)}"
//    /*
//     data class ParsedData(val id: Int, val blockNum: Int, val distance: Float)
//     private fun blockHandler(blockString: String) {
//        viewModelScope.launch { fileManager.writeTextFile(blockString + "\n") }
//        nowBlockString.value = blockString
//        val parsedData = parseData(blockString)
//        if (parsedData != null) {
//            nowRangingData.value = parsedData.distance
//        } else {
//            Log.e("SerialViewModel", "Invalid data format")
//        }
//    }
//    fun parseData(input: String): ParsedData? {
//        val cleanedInput = input.trim().removeSurrounding("{", "}").trim()
//        val parts = cleanedInput.split(",").map { it.trim() }
//        return if (parts.size == 3) {
//            try {
//                val id = parts[0].toInt()
//                val blockNum = parts[1].toInt()
//                val distance = parts[2].toFloat()
//                ParsedData(id, blockNum, distance)
//            } catch (e: NumberFormatException) {
//                Log.e("SerialViewModel", "Number format error: ${e.message}")
//                null
//            }
//        } else {
//            Log.e("SerialViewModel", "Data format error: expected 3 parts but got ${parts.size}")
//            null
//        }
//    }
//     */
//    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//
//    //for DWM3001CDK CLI
//    data class Result(
//        @SerializedName("Addr") val addr: String,
//        @SerializedName("Status") val status: String,
//        @SerializedName("D_cm") val dCm: Int,
//        @SerializedName("LPDoA_deg") val lPDoADeg: Float,
//        @SerializedName("LAoA_deg") val lAoADeg: Float,
//        @SerializedName("LFoM") val lfom: Int,
//        @SerializedName("RAoA_deg") val raDoADeg: Float,
//        @SerializedName("CFO_100ppm") val cfo100ppm: Int
//    )
//
//    data class Data(
//        @SerializedName("Block") val block: Int,
//        @SerializedName("results") val results: List<Result>
//    )
//    private fun blockHandler(blockString: String) {
//
//        try {
//            val data = Gson().fromJson(blockString, Data::class.java)
//            if(data.results[0].status != "Err"){
//                nowRangingData.value = data.results[0].dCm/100f
//                rangingList.add(PandD(arViewModel.vioPosition.value ?: Point(), nowRangingData.value))
//                if(rangingList.size > maxBufferSize){
//                    rangingList.removeAt(0)
//                }
//                nowDistanceFiltered.value = rangingList.takeLast(10).map{it.distance}.average().toFloat()
//                if(getVarianceOfPointList(rangingList.map{it.point})> minimumVariance && rangingList.size > 3 && count >= 20){ //3m 이상 이동했을 때
//                    count = 0
//                    anchorPointList = topThreePandDByKMeans(rangingList)
//                    arViewModel.targetPosition.value = calcBy3Side(anchorPointList)
//                }else{count += 1}
//                viewModelScope.launch { fileManager.writeTextFile("VIO:${arViewModel.vioPosition.value.toString()}, Anchor:${anchorPointList.map{it.point}.toString()}, Target:${arViewModel.targetPosition.value?.changeYandZ().toString()}, AnchorDistance:${anchorPointList.map{it.distance}} , realDistance:${nowDistanceFiltered.value}" + "\n") }
//
//            }
//        }catch(e: JsonSyntaxException){
//            Log.e("SerialViewModel", "signal error")
//        }catch(e: NullPointerException){
//            Log.e("SerialViewModel","nullPointer -")
//        }
//    }
//    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//
//
//    private var connectedUSBItem = MutableStateFlow<USBItem?>(null)
//    private enum class USBPermission {UnKnown, Requested, Granted, Denied}
//    private var usbPermission: USBPermission = USBPermission.UnKnown
//    private val INTENT_ACTION_GRANT_USB: String = BuildConfig.LIBRARY_PACKAGE_NAME + ".GRANT_USB"
//    private var usbIOManager: SerialInputOutputManager? = null
//    private val usbPermissionReceiver = object : BroadcastReceiver() {
//        override fun onReceive(context: Context, intent: Intent) {
//            if (INTENT_ACTION_GRANT_USB == intent.action) {
//                usbPermission = if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
//                    USBPermission.Granted
//                } else {
//                    USBPermission.Denied
//                }
//                connectSerialDevice(context)
//            }
//        }
//    }
//
//    fun connectSerialDevice(context: Context){
//        var count = 0
//        viewModelScope.launch(Dispatchers.IO) {
//            val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
//            while (connectedUSBItem.value == null) {
//                Log.d("SerialViewModel", "try to Connect")
//                for (device in usbManager.deviceList.values) {
//                    val driver = CdcAcmSerialDriver(device)
//                    if (driver.ports.size == 1) {
//                        connectedUSBItem.update {
//                            USBItem(device, driver.ports[0], driver)
//                        }
//                        Log.d("SerialViewModel", "device Connected")
//                    }
//                }
//                delay(1000L) //by 1 sec
//                count ++
//                if(count>5) {
//                    disConnectSerialDevice()
//                    cancel()
//                } //more than 5 sec
//            }
//            val device: UsbDevice = connectedUSBItem.value!!.device
//
//            Log.d("SerialViewModel", "usb connection try")
//            var usbConnection: UsbDeviceConnection? = null
//            if (usbPermission == USBPermission.UnKnown && !usbManager.hasPermission(device)) {
//                usbPermission = USBPermission.Requested
//                val intent: Intent = Intent(INTENT_ACTION_GRANT_USB)
//                intent.setPackage(getApplication<Application>().packageName)
//                Log.d("SerialViewModel", "request Permission")
//                usbManager.requestPermission(
//                    device,
//                    PendingIntent.getBroadcast(
//                        getApplication(),
//                        0,
//                        intent,
//                        PendingIntent.FLAG_IMMUTABLE
//                    )
//                )
//                return@launch
//            }
//            delay(1000L)
//            try {
//                Log.d("SerialViewModel", "Port open try")
//                usbConnection = usbManager.openDevice(device)
//                connectedUSBItem.value!!.port.open(usbConnection)
//            }catch(e:IllegalArgumentException){
//                disConnectSerialDevice()
//                return@launch
//            }catch(e: IOException){
//                if(e.message != "Already Open") throw IOException()
//            }
//            Log.d("SerialViewModel", "Port open")
//            connectedUSBItem.value!!.port.setParameters(baudRate, 8, 1, UsbSerialPort.PARITY_NONE)
//            usbIOManager = SerialInputOutputManager(connectedUSBItem.value!!.port, this@SerialViewModel)
//            usbIOManager!!.start()
//            Log.d("SerialViewModel","dtr On")
//            connectedUSBItem.value?.port?.dtr = true
//        }
//    }
//    fun disConnectSerialDevice(){
//        usbPermission = USBPermission.UnKnown
//        usbIOManager?.listener = null
//        usbIOManager?.stop()
//        if(connectedUSBItem.value == null) return
//        if(connectedUSBItem.value!!.port.isOpen()){
//            connectedUSBItem.value?.port?.close()
//        }
//        connectedUSBItem.update{ null }
//    }
//
//    val blockString: StateFlow<String> get() = _blockString
//    private val _blockString = MutableStateFlow<String>("not Connected")
//    private var _buffer = mutableStateOf("")
//
//    //for JSON
//
//    override fun onNewData(data: ByteArray?) { // called when get data
//        viewModelScope.launch{
//            if(data != null) {
//                if (data.isNotEmpty()) {
//                    val result : String = getLineString(data, data.size)
//                    if (_buffer.value.isEmpty()) {
//                        _buffer.value += result
//                    }else{
//                        if(result.length >=3 && result.substring(0,3) == "{\"B"){ //메시지를 받다말고 새로운 메시지가 들어옴
//                            _buffer.value = result
//                        }else if(result.length >=3 && result.substring(result.length - 3).equals("}  ")){ //메시지의 끝
//                            _buffer.value += result.substring(0,result.length-2)
//                            _blockString.value = _buffer.value
//                            blockHandler(_buffer.value)
//                            _buffer.value = ""
//                        }else{
//                            _buffer.value += result
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//    // For Custom Data Format ( {~~~~~} )
//    /*
//    override fun onNewData(data: ByteArray?) {
//        viewModelScope.launch{
//            if(data != null) {
//                if (data.isNotEmpty()) {
//                    val result : String = getLineString(data, data.size)
//                    if (_buffer.value.isEmpty()) {
//                        _buffer.value += result
//                    }else{
//                        result.replace(" ","")
//                        if(result.contains("}")){
//                            _buffer.value += result
//                            Log.d("blockHandle", _buffer.value)
//                            blockHandler(_buffer.value)
//                            _buffer.value = ""
//                        }else{
//                            _buffer.value += result
//                        }
//                    }
//                }
//            }
//        }
//    }
//     */
//
//
//
//    override fun onRunError(e: Exception) {
//        viewModelScope.launch() {
//            Log.e("SerialViewModel", "Disconnected: ${e.message}")
//            disConnectSerialDevice()
//        }
//    }
//
//    private fun getLineString(array: ByteArray, length: Int): String {
//        val result = StringBuilder()
//        val line = ByteArray(8)
//        var lineIndex = 0
//        for (i in 0 until 0 + length) {
//            if (lineIndex == line.size) {
//                for (j in line.indices) {
//                    if (line[j] > ' '.code.toByte() && line[j] < '~'.code.toByte()) {
//                        result.append(String(line, j, 1))
//                    } else {
//                        result.append(" ")
//                    }
//                }
//                lineIndex = 0
//            }
//            val b = array[i]
//            line[lineIndex++] = b
//        }
//        for (i in 0 until lineIndex) {
//            if (line[i] > ' '.code.toByte() && line[i] < '~'.code.toByte()) {
//                result.append(String(line, i, 1))
//            } else {
//                result.append(" ")
//            }
//        }
//        return result.toString()
//    }
//    init{
//        val filter = IntentFilter(INTENT_ACTION_GRANT_USB)
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            // Android 12 이상일 경우, 명시적으로 플래그를 지정
//            getApplication<Application>().registerReceiver(usbPermissionReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
//        } else {
//            // Android 12 미만일 경우, 기존 방식으로 등록
//            getApplication<Application>().registerReceiver(usbPermissionReceiver, filter)
//        }
//        fileManager.setFileTime()
//        viewModelScope.launch{
//            blockString.collect{
//                it?.let{
//                    blockHandler(it)
//                    //todo: ProcessData
//                }
//
//            }
//        }
//    }
//}
//
//data class USBItem(
//    val device: UsbDevice,
//    val port: UsbSerialPort,
//    val driver: UsbSerialDriver = CdcAcmSerialDriver(device)
//)
