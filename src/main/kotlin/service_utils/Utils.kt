package service_utils

import com.google.protobuf.BoolValue
import com.google.protobuf.StringValue
import user_registration.Location
import user_registration.User

object Utils {
    fun User.updateUserWithGeneratedId(uId: String): User = User.newBuilder()
        .setId(uId)
        .setFirstName(this.firstName)
        .setLastName(this.lastName)
        .setMobileNumber(this.mobileNumber)
        .setLocation(this.location)
        .setEmail(this.email)
        .setPassword(this.password)
        .setPreferences(this.preferences)
        .setAddress(this.address)
        .build()

    fun buildBoolVal(data: Boolean): BoolValue =
        BoolValue.newBuilder()
            .setValue(data)
            .build()

    fun buildStringVal(data: String): StringValue =
        StringValue.newBuilder()
            .setValue(data)
            .build()

    fun getHashMap(user: User): Map<String, Any> {
        return mapOf(
            "id" to user.id,
            "firstName" to user.firstName,
            "lastName" to user.lastName,
            "email" to user.email,
            "password" to user.password,
            "address" to user.address,
            "preferences" to user.preferences,
            "mobileNumber" to user.mobileNumber,
            "latitude" to user.location.latitude,
            "longitude" to user.location.longitude
        )
    }

    fun getFromDocumentSnapshot(data: Map<String, Any?>?): User {
        return try {
            User.newBuilder().apply {
                // Safely handle nullable values
                data?.get("id")?.toString()?.let { setId(it) }
                data?.get("firstName")?.toString()?.let { setFirstName(it) }
                data?.get("lastName")?.toString()?.let { setLastName(it) }
                data?.get("email")?.toString()?.let { setEmail(it) }
                data?.get("address")?.toString()?.let { setAddress(it) }
                data?.get("preferences")?.toString()?.let { setPreferences(it) }
                data?.get("password")?.toString()?.let { setPassword(it) }
                
                // Handle location
                setLocation(Location.newBuilder().apply {
                    latitude = (data?.get("latitude") as? Number)?.toInt() ?: 0
                    longitude = (data?.get("longitude") as? Number)?.toInt() ?: 0
                }.build())
                
                // Handle mobile number
                mobileNumber = (data?.get("mobileNumber") as? Number)?.toLong() ?: 0L
            }.build()
        } catch (e: Exception) {
            println("Error creating User from document: ${e.message}")
            User.getDefaultInstance()
        }
    }
}
