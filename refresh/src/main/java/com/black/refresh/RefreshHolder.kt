package com.black.lib.refresh

import android.content.Context
import android.view.View

abstract class RefreshHolder(protected var context: Context) {
    abstract fun getRefreshView(): RefreshView
    abstract fun getLoadView(): LoadView
    open fun setBackgroundColor() {}
    open fun setSecondFloorView(secondFloorView: View?) {}
    open fun setColorSchemeColors(colors: IntArray) {}

}