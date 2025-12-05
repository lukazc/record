package com.llfbandit.record

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.llfbandit.record.methodcall.MethodCallHandlerImpl
import com.llfbandit.record.permission.PermissionManager
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.FlutterPlugin.FlutterPluginBinding
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.embedding.engine.plugins.lifecycle.FlutterLifecycleAdapter
import io.flutter.plugin.common.MethodChannel

/**
 * RecordPlugin
 */
class RecordPlugin : FlutterPlugin, ActivityAware, DefaultLifecycleObserver {
  /// The MethodChannel that will the communication between Flutter and native Android
  private var methodChannel: MethodChannel? = null

  /// Our call handler
  private var callHandler: MethodCallHandlerImpl? = null
  private var permissionManager: PermissionManager? = null
  private var activityBinding: ActivityPluginBinding? = null

  /////////////////////////////////////////////////////////////////////////////
  /// FlutterPlugin
  override fun onAttachedToEngine(binding: FlutterPluginBinding) {
    startPlugin(binding)
  }

  override fun onDetachedFromEngine(binding: FlutterPluginBinding) {
    stopPlugin()
  }
  /// END FlutterPlugin
  /////////////////////////////////////////////////////////////////////////////

  /////////////////////////////////////////////////////////////////////////////
  /// ActivityAware
  override fun onAttachedToActivity(binding: ActivityPluginBinding) {
    activityBinding = binding

    val pm = permissionManager
    if (pm != null) {
      pm.setActivity(binding.activity)
      activityBinding?.addRequestPermissionsResultListener(pm)
    }
    
    // Add lifecycle observer to handle app backgrounding
    val lifecycle = FlutterLifecycleAdapter.getActivityLifecycle(binding)
    lifecycle.addObserver(this)
  }

  override fun onDetachedFromActivityForConfigChanges() {
    onDetachedFromActivity()
  }

  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
    onDetachedFromActivity()
    onAttachedToActivity(binding)
  }

  override fun onDetachedFromActivity() {
    val pm = permissionManager
    if (pm != null) {
      pm.setActivity(null)
      activityBinding?.removeRequestPermissionsResultListener(pm)
    }
    
    // Remove lifecycle observer
    if (activityBinding != null) {
      val lifecycle = FlutterLifecycleAdapter.getActivityLifecycle(activityBinding!!)
      lifecycle.removeObserver(this)
    }

    activityBinding = null
  }
  /// END ActivityAware
  /////////////////////////////////////////////////////////////////////////////
  
  /////////////////////////////////////////////////////////////////////////////
  /// DefaultLifecycleObserver
  override fun onStop(owner: LifecycleOwner) {
    // When app goes to background (onStop), stop all recordings to ensure
    // data is flushed to disk and not corrupted if the app is killed
    callHandler?.stopAllRecordings()
  }
  /// END DefaultLifecycleObserver
  /////////////////////////////////////////////////////////////////////////////

  private fun startPlugin(binding: FlutterPluginBinding) {
    permissionManager = PermissionManager()
    callHandler = MethodCallHandlerImpl(
      permissionManager!!,
      binding.binaryMessenger,
      binding.applicationContext
    )
    methodChannel = MethodChannel(binding.binaryMessenger, MESSAGES_CHANNEL)
    methodChannel?.setMethodCallHandler(callHandler)
  }

  private fun stopPlugin() {
    methodChannel?.setMethodCallHandler(null)
    methodChannel = null
    callHandler?.dispose()
    callHandler = null
  }

  companion object {
    const val MESSAGES_CHANNEL = "com.llfbandit.record/messages"
  }
}