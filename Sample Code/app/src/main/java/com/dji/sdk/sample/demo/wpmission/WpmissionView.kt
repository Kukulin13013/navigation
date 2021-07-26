package com.dji.sdk.sample.demo.wpmission

import android.app.Service
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.dji.frame.util.V_JsonUtil
import com.dji.sdk.sample.R
import com.dji.sdk.sample.internal.controller.DJISampleApplication
import com.dji.sdk.sample.internal.utils.Helper
import com.dji.sdk.sample.internal.utils.PopupUtils
import com.dji.sdk.sample.internal.utils.ToastUtils
import com.dji.sdk.sample.internal.view.PresentableView
import dji.common.error.DJIError
import dji.common.flightcontroller.flightassistant.PerceptionInformation
import dji.common.util.CommonCallbacks
import dji.common.util.CommonCallbacks.CompletionCallbackWith
import dji.sdk.products.Aircraft
import dji.sdk.radar.Radar
import kotlinx.coroutines.*
import java.lang.Runnable

class WpmissionView(context: Context?) : LinearLayout(context), PresentableView, View.OnClickListener {

    private lateinit var wpmissionInfoText: TextView

    init {
        initUI(context)
    }

    fun initUI(context: Context?) {
        isClickable = true
        orientation = VERTICAL
        val layoutInflater = context!!.getSystemService(Service.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        layoutInflater.inflate(R.layout.view_wpmission, this, true)
        wpmissionInfoText = findViewById(R.id.wpmission_info)
        findViewById<View>(R.id.btn_get_upward_avoidance).setOnClickListener(this)
        findViewById<View>(R.id.btn_set_upward_avoidance).setOnClickListener(this)
        findViewById<View>(R.id.btn_get_horizontal_avoidance).setOnClickListener(this)
        findViewById<View>(R.id.btn_set_horizontal_avoidance).setOnClickListener(this)
    }

    private fun ShowNoticeInfo() {
        ToastUtils.setResultToToast("navigation waypoint mission prepare")

    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_get_upward_avoidance -> ShowNoticeInfo()
            R.id.btn_set_upward_avoidance -> ShowNoticeInfo()
            R.id.btn_get_horizontal_avoidance -> ShowNoticeInfo()
            R.id.btn_set_horizontal_avoidance -> ShowNoticeInfo()
        }
    }

    override fun getDescription(): Int = R.string.component_listview_wpmission

    override fun getHint(): String = this.javaClass.simpleName
}