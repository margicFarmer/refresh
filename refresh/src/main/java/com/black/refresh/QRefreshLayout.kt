package com.black.lib.refresh

import android.animation.ValueAnimator
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.ListView
import android.widget.RelativeLayout
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.view.*
import androidx.core.widget.ListViewCompat
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs

/**
 * Created by zhangxiaoqi on 2019/4/16.
 */
class QRefreshLayout @JvmOverloads constructor(context: Context?, attrs: AttributeSet? = null) :
    ViewGroup(context, attrs), NestedScrollingParent, NestedScrollingChild {
    companion object {
        private const val N_DOWN = 1 //正常下拉
        private const val N_UP = 2 // 正常上拉
        private const val R_UP = 3 // 刷新中上拉
        private const val L_DOWN = 4 // 加载中下拉
    }

    private var viewTarget // 刷新目标
            : View? = null

    // 滑动事件相关参数
    private var lastMoveY // 上次移动到的位置y
            = 0f
    private var overScroll // 上拉和下拉的距离
            = 0f
    private var dragMode // 拖动模式
            = 0
    private val dragRate = 0.5f // 滑动速率

    // 刷新逻辑控制参数
    private var isRefreshing // 是否正在进行刷新动画
            = false
    private var isLoading // 是否正在进行加载更多动画
            = false

    private var isAnimating // 是否正在进行状态切换动画
            = false
    private var isLoadEnable // 是否可以加载更多
            = false
    private var isAutoLoad // 是否打开自动加载更多
            = false
    private var isTouchDown // 手指是否按下
            = false
    private var isPullingUp // 是否手指上滑
            = false

    /**
     * 当前是否在二楼
     *
     * @return
     */
    var isSecondFloor // 是否正在二楼
            = false
        private set
    private var isCanSecondFloor // 是否存在二楼
            = false
    private val viewRefreshContainer // 下拉刷新view容器
            : RelativeLayout?
    private val viewLoadContainer // 加载更多view容器
            : RelativeLayout?
    private var viewRefresh // 下拉刷新view
            : RefreshView? = null
    private var viewLoad // 加载更多view
            : LoadView? = null
    private var viewContentHeight = 2000 // 刷新动画内容区高度
    private var refreshMidHeight = 170 // 刷新高度，超过这个高度，松手即可刷新
    private var loadMidHeight = 170 // 加载更多高度，超过这个高度，松手即可加载更多
    private var secondFloorHeight = 500 // 二楼高度，超过这个高度，松手即可到达二楼
    private var refreshHeight = 150 // 刷新动画高度
    private var loadHeight = 110 // 加载更多动画高度
    private val animateDuration = 100 // 动画时间ms

    // nested 相关参数
    private var nestedOverScroll = 0f
    private var isNestedScrolling = false
    private val mParentScrollConsumed = IntArray(2)
    private val mParentOffsetInWindow = IntArray(2)

    // 动画
    var animatorToRefresh // 移动到刷新位置
            : ValueAnimator? = null
    var animatorToRefreshReset // 移动到刷新初始位置
            : ValueAnimator? = null
    var animatorToLoad // 移动到加载更多位置
            : ValueAnimator? = null
    var animatorToLoadReset // 移动到加载更多初始位置
            : ValueAnimator? = null
    var animatorToSecondFloor // 移动到二楼
            : ValueAnimator? = null
    var animatorToFirstFloor // 移动到一楼
            : ValueAnimator? = null
    private var refreshListener: OnRefreshListener? = null
    private var loadListener: OnLoadListener? = null
    private var listScrollListener: ListScrollListener? = null
    private var refreshHolder: RefreshHolder? = null
    private val mChildHelper: NestedScrollingChildHelper
    private val mParentHelper: NestedScrollingParentHelper

    init {
        mChildHelper = NestedScrollingChildHelper(this)
        mParentHelper = NestedScrollingParentHelper(this)
        isNestedScrollingEnabled = true
        viewRefreshContainer = RelativeLayout(context)
        viewRefreshContainer.gravity = Gravity.CENTER
        viewLoadContainer = RelativeLayout(context)
        viewLoadContainer.gravity = Gravity.CENTER
        viewLoadContainer.visibility = View.GONE
        addView(viewRefreshContainer)
        addView(viewLoadContainer)
        ensureTarget()
    }

    override fun setNestedScrollingEnabled(enabled: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            super.setNestedScrollingEnabled(enabled)
        } else {
            mChildHelper.isNestedScrollingEnabled = enabled
        }
    }

    override fun isNestedScrollingEnabled(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            super.isNestedScrollingEnabled()
        } else {
            mChildHelper.isNestedScrollingEnabled
        }
    }

    override fun startNestedScroll(axes: Int): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            super.startNestedScroll(axes)
        } else {
            mChildHelper.startNestedScroll(axes)
        }
    }

    override fun stopNestedScroll() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            super.stopNestedScroll()
        } else {
            mChildHelper.stopNestedScroll()
        }
        mChildHelper.stopNestedScroll()
    }

    override fun hasNestedScrollingParent(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            super.hasNestedScrollingParent()
        } else {
            mChildHelper.hasNestedScrollingParent()
        }
    }

    override fun dispatchNestedScroll(
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        offsetInWindow: IntArray?
    ): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            super.dispatchNestedScroll(
                dxConsumed,
                dyConsumed,
                dxUnconsumed,
                dyUnconsumed,
                offsetInWindow
            )
        } else {
            mChildHelper.dispatchNestedScroll(
                dxConsumed,
                dyConsumed,
                dxUnconsumed,
                dyUnconsumed,
                offsetInWindow
            )
        }
    }

    override fun dispatchNestedPreScroll(
        dx: Int,
        dy: Int,
        consumed: IntArray?,
        offsetInWindow: IntArray?
    ): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            super.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow)
        } else {
            mChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow)
        }
    }

    override fun dispatchNestedFling(
        velocityX: Float,
        velocityY: Float,
        consumed: Boolean
    ): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            super.dispatchNestedFling(velocityX, velocityY, consumed)
        } else {
            mChildHelper.dispatchNestedFling(velocityX, velocityY, consumed)
        }
    }

    override fun dispatchNestedPreFling(velocityX: Float, velocityY: Float): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            super.dispatchNestedPreFling(velocityX, velocityY)
        } else {
            mChildHelper.dispatchNestedPreFling(velocityX, velocityY)
        }
    }

    override fun onNestedFling(
        target: View,
        velocityX: Float,
        velocityY: Float,
        consumed: Boolean
    ): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            super.onNestedFling(target, velocityX, velocityY, consumed)
        } else {
            false
        }
    }

    override fun onNestedPreFling(target: View, velocityX: Float, velocityY: Float): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            super.onNestedPreFling(target, velocityX, velocityY)
        } else {
            false
        }
    }

    /**
     * 设置下拉到"释放即可更新"的高度（默认170px）
     *
     * @param height
     */
    fun setPullToRefreshHeight(height: Int) {
        refreshMidHeight = height
    }

    /**
     * 设置上拉到"释放即可加载更多"的高度（默认170px）
     *
     * @param height
     */
    fun setLoadToRefreshHeight(height: Int) {
        loadMidHeight = height
    }

    /**
     * 设置下拉刷新动画高度（默认150px，需要在setRefreshing之前调用）
     *
     * @param height
     */
    fun setRefreshHeight(height: Int) {
        refreshHeight = height
    }

    /**
     * 设置加载更多动画高度（默认110px）
     *
     * @param height
     */
    fun setLoadHeight(height: Int) {
        loadHeight = height
    }

    /**
     * 设置下拉到"释放到达二楼"的高度（默认500px）
     *
     * @param height
     */
    fun setPullToSecondFloorHeight(height: Int) {
        secondFloorHeight = height
    }

    /**
     * 回到一楼
     */
    fun setBackToFirstFloor() {
        animateToFirstFloor()
    }

    /**
     * 设置是否可以到达二楼
     *
     * @param isCanSecondFloor
     */
    fun setIsCanSecondFloor(isCanSecondFloor: Boolean) {
        this.isCanSecondFloor = isCanSecondFloor
    }

    /**
     * 设置二楼view，仅限于使用默认header
     *
     * @param secondFloorView
     */
    fun setSecondFloorView(secondFloorView: View?) {
        if (refreshHolder != null) {
            refreshHolder?.setSecondFloorView(secondFloorView)
        }
    }

    /**
     * 设置是否可以加载更多
     *
     * @param isEnable
     */
    fun setLoadEnable(isEnable: Boolean) {
        isLoadEnable = isEnable
        if (isLoadEnable) {
            viewLoadContainer?.visibility = View.VISIBLE
        } else {
            viewLoadContainer?.visibility = View.GONE
        }
    }

    /**
     * 设置自动加载更多开关，默认开启
     *
     * @param isAutoLoad
     */
    fun setAutoLoad(isAutoLoad: Boolean) {
        this.isAutoLoad = isAutoLoad
    }

    fun setRefreshHolder(refreshHolder: RefreshHolder) {
        this.refreshHolder = refreshHolder
        setRefreshView(refreshHolder.getRefreshView())
        setLoadView(refreshHolder.getLoadView())
    }

    /**
     * 设置下拉刷新view
     *
     * @param refreshView
     */
    fun setRefreshView(refreshView: RefreshView) {
        viewRefresh = refreshView
        viewRefreshContainer?.removeAllViews()
        viewRefreshContainer?.addView(viewRefresh)
    }

    /**
     * 设置加载更多view
     *
     * @param loadView
     */
    fun setLoadView(loadView: LoadView) {
        viewLoad = loadView
        viewLoadContainer?.removeAllViews()
        viewLoadContainer?.addView(viewLoad)
    }

    /**
     * 如果使用了默认加载动画，设置进度圈颜色资源
     *
     * @param colorResIds
     */
    fun setColorSchemeResources(@ColorRes vararg colorResIds: Int) {
        val context = context
        val colorRes = IntArray(colorResIds.size)
        for (i in 0 until colorResIds.size) {
            colorRes[i] = ContextCompat.getColor(context, colorResIds[i])
        }
        setColorSchemeColors(*colorRes)
    }

    /**
     * 如果使用了默认加载动画，设置进度圈颜色
     *
     * @param colors
     */
    fun setColorSchemeColors(@ColorInt vararg colors: Int) {
        if (refreshHolder != null) {
            refreshHolder?.setColorSchemeColors(colors)
        }
    }

    /**
     * Set the background color of the progress spinner disc.
     *
     * @param colorRes Resource id of the color.
     */
    fun setProgressBackgroundColorSchemeResource(@ColorRes colorRes: Int) {
        setProgressBackgroundColorSchemeColor(ContextCompat.getColor(context, colorRes))
    }

    /**
     * Set the background color of the progress spinner disc.
     *
     * @param color
     */
    fun setProgressBackgroundColorSchemeColor(@ColorInt color: Int) {
        if (refreshHolder != null) {
            refreshHolder?.setBackgroundColor()
        }
    }

    /**
     * 设置下拉刷新监听
     *
     * @param listener
     */
    fun setOnRefreshListener(listener: OnRefreshListener?) {
        refreshListener = listener
    }

    /**
     * 设置是否显示正在刷新
     *
     * @param refreshing
     */
    fun setRefreshing(refreshing: Boolean) {
        ensureTarget()
        if (refreshing) {
            if (isRefreshing || isLoading || dragMode != 0) return
            if (!isRefreshing) {
                animateToRefresh()
            }
        } else {
            isRefreshing = false
            if (overScroll >= 0) {
                animateToRefreshReset()
            }
        }
    }

    /**
     * 获取是否正在刷新
     *
     * @return
     */
    fun isRefreshing(): Boolean {
        return isRefreshing
    }

    /**
     * 设置是否显示正在加载更多
     *
     * @param loading
     */
    fun setLoading(loading: Boolean) {
        if (!isLoadEnable) return
        ensureTarget()
        if (loading) {
            if (isLoading || isRefreshing || dragMode != 0) return
            if (!isLoading) {
                animateToLoad()
            }
        } else {
            isLoading = false
            if (overScroll <= 0) {
                animateToLoadReset()
            }
        }
    }

    /**
     * 获取是否加载更多
     *
     * @return
     */
    fun isLoading(): Boolean {
        return isLoading
    }

    /**
     * 设置加载更多监听
     *
     * @param listener
     */
    fun setOnLoadListener(listener: OnLoadListener?) {
        loadListener = listener
        isLoadEnable = true
        setAutoLoad(true)
        viewLoadContainer?.visibility = View.VISIBLE
    }

    /**
     * 设置ListView滚动监听
     *
     * @param listener
     */
    fun setListViewScrollListener(listener: ListScrollListener?) {
        listScrollListener = listener
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val width = measuredWidth
        val height = measuredHeight
        viewContentHeight = height
        if (childCount == 0) {
            return
        }
        ensureTarget()
        if (viewTarget == null) {
            return
        }
        val child: View = viewTarget!!
        if (child.background == null) {
            child.setBackgroundColor(-0x1)
        } else {
            child.background.alpha = 255
        }
        val childLeft = paddingLeft
        val childTop = paddingTop
        val childWidth = width - paddingLeft - paddingRight
        val childHeight = height - paddingTop - paddingBottom
        child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight)
        viewRefreshContainer?.layout(0, -viewContentHeight / 2, width, viewContentHeight / 2)
        viewLoadContainer?.layout(
            0,
            height - viewContentHeight / 2,
            width,
            height + viewContentHeight / 2
        )
    }

    public override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        viewContentHeight = measuredHeight
        ensureTarget()
        if (viewTarget == null) {
            return
        }
        viewTarget?.measure(
            MeasureSpec.makeMeasureSpec(
                measuredWidth - paddingLeft - paddingRight,
                MeasureSpec.EXACTLY
            ),
            MeasureSpec.makeMeasureSpec(
                measuredHeight - paddingTop - paddingBottom,
                MeasureSpec.EXACTLY
            )
        )
        viewRefreshContainer?.measure(
            MeasureSpec.makeMeasureSpec(
                measuredWidth - paddingLeft - paddingRight,
                MeasureSpec.EXACTLY
            ),
            MeasureSpec.makeMeasureSpec(viewContentHeight, MeasureSpec.EXACTLY)
        )
        viewLoadContainer?.measure(
            MeasureSpec.makeMeasureSpec(
                measuredWidth - paddingLeft - paddingRight,
                MeasureSpec.EXACTLY
            ),
            MeasureSpec.makeMeasureSpec(viewContentHeight, MeasureSpec.EXACTLY)
        )
    }

    private fun onScroll() {
        if (!canChildScrollUp() && checkCanLoadMore() && isAutoLoad && isLoadEnable && !isLoading && isPullingUp && !isTouchDown) {
            animateToLoad()
        }
    }

    private fun ensureTarget() {
        if (viewTarget == null) {
            for (i in 0 until childCount) {
                val child = getChildAt(i)
                if (child != viewRefreshContainer && child != viewLoadContainer) {
                    viewTarget = child
                    viewTarget?.isClickable = true
                    setScrollListener()
                    break
                }
            }
        }
    }

    private fun setScrollListener() {
        if (viewTarget is ListView) {
            (viewTarget as ListView).setOnScrollListener(object : AbsListView.OnScrollListener {
                override fun onScrollStateChanged(view: AbsListView, scrollState: Int) {
                    if (listScrollListener != null) {
                        listScrollListener?.onScrollStateChanged(view, scrollState)
                    }
                }

                override fun onScroll(
                    view: AbsListView,
                    firstVisibleItem: Int,
                    visibleItemCount: Int,
                    totalItemCount: Int
                ) {
                    if (listScrollListener != null) {
                        listScrollListener?.onScroll(
                            view,
                            firstVisibleItem,
                            visibleItemCount,
                            totalItemCount
                        )
                    }
                    this@QRefreshLayout.onScroll()
                }
            })
        }
        if (viewTarget is RecyclerView) {
            (viewTarget as RecyclerView).addOnScrollListener(object :
                RecyclerView.OnScrollListener() {

                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    onScroll()
                }
            })
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (!isEnabled || isAnimating || isNestedScrolling || isSecondFloor) {
            return false
        }
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                isTouchDown = true
                lastMoveY = ev.y
                dragMode = 0
            }
            MotionEvent.ACTION_MOVE -> {
                val y = ev.y
                val yDiff = y - lastMoveY
                if (yDiff == 0f) return false
                if (yDiff < 0) isPullingUp = true
                if (yDiff > 0) { // 下拉
                    if (overScroll < 0 && isLoading) {
                        dragMode = L_DOWN
                    } else if (!canChildScrollDown()) {
                        dragMode = N_DOWN
                    }
                } else { // 上拉
                    if (overScroll > 0 && isRefreshing) {
                        dragMode = R_UP
                    } else if (!canChildScrollUp() && checkCanLoadMore()) {
                        if (isLoadEnable) {
                            dragMode = N_UP
                        }
                    }
                }
                if (dragMode != 0) {
                    lastMoveY = ev.y
                }
            }
            MotionEvent.ACTION_UP -> isTouchDown = false
        }
        return dragMode != 0
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled || isAnimating || isNestedScrolling || isSecondFloor) {
            return false
        }
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> dragMode = 0
            MotionEvent.ACTION_MOVE -> {
                val y = event.y
                overScroll += (y - lastMoveY) * dragRate
                lastMoveY = y
                when (dragMode) {
                    N_DOWN -> {
                        if (overScroll < 0) {
                            overScroll = 0f
                            viewRefreshContainer?.translationY = overScroll
                            viewTarget?.translationY = overScroll
                            return false
                        }
                        if (overScroll > viewContentHeight / 2) {
                            overScroll = viewContentHeight / 2.toFloat()
                        }
                        viewRefreshContainer?.translationY = overScroll / 2
                        viewTarget?.translationY = overScroll
                        if (!isRefreshing) {
                            viewRefresh?.setHeight(
                                overScroll,
                                refreshMidHeight.toFloat(),
                                viewContentHeight.toFloat()
                            )
                        }
                        if (!isRefreshing) {
                            if (overScroll > refreshMidHeight) {
                                if (overScroll > secondFloorHeight && isCanSecondFloor) {
                                    viewRefresh?.setReleaseToSecondFloor()
                                } else {
                                    viewRefresh?.setReleaseToRefresh()
                                }
                            } else {
                                viewRefresh?.setPullToRefresh()
                            }
                        }
                    }
                    N_UP -> if (checkCanLoadMore()) {
                        if (overScroll > 0) {
                            overScroll = 0f
                            viewLoadContainer?.translationY = overScroll
                            viewTarget?.translationY = overScroll
                            return false
                        }
                        if (abs(overScroll) > viewContentHeight / 2) {
                            overScroll = -viewContentHeight / 2.toFloat()
                        }
                        viewLoadContainer?.translationY = overScroll / 2
                        viewTarget?.translationY = overScroll
                        if (!isLoading) {
                            viewLoad?.setHeight(
                                abs(overScroll),
                                loadMidHeight.toFloat(),
                                viewContentHeight.toFloat()
                            )
                            if (overScroll < -loadMidHeight) {
                                viewLoad?.setReleaseToRefresh()
                            } else {
                                viewLoad?.setPullToRefresh()
                            }
                        }
                    }
                    R_UP -> {
                        if (overScroll < 0) {
                            overScroll = 0f
                            viewRefreshContainer?.translationY = overScroll
                            viewTarget?.translationY = overScroll
                            return false
                        }
                        viewRefreshContainer?.translationY = overScroll / 2
                        viewTarget?.translationY = overScroll
                    }
                    L_DOWN -> {
                        if (overScroll > 0) {
                            overScroll = 0f
                            viewTarget?.translationY = overScroll
                            viewLoadContainer?.translationY = overScroll
                            return false
                        }
                        viewTarget?.translationY = overScroll
                        viewLoadContainer?.translationY = overScroll / 2
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                onTouchUp()
                dragMode = 0
                return false
            }
        }
        return true
    }

    override fun requestDisallowInterceptTouchEvent(b: Boolean) {
        if (Build.VERSION.SDK_INT < 21 && viewTarget is AbsListView || viewTarget != null && !ViewCompat.isNestedScrollingEnabled(
                viewTarget!!
            )
        ) {
        } else {
            super.requestDisallowInterceptTouchEvent(b)
        }
    }

    // 处理与父view之间的关联滑动
    override fun onStartNestedScroll(child: View, target: View, nestedScrollAxes: Int): Boolean {
        return isEnabled && !isRefreshing && !isLoading && nestedScrollAxes and ViewCompat.SCROLL_AXIS_VERTICAL != 0
    }

    override fun onNestedScrollAccepted(child: View, target: View, axes: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            super.onNestedScrollAccepted(child, target, axes)
        } else {
            mParentHelper.onNestedScrollAccepted(child, target, axes)
        }
        isTouchDown = true
        startNestedScroll(axes and ViewCompat.SCROLL_AXIS_VERTICAL)
        nestedOverScroll = 0f
        isNestedScrolling = true
    }

    override fun getNestedScrollAxes(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            super.getNestedScrollAxes()
        } else {
            mParentHelper.nestedScrollAxes
        }
    }

    override fun onStopNestedScroll(target: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            super.onStopNestedScroll(target)
        } else {
            mParentHelper.onStopNestedScroll(target)
        }
        isNestedScrolling = false
        isTouchDown = false
        onTouchUp()
        nestedOverScroll = 0f
        stopNestedScroll()
    }

    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray) {
        if (dy > 0) isPullingUp = true
        if (nestedOverScroll > 0 && dy > 0 || nestedOverScroll < 0 && dy < 0) {
            nestedOverScroll -= dy.toFloat()
            consumed[1] = dy
            onNestedDraging(nestedOverScroll)
        }
        val parentConsumed = mParentScrollConsumed
        if (dispatchNestedPreScroll(dx - consumed[0], dy - consumed[1], parentConsumed, null)) {
            consumed[0] += parentConsumed[0]
            consumed[1] += parentConsumed[1]
        }
    }

    override fun onNestedScroll(
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int
    ) {
        dispatchNestedScroll(
            dxConsumed,
            dyConsumed,
            dxUnconsumed,
            dyUnconsumed,
            mParentOffsetInWindow
        )
        var dy = dyUnconsumed + mParentOffsetInWindow[1]
        if (dy > 0 && !canChildScrollUp()) {
            if (dy > 50) dy = 50
            nestedOverScroll -= dy.toFloat()
        }
        if (dy < 0 && !canChildScrollDown()) {
            if (dy < -50) dy = -50
            nestedOverScroll -= dy.toFloat()
        }
        onNestedDraging(nestedOverScroll)
    }

    private fun onNestedDraging(offset: Float) {
        overScroll = offset * dragRate * 0.7f
        if (overScroll > 0) {
            if (overScroll > viewContentHeight / 2) {
                overScroll = viewContentHeight / 2.toFloat()
            }
            viewRefreshContainer?.translationY = overScroll / 2
            viewTarget?.translationY = overScroll
            viewRefresh?.setHeight(
                overScroll,
                refreshMidHeight.toFloat(),
                viewContentHeight.toFloat()
            )
            if (overScroll > refreshMidHeight) {
                if (overScroll > secondFloorHeight && isCanSecondFloor && !isRefreshing) {
                    viewRefresh?.setReleaseToSecondFloor()
                } else {
                    viewRefresh?.setReleaseToRefresh()
                }
            } else {
                viewRefresh?.setPullToRefresh()
            }
        } else {
            if (!isLoadEnable) return
            if (!checkCanLoadMore()) return
            if (overScroll < -viewContentHeight / 2) {
                overScroll = -viewContentHeight / 2.toFloat()
            }
            viewLoadContainer?.translationY = overScroll / 2
            viewTarget?.translationY = overScroll
            viewLoad?.setHeight(
                abs(overScroll),
                loadMidHeight.toFloat(),
                viewContentHeight.toFloat()
            )
            if (overScroll < -loadMidHeight) {
                viewLoad?.setReleaseToRefresh()
            } else {
                viewLoad?.setPullToRefresh()
            }
        }
    }

    private fun onTouchUp() {
        if (overScroll == 0f) return
        if (overScroll > 0) {
            if (overScroll > refreshMidHeight) {
                if (overScroll > secondFloorHeight && isCanSecondFloor && !isRefreshing) {
                    animateToSecondFloor()
                } else {
                    animateToRefresh()
                }
            } else {
                if (!isRefreshing) {
                    animateToRefreshReset()
                }
            }
        } else {
            if (!isLoadEnable) return
            if (!checkCanLoadMore()) return
            if (overScroll < -loadMidHeight) {
                animateToLoad()
            } else {
                if (!isLoading) {
                    animateToLoadReset()
                }
            }
        }
    }

    /**
     * 动画移动到刷新位置
     */
    private fun animateToRefresh() {
        if (isAnimating) return
        isAnimating = true
        if (animatorToRefresh == null) {
            animatorToRefresh = ValueAnimator.ofFloat(abs(overScroll), refreshHeight.toFloat())
            animatorToRefresh?.addUpdateListener { animation ->
                val height = animation.animatedValue as Float
                overScroll = height
                viewRefreshContainer?.translationY = overScroll / 2
                if (!isRefreshing) {
                    viewRefresh?.setHeight(
                        overScroll,
                        refreshMidHeight.toFloat(),
                        viewContentHeight.toFloat()
                    )
                }
                viewTarget?.translationY = overScroll
                if (height == refreshHeight.toFloat()) {
                    if (!isRefreshing) {
                        viewRefresh?.setRefresh()
                        isRefreshing = true
                        if (refreshListener != null) {
                            refreshListener?.onRefresh()
                        }
                    }
                    isAnimating = false
                }
            }
            animatorToRefresh?.duration = animateDuration.toLong()
        } else {
            animatorToRefresh?.setFloatValues(abs(overScroll), refreshHeight.toFloat())
        }
        animatorToRefresh?.start()
    }

    /**
     * 动画移动到加载更多位置
     */
    private fun animateToLoad() {
        if (isAnimating) return
        if (checkCanLoadMore()) {
            isAnimating = true
            if (animatorToLoad == null) {
                animatorToLoad = ValueAnimator.ofFloat(overScroll, -loadHeight.toFloat())
                animatorToLoad?.addUpdateListener { animation ->
                    val height = animation.animatedValue as Float
                    overScroll = height
                    viewLoadContainer?.translationY = overScroll / 2
                    if (!isLoading) {
                        viewLoad?.setHeight(
                            abs(overScroll),
                            loadMidHeight.toFloat(),
                            viewContentHeight.toFloat()
                        )
                    }
                    viewTarget?.translationY = overScroll
                    if (height == -loadHeight.toFloat()) {
                        if (!isLoading) {
                            viewLoad?.setRefresh()
                            isLoading = true
                            if (loadListener != null) {
                                loadListener?.onLoad()
                            }
                        }
                        isAnimating = false
                    }
                }
                animatorToLoad?.duration = animateDuration.toLong()
            } else {
                animatorToLoad?.setFloatValues(overScroll, -loadHeight.toFloat())
            }
            animatorToLoad?.start()
            isPullingUp = false
        } else {
            isAnimating = false
        }
    }

    /**
     * 动画移动到刷新初始位置
     */
    private fun animateToRefreshReset() {
        if (overScroll == 0f) {
            isRefreshing = false
        } else {
            if (isAnimating) return
            isAnimating = true
            if (animatorToRefreshReset == null) {
                animatorToRefreshReset = ValueAnimator.ofFloat(abs(overScroll), 0f)
                animatorToRefreshReset?.addUpdateListener { animation ->
                    val height = animation.animatedValue as Float
                    overScroll = height
                    viewRefreshContainer?.translationY = overScroll / 2
                    viewRefresh?.setHeight(
                        overScroll,
                        refreshMidHeight.toFloat(),
                        viewContentHeight.toFloat()
                    )
                    viewTarget?.translationY = overScroll
                    isRefreshing = false
                    if (height == 0f) {
                        isAnimating = false
                    }
                }
                animatorToRefreshReset?.duration = animateDuration.toLong()
            } else {
                animatorToRefreshReset?.setFloatValues(abs(overScroll), 0f)
            }
            animatorToRefreshReset?.start()
        }
    }

    /**
     * 动画移动到加载更多初始位置
     */
    private fun animateToLoadReset() {
        if (overScroll == 0f) {
            isLoading = false
        } else {
            if (isAnimating) return
            isAnimating = true
            if (animatorToLoadReset == null) {
                animatorToLoadReset = ValueAnimator.ofFloat(overScroll, 0f)
                animatorToLoadReset?.addUpdateListener { animation ->
                    val height = animation.animatedValue as Float
                    overScroll = height
                    viewLoadContainer?.translationY = overScroll / 2
                    viewLoad?.setHeight(
                        abs(overScroll),
                        loadMidHeight.toFloat(),
                        viewContentHeight.toFloat()
                    )
                    viewTarget?.translationY = overScroll
                    isLoading = false
                    if (height == 0f) {
                        isAnimating = false
                    }
                }
                animatorToLoadReset?.duration = animateDuration.toLong()
            } else {
                animatorToLoadReset?.setFloatValues(overScroll, 0f)
            }
            animatorToLoadReset?.start()
        }
    }

    private fun animateToSecondFloor() {
        if (isAnimating) return
        isAnimating = true
        if (animatorToSecondFloor == null) {
            animatorToSecondFloor = ValueAnimator.ofFloat(overScroll, viewContentHeight.toFloat())
            animatorToSecondFloor?.addUpdateListener { animation ->
                val height = animation.animatedValue as Float
                overScroll = height
                viewRefreshContainer?.translationY = overScroll / 2
                viewLoadContainer?.translationY = overScroll / 2
                viewRefresh?.setHeight(
                    abs(overScroll),
                    loadMidHeight.toFloat(),
                    viewContentHeight.toFloat()
                )
                viewTarget?.translationY = overScroll
                if (height == viewContentHeight.toFloat()) {
                    isAnimating = false
                    isSecondFloor = true
                    viewRefresh?.setToSecondFloor()
                }
            }
            animatorToSecondFloor?.duration = animateDuration.toLong()
        } else {
            animatorToSecondFloor?.setFloatValues(overScroll, viewContentHeight.toFloat())
        }
        animatorToSecondFloor?.start()
    }

    private fun animateToFirstFloor() {
        if (isAnimating) return
        isAnimating = true
        if (animatorToFirstFloor == null) {
            animatorToFirstFloor = ValueAnimator.ofFloat(overScroll, 0f)
            animatorToFirstFloor?.addUpdateListener { animation ->
                val height = animation.animatedValue as Float
                overScroll = height
                viewRefreshContainer?.translationY = overScroll / 2
                viewLoadContainer?.translationY = overScroll / 2
                viewRefresh?.setHeight(
                    abs(overScroll),
                    loadMidHeight.toFloat(),
                    viewContentHeight.toFloat()
                )
                viewTarget?.translationY = overScroll
                if (height == 0f) {
                    isAnimating = false
                    isSecondFloor = false
                    viewRefresh?.setToFirstFloor()
                }
            }
        } else {
            animatorToFirstFloor?.setFloatValues(overScroll, 0f)
        }
        animatorToFirstFloor?.start()
    }

    /**
     * 全部恢复到初始位置
     */
    private fun reset() {
        if (animatorToRefresh != null) animatorToRefresh?.cancel()
        if (animatorToRefreshReset != null) animatorToRefreshReset?.cancel()
        if (animatorToLoad != null) animatorToLoad?.cancel()
        if (animatorToLoadReset != null) animatorToLoadReset?.cancel()
        if (animatorToSecondFloor != null) animatorToSecondFloor?.cancel()
        if (viewRefreshContainer != null) {
            viewRefreshContainer.translationY = 0f
        }
        if (viewLoadContainer != null) {
            viewLoadContainer.translationY = 0f
        }
        if (viewTarget != null) {
            viewTarget?.translationY = 0f
        }
    }

    /**
     * 目标是否可以向下滚动
     *
     * @return
     */
    private fun canChildScrollDown(): Boolean {
        return if (viewTarget is ListView) {
            ListViewCompat.canScrollList((viewTarget as ListView?)!!, -1)
        } else viewTarget?.canScrollVertically(-1) ?: false
    }

    /**
     * 目标是否可以向上滚动
     *
     * @return
     */
    private fun canChildScrollUp(): Boolean {
        return if (viewTarget is ListView) {
            ListViewCompat.canScrollList((viewTarget as ListView?)!!, 1)
        } else viewTarget?.canScrollVertically(1) ?: false
    }

    private fun checkCanLoadMore(): Boolean {
        return onLoadMoreCheckListener == null || onLoadMoreCheckListener?.onLoadMoreCheck() ?: false
        //        return true;
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        reset()
    }


    interface OnRefreshListener {
        fun onRefresh()
    }

    interface OnLoadListener {
        fun onLoad()
    }

    private var onLoadMoreCheckListener: OnLoadMoreCheckListener? = null
    fun setOnLoadMoreCheckListener(onLoadMoreCheckListener: OnLoadMoreCheckListener?) {
        this.onLoadMoreCheckListener = onLoadMoreCheckListener
    }

    interface OnLoadMoreCheckListener {
        fun onLoadMoreCheck(): Boolean
    }

    interface ListScrollListener {
        fun onScrollStateChanged(view: AbsListView?, scrollState: Int)
        fun onScroll(
            view: AbsListView?,
            firstVisibleItem: Int,
            visibleItemCount: Int,
            totalItemCount: Int
        )
    }
}