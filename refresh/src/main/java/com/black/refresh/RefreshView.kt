package com.black.lib.refresh

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.RelativeLayout

/**
 * Created by zhangxiaoqi on 2019/4/22.
 */
abstract class RefreshView @JvmOverloads constructor(context: Context?, attrs: AttributeSet? = null) : RelativeLayout(context, attrs), Refresh {
    /**
     * 释放即可到达二楼
     */
    abstract fun setReleaseToSecondFloor()

    /**
     * 展示二楼
     */
    abstract fun setToSecondFloor()

    /**
     * 回到一楼
     */
    abstract fun setToFirstFloor()

    init {
        gravity = Gravity.CENTER
    }
}