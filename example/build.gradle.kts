plugins {
    kotlin("jvm")
    kotlin("kapt")
}

dependencies {
    compile(kotlin("stdlib"))
    compile(kotlin("reflect"))
    compile(group = "com.tylerthrailkill.helpers", name = "pretty-print", version = "2.02")

    compileOnly(project(":compiler"))
    annotationProcessor(project(":compiler"))
    kapt(project(":compiler"))
}