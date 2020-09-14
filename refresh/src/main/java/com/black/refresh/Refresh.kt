package com.black.lib.refresh

/**
 * Created by zhangxiaoqi on 2019/4/22.
 */
interface Refresh {
    /**
     * 手指拖动中
     *
     * @param height        显示出来的区域高度
     * @param refreshHeight 下拉到触发刷新位置的高度
     * @param totalHeight   总的显示区域高度
     */
    fun setHeight(height: Float, refreshHeight: Float, totalHeight: Float)

    /**
     * 触发刷新
     */
    fun setRefresh()

    /**
     * 下拉刷新
     */
    fun setPullToRefresh()

    /**
     * 释放即可刷新
     */
    fun setReleaseToRefresh()
}