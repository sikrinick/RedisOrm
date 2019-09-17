package com.sikrinick.redis_orm.parsing

import com.squareup.kotlinpoet.*
import java.io.File
import javax.lang.model.element.Element
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

class RedisClassParserCreation(
        private val packageName: String,
        private val destDir: String,
        private val printError: (String) -> Unit
) {
    private val hasId: Boolean = false

    fun createParser(element: Element) {

        FileSpec.builder(packageName, "Redis${element.simpleName}Parser")
                .addType(TypeSpec.objectBuilder("Redis${element.simpleName}Parser")
                        .buildMainClass(element)
                        .build()
                )
                .build()
                .writeTo(
                        File(destDir, "Redis${element.simpleName}Parser.kt")
                )
        }
    }


    private fun TypeSpec.Builder.buildMainClass(element: Element): TypeSpec.Builder {
        addFunctions(listOf(
                addParseAllFunction(element),
                addChangeFunction(element)
        ))
        return this
    }

    private fun addParseAllFunction(element: Element): FunSpec =
            FunSpec.builder("parseAll")
                    .addModifiers(KModifier.SUSPEND)
                    .addParameter(ParameterSpec
                            .builder("results", Map::class.asClassName().parameterizedBy(
                                    String::class.asTypeName(),
                                    String::class.asTypeName()
                            )).build()
                    )
                    .addCode(buildCodeBlock {
                        val list = MemberName("java.util", "ArrayList")
                        addStatement("return %M()", list)
                    })
                    .returns(
                        List::class.asClassName().parameterizedBy(
                                element.asType().asTypeName()
                        )
                    )
                    .build()

    private fun addChangeFunction(element: Element): FunSpec {
        val stringType = String::class.asTypeName()
        val elementType = element.asType().asTypeName()
        return FunSpec.builder("parseChange")
                .addModifiers(KModifier.SUSPEND)
                .addParameter(ParameterSpec
                        .builder(
                                "result",
                                Pair::class.asClassName().parameterizedBy(
                                        stringType,
                                        stringType
                                )
                        ).build()
                )
                .addCode(buildCodeBlock {
                    addStatement("return { this.copy() }")
                })
                .returns(
                        LambdaTypeName.get(
                                elementType,
                                returnType = elementType
                        )
                )
                .build()
    }