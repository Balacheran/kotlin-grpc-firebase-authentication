package user_registration

import com.google.protobuf.BoolValue
import com.google.protobuf.StringValue
import com.google.firebase.FirebaseApp
import firebase_admin_sdk.api.FirebaseApiManager
import firebase_admin_sdk.listener.Failure
import firebase_admin_sdk.listener.Success
import io.grpc.Server
import io.grpc.ServerBuilder
import service_utils.Utils.buildBoolVal
import service_utils.Utils.buildStringVal
import service_utils.Utils.updateUserWithGeneratedId


class UserRegistrationServer(private val port: Int) {
    private val server: Server = ServerBuilder
        .forPort(port)
        .addService(UserRegistrationService())
        .build()


    fun start() {
        server.start()
        println("Server started, listening on $port")
        Runtime.getRuntime().addShutdownHook(
            Thread {
                println("*** shutting down gRPC server since JVM is shutting down")
                this@UserRegistrationServer.stop()
                println("*** server shut down")
            }
        )
    }


    fun stop() {
    try {
        server.shutdown()
        FirebaseApp.getInstance().delete()
    } catch (e: Exception) {
        println("Error during server shutdown: ${e.message}")
    }
}

    fun blockUntilShutdown() {
        server.awaitTermination()
    }

    private class UserRegistrationService : UserRegistrationGrpcKt.UserRegistrationCoroutineImplBase() {
        

        override suspend fun registerUser(request: User): BoolValue {
            return when (val result = FirebaseApiManager.registerNewUserWithEmailPassword(
                request.email,
                request.password
            )) {
                is Success -> {
                    val userId = result.data
                    if (userId != null) {
                        // Create a new user with the Firebase ID
                        val userWithId = request.toBuilder()
                            .setId(userId)
                            .build()
                        
                        when (FirebaseApiManager.storeUserData(userWithId)) {
                            is Success -> {
                                println("User registered with ID: $userId")
                                buildBoolVal(true)
                            }
                            is Failure -> buildBoolVal(false)
                        }
                    } else {
                        buildBoolVal(false)
                    }
                }
                is Failure -> buildBoolVal(false)
            }
        }
        override suspend fun getUserInfo(request: StringValue): User {
            println("Attempting to get user info for ID: ${request.value}")
            return when (val result = FirebaseApiManager.getUserFromId(request.value)) {
                is Success -> {
                    val user = result.data
                    println("Successfully retrieved user: ${user?.firstName} ${user?.lastName}")
                    user ?: User.getDefaultInstance()
                }
                is Failure -> {
                    println("Failed to get user info: ${result.msg}")
                    User.getDefaultInstance()
                }
            }
        }

        override suspend fun setUserCustomClaim(request: StringValue): BoolValue {
            return when (val result = FirebaseApiManager.setUserCustomClaims(request.value)) {
                is Success -> {
                    println("Set User Custom Claims: ${result.msg}")
                    buildBoolVal(true)
                }
                is Failure -> {
                    println("Set User Custom Claims: ${result.msg}")
                    buildBoolVal(false)
                }
            }
        }

        override suspend fun isUserAdmin(request: StringValue): BoolValue {
            return when (val result = FirebaseApiManager.isUserAdmin(request.value)) {
                is Success -> {
                    println("Is User Admin: ${result.msg}")
                    buildBoolVal(result.data)
                }
                is Failure -> {
                    println("Is User Admin: ${result.msg}")
                    buildBoolVal(false)
                }
            }
        }

        override suspend fun createCustomToken(request: StringValue): StringValue {
            return when (val result = FirebaseApiManager.createCustomToken(request.value)) {
                is Success -> {
                    println("Custom Token Creation: ${result.msg}")
                    buildStringVal(result.data ?: "")
                }
                is Failure -> {
                    println("Custom Token Creation: ${result.msg}")
                    buildStringVal(result.msg)
                }
            }
        }

        override suspend fun verifyIdToken(request: StringValue): StringValue {
            return when (val result = FirebaseApiManager.verifyIdToken(request.value)) {
                is Success -> {
                    println("Verify Id Token: ${result.msg}")
                    buildStringVal(result.data ?: "")
                }
                is Failure -> {
                    println("Verify Id Token: ${result.msg}")
                    buildStringVal(result.msg)
                }
            }
        }
    }
}

fun main() {
    val port = 50052
    val server = UserRegistrationServer(port)
    server.start()
    server.blockUntilShutdown()
}