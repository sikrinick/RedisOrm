package com.sikrinick.redis_orm.annotations

import java.lang.annotation.Inherited

@Inherited
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class RedisClass(
    val name: String
)