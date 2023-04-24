# Android_Network_1

소켓, OkHttp 기초부터 다시 해보기


Android에서 소켓 통신을 구현하려면, `java.net.Socket`클래스를 사용해야 한다.

이 클래스는 TCP 소켓을 생성하고 연결할 수 있는 기능을 제공한다.


1. 먼저 AndroidManifest.xml 파일에 인터넷 권한을 추가한다.

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

2. 코루틴을 사용하여 소켓 연결을 관리하고자 한다면, build.gradle(Module) 파일에 코루틴 라이브러리를 추가해야 한다.

```groovy
dependencies {
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2'
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.5.2'
}
```

3. 소켓 연결 및 데이터 전송을 처리하는 함수를 작성한다.

```kotlin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket

suspend fun connectToServer(host: String, port: Int) = withContext(Dispatchers.IO) {
    try {
        // 소켓 연결
        val socket = Socket(host, port)

        // 소켓의 입력 스트림과 출력 스트림을 가져옵니다.
        val inputStream = socket.getInputStream()
        val outputStream = socket.getOutputStream()

        // 스트림을 통해 데이터를 읽고 쓸 수 있는 객체를 생성합니다.
        val reader = BufferedReader(InputStreamReader(inputStream))
        val writer = PrintWriter(outputStream, true)

        // 서버에 메시지를 보냅니다.
        val messageToSend = "Hello, Server!"
        writer.println(messageToSend)

        // 서버로부터 메시지를 받습니다.
        val receivedMessage = reader.readLine()
        println("Received message: $receivedMessage")

        // 리소스를 정리하고 소켓을 닫습니다.
        reader.close()
        writer.close()
        socket.close()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
```

4. 액티비티에서 위에서 작성한 함수를 호출하여 소켓 연결을 시작한다.
```kotlin
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 코루틴을 사용하여 소켓 연결을 시작합니다.
        CoroutineScope(Dispatchers.Main).launch {
            connectToServer("your.server.ip", yourPort)
        }
    }
}
```

간단한 코드를 통해 소켓 통신을 구현하는 방법을 작성하였다.
- 현재 프로젝트에서는 이 외에 OkHttp를 이용하여 Client 앱을 만드는 것을 볼 수 있을 것이다.

# 소켓 통신을 구현하면서 고려할 사항

1. 연결 상태 및 에러 처리: 소켓 연결 과정에서 발생할 수 있는 다양한 예외와 에러를 처리해야 한다. 예를 들어, 연결 시간 초과, 끊어진 연결 등의 상황을 처리할 수 있는 방법을 구현해야 한다.
2. 백그라운드 실행: 앱이 백그라운드에 있을 때 소켓 통신을 유지하려면, 서비스를 사용하여 백그라운드에서 실행되도록 구현해야 한다. 서비스를 사용하면 앱이 종료되거나 백그라운드로 전환되더라도 소켓 통신을 유지할 수 있다.
3. UI 업데이트: 소켓 통신을 통해 수신한 데이터를 사용하여 UI를 업데이트해야 할 수 있다. 이 경우, 메인 스레드에서 UI를 업데이트하도록 코드를 작성해야 한다. 코루틴을 사용한다면, `Dispatchers.Main`을 통해 메인 스레드에서 UI를 업데이트할 수 있다.
4. 데이터 처리: 수신한 데이터를 적절하게 처리하고, 필요한 경우 데이터 형식을 변환해야 한다. 예를 들어, JSON 형식의 데이터를 받았다면, 해당 데이터를 파싱하여 객체로 변환해야 한다.
5. 소켓 종료: 앱이 종료되거나 통신이 더 이상 필요하지 않은 경우, 소켓을 종료해야 한다. 소켓을 닫지 않으면 자원 누수가 발생할 수 있기 때문이다. 반드시 필요한 시점에 소켓을 닫도록 하자.

## 추가: 연결 상태 및 에러 처리

연결 상태 및 에러 처리에 대해 더 자세히 알아볼 필요가 있다고 생각한다.

