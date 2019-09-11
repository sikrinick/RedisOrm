plugins {
    kotlin("jvm") version "1.3.50"
    kotlin("kapt") version "1.3.50"
}

group = "com.sikri"
version = "1.0-SNAPSHOT"

repositories {
    jcenter()
    mavenCentral()
    maven(url = "https://dl.bintray.com/kotlin/kotlinx")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.1")

    implementation("io.ktor:ktor-client-cio:1.2.4")
    implementation("io.ktor:ktor-client-gson:1.2.4")

//    kapt(project(":compiler"))
//    compileOnly(project(":compiler"))
//    annotationProcessor(project(":compiler"))

}