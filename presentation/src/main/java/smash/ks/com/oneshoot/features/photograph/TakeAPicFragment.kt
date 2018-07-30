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

package smash.ks.com.oneshoot.features.photograph

import android.Manifest.permission.CAMERA
import android.animation.ObjectAnimator
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat.PNG
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.KeyEvent.KEYCODE_BACK
import android.widget.Toast.LENGTH_SHORT
import android.widget.Toast.makeText
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.core.graphics.scale
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.navigation.findNavController
import com.devrapid.dialogbuilder.support.QuickDialogFragment
import com.devrapid.kotlinknifer.visible
import kotlinx.android.synthetic.main.dialog_fragment_options.view.ib_analyze
import kotlinx.android.synthetic.main.dialog_fragment_options.view.ib_upload
import kotlinx.android.synthetic.main.dialog_fragment_options.view.iv_snippet
import kotlinx.android.synthetic.main.fragment_take_a_pic.cv_camera
import kotlinx.android.synthetic.main.fragment_take_a_pic.ib_flash
import kotlinx.android.synthetic.main.fragment_take_a_pic.ib_shot
import kotlinx.android.synthetic.main.fragment_take_a_pic.iv_preview
import kotlinx.android.synthetic.main.fragment_take_a_pic.sav_selection
import kotlinx.android.synthetic.main.fragment_take_a_pic.v_flash
import kotlinx.android.synthetic.main.fragment_take_a_pic.view.ib_flash
import kotlinx.coroutines.experimental.delay
import org.jetbrains.anko.collections.forEachWithIndex
import org.jetbrains.anko.sdk25.coroutines.onClick
import smash.ks.com.ext.const.Constant.CAMERA_QUILITY
import smash.ks.com.ext.const.Constant.DEBOUNCE_DELAY
import smash.ks.com.ext.const.DEFAULT_INT
import smash.ks.com.oneshoot.R
import smash.ks.com.oneshoot.bases.AdvFragment
import smash.ks.com.oneshoot.ext.image.glide.loadByAny
import smash.ks.com.oneshoot.ext.resource.gStrings
import smash.ks.com.oneshoot.features.fake.FakeFragment.Parameter.REQUEST_CAMERA_PERMISSION
import smash.ks.com.oneshoot.features.photograph.AnalyzeFragment.Parameter.ARG_IMAGE_DATA
import smash.ks.com.oneshoot.widgets.customize.camera.module.Constants.FLASH_AUTO
import smash.ks.com.oneshoot.widgets.customize.camera.module.Constants.FLASH_OFF
import smash.ks.com.oneshoot.widgets.customize.camera.module.Constants.FLASH_ON
import smash.ks.com.oneshoot.widgets.customize.camera.view.CameraView
import smash.ks.com.oneshoot.widgets.customize.camera.view.CameraView.Flash
import java.io.ByteArrayOutputStream
import kotlinx.android.synthetic.main.dialog_fragment_options.view.ib_close as option_close

class TakeAPicFragment : AdvFragment<PhotographActivity, TakeAPicViewModel>() {
    //region Static parameters
    companion object Parameter {
        private const val INPUT_SIZE = 224
        private const val FLASH_DURATION = 250L
    }
    //endregion

    //region *** Private Variable ***
    private lateinit var byteArrayPhoto: ByteArray
    private var selectionDialog: QuickDialogFragment? = null
    private var shotDebounce = false
    private val flashCycle by lazy {
        listOf(FLASH_OFF to R.drawable.ic_flash_off,
               FLASH_ON to R.drawable.ic_flash_on,
               FLASH_AUTO to R.drawable.ic_flash_auto)
    }
    private val cameraCallback by lazy {
        object : CameraView.Callback() {
            override fun onPictureTaken(cameraView: CameraView, data: ByteArray) {
                BitmapFactory.decodeByteArray(data, 0, data.size).also { bmp ->
                    selectedRectF.apply {
                        // Round the x, y, width, and height for avoiding the range is over than bitmap size.
                        lateinit var cropBitmap: Bitmap
                        var roundWidth = (x + w).let { if (it > bmp.width) bmp.width - x else w }
                        var roundHeight = (y + h).let { if (it > bmp.height) bmp.height - y else h }
                        val roundX = x.takeIf { 0 < it } ?: let { roundWidth = w + x; 0 }
                        val roundY = y.takeIf { 0 < it } ?: let { roundHeight = h + y; 0 }
                        val bitmap = Bitmap.createBitmap(bmp, roundX, roundY, roundWidth, roundHeight)

                        // Show the image into the view.
                        bitmap.scale(INPUT_SIZE, INPUT_SIZE, false).apply {
                            cropBitmap = this
                            iv_preview.loadByAny(this)
                            val stream = ByteArrayOutputStream()
                            compress(PNG, CAMERA_QUILITY, stream)
                            byteArrayPhoto = stream.toByteArray()
                        }

//                        ui { vm.analyzeImage(byteArray) }
                        showSelectionDialog(cropBitmap)
                    }
                }.recycle()
            }
        }
    }
    private val selectedRectF = object {
        var x = 0
        var y = 0
        var w = 0
        var h = 0
    }
    //endregion

