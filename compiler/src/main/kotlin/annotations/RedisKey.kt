package com.sikrinick.redis_orm.annotations

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.SOURCE)
annotation class RedisKey(
    val name: String
)