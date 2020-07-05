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
@file:Suppress("MemberVisibilityCanBePrivate")

package com.naman14.timberx.ui.activities.base

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.naman14.timberx.permissions.PermissionsManager
import org.koin.android.ext.android.inject

/**
 * Automatically attaches and detaches the activity from [PermissionsManager]. Also automatically
 * handles permission results by pushing them back into the manager.
 */
/*
* 从[PermissionsManager]自动附加和分离活动。也会自动
* 通过将权限结果推回给管理器来处理它们。
* */
/*
*抽象类 需要子类实现。这是系统权限获得
* 在onCreate()加载时初始化(获得管理接口)。
* 系统弹出申请的对话框后无论用户会选择同意或拒绝 都会回调到onRequestPermissionsResult()。
* 销毁(活动管理)接口。
* */
abstract class PermissionsActivity : AppCompatActivity() {
    /*委托:by*/
    /*使用Koin:使用依赖注入方法*/
    protected val permissionsManager by inject<PermissionsManager>()

    /*在onCreate()加载时初始化(获得管理接口)*/
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionsManager.attach(this)
    }

    /*系统弹出申请的对话框后无论用户会选择同意或拒绝 都会回调到onRequestPermissionsResult()*/
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        permissionsManager.processResult(requestCode, permissions, grantResults)
    }

    /*销毁(活动管理)接口*/
    override fun onDestroy() {
        permissionsManager.detach(this)
        super.onDestroy()
    }
}
