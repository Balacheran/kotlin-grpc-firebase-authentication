package user_registration

import com.google.protobuf.StringValue
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import user_registration.UserRegistrationGrpcKt.UserRegistrationCoroutineStub
import java.io.Closeable
import java.util.concurrent.TimeUnit

class UserRegistrationClient(private val channel: ManagedChannel) : Closeable {
    private val userStub: UserRegistrationCoroutineStub by lazy { UserRegistrationCoroutineStub(channel) }

    suspend fun registerUser(user: User): Boolean {
        userStub.registerUser(user).let {
            return if (it.value) {
                println("${user.firstName} is registered.")
                true
            } else {
                println("Failed to register the user.")
                false
            }
        }
    }

    suspend fun getUserInfo(userId: StringValue): User {
        userStub.getUserInfo(userId).let {
            return if (it.isInitialized) {
                println("Id: ${it.id}")
                println("First Name: ${it.firstName}")
                println("Last Name: ${it.lastName}")
                println("Mobile Number: ${it.mobileNumber}")
                println("Email: ${it.email}")
                println("Address: ${it.address}")
                println("Preferences: ${it.preferences}")
                println("Latitude: ${it.location.latitude}")
                println("Latitude: ${it.location.longitude}")
                it
            } else {
                println("No user found")
                User.getDefaultInstance()
            }
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

    suspend fun verifyIdToken(customToken: StringValue): String {
        userStub.verifyIdToken(customToken).let {
            return if (it.value != "Error occurred") {
                println("Id Token Decoded for User with uId: ${it.value}")
                it.value
            } else {
                println("Error occurred")
                it.value
            }
        }
    }


    override fun close() {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
    }

}

// UserRegistrationClient.kt
suspend fun main() {
    println("Starting client...")
    val port = 50052
    var channel: ManagedChannel? = null
    
    try {
        println("Connecting to server on port $port...")
        channel = ManagedChannelBuilder.forAddress("localhost", port)
            .usePlaintext()
            .build()
        
        val client = UserRegistrationClient(channel)
        
        // Create a test user
        val location = Location.newBuilder()
            .setLatitude(71121212)
            .setLongitude(91122121)
            .build()
            
        val user = User.newBuilder()
            .setId("testId123")
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
        try {
            val registrationResult = client.registerUser(user)
            println("Registration result: $registrationResult")
            
            if (registrationResult) {
                // Get user info
                println("\n=== Getting user info ===")
                val userId = StringValue.newBuilder().setValue(user.id).build()
                val userInfo = client.getUserInfo(userId)
                
                // Display user info
                println("\nUser Information:")
                println("ID: ${userInfo.id}")
                println("Name: ${userInfo.firstName} ${userInfo.lastName}")
                println("Email: ${userInfo.email}")
                println("Address: ${userInfo.address}")
                println("Mobile: ${userInfo.mobileNumber}")
                println("Preferences: ${userInfo.preferences}")
                println("Location: (${userInfo.location.latitude}, ${userInfo.location.longitude})")
                
                // Check admin status
                println("\n=== Checking admin status ===")
                val isAdmin = client.isUserAdmin(userId)
                println("Is admin: $isAdmin")

                // Create custom token
                println("\n=== Creating custom token ===")
                val customToken = client.createCustomToken(userId)
                println("Custom token: $customToken")

                // Verify token
                if (customToken != "Error occurred") {
                    println("\n=== Verifying token ===")
                    val tokenValue = StringValue.newBuilder().setValue(customToken).build()
                    val verifiedToken = client.verifyIdToken(tokenValue)
                    println("Verified token result: $verifiedToken")
                }
            }
        } catch (e: Exception) {
            println("\nError during operations: ${e.message}")
            e.printStackTrace()
        }

    } catch (e: Exception) {
        println("\nFatal error: ${e.message}")
        e.printStackTrace()
    } finally {
        println("\nShutting down client...")
        channel?.shutdown()?.awaitTermination(5, TimeUnit.SECONDS)
    }
}