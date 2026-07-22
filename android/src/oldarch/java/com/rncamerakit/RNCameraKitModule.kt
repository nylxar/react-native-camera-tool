package com.rncamerakit

import android.app.Activity
import android.content.Intent
import android.net.Uri
import com.facebook.react.bridge.*
import com.facebook.react.uimanager.UIManagerHelper

class RNCameraKitModule(private val reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext), ActivityEventListener {

    companion object {
        const val PORTRAIT = 0
        const val LANDSCAPE_LEFT = 1
        const val PORTRAIT_UPSIDE_DOWN = 2
        const val LANDSCAPE_RIGHT = 3

        const val REACT_CLASS = "RNCameraKitModule"
        private const val PICK_IMAGE_REQUEST = 1001
    }

    private var pickAndScanPromise: Promise? = null
    private var pickAndScanOptions: ReadableMap? = null

    init {
        reactContext.addActivityEventListener(this)
    }

    override fun getName(): String {
        return REACT_CLASS
    }

    override fun getConstants(): Map<String, Any> {
        return hashMapOf(
            "PORTRAIT" to PORTRAIT,
            "PORTRAIT_UPSIDE_DOWN" to PORTRAIT_UPSIDE_DOWN,
            "LANDSCAPE_LEFT" to LANDSCAPE_LEFT,
            "LANDSCAPE_RIGHT" to LANDSCAPE_RIGHT
        )
    }

    fun requestDeviceCameraAuthorization(promise: Promise?) = Unit

    fun checkDeviceCameraAuthorizationStatus(promise: Promise?) = Unit

    @ReactMethod
    fun capture(options: ReadableMap?, tag: Double?, promise: Promise) {
        val viewTag = tag?.toInt()
        if (viewTag != null && options != null) {
            val uiManager = UIManagerHelper.getUIManagerForReactTag(reactContext, viewTag)
            reactContext.runOnUiQueueThread {
                val camera = uiManager?.resolveView(viewTag) as CKCamera
                val optionsMap = options.toHashMap()
                    .mapValues { (_, value) ->
                        when (value) {
                            is ReadableMap -> value.toHashMap()
                            is ReadableArray -> value.toArrayList()
                            else -> value
                        }
                    }
                    .mapNotNull { (key, value) ->
                        if (value != null) key to value else null
                    }
                    .toMap()
                camera.capture(optionsMap, promise)
            }
        } else {
            promise.reject("E_CAPTURE_FAILED", "options or/and tag arguments are null, options: $options, tag: $viewTag")
        }
    }

    @ReactMethod
    fun scanFromUri(uri: String, options: ReadableMap?, promise: Promise) {
        try {
            val imageUri = Uri.parse(uri)
            val allowedTypes = options?.getArray("allowedBarcodeTypes")?.let { array ->
                (0 until array.size()).mapNotNull { array.getString(it) }
            }

            ImageScanner.scanFromUri(reactContext, imageUri, allowedTypes) { results ->
                promise.resolve(resultsToWritableArray(results))
            }
        } catch (e: Exception) {
            promise.reject("E_SCAN_FAILED", e.message, e)
        }
    }

    @ReactMethod
    fun pickAndScan(options: ReadableMap?, promise: Promise) {
        pickAndScanPromise = promise
        pickAndScanOptions = options

        val activity = currentActivity
        if (activity == null) {
            promise.reject("E_NO_ACTIVITY", "No current activity found")
            return
        }

        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        activity.startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(activity: Activity?, requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != PICK_IMAGE_REQUEST) return

        val promise = pickAndScanPromise
        pickAndScanPromise = null

        if (resultCode == Activity.RESULT_CANCELED || data?.data == null) {
            promise?.reject("E_PICKER_CANCELLED", "Image picker was cancelled")
            return
        }

        val uri = data.data ?: run {
            promise?.reject("E_PICKER_CANCELLED", "Image picker was cancelled")
            return
        }

        val allowedTypes = pickAndScanOptions?.getArray("allowedBarcodeTypes")?.let { array ->
            (0 until array.size()).mapNotNull { array.getString(it) }
        }

        ImageScanner.scanFromUri(reactContext, uri, allowedTypes) { results ->
            promise?.resolve(resultsToWritableArray(results))
        }
    }

    override fun onNewIntent(intent: Intent?) {}

    private fun resultsToWritableArray(results: List<Map<String, Any>>): WritableArray {
        val writableArray = Arguments.createArray()
        results.forEach { result ->
            val map = Arguments.createMap()
            map.putString("codeStringValue", result["codeStringValue"] as? String)
            map.putString("codeFormat", result["codeFormat"] as? String)
            map.putString("displayValue", result["displayValue"] as? String)

            @Suppress("UNCHECKED_CAST")
            val boundingBox = result["boundingBox"] as? Map<String, Any>
            if (boundingBox != null) {
                val boxMap = Arguments.createMap()
                boxMap.putDouble("x", (boundingBox["x"] as? Number)?.toDouble() ?: 0.0)
                boxMap.putDouble("y", (boundingBox["y"] as? Number)?.toDouble() ?: 0.0)
                boxMap.putDouble("width", (boundingBox["width"] as? Number)?.toDouble() ?: 0.0)
                boxMap.putDouble("height", (boundingBox["height"] as? Number)?.toDouble() ?: 0.0)
                map.putMap("boundingBox", boxMap)
            }

            writableArray.pushMap(map)
        }
        return writableArray
    }
}
