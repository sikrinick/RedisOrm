plugins {
    kotlin("jvm") version "1.3.31"
}

dependencies {
    // Make the root project archives configuration depend on every subproject
    subprojects.forEach {
        archives(it)
    }
}

allprojects {
    repositories {
        mavenCentral()
    }
}

