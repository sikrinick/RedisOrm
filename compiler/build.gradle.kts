
plugins {
    kotlin("jvm")
    kotlin("kapt")
}

dependencies {
    api(kotlin("stdlib"))
    implementation("com.squareup:kotlinpoet:1.1.0")

    implementation("com.google.auto.service:auto-service:1.0-rc4")
    kapt("com.google.auto.service:auto-service:1.0-rc4")

    testImplementation(group = "junit", name = "junit", version = "4.12")
}