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

    // ১. স্ক্যানার ওপেন করার ফাংশন
    fun launchScanner(launcher: ActivityResultLauncher<ScanOptions>) {
        val options = ScanOptions()
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
        options.setPrompt("Scan TV QR Code")
        options.setBeepEnabled(false)
        options.setOrientationLocked(false)
        launcher.launch(options)
    }

    // ২. স্ক্যান রেজাল্ট প্রসেস করা (টিভির আইপি নেওয়া এবং ভিডিও লিংক পাঠানো)
    fun handleScanResult(result: String?, currentVideoUrl: String?, context: Activity) {
        if (result == null) {
            Toast.makeText(context, "Scan Cancelled", Toast.LENGTH_SHORT).show()
            return
        }

        if (currentVideoUrl.isNullOrBlank()) {
            Toast.makeText(context, "No video is playing right now", Toast.LENGTH_SHORT).show()
            return
        }

        // টিভির আইপি (যেমন: http://192.168.1.5:8080/play) এর সাথে ভিডিও লিংক যুক্ত করা
        val encodedUrl = Uri.encode(currentVideoUrl)
        val finalUrl = "$result?url=$encodedUrl"

        sendRequestToTv(finalUrl, context)
    }

    // ৩. টিভিতে রিকোয়েস্ট পাঠানো
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
