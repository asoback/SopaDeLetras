package com.example.sopadeletras2

import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import okhttp3.OkHttpClient
import okhttp3.Request
import android.os.AsyncTask
import android.os.Bundle

class PrivacyPolicyActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_privacy_policy)

        webView = findViewById(R.id.webView)

        // Fetch the HTML content from the remote URL
        FetchHtmlTask().execute("https://raw.githubusercontent.com/asoback/SopadeLetras/main/PrivacyPolicy.html")
    }

    private inner class FetchHtmlTask : AsyncTask<String, Void, String>() {

        override fun doInBackground(vararg urls: String): String {
            val client = OkHttpClient()
            val request = Request.Builder().url(urls[0]).build()
            client.newCall(request).execute().use { response ->
                return response.body?.string() ?: ""
            }
        }

        override fun onPostExecute(htmlContent: String) {
            // Load HTML content into WebView
            webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
        }
    }
}