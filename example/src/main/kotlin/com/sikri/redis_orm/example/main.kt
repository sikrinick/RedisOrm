package com.sikri.redis_orm.example

import com.sikri.redis_orm.module.RedisOrm

fun main() {

    val redisOrm = RedisOrm(
            Device::class,
            Scene::class,
            Room::class,
            ComplicatedDevice::class,
            School::class,
            HAEngine::class,
            Stb::class
    )
    redisOrm.updateAll()
    redisOrm.subscribe()
}