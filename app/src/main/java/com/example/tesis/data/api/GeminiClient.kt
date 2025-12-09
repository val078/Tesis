package com.example.tesis.data.api

import com.google.ai.client.generativeai.GenerativeModel
import com.example.tesis.BuildConfig

object GeminiClient {

    val model = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY
    )
}