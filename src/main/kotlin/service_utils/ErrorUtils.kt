package service_utils

import firebase_admin_sdk.listener.Failure
import firebase_admin_sdk.listener.Result

object ErrorUtils {
    fun handleFirebaseError(e: Exception, operation: String): Result<Nothing> {
        println("Firebase $operation error: ${e.message}")
        e.printStackTrace()
        return Failure(msg = "Error in $operation: ${e.message}")
    }

    fun logError(tag: String, message: String, e: Exception? = null) {
        println("[$tag] Error: $message")
        e?.let { 
            println("Exception: ${e.message}")
            e.printStackTrace()
        }
    }
}