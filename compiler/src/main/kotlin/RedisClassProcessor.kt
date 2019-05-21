package com.sikrinick.redis_orm

import com.sikrinick.redis_orm.annotations.RedisClass
import com.sikrinick.redis_orm.annotations.RedisId
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import java.io.File
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

class RedisClassProcessor: AbstractProcessor() {
    private companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"

        val SUFFIX_OPTION = "suffix"
        val GENERATE_KOTLIN_CODE_OPTION = "generate.kotlin.code"
        val GENERATE_ERROR = "generate.error"
        val KAPT_KOTLIN_GENERATED_OPTION = "kapt.kotlin.generated"
    }

    override fun process(annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment?): Boolean {
        val generatedDir = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]
            ?: run {
                processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "Can't find the target directory for generated Kotlin files.")
                return true
            }
        val annotatedElements = roundEnv?.getElementsAnnotatedWith(RedisClass::class.java)
        if (annotatedElements.isNullOrEmpty()) {
            processingEnv.messager.printMessage(Diagnostic.Kind.WARNING, "No annotated elements")
            return true
        }

        processAnnotation(annotatedElements, generatedDir)
        return true
    }

    private fun processAnnotation(elements: Set<Element>, generatedDir: String) {

        val elementUtils = processingEnv.elementUtils
        val options = processingEnv.options
        val generatedFileSuffix = options[SUFFIX_OPTION] ?: "Generated"

        val packageName = javaClass.`package`.name

        FileSpec.builder(elementUtils.getPackageOf(elements.first()).qualifiedName.toString(), "RedisOrm")
            .addType(TypeSpec.objectBuilder("RedisOrm")
                .buildMainClass(elements)
                .build()
            )
            .build()
            .writeTo(
                File(generatedDir, "RedisOrm.kt")
            )
    }

    fun TypeSpec.Builder.buildMainClass(elements: Set<Element>): TypeSpec.Builder {
        elements.forEach { element ->
            processAndAdd(element)
        }
        return this

        //if (options[GENERATE_ERROR] == "true") {
        //    processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "Error from annotation processor!")
        //}
    }

    fun TypeSpec.Builder.processAndAdd(element: Element) {
        if (element is TypeElement) {
            element.enclosedElements
                .filter { it.kind == ElementKind.FIELD }
                .filter { it.getAnnotation(RedisId::class.java) != null }
                .let {
                    when(it.size) {
                        //0 -> createSingleton(element)
                        1 -> createMapOf(element)
                        else -> printError("In ${element.simpleName} should be zero or one RedisId!")
                    }
                }
        } else {
           printError("${element.simpleName} should be a class!")
        }
    }

    fun TypeSpec.Builder.createSingleton(element: TypeElement) {
        addProperty(
            PropertySpec.builder(
                element.asClassName().simpleName,
                element.asType().asTypeName().copy(nullable = true))
                .initializer("null")
                .build())
    }

    fun TypeSpec.Builder.createMapOf(element: TypeElement) {
        addProperty(
            PropertySpec.builder(
                element.asClassName().simpleName,
                HashMap::class.asClassName()
                    .parameterizedBy(String::class.asTypeName(), element.asType().asTypeName()))
                .initializer(CodeBlock.of("%T()", HashMap::class))
                .build()
        )
    }

    fun printError(msg: String) {
        processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, msg)
    }

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latestSupported()

    override fun getSupportedAnnotationTypes(): Set<String> {
        return setOf(RedisClass::class.java.canonicalName)
    }

    override fun getSupportedOptions() = setOf(SUFFIX_OPTION, GENERATE_KOTLIN_CODE_OPTION, GENERATE_ERROR)
}