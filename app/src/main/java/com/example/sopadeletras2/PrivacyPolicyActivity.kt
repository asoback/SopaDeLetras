package com.example.sopadeletras2

import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.*
import android.os.Bundle
import android.util.Log
import android.widget.Toast

class PrivacyPolicyActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_privacy_policy)

        webView = findViewById(R.id.webView)

        // Fetch the HTML content from the remote URL
        fetchHtmlContent("https://raw.githubusercontent.com/asoback/SopadeLetras/main/PrivacyPolicy.html")
    }

    private fun fetchHtmlContent(url: String) {
        print("Hello from fetch HTML Content")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                print("Trying request")
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val htmlContent = response.body?.string()
                print(htmlContent)

                withContext(Dispatchers.Main) {
                    if (htmlContent != null) {
                        // Load HTML content into WebView
                        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                    } else {
                        Toast.makeText(this@PrivacyPolicyActivity, "Failed to load Privacy Policy", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("PrivacyPolicyActivity", "Error fetching HTML", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PrivacyPolicyActivity, "Error loading Privacy Policy", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}