plugins {
    kotlin("jvm") version "1.3.31"
}

group = "com.sikri"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven(url = "https://dl.bintray.com/kotlin/kotlinx")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.2.1")

    implementation("io.ktor:ktor-client-cio:1.2.1")
    implementation("io.ktor:ktor-client-gson:1.2.1")

    //    kapt(project(":compiler"))
    //    compileOnly(project(":compiler"))
    //    annotationProcessor(project(":compiler"))

}