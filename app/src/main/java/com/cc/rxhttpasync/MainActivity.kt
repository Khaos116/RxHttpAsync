package com.cc.rxhttpasync

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ayvytr.okhttploginterceptor.LoggingInterceptor
import com.blankj.utilcode.util.DeviceUtils
import com.cc.rxhttpasync.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import rxhttp.*
import rxhttp.cc.RxHttp
import rxhttp.wrapper.cache.CacheMode

@DelicateCoroutinesApi
class MainActivity : AppCompatActivity() {
  //<editor-fold defaultstate="collapsed" desc="变量">
  private lateinit var mVB: ActivityMainBinding
  private var mCount = 0
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="初始化">
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    RxHttpPlugins.init(
      RxHttpPlugins.getOkHttpClient()
        .newBuilder()
        .addInterceptor(LoggingInterceptor(isShowAll = true))
        .build()
    )
    mVB = ActivityMainBinding.inflate(layoutInflater)
    setContentView(mVB.root)
    mVB.tvBtn.setOnClickListener {
      if (mCount % 2 == 0) asyncRequest1() else asyncRequest2()
      mCount++
    }
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="多launch并发模式">
  private fun asyncRequest1() {
    mVB.tvInfo.text = "↓↓↓↓开始并发请求1↓↓↓↓"
    GlobalScope.launch(context = Dispatchers.Main) {
      RxHttp.get("https://XXXX")
        .addAllHeader(getHeaders())
        .setCacheMode(CacheMode.ONLY_NETWORK)
        .toAwaitString()
        .awaitResult()
        .onSuccess {
          //成功回调
          mVB.tvInfo.append("\n\n\n信息1获取成功:\n$it")
        }.onFailure {
          //异常回调
          mVB.tvInfo.append("\n\n\n信息1获取失败：\n\n${it.message}")
        }
    }
    GlobalScope.launch(context = Dispatchers.Main) {
      RxHttp.get("https://XXXX")
        .addAllHeader(getHeaders())
        .add("type", "6")
        .setCacheMode(CacheMode.ONLY_NETWORK)
        .toAwaitString()
        .awaitResult()
        .onSuccess { mVB.tvInfo.append("\n\n\n信息2获取成功:\n$it") }
        .onFailure { mVB.tvInfo.append("\n\n\n信息2获取失败：\n\n${it.message}") }
    }
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="Flow模式并发请求()">
  //https://juejin.cn/post/7017604875764629540#heading-13
  private fun asyncRequest2() {
    mVB.tvInfo.text = "↓↓↓↓开始并发请求2↓↓↓↓"
    val f1 = RxHttp.get("https://XXXX")
      .addAllHeader(getHeaders())
      .setCacheMode(CacheMode.ONLY_NETWORK)
      .toFlowString()
      .catch { emit("Request1 Fail:" + it.message) }
    val f2 = RxHttp.get("https://XXXX")
      .addAllHeader(getHeaders())
      .add("type", "6")
      .setCacheMode(CacheMode.ONLY_NETWORK)
      .toFlowString()
      .catch { emit("Request1 Fail:" + it.message) }
    GlobalScope.launch(context = Dispatchers.Main) {
      combine(f1, f2) { array ->
        array.forEachIndexed { index, s ->
          mVB.tvInfo.append("\n\n\n信息${index}获取成功:\n$s")
        }
      }.collect()
    }
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="请求头">
  private fun getHeaders(): HashMap<String, String> {
    val maps: HashMap<String, String> = hashMapOf()
    maps["Content-Type"] = "application/json"
    maps["X-CHANNEL"] = "ANDROID"
    maps["X-APP-VERSION"] = "1.0.0"
    maps["x-device-id"] = DeviceUtils.getUniqueDeviceId()
    maps["X-TENANT-CODE"] = "XXXX"
    maps["LANG"] = "zh-CN"
    maps["Android-DeviceId"] = DeviceUtils.getUniqueDeviceId()
    return maps
  }
  //</editor-fold>
}