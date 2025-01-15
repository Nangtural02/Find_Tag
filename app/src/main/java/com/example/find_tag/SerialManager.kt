package com.example.find_tag

/** How to Use
 * First, You need to Initialize and Release SerialManager.
 * 1. Initialize Serial Manager at MainActivity.onCreate() (or whenever you want to start).
 *    you can call SerialManager.initialize(context,blockHandler) function.
 *    in Context, it needs to application(or Activity) context.
 *    in blockHandler, it needs to function which handle a block data with Serial.
 * 2. Release Serial Manager at MainActivity.onDestroy() (or whenever you want to end).
 *    you can call SerialManager.release() function.
 *
 * Second, You Can Connect or Disconnect with SerialManager.connectSerialDevice or SerialManager.disconnectSerialDevice
 * After Connect, the blockHandler which you registered in Initialization will call every new block data.
 */

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.util.Log
import com.hoho.android.usbserial.BuildConfig
import com.hoho.android.usbserial.driver.CdcAcmSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.util.SerialInputOutputManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException


object SerialManager {
    var baudRate: Int = 115200

    val blockString: StateFlow<String> = SerialListener._blockString
    private val _serialIsConnected = MutableStateFlow(false)
    val serialIsConnected: StateFlow<Boolean> = _serialIsConnected

    // --------------------
    //  Internal fields
    // --------------------
    private lateinit var appContext: Context
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private enum class USBPermission { UnKnown, Requested, Granted, Denied }
    private var usbPermission: USBPermission = USBPermission.UnKnown
    private const val INTENT_ACTION_GRANT_USB: String =
        BuildConfig.LIBRARY_PACKAGE_NAME + ".GRANT_USB"
    private var usbIOManager: SerialInputOutputManager? = null
    private var connectedUSBItem = MutableStateFlow<USBItem?>(null)
    /**
     * BroadcastReceiver for USB permission result
     */
    private val usbPermissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (INTENT_ACTION_GRANT_USB == intent.action) {
                usbPermission = if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    USBPermission.Granted
                } else {
                    USBPermission.Denied
                }
                connectSerialDevice()
            }
        }
    }

    /**
     * 싱글톤 초기화
     * - Application Context를 전달받아야 함
     * - USB 권한 브로드캐스트 리시버 등록
     */
    fun initialize(context: Context, blockHandler: (String)->Unit) {
        appContext = context.applicationContext

        val filter = IntentFilter(INTENT_ACTION_GRANT_USB)
        appContext.registerReceiver(usbPermissionReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        Log.d("SerialManager", "Initialized with application context.")

        SerialListener.blockHandler = blockHandler
        Log.d("SerialListener", "blockHandler Callback has been registered")
    }

    fun release() {
        try {
            appContext.unregisterReceiver(usbPermissionReceiver)
        } catch (e: Exception) {
            Log.e("SerialManager", "Receiver already unregistered: ${e.message}")
        }
        disconnectSerialDevice()
        scope.cancel()
        Log.d("SerialManager", "Released resources.")
    }

    /**
     * 시리얼 연결 시도
     * - USBManager에서 디바이스 목록을 스캔
     * - CdcAcmSerialDriver로 열 수 있는 디바이스 찾기
     * - 권한 요청 → 권한 승인 시 포트 열기
     */
    fun connectSerialDevice() {
        scope.launch {
            val usbManager = appContext.getSystemService(Context.USB_SERVICE) as UsbManager
            if (connectedUSBItem.value == null) {
                Log.d("SerialManager", "Try to find a CDC device.")
                val foundItem = findCdcDevice(usbManager)
                if (foundItem == null) {
                    Log.d("SerialManager", "No suitable device found.")
                    return@launch
                }
                connectedUSBItem.update { foundItem }
            }

            val device: UsbDevice = connectedUSBItem.value!!.device
            if (usbPermission == USBPermission.UnKnown && !usbManager.hasPermission(device)) {
                // 아직 권한이 없으면 권한 요청
                usbPermission = USBPermission.Requested
                val intent = Intent(INTENT_ACTION_GRANT_USB).apply {
                    setPackage(appContext.packageName)
                }
                val pi = PendingIntent.getBroadcast(
                    appContext,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE
                )
                Log.d("SerialManager", "request Permission for device: $device")
                usbManager.requestPermission(device, pi)
                return@launch
            }
            // 권한 받은 후 → 실제 포트 열기 시도
            delay(500) // 조금 대기
            openSerialPort(usbManager)
        }
    }

    private suspend fun openSerialPort(usbManager: UsbManager) {
        val device: UsbDevice = connectedUSBItem.value!!.device
        Log.d("SerialManager", "usb connection try for device: $device")

        var usbConnection: UsbDeviceConnection? = null
        try {
            usbConnection = usbManager.openDevice(device)
            connectedUSBItem.value!!.port.open(usbConnection)
        } catch (e: IllegalArgumentException) {
            Log.e("SerialManager", "Port open error(IAE): ${e.message}")
            disconnectSerialDevice()
            return
        } catch (e: IOException) {
            if (e.message != "Already Open") {
                Log.e("SerialManager", "Port open error(IOE): ${e.message}")
                disconnectSerialDevice()
                return
            }
        }

        Log.d("SerialManager", "Port open success.")
        connectedUSBItem.value!!.port.setParameters(baudRate, 8, 1, UsbSerialPort.PARITY_NONE)


        usbIOManager = SerialInputOutputManager(connectedUSBItem.value!!.port, SerialListener).apply {
            start()
        }
        Log.d("SerialManager", "IOManager started.")
        connectedUSBItem.value?.port?.dtr = true
        _serialIsConnected.value = true
    }

    fun disconnectSerialDevice() {
        scope.launch {
            usbPermission = USBPermission.UnKnown
            usbIOManager?.listener = null
            usbIOManager?.stop()
            usbIOManager = null

            val item = connectedUSBItem.value ?: return@launch
            item.port.dtr = false
            if (item.port.isOpen) {
                item.port.close()
            }
            connectedUSBItem.update { null }
            _serialIsConnected.value = false

            Log.d("SerialManager", "Serial disconnected.")
        }
    }

    /**
     * CDC ACM 타입의 USB 디바이스를 찾고,
     * driver.ports[0]을 USBItem으로 래핑하여 반환
     */
    private fun findCdcDevice(usbManager: UsbManager): USBItem? {
        for (device in usbManager.deviceList.values) {
            val driver = CdcAcmSerialDriver(device)
            if (driver.ports.size == 1) {
                Log.d("SerialManager", "Found a CDC device: $device")
                return USBItem(device, driver.ports[0], driver)
            }
        }
        return null
    }
    fun getLineString(array: ByteArray, length: Int): String {
        val result = StringBuilder()
        val line = ByteArray(8)
        var lineIndex = 0
        for (i in 0 until 0 + length) {
            if (lineIndex == line.size) {
                for (j in line.indices) {
                    if (line[j] > ' '.code.toByte() && line[j] < '~'.code.toByte()) {
                        result.append(String(line, j, 1))
                    } else {
                        result.append(" ")
                    }
                }
                lineIndex = 0
            }
            val b = array[i]
            line[lineIndex++] = b
        }
        for (i in 0 until lineIndex) {
            if (line[i] > ' '.code.toByte() && line[i] < '~'.code.toByte()) {
                result.append(String(line, i, 1))
            } else {
                result.append(" ")
            }
        }
        return result.toString()
    }

    private data class USBItem(
        val device: UsbDevice,
        val port: UsbSerialPort,
        val driver: UsbSerialDriver
    )
}