    //region Fragment Lifecycle
    override fun onResume() {
        super.onResume()

        // Request the authority of the camera.
        when {
            checkSelfPermission(parent, CAMERA) == PERMISSION_GRANTED -> cv_camera.start()
            shouldShowRequestPermissionRationale(CAMERA) -> QuickDialogFragment.Builder(this) {
                message = gStrings(R.string.camera_permission_confirmation)
                btnPositiveText = "Ok" to { _ ->
                    requestPermissions(parent, arrayOf(CAMERA), REQUEST_CAMERA_PERMISSION)
                }
                btnNegativeText = "Deny" to { _ ->
                    makeText(parent, gStrings(R.string.camera_permission_not_granted), LENGTH_SHORT).show()
                }
            }.build().show()
            else -> requestPermissions(parent, arrayOf(CAMERA), REQUEST_CAMERA_PERMISSION)
        }
    }

    override fun onPause() {
        super.onPause()

        cv_camera.stop()
    }

    override fun onDestroy() {
        super.onDestroy()

        selectionDialog?.takeIf { it.isVisible }?.dismiss()
        selectionDialog = null
    }
    //endregion

    //region Base Fragment
    override fun rendered(savedInstanceState: Bundle?) {
        cv_camera.apply { cameraCallback.takeUnless(::hasCallback)?.let(::addCallback) }
        ib_shot.onClick {
            if (!shotDebounce) {
                shotDebounce = true
                cv_camera.takePicture()
                makeCameraFlashEffecting()
            }
        }
        ib_flash.apply {
            currentFlashState()?.second?.let(::setImageResource)
            onClick {
                val state = nextFlashState()

                cv_camera.setFlash(state.first)
                ib_flash.setImageResource(state.second)
            }
        }
        sav_selection.selectedAreaCallback = { x, y, w, h ->
            selectedRectF.x = x
            selectedRectF.y = y
            selectedRectF.w = w
            selectedRectF.h = h
        }
    }

    override fun provideInflateView() = R.layout.fragment_take_a_pic
    //endregion

    //region Showing From ViewModel
    private fun showSelectionDialog(bitmap: Bitmap) {
        selectionDialog = QuickDialogFragment.Builder(this) {
            var debouncing = false

            viewResCustom = R.layout.dialog_fragment_options
            cancelable = false
            onStartBlock = { it.dialog.window.setWindowAnimations(R.style.KsDialog) }
            fetchComponents = { v, df ->
                v.apply {
                    iv_snippet.loadByAny(bitmap)
                    option_close.onClick {
                        if (false == debouncing) {
                            debouncing = true
                            delay(DEBOUNCE_DELAY)
                            dismissOptionDialog()
                        }
                    }
                    ib_analyze.onClick {
                        dismissOptionDialog()
                        view?.findNavController()?.navigate(R.id.action_takeAPicFragment_to_analyzeFragment,
                                                            bundleOf(ARG_IMAGE_DATA to byteArrayPhoto))
                    }
                    ib_upload.onClick {
                        dismissOptionDialog()
                        view?.findNavController()?.navigate(R.id.action_takeAPicFragment_to_uploadPicFragment)
                    }
                }

                df.dialog.setOnKeyListener { _, keyCode, _ ->
                    when (keyCode) {
                        KEYCODE_BACK -> {
                            dismissOptionDialog()
                            true
                        }
                        else -> false
                    }
                }
            }
        }.build()

        selectionDialog?.takeUnless(QuickDialogFragment::isVisible)?.show()
        shotDebounce = false
    }

    private fun dismissOptionDialog() {
        selectionDialog?.dismissAllowingStateLoss()
        selectionDialog = null
    }
    //endregion

    //region Camera Effective
    private fun makeCameraFlashEffecting() {
        if (!v_flash.isVisible) v_flash.visible()

        ObjectAnimator.ofFloat(v_flash, "alpha", 1f, 0f)
            .apply { duration = FLASH_DURATION }
            .start()
    }

    private fun currentFlashState() = flashCycle.find { cv_camera.getFlash() == it.first }

    private fun currentFlashStateIndex(): Int {
        @Flash var currentIndex = DEFAULT_INT

        flashCycle.forEachWithIndex { index, flash ->
            if (cv_camera.getFlash() == flash.first) currentIndex = index
        }

        return currentIndex
    }

    private fun nextFlashState() = flashCycle[(currentFlashStateIndex() + 1) % flashCycle.size]
    //endregion
}
