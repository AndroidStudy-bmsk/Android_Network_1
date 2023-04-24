package org.bmsk.android_network_1

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ConnectException
import java.net.Socket
import java.net.SocketException
import java.net.SocketTimeoutException

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 코루틴을 사용하여 소켓 연결을 시작한다.
        CoroutineScope(Dispatchers.Main).launch {
            val result = connectToServer("10.0.2.2")
            when (result) {
                is SocketResult.Success -> {

                }

                is SocketResult.Error -> {
                    Log.e("Error", result.error)
                }
            }
        }
    }

    private suspend fun connectToServer(host: String) = withContext(Dispatchers.IO) {

        return@withContext try {
            val socket = Socket(host, PORT)

            // 소켓의 입력 스트림과 출력 스트림을 가져온다.
            val inputStream = socket.getInputStream()
            val outputStream = socket.getOutputStream()

            // 스트림을 통해 데이터를 읽고 쓸 수 있는 객체를 생성한다.
            val reader = BufferedReader(InputStreamReader(inputStream))
            val writer = PrintWriter(outputStream, true)

            // 서버에 메시지를 보낸다.
            val messageToSend = "GET / HTTP/1.1\r\n" +
                    "Host: 127.0.0.1:$PORT\r\n" +
                    "User-Agent: android\r\n"
            writer.println(messageToSend)
            writer.flush()

            // 서버로부터 메시지를 받는다.
            val receivedMessages = StringBuilder()
            var receivedMessage: String? = "-1"
            while (receivedMessage != null) {
                receivedMessage = reader.readLine()
                receivedMessages.append(receivedMessage)
                Log.e("Client", "Received message: $receivedMessage")
            }

            // 리소스를 정리하고 소켓을 닫는다.
            reader.close()
            writer.close()
            socket.close()
            
            SocketResult.Success(receivedMessages.toString())
        } catch (e: ConnectException) {
            SocketResult.Error("서버에 연결할 수 없습니다: ${e.localizedMessage}")
        } catch (e: SocketTimeoutException) {
            SocketResult.Error("서버 연결 시간이 초과되었습니다: ${e.localizedMessage}")
        } catch (e: SocketException) {
            SocketResult.Error("소켓 오류가 발생했습니다: ${e.localizedMessage}")
        } catch (e: Exception) {
            SocketResult.Error("알 수 없는 오류가 발생했습니다: ${e.localizedMessage}")
        }
    }
}

sealed class SocketResult {
    data class Success(val message: String) : SocketResult()
    data class Error(val error: String) : SocketResult()
}