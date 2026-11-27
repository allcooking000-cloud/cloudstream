package com.lagradost.cloudstream3.utils

import android.app.Activity
import android.net.Uri
import android.widget.Toast
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import okhttp3.OkHttpClient
import okhttp3.Request
import androidx.activity.result.ActivityResultLauncher

object RemoteCastHelper {

    fun launchScanner(launcher: ActivityResultLauncher<ScanOptions>) {
        val options = ScanOptions()
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
        options.setPrompt("Scan TV QR Code")
        options.setBeepEnabled(false)
        options.setOrientationLocked(false)
        launcher.launch(options)
    }

    // এখানে ৪টি প্যারামিটার (headers সহ) 
    fun handleScanResult(result: String?, currentVideoUrl: String?, headers: Map<String, String>?, context: Activity) {
        if (result == null) {
            Toast.makeText(context, "Scan Cancelled", Toast.LENGTH_SHORT).show()
            return
        }

        if (currentVideoUrl.isNullOrBlank()) {
            Toast.makeText(context, "No video is playing right now", Toast.LENGTH_SHORT).show()
            return
        }

        val encodedUrl = Uri.encode(currentVideoUrl)
        
        // হেডারগুলোকে স্ট্রিং এ কনভার্ট করে এনকোড করা
        val headersString = headers?.entries?.joinToString("|") { "${it.key}:${it.value}" } ?: ""
        val encodedHeaders = Uri.encode(headersString)

        val finalUrl = "$result?url=$encodedUrl&headers=$encodedHeaders"

        sendRequestToTv(finalUrl, context)
    }

    private fun sendRequestToTv(finalUrl: String, context: Activity) {
        val client = OkHttpClient()
        val request = Request.Builder().url(finalUrl).build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                context.runOnUiThread {
                    Toast.makeText(context, "Failed to connect to TV: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                context.runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(context, "Sent to TV successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "TV Error: ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
}
