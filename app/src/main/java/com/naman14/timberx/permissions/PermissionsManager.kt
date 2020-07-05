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
package com.naman14.timberx.permissions

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.pm.PackageManager.PERMISSION_GRANTED
import androidx.annotation.VisibleForTesting
import androidx.annotation.VisibleForTesting.PRIVATE
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.naman14.timberx.extensions.asString
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.Single.just
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

/*data类:可以让编译器生成若干标准方法*/
/*创建一个权限结果类*/
data class GrantResult(
        val permission: String,/*权限名称*/
        val granted: Boolean/*权限申请结果*/
)

/**
 * Helps us manage, check, and dispatch permission requests without much boiler plate in our Activities
 * or views.
 * 帮助我们管理、检查和发送许可请求，在我们的活动或视图中没有太多的模板。
 */
/*
* 接口
* 权限信息
* */
interface PermissionsManager {
    /*授权结果*/
    /*Observable:被订阅者*/
    fun onGrantResult(): Observable<GrantResult>

    /*在加载时先初始化上下文Content*/
    fun attach(activity: Activity)

    /*是否保存权限?*/
    fun hasStoragePermission(): Boolean

    /*
    * 响应存储权限
    * */
    fun requestStoragePermission(waitForGranted: Boolean = false): Single<GrantResult>

    /*
    *申请进度
    * requestCode:申请code
    * permissions:要申请的权限数组
    * grantResults:授权数组
    * */
    fun processResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray)

    /*销毁上下文*/
    fun detach(activity: Activity)
}

/**
 * context:上下文
 * mainScheduler:订阅对象
 * 管理获得系统权限的类。
 * 创建伴生对象。
 * onGrantResult():获得被观察者对象。
 * attach():获得当前活动。
 * hasStoragePermission():是否保存权限。
 * requestStoragePermission():获得存储权限。
 * processResult():加载多条申请的权限保存到被订阅中。
 * detach():销毁权限。
 * hasPermission():申请权限是否成功。
 * requestPermission():根据是否保存权限保存被观察者的数据然后发送。
 * */
class RealPermissionsManager(
        private val context: Application,
        private val mainScheduler: Scheduler
) : PermissionsManager {
    /*伴生对象 保存常量Java静态变量*/
    companion object {
        @VisibleForTesting(otherwise = PRIVATE)
        const val REQUEST_CODE_STORAGE = 69
    }

    @VisibleForTesting(otherwise = PRIVATE)
    var activity: Activity? = null
    /*被观察者*/
    private val relay = PublishSubject.create<GrantResult>()

    /*share:在订阅时同时发射数据到多个观察者*/
    /*observeOn:改变调用它之后代码的线程*/
    override fun onGrantResult(): Observable<GrantResult> = relay.share().observeOn(mainScheduler)

    override fun attach(activity: Activity) {
        Timber.d("attach(): $activity")
        this.activity = activity
    }

    /*是否保存权限*/
    override fun hasStoragePermission() = hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)

    /*活动存储权限*/
    override fun requestStoragePermission(waitForGranted: Boolean) =
            requestPermission(REQUEST_CODE_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, waitForGranted)

    /*
    * 加载多条申请的权限保存到被订阅中
    * requestCode:申请code
    * permissions:要申请的权限数组
    * grantResults:授权数组
    * */
    override fun processResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        Timber.d("processResult(): requestCode= %d, permissions: %s, grantResults: %s",
                requestCode, permissions.asString(), grantResults.asString())
        for ((index, permission) in permissions.withIndex()) {
            val granted = grantResults[index] == PERMISSION_GRANTED
            val result = GrantResult(permission, granted)
            Timber.d("Permission grant result: %s", result)
            relay.onNext(result)
        }
    }

    /*销毁活动*/
    override fun detach(activity: Activity) {
        // === is referential equality - returns true if they are the same instance
        if (this.activity === activity) {
            Timber.d("detach(): $activity")
            this.activity = null
        }
    }

    /*申请权限是否成功*/
    private fun hasPermission(permission: String): Boolean {
        /*申请权限*/
        /*检测某项权限是否开启*/
        /*PERMISSION_GRANTED 为 true 表示申请权限成功*/
        return ContextCompat.checkSelfPermission(context, permission) == PERMISSION_GRANTED
    }

    /*
    * 根据是否保存权限保存被观察者的数据
    * code:申请权限的code代码
    * permission:权限
    * waitForGranted:?
    * */
    private fun requestPermission(code: Int, permission: String, waitForGranted: Boolean): Single<GrantResult> {
        Timber.d("Requesting permission: %s", permission)
        /*hasPermission():权限是否申请*/
        if (hasPermission(permission)) {
            Timber.d("Already have this permission!")
            /*just:简单的原样发射*/
            /*also:允许你对表达式求值，检查求职结果为null，并把结果保存为一个变量*/
            return just(GrantResult(permission, true).also {
                relay.onNext(it)
            })
        }

        val attachedTo = activity ?: throw IllegalStateException("Not attached")
        /*申请权限*/
        ActivityCompat.requestPermissions(attachedTo, arrayOf(permission), code)
        /*被观察者过滤*/
        return onGrantResult()
                /*it:集合中的类*/
                /*filter:选择匹配的给定判断元素*/
                .filter { it.permission == permission }
                .filter {
                    if (waitForGranted) {
                        // If we are waiting for granted, only allow emission if granted is true
                        it.granted
                    } else {
                        // Else continue
                        true
                    }
                }
                /*take:取操作符的前几项*/
                .take(1)
                /* 如果只发射一个值，进入 onSuccess，否则都是 onError*/
                .singleOrError()
    }
}
