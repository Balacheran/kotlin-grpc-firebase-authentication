import com.google.protobuf.gradle.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val grpcVersion = "1.44.0"  // Updated version
val grpcKotlinVersion = "1.2.1"  // Updated version
val protobufVersion = "3.19.4"  // Updated version
val coroutinesVersion = "1.6.0"  // Updated version

plugins {
    application
    kotlin("jvm") version "1.6.10"  // Updated Kotlin version
    id("com.google.protobuf") version "0.8.18"  // Updated protobuf plugin version
}

group = "me.windows"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    google()
    mavenCentral()  // Prefer mavenCentral over jcenter (jcenter is deprecated)
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("javax.annotation:javax.annotation-api:1.3.2")
    implementation("com.google.protobuf:protobuf-java-util:$protobufVersion")
    implementation("io.grpc:grpc-protobuf:$grpcVersion")
    implementation("io.grpc:grpc-stub:$grpcVersion")
    implementation("io.grpc:grpc-kotlin-stub:$grpcKotlinVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("com.google.firebase:firebase-admin:9.1.1")
    implementation("com.google.cloud:google-cloud-firestore:3.7.0")
    implementation("org.slf4j:slf4j-api:1.7.36")
    implementation("ch.qos.logback:logback-classic:1.2.11")
    implementation("org.slf4j:slf4j-simple:1.7.36")
    runtimeOnly("io.grpc:grpc-netty-shaded:$grpcVersion")
    testImplementation(kotlin("test-junit"))
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:$protobufVersion"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:$grpcVersion"
        }
        id("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:$grpcKotlinVersion:jdk7@jar"  // Updated artifact specification
        }
    }
    generateProtoTasks {
        ofSourceSet("main").forEach {
            it.plugins {
                id("grpc")
                id("grpckt")
            }
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

sourceSets {
    main {
        java {
            srcDirs("build/generated/source/proto/main/grpc")
            srcDirs("build/generated/source/proto/main/grpckt")
            srcDirs("build/generated/source/proto/main/java")
        }
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
application {
    mainClass.set("user_registration.UserRegistrationServerKt")
}

tasks.register<JavaExec>("runServer") {
    group = "application"
    mainClass.set("user_registration.UserRegistrationServerKt")
    classpath = sourceSets["main"].runtimeClasspath
}

tasks.register<JavaExec>("runClient") {
    group = "application"
    mainClass.set("user_registration.UserRegistrationClientKt")
    classpath = sourceSets["main"].runtimeClasspath
    // Add this for better debugging
    // jvmArgs = listOf("-Dorg.slf4j.simpleLogger.defaultLogLevel=DEBUG")
}