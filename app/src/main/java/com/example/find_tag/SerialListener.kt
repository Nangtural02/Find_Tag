package com.example.find_tag
/**
 * You Can Choose SerialListener (CustomSerialListener/JSONSerialListener),
 * or edit Custom SerialListener to parse a block with your custom format.
 */
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import com.example.find_tag.SerialManager.getLineString
import com.hoho.android.usbserial.util.SerialInputOutputManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
// Choose One Serial Listener
val SerialListener = CustomJSONSerialListener
//

// For Custom Data Format ( {~~~~~} )
object CustomSerialListener: SerialInputOutputManager.Listener{
    var blockHandler:(String)->Unit = {_:String -> }
    val _blockString = MutableStateFlow("Not Connected Yet")
    private var _buffer = mutableStateOf("")
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())


    override fun onNewData(data: ByteArray?) {
        scope.launch{
            if(data != null && data.isNotEmpty()) {
                val result: String = getLineString(data, data.size)
                val countEnd = result.count{ it == '}' }
                if(countEnd >= 2){
                    Log.e("SerialListener", "duplicate '}'")
                    _buffer.value = ""
                }else if(countEnd == 1){
                    val remain : String = result.substringAfter("}")
                    _buffer.value += result.substringBefore("}") + "}"
                    _buffer.value = _buffer.value.replace(" ", "")
                    Log.d("blockHandle", _buffer.value)
                    blockHandler(_buffer.value)
                    _blockString.value = _buffer.value
                    _buffer.value = remain
                }else if (_buffer.value.contains("{") && result.contains("{")){
                    Log.e("SerialListener", "duplicate '{'")
                    _buffer.value = "{" + result.substringAfter("{")
                }else{
                    _buffer.value += result
                }
            }
        }
    }

    override fun onRunError(e: Exception) {
        scope.launch() {
            Log.e("SerialViewModel", "Disconnected: ${e.message}")
            SerialManager.disconnectSerialDevice()
        }
    }
}

object CustomJSONSerialListener: SerialInputOutputManager.Listener{
    //for Customized JSON format which Start "{\"B" and end "}]}"
    var blockHandler:(String)->Unit = {_:String -> }
    val _blockString = MutableStateFlow("")
    private var _buffer = mutableStateOf("")
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onNewData(data: ByteArray?) { // called when get data
        scope.launch {
            if (data != null && data.isNotEmpty()) {
                val result: String = getLineString(data, data.size)
                if (_buffer.value.isEmpty()) {
                    _buffer.value += result
                } else {
                    if (result.contains("{\"B")) { // 메시지를 받다말고 새로운 메시지가 들어옴
                        _buffer.value = result
                    } else if ((_buffer.value + result).contains("}]}")) { // 메시지의 끝
                        _buffer.value += result
                        Log.d("SerialListener","block: ${_buffer.value}")
                        blockHandler(_buffer.value)
                        _blockString.value = _buffer.value
                        _buffer.value = ""
                    } else {
                        _buffer.value += result
                    }
                }
            }
        }
    }

    override fun onRunError(e: Exception) {
        scope.launch() {
            Log.e("SerialViewModel", "Disconnected: ${e.message}")
            SerialManager.disconnectSerialDevice()
        }
    }
}

object UniversalJSONSerialListener: SerialInputOutputManager.Listener{
    var blockHandler:(String)->Unit = {_:String -> }
    val _blockString = MutableStateFlow("")
    private var _buffer = mutableStateOf("")
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onNewData(data: ByteArray?) {
        scope.launch {
            if (data != null && data.isNotEmpty()) {
                // 1) 들어온 바이트 배열 → 문자열 변환
                val chunk = getLineString(data, data.size)

                // 2) (옵션) 공백 제거, 줄바꿈 제거 등
                //   원한다면 "\\s+".toRegex()로 모든 공백문자 제거 가능
                val sanitized = chunk.replace(" ", "")

                // 3) 버퍼에 추가
                _buffer.value += sanitized

                // 4) 중괄호 카운팅으로 여러 JSON 오브젝트 파싱
                parseJsonObjectsFromBuffer()
            }
        }
    }

    /**
     * _buffer.value 안에 여러 JSON 오브젝트가 있을 수 있으므로,
     * 중괄호 개수를 세면서 완성된 { ... } 블록을 찾아내 blockHandler에 넘기는 로직
     */
    private fun parseJsonObjectsFromBuffer() {
        var braceCount = 0
        var startIndex = -1
        var i = 0

        // 문자열을 탐색하며 {, }를 만나면 카운팅
        while (i < _buffer.value.length) {
            val c = _buffer.value[i]

            if (c == '{') {
                // { 만나면
                braceCount++
                if (braceCount == 1) {
                    // JSON 블록이 시작되는 지점
                    startIndex = i
                }
            } else if (c == '}') {
                // } 만나면
                braceCount--
                // 블록이 완성되는 순간
                if (braceCount == 0 && startIndex >= 0) {
                    // startIndex ~ i 까지가 하나의 JSON 블록
                    val jsonBlock = _buffer.value.substring(startIndex, i + 1)

                    // 1) blockHandler에 전달
                    blockHandler(jsonBlock)
                    Log.d("jsonBlockHandle", "Complete JSON: $jsonBlock")

                    // 2) 버퍼에서 해당 블록 제거
                    //    startIndex 이전 부분 + i+1 이후 부분을 이어붙이기
                    _buffer.value =
                        _buffer.value.removeRange(startIndex, i + 1)

                    // 제거했으니, 인덱스와 길이를 다시 조정
                    // removeRange 후, 문자열이 짧아졌으니 i를 startIndex로 되돌림
                    i = startIndex
                    startIndex = -1
                }
            }

            i++
            // braceCount < 0 가 되면? -> 데이터 꼬임, 에러상황...
            // 필요시 오류 처리 가능
        }
    }

    override fun onRunError(e: Exception) {
        scope.launch() {
            Log.e("SerialViewModel", "Disconnected: ${e.message}")
            SerialManager.disconnectSerialDevice()
        }
    }
}

