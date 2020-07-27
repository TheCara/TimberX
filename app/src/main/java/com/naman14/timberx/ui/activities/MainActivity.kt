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
package com.naman14.timberx.ui.activities

import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore.EXTRA_MEDIA_TITLE
import android.provider.MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.NonNull
import androidx.mediarouter.app.MediaRouteButton
import com.afollestad.rxkprefs.Pref
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_DRAGGING
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN
import com.naman14.timberx.PREF_APP_THEME
import com.naman14.timberx.R
import com.naman14.timberx.constants.AppThemes
import com.naman14.timberx.databinding.MainActivityBinding
import com.naman14.timberx.extensions.addFragment
import com.naman14.timberx.extensions.filter
import com.naman14.timberx.extensions.hide
import com.naman14.timberx.extensions.map
import com.naman14.timberx.extensions.observe
import com.naman14.timberx.extensions.replaceFragment
import com.naman14.timberx.extensions.setDataBindingContentView
import com.naman14.timberx.extensions.show
import com.naman14.timberx.models.MediaID
import com.naman14.timberx.repository.SongsRepository
import com.naman14.timberx.ui.activities.base.PermissionsActivity
import com.naman14.timberx.ui.dialogs.DeleteSongDialog
import com.naman14.timberx.ui.fragments.BottomControlsFragment
import com.naman14.timberx.ui.fragments.MainFragment
import com.naman14.timberx.ui.fragments.base.MediaItemFragment
import com.naman14.timberx.ui.viewmodels.MainViewModel
import com.naman14.timberx.ui.widgets.BottomSheetListener
import kotlinx.android.synthetic.main.main_activity.bottom_sheet_parent
import kotlinx.android.synthetic.main.main_activity.dimOverlay
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
/*
*
* */
class MainActivity : PermissionsActivity(), DeleteSongDialog.OnSongDeleted {

    private val viewModel by viewModel<MainViewModel>()
    private val songsRepository by inject<SongsRepository>()
    private val appThemePref by inject<Pref<AppThemes>>(name = PREF_APP_THEME)

    //    不为空
    //    MainActivityBinding类(DataBinding库会为每个布局文件生成一个binding类)为DataBinging绑定类
    private var binding: MainActivityBinding? = null
    private var bottomSheetListener: BottomSheetListener? = null

    //    初始化BottomSheetBehavior<View>泛型为<View>
    private var bottomSheetBehavior: BottomSheetBehavior<View>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        //更改主题,使用setTheme()方法要在onCreate()方法之前,表示设置基础主题上下文,被称为视图实例化之前的上下文
        setTheme(appThemePref.get().themeRes)
        super.onCreate(savedInstanceState)
        binding = setDataBindingContentView(R.layout.main_activity)
        // 访问setSupportActionBar()方法:"?."(安全调用运算符)用来确保不为空(null) 设置Toolbar时候显示
        supportActionBar?.setDisplayShowTitleEnabled(false)
        //是否存储权限
        if (!permissionsManager.hasStoragePermission()) {
            permissionsManager.requestStoragePermission()
            return
        }

