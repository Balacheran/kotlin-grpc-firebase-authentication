// src/main/kotlin/firebase_admin_sdk/FirebaseInitializer.kt
package firebase_admin_sdk

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.cloud.FirestoreClient
import java.io.FileInputStream

object FirebaseInitializer {
    @Volatile
    private var initialized = false

    fun initialize() {
        if (!initialized) {
            synchronized(this) {
                if (!initialized) {
                    try {
                        val serviceAccount = FileInputStream("kotlin-grpc-firebase-adminsdk-ideda-8f9862ba21.json")
                        println("Service account file found")
                        
                        val options = FirebaseOptions.builder()
                            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                            .setProjectId("kotlin-grpc")
                            .setDatabaseUrl("https://kotlin-grpc.firebaseio.com")
                            .build()

                        if (FirebaseApp.getApps().isEmpty()) {
                            FirebaseApp.initializeApp(options)
                            println("Firebase initialized successfully")
                        }
                        
                        initialized = true
                    } catch (e: Exception) {
                        println("Firebase initialization error: ${e.message}")
                        throw e
                    }
                }
            }
        }
    }
}