plugins {
    kotlin("jvm") version "2.0.0"
//    kotlin("plugin.serialization") version "2.0.0"
}

group = "me.mantou.koko"

repositories {
    mavenCentral()
}

dependencies {
    // misc
//    implementation(libs.kotlinxSerializationJSON)
    implementation(kotlin("reflect"))

    // log
    implementation(libs.kotlinLoggingJVM)
    implementation(libs.logback)

    // ktor
    implementation(libs.ktorClientCore)
    implementation(libs.ktorClientCIO)
    implementation(libs.ktorClientWebSockets)
//    implementation(libs.ktorClientContentNegotiation)
//    implementation(libs.ktorSerializationJackson)

    // jackson
    implementation(libs.jackson)
    implementation(libs.jacksonModuleKotlin)

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}