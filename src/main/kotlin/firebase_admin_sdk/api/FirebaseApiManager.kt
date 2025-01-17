package firebase_admin_sdk.api

import com.google.cloud.firestore.CollectionReference
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserRecord
import firebase_admin_sdk.FirebaseAdminSdk
import firebase_admin_sdk.listener.Failure
import firebase_admin_sdk.listener.Result
import firebase_admin_sdk.listener.Success
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import service_utils.ErrorUtils
import service_utils.Utils
import user_registration.User

object FirebaseApiManager : FirebaseAdminSdk() {
    private val userCollection: CollectionReference? = db?.collection(BaseUrl.USER)
 

    override suspend fun registerNewUserWithEmailPassword(
        userEmail: String,
        password: String
    ): Result<String?> {
        return withContext(Dispatchers.IO) {
            try {
                val request = UserRecord.CreateRequest()
                    .setEmail(userEmail)
                    .setPassword(password)
                val userRecord = FirebaseAuth.getInstance().createUser(request)
                Success(msg = "User created successfully", data = userRecord.uid)
            } catch (e: Exception) {
                ErrorUtils.logError("Firebase", "Error creating user", e)
                Failure(msg = "Error creating user: ${e.message}")
            }
        }
    }

    suspend fun storeUserData(user: User): Result<String?> {
        return withContext(Dispatchers.IO) {
            try {
                val dr = userCollection?.document(user.id)
                    ?: return@withContext Failure(msg = "Failed to get document reference")

                val userMap = Utils.getHashMap(user)
                dr.set(userMap).get()

                Success(msg = "User data stored successfully", data = user.id)
            } catch (e: Exception) {
                ErrorUtils.logError("Firebase", "Error storing user data", e)
                Failure(msg = "Error storing user data: ${e.message}")
            }
        }
    }

    suspend fun getUserFromId(userId: String): Result<User?> {
        return withContext(Dispatchers.IO) {
            try {
                println("Fetching user with ID: $userId")
                val documentRef = userCollection?.document(userId)
                    ?: return@withContext Failure(msg = "Collection not initialized")
    
                val documentSnapshot = documentRef.get().get()
                if (!documentSnapshot.exists()) {
                    return@withContext Failure(msg = "User not found for ID: $userId")
                }
    
                val userData = documentSnapshot.data
                if (userData == null) {
                    return@withContext Failure(msg = "No data found for user ID: $userId")
                }
    
                val user = Utils.getFromDocumentSnapshot(userData)
                println("Successfully retrieved user data: ${user.firstName} ${user.lastName}")
                Success(msg = "User retrieved successfully", data = user)
            } catch (e: Exception) {
                ErrorUtils.logError("Firebase", "Error retrieving user data", e)
                Failure(msg = "Error retrieving user data: ${e.message}")
            }
        }
    }

    object BaseUrl {
        const val USER = "users"
    }
}