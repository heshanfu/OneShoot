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

package smash.ks.com.oneshoot.widgets.customize.camera.preview

import android.annotation.TargetApi
import android.content.Context
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import smash.ks.com.oneshoot.R
import smash.ks.com.oneshoot.widgets.customize.camera.module.Preview

@TargetApi(14)
internal class TextureViewPreview(context: Context, parent: ViewGroup) : Preview() {

    private val mTextureView: TextureView

    private var mDisplayOrientation: Int = 0

    override val surface: Surface
        get() = Surface(mTextureView.surfaceTexture)

    override var surfaceTexture: SurfaceTexture? = null
        get() = mTextureView.surfaceTexture

    override val view: View
        get() = mTextureView

    override val outputClass: Class<*>
        get() = SurfaceTexture::class.java

    override val isReady: Boolean
        get() = mTextureView.surfaceTexture != null

    init {
        val view = View.inflate(context, R.layout.texture_view, parent)
        mTextureView = view.findViewById(R.id.texture_view)
        mTextureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {

            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                setSize(width, height)
                configureTransform()
                dispatchSurfaceChanged()
            }

            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
                setSize(width, height)
                configureTransform()
                dispatchSurfaceChanged()
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                setSize(0, 0)
                return true
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
        }
    }

    // This method is called only from Camera2.
    @TargetApi(15)
    override fun setBufferSize(width: Int, height: Int) {
        mTextureView.surfaceTexture.setDefaultBufferSize(width, height)
    }

    override fun setDisplayOrientation(displayOrientation: Int) {
        mDisplayOrientation = displayOrientation
        configureTransform()
    }

    /**
     * Configures the transform matrix for TextureView based on [.mDisplayOrientation] and
     * the surface size.
     */
    fun configureTransform() {
        val matrix = Matrix()

        if (mDisplayOrientation % 180 == 90) {
            val width = this.width
            val height = this.height

            // Rotate the camera preview when the screen is landscape.
            matrix.setPolyToPoly(
                floatArrayOf(0f, 0f, // top left
                             width.toFloat(), 0f, // top right
                             0f, height.toFloat(), // bottom left
                             width.toFloat(), height.toFloat())// bottom right
                , 0,
                if (mDisplayOrientation == 90)
                // Clockwise
                    floatArrayOf(0f, height.toFloat(), // top left
                                 0f, 0f, // top right
                                 width.toFloat(), height.toFloat(), // bottom left
                                 width.toFloat(), 0f)// bottom right
                else
                // mDisplayOrientation == 270
                // Counter-clockwise
                    floatArrayOf(width.toFloat(), 0f, // top left
                                 width.toFloat(), height.toFloat(), // top right
                                 0f, 0f, // bottom left
                                 0f, height.toFloat())// bottom right
                , 0,
                4)
        }
        else if (mDisplayOrientation == 180) {
            matrix.postRotate(180f, (width / 2).toFloat(), (height / 2).toFloat())
        }
        mTextureView.setTransform(matrix)
    }
}