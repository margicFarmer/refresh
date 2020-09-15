package com.black.refresh.demo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import com.black.lib.refresh.QRefreshLayout

class MainActivity : AppCompatActivity(), QRefreshLayout.OnRefreshListener, QRefreshLayout.OnLoadListener, QRefreshLayout.OnLoadMoreCheckListener {
    var refreshLayout: QRefreshLayout? = null
    val handler = Handler()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        refreshLayout = findViewById(R.id.refresh)
        refreshLayout?.setRefreshHolder(RefreshHolderSimple(this))
        refreshLayout?.setOnRefreshListener(this)
        refreshLayout?.setOnLoadListener(this)
        refreshLayout?.setOnLoadMoreCheckListener(this)
    }

    override fun onRefresh() {
        handler.postDelayed({
            refreshLayout?.setRefreshing(false)
        }, 300)
    }

    override fun onLoad() {
        handler.postDelayed({
            refreshLayout?.setLoading(false)
        }, 300)
    }

    override fun onLoadMoreCheck(): Boolean {
        return true
    }
}