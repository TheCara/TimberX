/*
 * Copyright (c) 2019 Naman Dwivedi.
 *
 * Licensed under the GNU General Public License v3
 *
 * This is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 */
package com.naman14.timberx

import android.app.Application
import android.content.ComponentName
import android.content.ContentResolver
import com.naman14.timberx.playback.MediaSessionConnection
import com.naman14.timberx.playback.RealMediaSessionConnection
import com.naman14.timberx.playback.TimberMusicService
import io.reactivex.android.schedulers.AndroidSchedulers
import org.koin.dsl.module.module

const val MAIN = "main"

val mainModule = module {
    // 设置匹配类型
    // 绑定一个接口 ContentResolver
    factory<ContentResolver> {
        //指定类型
        // 得到Cursor类型
        get<Application>().contentResolver
    }
    // bind：为给定的对象声明添加要绑定的类型
    // ComponentName： 用于开启一个服务
    single {
        val component = ComponentName(get(), TimberMusicService::class.java)
        // 自定义类型
        RealMediaSessionConnection(get(), component)
    } bind MediaSessionConnection::class
    // name: String - for a Koin factory or single bean definition
    factory(name = MAIN) {
        // 主线程
        AndroidSchedulers.mainThread()
    }
}
