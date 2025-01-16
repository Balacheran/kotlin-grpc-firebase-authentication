package firebase_admin_sdk

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.DocumentReference
import com.google.cloud.firestore.Firestore
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserRecord
import com.google.firebase.cloud.FirestoreClient
import firebase_admin_sdk.interfaces.FirebaseUserInterface
import firebase_admin_sdk.listener.Failure
import firebase_admin_sdk.listener.Result
import firebase_admin_sdk.listener.Success
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.IOException
import java.util.*
import kotlin.system.exitProcess

abstract class FirebaseAdminSdk : FirebaseUserInterface {

    var db: Firestore? = null
    // Add this method to your FirebaseAdminSdk class
    private fun initializeFirestoreCollections() {
    try {
        // Create users collection if it doesn't exist
        db?.collection("users")?.document("dummy")?.set(mapOf("init" to true))?.get()
        db?.collection("users")?.document("dummy")?.delete()?.get()
        println("Firestore collections initialized successfully")
    } catch (e: Exception) {
        println("Error initializing Firestore collections: ${e.message}")
    }
}


    init {
        try {
            val serviceAccount = FileInputStream("kotlin-grpc-firebase-adminsdk-ideda-8f9862ba21.json")
            println("Service account file found")
            
            val options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .setProjectId("kotlin-grpc") // Your project ID
                .setDatabaseUrl("https://kotlin-grpc.firebaseio.com") // Your database URL
                .build()

            if (FirebaseApp.getApps().isEmpty()) {
                val firebaseApp = FirebaseApp.initializeApp(options)
                println("Firebase initialized successfully")
                db = FirestoreClient.getFirestore(firebaseApp)
                println("Firestore initialized successfully")
                initializeFirestoreCollections() // Add this line
            } else {
                println("Firebase already initialized")
                db = FirestoreClient.getFirestore(FirebaseApp.getInstance())
            }
        } catch (e: Exception) {
        println("Firebase initialization error: ${e.message}")
        e.printStackTrace()
        exitProcess(1)
        }
    }

    override suspend fun listAllUsers(): Result<List<UserRecord>> {
        return withContext(Dispatchers.IO) {
            try {
                val userList: MutableList<UserRecord> = mutableListOf()
                var page = FirebaseAuth.getInstance().listUsers(null)
                while (page != null) {
                    for (user in page.values) {
                        userList.add(user)
                    }
                    page = page.nextPage
                }
                if (userList.size != 0) {
                    Success(msg = "Successful", data = userList)
                } else {
                    Failure(msg = "Error occurred")
                }
            } catch (e: Exception) {
                println(e.localizedMessage)
                Failure(msg = "Error occurred")
            }
        }
    }

    override suspend fun registerNewUserWithEmailPassword(userEmail: String, password: String): Result<String?> {
        return withContext(Dispatchers.IO) {
            try {
                val request = UserRecord.CreateRequest()
                    .setEmail(userEmail)
                    .setPassword(password)
                val result = FirebaseAuth.getInstance().createUser(request)
                if (result != null) {
                    Success(
                        msg = "Successful", data = result.uid
                    )
                } else {
                    Failure(msg = "Error occurred")
                }

            } catch (e: Exception) {
                println(e.localizedMessage)
                Failure("Error occurred")
            }
        }
    }

    override suspend fun getUserById(uId: String): Result<UserRecord?> {
        return withContext(Dispatchers.IO) {
            try {
                val userRecord = FirebaseAuth.getInstance().getUser(uId)
                if (userRecord != null) {
                    Success(msg = "Successful", data = userRecord)
                } else {
                    Failure(msg = "Error occurred")
                }
            } catch (e: Exception) {
                println(e.localizedMessage)
                Failure("Error occurred")
            }
        }
    }

    override suspend fun getUserByEmail(userEmail: String): Result<UserRecord?> {
        return withContext(Dispatchers.IO) {
            try {
                val userRecord = FirebaseAuth.getInstance().getUserByEmail(userEmail)
                if (userRecord != null) {
                    Success(msg = "Successful", data = userRecord)
                } else {
                    Failure(msg = "Error occurred")
                }
            } catch (e: Exception) {
                println(e.localizedMessage)
                Failure("Error occurred")
            }
        }
    }

    override suspend fun deleteUserById(uId: String) {
        return withContext(Dispatchers.IO) {
            FirebaseAuth.getInstance().deleteUser(uId)
        }
    }

    override suspend fun setUserCustomClaims(uId: String): Result<String?> {
        return withContext(Dispatchers.IO) {
            try {
                val claims: MutableMap<String, Any> = HashMap()
                claims["admin"] = true
                FirebaseAuth.getInstance().setCustomUserClaims(uId, claims)
                return@withContext Success(msg = "Successful", data = "")
            } catch (e: Exception) {
                println(e.localizedMessage)
                Failure(msg = "Error occurred")
            }
        }
    }

    override suspend fun isUserAdmin(uId: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val user = FirebaseAuth.getInstance().getUser(uId)
                val currentClaims: MutableMap<String, Any> = user.customClaims
                if (currentClaims["admin"] == true) {
                    Success(msg = "Successful", data = true)
                } else {
                    Failure(msg = "Error occurred")
                }
            } catch (e: Exception) {
                println(e.localizedMessage)
                Failure(msg = "Error occurred")
            }
        }
    }

    override suspend fun createCustomToken(uId: String): Result<String?> {
        return withContext(Dispatchers.IO) {
            try {
                val customToken = FirebaseAuth.getInstance().createCustomToken(uId)
                if (customToken != null) {
                    return@withContext Success(msg = "Successful", data = customToken)
                } else {
                    return@withContext Failure(msg = "Error occurred")
                }
            } catch (e: Exception) {
                println(e.localizedMessage)
                Failure(msg = "Error occurred")
            }
        }
    }

    override suspend fun verifyIdToken(customToken: String): Result<String?> {
        return withContext(Dispatchers.IO) {
            try {
                val decodedToken = FirebaseAuth.getInstance().verifyIdToken(customToken)
                val uId = decodedToken.uid
                if (uId != null) {
                    return@withContext Success(msg = "Successful", data = uId)
                } else {
                    return@withContext Failure(msg = "Error occurred")
                }
            } catch (e: Exception) {
                println(e.localizedMessage)
                Failure(msg = "Error occurred")
            }
        }
    }

    suspend fun writeToFireStore(
        dr: DocumentReference,
        data: MutableMap<String, Any?>?
    ) {
        return withContext(Dispatchers.IO) {
            try {
                val future = data?.let {
                    dr.set(it)
                }
                println(future?.get()?.updateTime)
            } catch (e: Exception) {
                println(e.localizedMessage)
                println("Could not write to document")
            }
        }
    }

    suspend fun updateToFirestore(
        dr: DocumentReference,
        data: MutableMap<String, Any?>?
    ) {
        return withContext(Dispatchers.IO) {
            try {
                val future = data?.let {
                    dr.update(it)
                }
                println(future?.get()?.updateTime)
            } catch (e: Exception) {
                println("Could not write to document")
            }
        }
    }
}


