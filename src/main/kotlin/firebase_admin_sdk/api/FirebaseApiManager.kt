package firebase_admin_sdk.api

import com.google.cloud.firestore.CollectionReference
import firebase_admin_sdk.FirebaseAdminSdk
import firebase_admin_sdk.listener.Failure
import firebase_admin_sdk.listener.Result
import firebase_admin_sdk.listener.Success
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import service_utils.ErrorUtils
import service_utils.Utils.getFromDocumentSnapshot
import service_utils.Utils.getHashMap
import user_registration.User

object FirebaseApiManager : FirebaseAdminSdk() {
    private val userCollection: CollectionReference? = db?.collection(BaseUrl.USER)

    suspend fun storeUserData(user: User): Result<String?> {
        return withContext(Dispatchers.IO) {
            try {
                val dr = userCollection?.document(user.id)
                    ?: return@withContext Failure(msg = "Failed to get document reference")

                val userMap = getHashMap(user)
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
                val documentRef = userCollection?.document(userId)
                    ?: return@withContext Failure(msg = "Collection not initialized")

                val documentSnapshot = documentRef.get().get()
                if (!documentSnapshot.exists()) {
                    return@withContext Failure(msg = "User not found")
                }

                val userData = documentSnapshot.data
                val user = getFromDocumentSnapshot(userData)
                
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