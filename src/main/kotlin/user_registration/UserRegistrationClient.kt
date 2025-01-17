package user_registration

import com.google.firebase.auth.FirebaseAuth
import com.google.protobuf.StringValue
import firebase_admin_sdk.FirebaseInitializer
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.delay
import service_utils.TokenUtils  
import user_registration.UserRegistrationGrpcKt.UserRegistrationCoroutineStub
import java.io.Closeable
import java.util.concurrent.TimeUnit

class UserRegistrationClient(private val channel: ManagedChannel) : Closeable {
    private val userStub: UserRegistrationCoroutineStub by lazy { UserRegistrationCoroutineStub(channel) }
    var registeredUserId: String? = null
        private set

    init {
        try {
            FirebaseInitializer.initialize()
        } catch (e: Exception) {
            println("Failed to initialize Firebase in client: ${e.message}")
        }
    }

    suspend fun registerUser(user: User): Boolean {
        return try {
            val response = userStub.registerUser(user)
            if (response.value) {
                // Store the registered user ID for later use
                registeredUserId = getUserIdByEmail(user.email)
                println("User registered with ID: $registeredUserId")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            println("Registration error: ${e.message}")
            false
        }
    }

    private suspend fun getUserIdByEmail(email: String): String? {
        return try {
            println("Attempting to get user by email: $email")
            val userRecord = FirebaseAuth.getInstance().getUserByEmail(email)
            println("Found user with ID: ${userRecord.uid}")
            userRecord.uid
        } catch (e: Exception) {
            println("Error getting user ID by email: ${e.message}")
            null
        }
    }

    suspend fun getUserInfo(userId: String): User {
        return try {
            val request = StringValue.newBuilder().setValue(userId).build()
            val response = userStub.getUserInfo(request)
            println("Retrieved user info for ID $userId")
            response
        } catch (e: Exception) {
            println("Error getting user info: ${e.message}")
            User.getDefaultInstance()
        }
    }

    suspend fun setUserCustomClaims(userId: StringValue): Boolean {
        userStub.setUserCustomClaim(userId).let {
            return if (it.value) {
                println("User with id: $userId has been authorized as an admin.")
                true
            } else {
                println("Error occurred")
                false
            }
        }
    }

    suspend fun isUserAdmin(userId: StringValue): Boolean {
        userStub.isUserAdmin(userId).let {
            return if (it.value) {
                println("User is Admin")
                true
            } else {
                println("User is not an Admin")
                false
            }
        }
    }

    suspend fun createCustomToken(userId: StringValue): String {
        userStub.createCustomToken(userId).let {
            return if (it.value != "Error occurred") {
                println("Custom Token for User with uId $userId: ${it.value}")
                it.value
            } else {
                println("Error occurred")
                it.value
            }
        }
    }

    suspend fun verifyIdToken(customToken: String): String {
        return try {
            // First exchange custom token for ID token
            println("Exchanging custom token for ID token...")
            val idToken = TokenUtils.exchangeCustomTokenForIdToken(customToken)
            println("Successfully obtained ID token")
    
            // Now verify the ID token
            val request = StringValue.newBuilder().setValue(idToken).build()
            val response = userStub.verifyIdToken(request)
            
            if (response.value != "Error occurred") {
                println("ID Token verified successfully")
                response.value
            } else {
                println("Failed to verify ID token")
                "Error occurred"
            }
        } catch (e: Exception) {
            println("Error during token verification: ${e.message}")
            "Error occurred"
        }
    }


    override fun close() {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
    }

}

suspend fun main() {
    println("Starting client...")
    val port = 50052
    var channel: ManagedChannel? = null
    var userRegistrationClient: UserRegistrationClient? = null
    
    try {
        // Initialize Firebase first
        FirebaseInitializer.initialize()
        
        channel = ManagedChannelBuilder.forAddress("localhost", port)
            .usePlaintext()
            .build()
        
        userRegistrationClient = UserRegistrationClient(channel)

        // Create a test user
        val location = Location.newBuilder()
            .setLatitude(71121212)
            .setLongitude(91122121)
            .build()
            
        val user = User.newBuilder()
            .setFirstName("John")
            .setLastName("Doe")
            .setPassword("testPassword123")
            .setEmail("john.doe@example.com")
            .setAddress("123 Test Street")
            .setMobileNumber(1234567890)
            .setPreferences("No preferences")
            .setLocation(location)
            .build()

        println("\n=== Attempting to register user ===")
        val registrationSuccess = userRegistrationClient.registerUser(user)
        println("Registration result: $registrationSuccess")
        
        if (registrationSuccess) {
            // Add a small delay to allow Firebase to propagate
            delay(2000) // 2 seconds delay
            // Get the Firebase-generated user ID
            val userId = userRegistrationClient.registeredUserId
            if (userId != null) {
                println("\n=== Getting user info for ID: $userId ===")
                val userInfo = userRegistrationClient.getUserInfo(userId)
                
                // Display user info
                println("\nUser Information:")
                println("ID: ${userInfo.id}")
                println("Name: ${userInfo.firstName} ${userInfo.lastName}")
                println("Email: ${userInfo.email}")
                println("Address: ${userInfo.address}")
                println("Mobile: ${userInfo.mobileNumber}")
                println("Location: (${userInfo.location.latitude}, ${userInfo.location.longitude})")

                // Continue with other operations
                val userIdValue = StringValue.newBuilder().setValue(userId).build()
                
                println("\n=== Setting user as admin ===")
                val setAdminResult = userRegistrationClient.setUserCustomClaims(userIdValue)
                println("Set admin result: $setAdminResult")

                println("\n=== Checking admin status ===")
                val isAdmin = userRegistrationClient.isUserAdmin(userIdValue)
                println("Is admin: $isAdmin")

                println("\n=== Creating custom token ===")
                val customToken = userRegistrationClient.createCustomToken(userIdValue)
                println("Custom token: $customToken")

                // Verify token
                if (customToken != "Error occurred") {
                    println("\n=== Verifying token ===")
                    val verifiedToken = userRegistrationClient.verifyIdToken(customToken)
                    println("Verified token result: $verifiedToken")
                }
            } else {
                println("Failed to get user ID after registration")
            }
        }

    } catch (e: Exception) {
        println("Fatal error: ${e.message}")
        e.printStackTrace()
    } finally {
        userRegistrationClient?.close()
        channel?.shutdown()?.awaitTermination(5, TimeUnit.SECONDS)
        println("\nShutting down client...")
    }
}