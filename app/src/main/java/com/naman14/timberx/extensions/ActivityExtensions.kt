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
package com.naman14.timberx.extensions

import android.app.Activity
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.naman14.timberx.R
/*泛型*/
/*扩展函数:在Activity类中添加了方法*/
/*<T : ViewDataBinding> 类型参数约束 扩展函数实现DataBinding绑定的方法*/
fun <T : ViewDataBinding> Activity.setDataBindingContentView(@LayoutRes res: Int): T {

    return DataBindingUtil.setContentView(this, res)
}

fun Activity?.addFragment(
    @IdRes id: Int = R.id.container,
    fragment: Fragment,
    tag: String? = null,
    addToBackStack: Boolean = true
) {
    val compatActivity = this as? AppCompatActivity ?: return
    compatActivity.supportFragmentManager.beginTransaction()
            .apply {
                add(id, fragment, tag)
                setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                if (addToBackStack) {
                    addToBackStack(null)
                }
                commit()
            }
}
/*
* 在Activity下添加扩展函数(?."为安全安全调用运算符,确保函数不会出现空指针异常")
* 需要动态添加一个碎片到 R.id.container
* */
fun Activity?.replaceFragment(
    @IdRes id: Int = R.id.container,
    fragment: Fragment,
    tag: String? = null,
    addToBackStack: Boolean = false
) {
    // "as?" 安全转换符 当左侧可以转换成右侧类型时,返回本身.不可以时返回null
    // "?:" Elvis 转换符 当左侧不为空时方法本视,为空时反会右侧方法
    val compatActivity = this as? AppCompatActivity ?: return
    // "apply" 对同一个对象进行多次返回用 "this"代替当前对象。完成后返回调用对象。
    compatActivity.supportFragmentManager.beginTransaction()
            .apply {
                // 需要replace()方法 向容器中添加碎片
                replace(id, fragment, tag)
                // 需要 addToBackStack()方法 将此事务添加到后台堆栈。意味着该事务被提交后会被记住，退回操作后,事务会从堆栈中弹回。
                // 需要 addToBackStack() 方法 为null时会被放入到堆栈中。
                if (addToBackStack) {
                    addToBackStack(null)
                }
                // 需要 commit()方法 执行容器
                commit()
            }
}
