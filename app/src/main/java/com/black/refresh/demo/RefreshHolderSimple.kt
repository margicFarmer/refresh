package com.black.refresh.demo

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.AnimationDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.black.lib.refresh.LoadView
import com.black.lib.refresh.RefreshHolder
import com.black.lib.refresh.RefreshView

class RefreshHolderSimple(context: Context) : RefreshHolder(context) {
    private var refreshView: RefreshView? = null
    private var loadView: FryingLoadView? = null
    private val mLoadingMoreText: String = context.getString(R.string.loading)
    override fun getRefreshView(): RefreshView {
        return if (refreshView != null) {
            refreshView!!
        } else {
            FryingRefreshView(context).also { refreshView = it }
        }
    }

    override fun getLoadView(): LoadView {
        return if (loadView != null) {
            loadView!!
        } else {
            FryingLoadView(context).also { loadView = it }
        }
    }

    private inner class FryingRefreshView @JvmOverloads constructor(
        context: Context?,
        attrs: AttributeSet? = null
    ) : RefreshView(context, attrs) {
        private val contentView: LinearLayout = LayoutInflater.from(context).inflate(R.layout.view_refresh_header_simple, null) as LinearLayout
        private val loadingCircleView: ImageView
        private val mAnimation: Animation

        private val maxRefreshHeight: Int

        init {
            addView(contentView)
            loadingCircleView = contentView.findViewById(R.id.loading_circle)
            mAnimation = AnimationUtils.loadAnimation(context, R.anim.anim_loading_circle)
            mAnimation.repeatCount = Animation.INFINITE
            mAnimation.repeatMode = Animation.RESTART
            loadingCircleView.animation = mAnimation
            maxRefreshHeight = -refreshHeaderViewHeight
            setPadding(0, maxRefreshHeight, 0, 0)
        }

        override fun setReleaseToSecondFloor() {}
        override fun setToSecondFloor() {}
        override fun setToFirstFloor() {}
        override fun setHeight(height: Float, refreshHeight: Float, totalHeight: Float) {
            setPadding(0, Math.min((maxRefreshHeight + height).toInt(), 0), 0, 0)
            if (height > 0) {
                changeToReleaseRefresh()
            } else {
                onEndRefreshing()
            }
        }

        override fun setRefresh() {}
        override fun setPullToRefresh() {}
        override fun setReleaseToRefresh() {}
        fun changeToReleaseRefresh() {
            loadingCircleView.startAnimation(mAnimation)
        }

        fun onEndRefreshing() {
            loadingCircleView.clearAnimation()
        }

        val refreshHeaderViewHeight: Int
            get() {
                // 测量下拉刷新控件的高度
                measure(0, 0)
                return measuredHeight ?: 0
            }
    }

    private inner class FryingLoadView @JvmOverloads constructor(
        context: Context?,
        attrs: AttributeSet? = null
    ) : LoadView(context, attrs) {

        private val maxRefreshHeight: Int

        /**
         * 底部加载更多提示控件
         */
        protected var mFooterStatusTv: TextView

        /**
         * 底部加载更多菊花控件
         */
        protected var mFooterChrysanthemumIv: ImageView

        /**
         * 底部加载更多菊花drawable
         */
        protected var mFooterChrysanthemumAd: AnimationDrawable

        init {
            val view = View.inflate(context, R.layout.view_refresh_footer_simple, null)
            view.setBackgroundColor(Color.TRANSPARENT)
            mFooterStatusTv = view.findViewById(R.id.tv_normal_refresh_footer_status)
            mFooterChrysanthemumIv = view.findViewById(R.id.iv_normal_refresh_footer_chrysanthemum)
            mFooterChrysanthemumAd = mFooterChrysanthemumIv.drawable as AnimationDrawable
            mFooterStatusTv.text = mLoadingMoreText
            maxRefreshHeight = -refreshHeaderViewHeight
            setPadding(0, 0, 0, maxRefreshHeight)
            addView(view)
        }

        override fun setHeight(height: Float, refreshHeight: Float, totalHeight: Float) {
            setPadding(0, 0, 0, Math.min((maxRefreshHeight + height).toInt(), 0))
        }

        override fun setRefresh() {
            mFooterChrysanthemumAd.start()
        }

        override fun setPullToRefresh() {
            mFooterChrysanthemumAd.start()
        }

        override fun setReleaseToRefresh() {
            mFooterChrysanthemumAd.stop()
        }

        val refreshHeaderViewHeight: Int
            get() {
                // 测量下拉刷新控件的高度
                measure(0, 0)
                return (context.resources.displayMetrics.density * 64).toInt()
            }
    }

}
