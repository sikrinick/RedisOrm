plugins {
    kotlin("jvm")
    kotlin("kapt")
}

dependencies {
    compile(kotlin("stdlib"))
    compile(kotlin("reflect"))
    compile("com.tylerthrailkill.helpers:pretty-print:2.0.2")

    compileOnly(project(":compiler"))
    annotationProcessor(project(":compiler"))
    kapt(project(":compiler"))
}