/*
 * Copyright (C) 2018 The Smash Ks Open Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package smash.ks.com.oneshoot.widgets.customize.camera.view

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.support.annotation.IntDef
import android.support.v4.view.ViewCompat
import android.util.AttributeSet
import android.view.View
import android.view.View.MeasureSpec.AT_MOST
import android.view.View.MeasureSpec.EXACTLY
import android.view.View.MeasureSpec.getMode
import android.view.View.MeasureSpec.getSize
import android.view.View.MeasureSpec.makeMeasureSpec
import android.widget.FrameLayout
import smash.ks.com.oneshoot.R
import smash.ks.com.oneshoot.widgets.customize.camera.Camera2
import smash.ks.com.oneshoot.widgets.customize.camera.Camera2Api23
import smash.ks.com.oneshoot.widgets.customize.camera.module.AspectRatio
import smash.ks.com.oneshoot.widgets.customize.camera.module.CameraViewModule
import smash.ks.com.oneshoot.widgets.customize.camera.module.Constants
import smash.ks.com.oneshoot.widgets.customize.camera.module.Constants.FACING_BACK
import smash.ks.com.oneshoot.widgets.customize.camera.module.Constants.FACING_FRONT
import smash.ks.com.oneshoot.widgets.customize.camera.module.Constants.FLASH_AUTO
import smash.ks.com.oneshoot.widgets.customize.camera.module.Constants.FLASH_OFF
import smash.ks.com.oneshoot.widgets.customize.camera.module.Constants.FLASH_ON
import smash.ks.com.oneshoot.widgets.customize.camera.module.Constants.FLASH_RED_EYE
import smash.ks.com.oneshoot.widgets.customize.camera.module.Constants.FLASH_TORCH
import smash.ks.com.oneshoot.widgets.customize.camera.preview.SurfaceViewPreview
import smash.ks.com.oneshoot.widgets.customize.camera.preview.TextureViewPreview

open class CameraView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    /** Direction the camera faces relative to device screen.  */
    @IntDef(FACING_BACK, FACING_FRONT)
    @Retention(AnnotationRetention.SOURCE)
    annotation class Facing

    /** The mode for for the camera device's flash control  */
    @IntDef(FLASH_OFF, FLASH_ON, FLASH_TORCH, FLASH_AUTO, FLASH_RED_EYE)
    annotation class Flash

    var mImpl: CameraViewModule
    private var callbacks: CallbackBridge?
    private var adjustViewBounds: Boolean = false
    private var displayOrientationDetector: DisplayOrientationDetector?

    init {
        if (isInEditMode) {
            callbacks = null
            displayOrientationDetector = null
        }
        //  setup
        val preview = createPreview(context)
        callbacks = CallbackBridge()
        mImpl = if (Build.VERSION.SDK_INT < 23) {
            Camera2(callbacks, preview, context)
        }
        else {
            Camera2Api23(callbacks, preview, context)
        }
        // Attributes
        val a = context.obtainStyledAttributes(attrs, R.styleable.CameraView, defStyleAttr,
                                               R.style.Widget_CameraView)
        adjustViewBounds = a.getBoolean(R.styleable.CameraView_android_adjustViewBounds, false)
        setFacing(a.getInt(R.styleable.CameraView_facing, FACING_BACK))
        val aspectRatio = a.getString(R.styleable.CameraView_aspectRatio)
        if (aspectRatio != null) {
            setAspectRatio(AspectRatio.parse(aspectRatio))
        }
        else {
            setAspectRatio(Constants.DEFAULT_ASPECT_RATIO)
        }
        setAutoFocus(a.getBoolean(R.styleable.CameraView_autoFocus, true))
        setFlash(a.getInt(R.styleable.CameraView_flash, Constants.FLASH_AUTO))
        a.recycle()
        // Display orientation detector
        displayOrientationDetector = object : DisplayOrientationDetector(context) {
            override fun onDisplayOrientationChanged(displayOrientation: Int) {
                mImpl.setDisplayOrientation(displayOrientation)
            }
        }
    }

    private fun createPreview(context: Context) = if (Build.VERSION.SDK_INT < 14) {
        SurfaceViewPreview(context, this)
    }
    else {
        TextureViewPreview(context, this)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        if (!isInEditMode) {
            displayOrientationDetector?.enable(ViewCompat.getDisplay(this))
        }
    }

    override fun onDetachedFromWindow() {
        if (!isInEditMode) {
            displayOrientationDetector?.disable()
        }
        super.onDetachedFromWindow()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (isInEditMode) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            return
        }

        // Handle android:adjustViewBounds
        if (adjustViewBounds) {
            if (!isCameraOpened()) {
                callbacks?.reserveRequestLayoutOnOpen()
                super.onMeasure(widthMeasureSpec, heightMeasureSpec)
                return
            }
            val widthMode = getMode(widthMeasureSpec)
            val heightMode = getMode(heightMeasureSpec)

            if (widthMode == EXACTLY && heightMode != EXACTLY) {
                val ratio = getAspectRatio()
                var height = (getSize(widthMeasureSpec) * ratio!!.toFloat()).toInt()

                if (heightMode == AT_MOST) {
                    height = Math.min(height, getSize(heightMeasureSpec))
                }

                super.onMeasure(widthMeasureSpec, makeMeasureSpec(height, EXACTLY))
            }
            else if (widthMode != EXACTLY && heightMode == EXACTLY) {
                val ratio = getAspectRatio()
                var width = (getSize(heightMeasureSpec) * ratio!!.toFloat()).toInt()
                if (widthMode == AT_MOST) {
                    width = Math.min(width, getSize(widthMeasureSpec))
                }

                super.onMeasure(makeMeasureSpec(width, EXACTLY), heightMeasureSpec)
            }
            else {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            }
        }
        else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }

        // Measure the TextureView
        val width = measuredWidth
        val height = measuredHeight
        var ratio = getAspectRatio()

        if (displayOrientationDetector!!.lastKnownDisplayOrientation % 180 === 0) {
            ratio = ratio!!.inverse()
        }
        if (null == ratio) throw Exception("")
        if (height < width * ratio.y / ratio.x) {
            mImpl.view.measure(
                makeMeasureSpec(width, EXACTLY),
                makeMeasureSpec(width * ratio.y / ratio.x, EXACTLY))
        }
        else {
            mImpl.view.measure(
                makeMeasureSpec(height * ratio.x / ratio.y, EXACTLY),
                makeMeasureSpec(height, EXACTLY))
        }
    }

    @SuppressLint("WrongConstant")
    override fun onSaveInstanceState(): Parcelable? {
        val state = SavedState(super.onSaveInstanceState())
        state.facing = getFacing()
        state.ratio = getAspectRatio()
        state.autoFocus = getAutoFocus()
        state.flash = getFlash()
        return state
    }

    override fun onRestoreInstanceState(state: Parcelable?) = state.let { it as? SavedState }?.let {
        super.onRestoreInstanceState(it.superState)
        setFacing(it.facing)
        setAspectRatio(it.ratio)
        setAutoFocus(it.autoFocus)
        setFlash(it.flash)
    } ?: super.onRestoreInstanceState(state)

    /**
     * Open a camera device and start showing camera preview. This is typically called from
     * [Activity.onResume].
     */
    fun start() {
        if (!mImpl.start()) {
            //store the state ,and restore this state after fall back o Camera1
            val state = onSaveInstanceState()
            // Camera2 uses legacy hardware layer; fall back to Camera1
//            mImpl = Camera1(callbacks, createPreview(context))
            onRestoreInstanceState(state)
            mImpl.start()
        }
    }

    /**
     * Stop camera preview and close the device. This is typically called from
     * [Activity.onPause].
     */
    open fun stop() {
        mImpl.stop()
    }

    /**
     * @return `true` if the camera is opened.
     */
    fun isCameraOpened() = mImpl.isCameraOpened

    /**
     * Add a new callback.
     *
     * @param callback The [Callback] to add.
     * @see .removeCallback
     */
    fun addCallback(callback: Callback) = callbacks?.add(callback)

    /**
     * Remove a callback.
     *
     * @param callback The [Callback] to remove.
     * @see .addCallback
     */
    fun removeCallback(callback: Callback) = callbacks?.remove(callback)

    /**
     * @param adjustViewBounds `true` if you want the CameraView to adjust its bounds to
     * preserve the aspect ratio of camera.
     * @see .getAdjustViewBounds
     */
    fun setAdjustViewBounds(adjustViewBounds: Boolean) {
        if (this.adjustViewBounds != adjustViewBounds) {
            this.adjustViewBounds = adjustViewBounds
            requestLayout()
        }
    }

    /**
     * @return True when this CameraView is adjusting its bounds to preserve the aspect ratio of
     * camera.
     * @see .setAdjustViewBounds
     */
    fun getAdjustViewBounds(): Boolean = adjustViewBounds

    /**
     * Chooses camera by the direction it faces.
     *
     * @param facing The camera facing. Must be either [.FACING_BACK] or
     * [.FACING_FRONT].
     */
    fun setFacing(@Facing facing: Int) {
        mImpl.facing = facing
    }

    /**
     * Gets the direction that the current camera faces.
     *
     * @return The camera facing.
     */
    @Facing
    fun getFacing() = mImpl.facing

    /**
     * Gets all the aspect ratios supported by the current camera.
     */
    fun getSupportedAspectRatios() = mImpl.supportedAspectRatios

    /**
     * Sets the aspect ratio of camera.
     *
     * @param ratio The [AspectRatio] to be set.
     */
    fun setAspectRatio(ratio: AspectRatio?) {
        if (null != ratio && mImpl.setAspectRatio(ratio)) {
            requestLayout()
        }

    }

    /**
     * Gets the current aspect ratio of camera.
     *
     * @return The current [AspectRatio]. Can be `null` if no camera is opened yet.
     */
    fun getAspectRatio() = mImpl.aspectRatio

    /**
     * Enables or disables the continuous auto-focus mode. When the current camera doesn't support
     * auto-focus, calling this method will be ignored.
     *
     * @param autoFocus `true` to enable continuous auto-focus mode. `false` to
     * disable it.
     */
    fun setAutoFocus(autoFocus: Boolean) {
        mImpl.autoFocus = autoFocus
    }

    /**
     * Returns whether the continuous auto-focus mode is enabled.
     *
     * @return `true` if the continuous auto-focus mode is enabled. `false` if it is
     * disabled, or if it is not supported by the current camera.
     */
    fun getAutoFocus(): Boolean = mImpl.autoFocus

    /**
     * Sets the flash mode.
     *
     * @param flash The desired flash mode.
     */
    fun setFlash(@Flash flash: Int) {
        mImpl.flash = flash
    }

    /**
     * Gets the current flash mode.
     *
     * @return The current flash mode.
     */
    @Flash
    fun getFlash() = mImpl.flash

    /**
     * Take a picture. The result will be returned to
     * [Callback.onPictureTaken].
     */
    fun takePicture() {
        mImpl.takePicture()
    }

    private inner class CallbackBridge : CameraViewModule.Callback {
        private val callbacks by lazy { ArrayList<Callback>() }
        private var requestLayoutOnOpen = false

        fun add(callback: Callback) = callbacks.add(callback)

        fun remove(callback: Callback) = callbacks.remove(callback)

        override fun onCameraOpened() {
            if (requestLayoutOnOpen) {
                requestLayoutOnOpen = false
                requestLayout()
            }
            for (callback in callbacks) callback.onCameraOpened(this@CameraView)
        }

        override fun onCameraClosed() {
            for (callback in callbacks) callback.onCameraClosed(this@CameraView)
        }

        override fun onPictureTaken(data: ByteArray) {
            for (callback in callbacks) callback.onPictureTaken(this@CameraView, data)
        }

        fun reserveRequestLayoutOnOpen() {
            requestLayoutOnOpen = true
        }
    }

    protected class SavedState : View.BaseSavedState {
        @Facing
        var facing: Int = 0
        var ratio: AspectRatio? = null
        var autoFocus: Boolean = false
        @Flash
        var flash: Int = 0

        constructor(source: Parcel, loader: ClassLoader) : super(source) {
            facing = source.readInt()
            ratio = source.readParcelable(loader)
            autoFocus = source.readByte().toInt() != 0
            flash = source.readInt()
        }

        constructor(superState: Parcelable) : super(superState)

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeInt(facing)
            out.writeParcelable(ratio, 0)
            out.writeByte((if (autoFocus) 1 else 0).toByte())
            out.writeInt(flash)
        }
    }

    /**
     * Callback for monitoring events about [CameraView].
     */
    abstract class Callback {
        /**
         * Called when camera is opened.
         *
         * @param cameraView The associated [CameraView].
         */
        fun onCameraOpened(cameraView: CameraView) {}

        /**
         * Called when camera is closed.
         *
         * @param cameraView The associated [CameraView].
         */
        fun onCameraClosed(cameraView: CameraView) {}

        /**
         * Called when a picture is taken.
         *
         * @param cameraView The associated [CameraView].
         * @param data       JPEG data.
         */
        fun onPictureTaken(cameraView: CameraView, data: ByteArray) {}
    }
}