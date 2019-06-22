package com.sikrinick.redis_orm.modules

import com.sikrinick.redis_orm.annotations.RedisId
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import java.io.File
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement

class RedisDatabaseCreation(
        private val packageName: String,
        private val destDir: String,
        private val printError: (String) -> Unit
) {

    fun createDatabase(elements: Collection<Element>) {
        FileSpec.builder(packageName, "RedisDatabase")
                .addType(TypeSpec.objectBuilder("RedisDatabase")
                        .buildMainClass(elements)
                        .build()
                )
                .build()
                .writeTo(
                        File(destDir, "RedisDatabase.kt")
                )
    }


    private fun TypeSpec.Builder.buildMainClass(elements: Collection<Element>): TypeSpec.Builder {
        elements.forEach { element ->
            processAndAdd(element)
        }
        return this
    }


    private fun TypeSpec.Builder.processAndAdd(element: Element) {
        if (element !is TypeElement) {
            printError("${element.simpleName} should be a class or, preferably, data class")
            return
        }
        element.enclosedElements
                .first {
                    it.kind == ElementKind.CONSTRUCTOR
                }
                .let {
                    (it as ExecutableElement).parameters
                }
                .filter { it.getAnnotation(RedisId::class.java) != null }
                .let {
                    when (it.size) {
                        //0 -> createSingleton(element)
                        1 -> createMapOf(element)
                        else -> printError("In ${element.simpleName} should be none or one RedisId. Not ${it.size}")
                    }
                }
    }

    private fun TypeSpec.Builder.createSingleton(element: TypeElement) {
        addProperty(
                PropertySpec.builder(
                        element.asClassName().simpleName,
                        element.asType().asTypeName().copy(nullable = true))
                        .initializer("null")
                        .build())
    }

    private fun TypeSpec.Builder.createMapOf(element: TypeElement) {
        addProperty(PropertySpec
                .builder(
                        element.asClassName().simpleName.decapitalize(),
                        HashMap::class.asClassName().parameterizedBy(
                                String::class.asTypeName(),
                                element.asType().asTypeName()
                        )
                )
                .initializer(CodeBlock.of("%T()", HashMap::class))
                .build()
        )
    }
}
