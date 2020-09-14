package com.black.lib.refresh

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.RelativeLayout

abstract class LoadView @JvmOverloads constructor(context: Context?, attrs: AttributeSet? = null) : RelativeLayout(context, attrs), Refresh {
    init {
        gravity = Gravity.CENTER
    }
}
