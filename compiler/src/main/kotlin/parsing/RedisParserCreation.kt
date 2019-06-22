package com.sikrinick.redis_orm.parsing

import com.squareup.kotlinpoet.*
import java.io.File
import java.util.*
import javax.lang.model.element.Element

class RedisParserCreation(
        private val packageName: String,
        private val destDir: String,
        private val printError: (String) -> Unit
) {
    companion object {
        const val DELIMITER = ":"
    }

    fun createParser(elements: Collection<Element>) {
        FileSpec.builder(packageName, "RedisParser")
                .addType(TypeSpec.objectBuilder("RedisParser")
                        .buildMainClass(elements)
                        .build()
                )
                .build()
                .writeTo(
                        File(destDir, "RedisParser.kt")
                )
    }

    private fun TypeSpec.Builder.buildMainClass(elements: Collection<Element>): TypeSpec.Builder {
        elements.forEach {
            RedisClassParserCreation(packageName, destDir, printError).createParser(it)
        }
        return this
    }

    private fun TypeSpec.Builder.addGetAllFunction(element: Element) {

    }

/*
    private fun addParseFunction(elements: Collection<Element>): FunSpec {
        val callbacks = elements.map {
            ParameterSpec.builder(
                    "on${it.simpleName}",
                    LambdaTypeName.get(
                            parameters = listOf(ParameterSpec.builder("", it.asType().asTypeName()).build()),
                            returnType = Unit::class.asTypeName()
                    )
            ).build()
        }

        val whenConditions = elements.map {
            "is ${it.simpleName} -> on${it.simpleName}(parse${it.simpleName})"
        }

        return FunSpec.builder("parse")
                .addParameters(listOf(
                        ParameterSpec.builder("key", String::class.asTypeName()).build(),
                        ParameterSpec.builder("value", String::class.asTypeName()).build()
                ))
                .addParameters(callbacks)
                .addCode(buildCodeBlock {
                    val linkedList = MemberName("java.util", "LinkedList")
                    addStatement("val keys = %M(key.split(\"$DELIMITER\"))", linkedList)
                    addStatement("val type = keys.poll()")
                    addStatement("when (type) {")
                    indent()
                    addStatements(whenConditions)
                    unindent()
                    addStatement("}")
                })
                .build()
    }
*/
    private fun CodeBlock.Builder.addStatements(statements: List<String>): CodeBlock.Builder {
        statements.forEach { addStatement(it) }
        return this
    }
}
