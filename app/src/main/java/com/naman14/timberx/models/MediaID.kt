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
package com.naman14.timberx.models

import android.support.v4.media.MediaBrowserCompat

/**/
class MediaID(var type: String? = null, var mediaId: String? = "NA", var caller: String? = currentCaller) {
    companion object {
        const val CALLER_SELF = "self"
        const val CALLER_OTHER = "other"

        private const val TYPE = "type: "
        private const val MEDIA_ID = "media_id: "
        private const val CALLER = "caller: "
        private const val SEPARATOR = " | "

        var currentCaller: String? = MediaID.CALLER_SELF
    }

    /*MediaItem:一个包含单个媒体项目信息的类，用于浏览媒体。*/
    var mediaItem: MediaBrowserCompat.MediaItem? = null

    /*返回字符串,这是整个依赖对象的变量*/
    fun asString(): String {
        /*组合*/
        return TYPE + type + SEPARATOR + MEDIA_ID + mediaId + SEPARATOR + CALLER + caller
    }

    /*返回依赖对象并重新初始化值*/
    fun fromString(s: String): MediaID {
        /*根据参数返回当前字符串参数后的字符串*/
        this.type = s.substring(6, s.indexOf(SEPARATOR))
        /*lastIndexOf()返回指定字符在此字符串中最后一次出现处的索引*/
        this.mediaId = s.substring(s.indexOf(SEPARATOR) + 3 + 10, s.lastIndexOf(SEPARATOR))
        this.caller = s.substring(s.lastIndexOf(SEPARATOR) + 3 + 8, s.length)
        return this
    }
}
