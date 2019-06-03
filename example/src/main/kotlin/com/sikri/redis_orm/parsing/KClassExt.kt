package com.sikri.redis_orm.parsing

import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.jvmErasure


val KParameter.jvmType
    get() = type.jvmErasure

inline fun <reified T: Annotation> Array<out KClass<*>>.filterByAnnotation() = this
        .map { it to it.findAnnotation<T>() }
        .filter { (_, annotation) -> annotation != null }
        .map { (clazz, annotation) -> clazz to annotation!! }

inline fun <reified T: Annotation> KClass<*>.getConstructorParametersWith() =
        primaryConstructor?.parameters
                ?.map { it to it.findAnnotation<T>() }
                ?.filter { (_, annotation) -> annotation != null }
                ?.map { (clazz, annotation) -> clazz to annotation!! }

inline fun <F, S> Sequence<Pair<F?, S?>>.filterPairNotNull() = this
        .filter { (f, s) -> f != null && s != null }
        .map { (f, s) -> Pair(f!!, s!!) }