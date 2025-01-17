// src/main/kotlin/config/AppConfig.kt
package config

import io.github.cdimascio.dotenv.dotenv

object AppConfig {
    private val dotenv = dotenv {
        ignoreIfMissing = true  // Won't throw error if .env file is missing
    }

    val firebaseWebApiKey: String by lazy {
        dotenv["FIREBASE_WEB_API_KEY"] ?: throw IllegalStateException("FIREBASE_WEB_API_KEY not found in .env file")
    }

    // Add other configuration values here as needed
}