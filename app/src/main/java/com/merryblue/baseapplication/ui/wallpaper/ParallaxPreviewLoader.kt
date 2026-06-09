package com.merryblue.baseapplication.ui.wallpaper

import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.merryblue.baseapplication.R

class ParallaxPreviewLoader(
    private val previewView: ParallaxPreviewView,
    @DrawableRes private val placeholderRes: Int = R.drawable.placeholder_image,
) {
    private var target: CustomTarget<Drawable>? = null
    private var boundUrl: String? = null

    fun bind(url: String, motionEnabled: Boolean) {
        val nextUrl = url.takeIf { it.isNotBlank() }
        if (boundUrl != nextUrl) {
            boundUrl = nextUrl
            clearTarget()
            previewView.clearPreview()

            if (nextUrl == null) {
                previewView.setPreviewDrawable(fallbackDrawable())
            } else {
                val nextTarget = object : CustomTarget<Drawable>() {
                    override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                        previewView.setPreviewDrawable(resource)
                    }

                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        previewView.setPreviewDrawable(errorDrawable ?: fallbackDrawable())
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                        previewView.setPreviewDrawable(placeholder)
                    }
                }
                target = nextTarget
                Glide.with(previewView)
                    .load(nextUrl)
                    .placeholder(placeholderRes)
                    .error(placeholderRes)
                    .dontAnimate()
                    .apply(RequestOptions().disallowHardwareConfig())
                    .into(nextTarget)
            }
        }
        previewView.setMotionEnabled(motionEnabled)
    }

    fun setMotionEnabled(enabled: Boolean) {
        previewView.setMotionEnabled(enabled)
    }

    fun recycle() {
        clearTarget()
        boundUrl = null
        previewView.setMotionEnabled(false)
        previewView.release()
    }

    private fun clearTarget() {
        target?.let { Glide.with(previewView).clear(it) }
        target = null
    }

    private fun fallbackDrawable(): Drawable? {
        return AppCompatResources.getDrawable(previewView.context, placeholderRes)
    }
}
