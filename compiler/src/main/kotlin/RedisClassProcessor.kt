package com.sikrinick.redis_orm

import com.google.auto.service.AutoService
import com.sikrinick.redis_orm.annotations.RedisClass
import com.sikrinick.redis_orm.parsing.RedisParserCreation
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

@AutoService(Processor::class)
@SupportedOptions(RedisClassProcessor.KAPT_KOTLIN_GENERATED_OPTION_NAME)
class RedisClassProcessor: AbstractProcessor() {

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latestSupported()

    override fun getSupportedAnnotationTypes(): Set<String> {
        return setOf(RedisClass::class.java.canonicalName)
    }
    override fun getSupportedOptions() = setOf(SUFFIX_OPTION, GENERATE_KOTLIN_CODE_OPTION, GENERATE_ERROR)

    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"

        const val SUFFIX_OPTION = "suffix"
        const val GENERATE_KOTLIN_CODE_OPTION = "generate.kotlin.code"
        const val GENERATE_ERROR = "generate.error"
    }

    override fun process(annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment?): Boolean {
        val generatedDir = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]
            ?: run {
                printError("Can't find the target directory for generated Kotlin files.")
                return true
            }
        val annotatedElements = roundEnv?.getElementsAnnotatedWith(RedisClass::class.java)
        if (annotatedElements.isNullOrEmpty()) {
            printWarning("No annotated elements")
            return true
        }
        processAnnotation(annotatedElements, generatedDir)
        return true
    }

    private fun processAnnotation(elements: Set<Element>, generatedDir: String) {

        val packageName = processingEnv.elementUtils.getPackageOf(elements.first()).qualifiedName.toString()
        val options = processingEnv.options
        val generatedFileSuffix = options[SUFFIX_OPTION] ?: "Generated"

        //RedisDatabaseCreation(packageName, generatedDir, ::printError)
        //        .createDatabase(elements)
        RedisParserCreation(packageName, generatedDir, ::printError)
                .createParser(elements)
    }

    fun printWarning(msg: String) {
        processingEnv.messager.printMessage(Diagnostic.Kind.WARNING, msg)
    }
    fun printError(msg: String) {
        processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, msg)
    }

}