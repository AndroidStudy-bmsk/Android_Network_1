package org.bmsk.android_network_1

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket

fun main() {
    Thread {
        val server = ServerSocket(PORT)

        while (true) {
            val socket = server.accept()

            // socket.getInputStream() // 클라이언트로부터 들어오는 스트림 == 클라이언트의 socket.outputStream
            // socket.getOutputStream() // 클라이언트에게 데이터를 내보내는 스트림 == 클라이언트의 socket.inputStream
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val printer = PrintWriter(socket.getOutputStream())

            var input: String? = "-1"
            while (input != null && input != "") {
                input = reader.readLine()
            }

            println("READ DATA $input")

            printer.println("HTTP/1.1 200 OK")
            printer.println("Content-Type: text/html\r\n")
            printer.println("<h1>Hello World!</h1>")
            printer.println("\r\n")
            printer.flush()
            printer.close()

            reader.close()
        }
    }.start()
}