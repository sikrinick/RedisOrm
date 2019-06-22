package com.sikrinick.redis_orm.annotations

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
annotation class RedisKey(
    val name: String
)