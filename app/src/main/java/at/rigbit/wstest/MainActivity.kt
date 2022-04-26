package at.rigbit.wstest

import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okio.ByteString
import java.util.*
import kotlin.concurrent.schedule

class MainActivity : AppCompatActivity() {
    private lateinit var txtResult: TextView;
    private lateinit var edtInput: EditText;
    private lateinit var sv: ScrollView

    private var webSocket: WebSocket? = null;

    companion object {
        const val TAG = "WebSocket"
        const val CLOSE_STATUS = 1000;
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        txtResult = findViewById(R.id.txtResult);
        edtInput = findViewById(R.id.edtInput);
        sv = findViewById(R.id.sv)

        findViewById<Button>(R.id.btnSend).setOnClickListener { send() }
        findViewById<Button>(R.id.btnClose).setOnClickListener { closeConnection() }

        start()
    }

    override fun onStop() {
        super.onStop()
        closeConnection()
    }

    private fun printScreen(message: String) {
        runOnUiThread {
            txtResult.text = "${txtResult.text}\n${message}"
            sv.scrollTo(0, sv.bottom)
        }
    }

    private fun start() {
        val client = OkHttpClient()
        val request: Request = Request.Builder().url("ws://192.168.11.120:8080").build()
        val listener = EchoWebSocketListener()
        webSocket = client.newWebSocket(request, listener)
        client.dispatcher().executorService().shutdown()
    }

    private fun send() {
        if (webSocket == null) {
            Log.w(TAG, "try to start websocket")
            start()
        }
        if (edtInput.text.toString().trim().isNotBlank()) {
            Log.d(TAG, "send ${edtInput.text}")
            webSocket?.send(edtInput.text.toString())
            edtInput.text.clear()
        }
    }

    private fun closeConnection() {
        webSocket?.close(CLOSE_STATUS, "closed by user")
    }

    inner class EchoWebSocketListener : WebSocketListener() {
        var retryCount = 0
        override fun onOpen(webSocket: WebSocket, response: Response) {
            webSocket.send(
                "Hello from ${
                    Settings.Secure.getString(
                        applicationContext.contentResolver,
                        Settings.Secure.ANDROID_ID
                    )
                }"
            );
        }

        override fun onMessage(webSocket: WebSocket, message: String) {
            printScreen("Receive Message: $message")
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            printScreen("Receive Message: ${bytes.hex()}")
        }

        override fun onClosing(ws: WebSocket, code: Int, reason: String) {
            ws.close(CLOSE_STATUS, null);
            printScreen("Closing Socket: ${code}/${reason}")
            webSocket = null
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            printScreen("Error: ${t.message}")
            if (retryCount++ < 3) {
                Timer().schedule(1000L * retryCount) {
                    start()
                }
            } else {
                printScreen("Could not (re)-Connect to WebSocket")
            }
        }
    }
}