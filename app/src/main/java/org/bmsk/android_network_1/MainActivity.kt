package org.bmsk.android_network_1

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ConnectException
import java.net.Socket
import java.net.SocketException
import java.net.SocketTimeoutException
import kotlin.coroutines.CoroutineContext

class MainActivity : AppCompatActivity() {
    private val okHttpClient = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val editText = findViewById<EditText>(R.id.serverHostEditText)
        val confirmButton = findViewById<Button>(R.id.confirmButton)
        val informationTextView = findViewById<TextView>(R.id.informationTextView)
        var serverHost = ""

        editText.addTextChangedListener {
            serverHost = it.toString()
        }

        confirmButton.setOnClickListener {
            val request: Request = Request.Builder()
                .url("http://$serverHost:$PORT")
                .build()

            val callback = object : Callback {
                // 요청 자체가 실패거나 통신 과정에서 오류가 났을 때 발생
                override fun onFailure(call: Call, e: IOException) {

                    CoroutineScope(Dispatchers.Main).launch {
                        Toast.makeText(this@MainActivity, "수신에 실패했습니다.", Toast.LENGTH_SHORT).show()
                        Log.e("Client", e.toString())
                    }
                }

                override fun onResponse(call: Call, response: Response) {

                    // 응답은 성공했지만, 데이터는 실패로 내려질 수 있다
                    if (response.isSuccessful) {
                        val responseMessage = response.body?.string()

                        CoroutineScope(Dispatchers.Main).launch {
                            informationTextView.isVisible = true
                            informationTextView.text = responseMessage

                            editText.isVisible = false
                            confirmButton.isVisible = false
                        }
                    } else {
                        CoroutineScope(Dispatchers.Main).launch {
                            Toast.makeText(this@MainActivity, "수신에 실패했습니다.", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                }
            }

            okHttpClient.newCall(request).enqueue(callback)
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