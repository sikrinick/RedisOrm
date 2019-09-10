plugins {
    kotlin("jvm")
    kotlin("kapt")
}

repositories {
    jcenter()
    mavenCentral()
    maven(url = "https://dl.bintray.com/kotlin/kotlinx")
}

dependencies {
    api(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.1")

    implementation("com.squareup:kotlinpoet:1.1.0")

    implementation("com.google.auto.service:auto-service:1.0-rc6")
    kapt("com.google.auto.service:auto-service:1.0-rc6")

    testImplementation(group = "junit", name = "junit", version = "4.12")
}