        setupUI()
    }

    /**/
    private fun setupUI() {
        //延迟跳转到一个碎片
        //observe()方法第一个参数是作为LifecycleOwner实例的Fragment。
        //这样做表示此Observer绑定了Lifecycle对象的生命周期。使用observe()方法将Observer对象注册到LiveData对象
        //第二个参数:用作观察者模式
        viewModel.rootMediaId.observe(this) {
            replaceFragment(fragment = MainFragment())

            Handler().postDelayed({
                replaceFragment(
                        R.id.bottomControlsContainer,
                        BottomControlsFragment()

                )
            }, 150)

            //handle playback intents, (search intent or ACTION_VIEW intent)
            handlePlaybackIntent(intent)
        }
        //使用 LiveData<Event<MediaID>>的
        //map():将list中的元素按照参数转换成新的元素，并且将List返回
        //filter():根据参数过滤掉返回一个新的List
        viewModel.navigateToMediaItem
                .map { it.getContentIfNotHandled() }
                .filter { it != null }
                .observe(this) { navigateToMediaItem(it!!) }
        // "?."安全调用运算符
        // 需要 初始化ViewModel 和 setLifeCyclerOwner
        binding?.let {
            it.viewModel = viewModel
            it.lifecycleOwner = this
        }
        //"as":as运算符用于执行引用类型的显式类型转换。
        // 如果要转换的类型与指定的类型兼容，转换就会成功进行；如果类型不兼容，使用as?运算符就会返回值null。
        // 在Kotlin中，父类是禁止转换为子类型的。
        val parentThatHasBottomSheetBehavior = bottom_sheet_parent as FrameLayout

        bottomSheetBehavior = BottomSheetBehavior.from(parentThatHasBottomSheetBehavior)
        bottomSheetBehavior?.isHideable = true
        // 需要 设置监听在联动的控件中拖动、滑动、切换时发生改变
        bottomSheetBehavior?.setBottomSheetCallback(BottomSheetCallback())

        dimOverlay.setOnClickListener { collapseBottomSheet() }
    }
    private fun handlePlaybackIntent(intent: Intent?) {
        if (intent == null || intent.action == null) return
        // "!!" 非空断言 当左侧值不为空时返回本身,为空时返回NullPointer
        when (intent.action!!) {
            INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH -> {
                val songTitle = intent.extras?.getString(EXTRA_MEDIA_TITLE, null)
                viewModel.transportControls().playFromSearch(songTitle, null)
            }
            ACTION_VIEW -> {
                val path = getIntent().data?.path ?: return
                val song = songsRepository.getSongFromPath(path)
                viewModel.mediaItemClicked(song, null)
            }
        }
    }

    private inner class BottomSheetCallback : BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(@NonNull bottomSheet: View, newState: Int) {
            if (newState == STATE_DRAGGING || newState == STATE_EXPANDED) {
                dimOverlay.show()
            } else if (newState == STATE_COLLAPSED) {
                dimOverlay.hide()
            }
            bottomSheetListener?.onStateChanged(bottomSheet, newState)
        }
        // 需要 onSlide() 拖拽中的回调
        // 需要 slideOffset为0-1 完全收起为0 完全展开为1
        override fun onSlide(@NonNull bottomSheet: View, slideOffset: Float) {
            if (slideOffset > 0) {
                // 需要 修改 "alpha" 设置透明度 setAlpha（0）时view和子view就会消失.
                dimOverlay.alpha = slideOffset
            } else if (slideOffset == 0f) {
                dimOverlay.hide()
            }
            bottomSheetListener?.onSlide(bottomSheet, slideOffset)
        }
    }

    fun collapseBottomSheet() {
        bottomSheetBehavior?.state = STATE_COLLAPSED
    }

    fun setBottomSheetListener(bottomSheetListener: BottomSheetListener) {
        this.bottomSheetListener = bottomSheetListener
    }


    fun hideBottomSheet() {
        bottomSheetBehavior?.state = STATE_HIDDEN
    }

    fun showBottomSheet() {
        if (bottomSheetBehavior?.state == STATE_HIDDEN) {
            bottomSheetBehavior?.state = STATE_COLLAPSED
        }
    }

    override fun onBackPressed() {
        bottomSheetBehavior?.let {
            if (it.state == STATE_EXPANDED) {
                collapseBottomSheet()
            } else {
                super.onBackPressed()
            }
        }
    }

    fun setupCastButton(mediaRouteButton: MediaRouteButton) {
        viewModel.setupCastButton(mediaRouteButton)
    }

    override fun onResume() {
        viewModel.setupCastSession()
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
        viewModel.pauseCastSession()
    }

    override fun onSongDeleted(songId: Long) {
        viewModel.onSongDeleted(songId)
    }


    private fun navigateToMediaItem(mediaId: MediaID) {
        if (getBrowseFragment(mediaId) == null) {
            val fragment = MediaItemFragment.newInstance(mediaId)
            addFragment(
                    fragment = fragment,
                    tag = mediaId.type,
                    addToBackStack = !isRootId(mediaId)
            )
        }
    }





    private fun isRootId(mediaId: MediaID) = mediaId.type == viewModel.rootMediaId.value?.type

    private fun getBrowseFragment(mediaId: MediaID): MediaItemFragment? {
        return supportFragmentManager.findFragmentByTag(mediaId.type) as MediaItemFragment?
    }
}
