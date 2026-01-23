package com.merryblue.baseapplication.ui.wallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.widget.Button
import android.widget.FrameLayout
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.viewModels
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.databinding.ActivityRippleWallpaperSettingsBinding
import com.merryblue.baseapplication.helpers.ripple.PreviewGLRenderer
import com.merryblue.baseapplication.helpers.ripple.WaterCore
import com.merryblue.baseapplication.helpers.ripple.WaterRenderer
import com.merryblue.baseapplication.service.RippleWallpaperService
import com.merryblue.baseapplication.ui.home.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import okhttp3.OkHttpClient
import okhttp3.Request
import org.app.core.base.BaseActivity
import kotlin.concurrent.thread
import kotlin.getValue

@AndroidEntryPoint
class RippleWallpaperSettingsActivity : AppCompatActivity() {
    private val homeViewModel: HomeViewModel by viewModels()
    private lateinit var glView: GLSurfaceView
    private val core = WaterCore()
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableImmersiveFullscreen()

        glView = GLSurfaceView(this).apply {
            setEGLContextClientVersion(3)
            preserveEGLContextOnPause = true
            setRenderer(PreviewGLRenderer(core))
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY

            setOnTouchListener { v, e ->
                if (e.actionMasked == MotionEvent.ACTION_DOWN || e.actionMasked == MotionEvent.ACTION_MOVE) {
                    val nx = e.x / v.width.toFloat()
                    val ny = 1f - (e.y / v.height.toFloat())
                    queueEvent { core.addDrop(nx, ny, strength = 0.65f, radius = 0.025f) }
                }
                true
            }
        }

        val btn = Button(this).apply {
            text = "Set Live Wallpaper"
            setOnClickListener { openSetLiveWallpaper() }
        }

        val root = FrameLayout(this).apply {
            addView(glView, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ))
            addView(btn, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                bottomMargin = (24 * resources.displayMetrics.density).toInt()
            })
        }

        setContentView(root)

        // Load bitmap from URL -> upload to GL thread
        loadBitmapFromUrl(homeViewModel.rippleEffectUrl) { bmp ->
            glView.queueEvent {
                core.updateBaseBitmap(bmp)
                // texture đã upload -> recycle để tránh giữ RAM
            }
        }
    }

    private fun enableImmersiveFullscreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun openSetLiveWallpaper() {
        val cn = ComponentName(this, RippleWallpaperService::class.java)
        val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
            putExtra(android.app.WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, cn)
        }
        startActivity(intent)
    }

    private fun loadBitmapFromUrl(url: String, onDone: (Bitmap) -> Unit) {
        thread {
            val req = Request.Builder().url(url).build()
            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) { resp.close(); return@thread }
            val bytes = resp.body?.bytes() ?: run { resp.close(); return@thread }
            resp.close()

            // decode
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@thread
            onDone(bmp)
        }
    }

    override fun onResume() {
        super.onResume()
        glView.onResume()
        enableImmersiveFullscreen()
    }

    override fun onPause() {
        glView.onPause()
        super.onPause()
    }

}