- 예를 들어 다음과 같이 작성할 수 있다.

```kotlin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ConnectException
import java.net.Socket
import java.net.SocketException
import java.net.SocketTimeoutException

sealed class SocketResult {
    data class Success(val message: String) : SocketResult()
    data class Error(val error: String) : SocketResult()
}

suspend fun connectToServerWithExceptionHandling(host: String, port: Int): SocketResult = withContext(Dispatchers.IO) {
    return@withContext try {
        val socket = Socket()
        socket.connect(InetSocketAddress(host, port), 10000) // 연결 시간 초과 설정: 10초

        val inputStream = socket.getInputStream()
        val outputStream = socket.getOutputStream()

        val reader = BufferedReader(InputStreamReader(inputStream))
        val writer = PrintWriter(outputStream, true)

        val messageToSend = "Hello, Server!"
        writer.println(messageToSend)

        val receivedMessage = reader.readLine()
        println("Received message: $receivedMessage")

        reader.close()
        writer.close()
        socket.close()

        SocketResult.Success(receivedMessage)
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

// 예외 처리를 위한 함수를 호출하여 소켓 연결 시작
CoroutineScope(Dispatchers.Main).launch {
    val result = connectToServerWithExceptionHandling("your.server.ip", yourPort)
    when (result) {
        is SocketResult.Success -> {
            // 성공적인 연결 후 UI 업데이트
            updateUI(result.message)
        }
        is SocketResult.Error -> {
            // 에러 발생 시 UI 업데이트
            showError(result.error)
        }
    }
}
```

1. `ConnectException`: 서버에 연결할 수 없는 경우 이 예외가 발생하도록 하였다. 이 경우, 사용자에게 서버에 연결할 수 없음을 알려주어야 한다.
2. `SocketTimeoutException`: 서버 연결 시간이 초과된 경우 이 예외가 발생한다. 이 경우, 사용자에게 연결 시간이 초과되었음을 알려주어야 한다.
3. `SocketException`: 소켓 관련 오류가 발생한 경우 이 예외가 발생하도록 하였다. 이 경우엔, 사용자에게 소켓 오류가 발생했음을 알려주어야 한다.
4. `Exception`: 기타 예외 및 에러를 처리하기 위해 기본 `Exception`을 사용하였다. 이 경우, 사용자에게 알 수 없는 오류가 발생했음을 알려주어야 한다.

## OkHttp - WebSocket

OkHttp를 사용하여 소켓 통신을 구현하려면, OkHttp의 WebSocket 기능을 사용할 수 있다.
WebSocket은 양방향 통신이 가능한 프로토콜로, 클라이언트와 서버 간의 실시간 데이터 교환에 적합하다.

```groovy
dependencies {
    implementation 'com.squareup.okhttp3:okhttp:4.9.2'
    implementation 'com.squareup.okhttp3:logging-interceptor:4.9.2'
}
```

### WebSocket 연결 구현

```kotlin
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class MainActivity : AppCompatActivity() {

    private val client = OkHttpClient()
    private lateinit var webSocket: WebSocket

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // WebSocket 연결을 시작합니다.
        startWebSocketConnection()
    }

    private fun startWebSocketConnection() {
        val request = Request.Builder()
            .url("wss://your.websocket.server")
            .build()

        val webSocketListener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                // 연결이 성공적으로 열린 경우
                webSocket.send("Hello, Server!")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                // 서버로부터 메시지를 받은 경우
                runOnUiThread {
                    // UI 업데이트
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
                // 연결 실패 또는 오류 발생 시
                t.printStackTrace()
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                // 연결이 닫히는 경우
            }
        }

        webSocket = client.newWebSocket(request, webSocketListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        // WebSocket 연결 종료
        webSocket.close(NORMAL_CLOSURE_STATUS, "Activity 종료")
    }

    companion object {
        private const val NORMAL_CLOSURE_STATUS = 1000
    }
}
```
