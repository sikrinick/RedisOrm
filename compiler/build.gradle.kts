plugins {
    kotlin("jvm")
}

dependencies {
    compile(kotlin("stdlib"))
    compile("com.squareup:kotlinpoet:1.1.0")
    compile("io.reactivex.rxjava2:rxjava:2.2.0")

    testCompile(group = "junit", name = "junit", version = "4.12")
}