package com.merryblue.baseapplication.coredata

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.media.MediaScannerConnection
import android.net.ConnectivityManager
import android.net.Network
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.database.getStringOrNull
import com.google.gson.Gson
import com.merryblue.baseapplication.coredata.local.AppPreferences
import com.merryblue.baseapplication.coredata.model.LanguageModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.app.core.base.utils.NetworkUtil
import org.app.core.base.utils.getPath
import org.app.core.ads.openads.AdapterOpenAppManager
import org.app.core.ads.remoteconfig.CoreRemoteConfig
import org.app.core.ads.remoteconfig.config.AdsConfigure
import org.app.core.base.extensions.coroutinesIO
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepository @Inject constructor(
    private val appPreferences: AppPreferences,
    @ApplicationContext val context: Context,
    ) {
    private val defaultImageFolder = "DOC_SCAN_IMAGE"
    private var _editingUri: Uri? = null
    private var _showLock = false
    private var _onboardedLanguage = false
    private var _lockedCount = 0

    var isStartSession =  true

    var isFirstLaunch: Boolean
        get() = appPreferences.isFirstTime
        set(value) {
            appPreferences.isFirstTime = value
            appPreferences.isPreventUninstall = true
        }
    
    var rated: Int
        get() = appPreferences.rated
        set(value) { appPreferences.rated = value }

    var isPreventUninstall: Boolean
        get() = appPreferences.isPreventUninstall
        set(value) { appPreferences.isPreventUninstall = value }

    val isServiceRunning: Boolean
        get() = appPreferences.isServiceRunning

    val openCount: Long
        get() = appPreferences.openCount

    val lockedAppCount: Int
        get() = _lockedCount

    private var _isShownPremium: Boolean = false
    var isShownPremium: Boolean
        get() = _isShownPremium
        set(value) { _isShownPremium = value }

    val onboardedLanguage: Boolean
        get() = _onboardedLanguage

    var isFirstTutorial: Boolean
        get() = appPreferences.isFirstTutorial
        set(value) {
            appPreferences.isFirstTutorial = value
        }

    private var _isInternetConnected = true
    private val _networkState = MutableStateFlow(false)
    val networkState = _networkState.asStateFlow()

    init {
        val count = appPreferences.openCount
        appPreferences.openCount = count + 1

        _isInternetConnected = NetworkUtil.isNetworkConnected(context)
        _networkState.value = _isInternetConnected
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        cm?.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                if (!_isInternetConnected) {
                    _isInternetConnected = true
                    _networkState.value = true
                    AdapterOpenAppManager.instance.reloadAdsIfNeed()
                }
            }

            override fun onLost(network: Network) {
                super.onLost(network)

                if (_isInternetConnected) {
                    _isInternetConnected = false
                    _networkState.value = false
                }
            }
        })
    }

    fun clearPreferences() = appPreferences.clearPreferences()

    fun getUserLanguage() = appPreferences.currentLanguageModel

    fun setUserLanguage(data: LanguageModel) {
        _onboardedLanguage = true
        appPreferences.currentLanguageModel = data
    }
    
    fun createFile(fileName: String, relativePath: String? = null) : String {
        val defaultPath = defaultImageFolder(relativePath?.split("/")?.lastOrNull())
        if (isSdkHigherThan28()) {
            var cursor: Cursor? = null
            try {
                val newImageDetails = ContentValues().apply {
                    if (isSdkHigherThan28()) {
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    relativePath?.let {
                        put(MediaStore.Images.Media.RELATIVE_PATH, relativePath)
                    }
                }
                val imageUri = if (isSdkHigherThan28()) {
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                }
                _editingUri = context.contentResolver.insert(imageUri, newImageDetails)
                val outputStream = context.contentResolver.openOutputStream(_editingUri ?: return defaultPath)
                outputStream?.close()
                
                cursor = context.contentResolver.query(
                    _editingUri ?: return defaultPath,
                    arrayOf(MediaStore.Images.Media.DATA),
                    null,
                    null,
                    null
                )
                val columnIndex = cursor?.getColumnIndexOrThrow(MediaStore.Images.Media.DATA) ?: return defaultPath
                cursor.moveToFirst()
                
                return cursor.getStringOrNull(columnIndex) ?: defaultPath
            } catch (ex: Exception) {
                ex.printStackTrace()
            } finally {
                cursor?.close()
            }
            
            return defaultPath
        } else {
            return defaultPath
        }
    }
    
    private fun defaultImageFolder(pFolder: String? = null) : String {
        val directoryPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString()
        val folder = if (pFolder.isNullOrBlank()) "$directoryPath/$defaultImageFolder" else "$directoryPath/$pFolder"
        if (!File(folder).exists()) {
            File(folder).mkdir()
        }
        
        return folder
    }
    
    @Throws(IOException::class)
    private fun getEditedImageUri(
        fileNameToSave: String,
        newImageDetails: ContentValues,
        imageCollection: Uri
    ): Uri {
        newImageDetails.put(MediaStore.Images.Media.DISPLAY_NAME, fileNameToSave)
        val editedImageUri = context.contentResolver.insert(imageCollection, newImageDetails)
        val outputStream = context.contentResolver.openOutputStream(editedImageUri!!)
        outputStream!!.close()
        return editedImageUri
    }
    
    private fun notifyThatFileIsNowPubliclyAvailable() {
        if (isSdkHigherThan28()) {
            _editingUri?.let { uri ->
                val contentValues = ContentValues()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(uri, contentValues, null, null)
            }
        }
    }
    
    fun updateContentProvider(path: String) : String {
        val file = File(path)
        notifyThatFileIsNowPubliclyAvailable()
        
        _editingUri?.let {
            val editingPath = getPath(context, it)
            if (editingPath != null) {
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(path, editingPath),
                    arrayOf(file.name),
                    null
                )
                
                return editingPath
            } else {
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(path),
                    arrayOf(file.name),
                    null
                )
            }
        } ?: kotlin.run {
            MediaScannerConnection.scanFile(
                context,
                arrayOf(path),
                arrayOf(file.name),
                null
            )
        }
        
        _editingUri = null
        
        return path
    }

    fun setShowLock(needLock: Boolean = true) {
        _showLock = needLock
        if (needLock) {
            AdapterOpenAppManager.instance.disableOpenAds()
        } else {
            AdapterOpenAppManager.instance.enableOpenAds()
        }
    }
    
    fun loadAdsConfiguration() : AdsConfigure? {
        val rmConfig = CoreRemoteConfig.instance.adsRemoteConfig
        if (rmConfig?.active_version != null) {
            return rmConfig
        } else {
            try {
                val jsonConfig = getJsonAdsConfigure()
                val localConfig = Gson().fromJson(jsonConfig, AdsConfigure::class.java)

                return localConfig
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }

            return null
        }
    }

    fun getJsonAdsConfigure(): String {
        try {
            val ins = context.resources.openRawResource(
                context.resources.getIdentifier(
                    "config_ads_default",
                    "raw", context.packageName
                )
            )
            var text: String
            ins.bufferedReader().use {
                text = it.readText()
                it.close()
            }
            ins.close()
            return text
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            return ""
        }
    }

    companion object {
        fun isSdkHigherThan28(): Boolean {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        }
    }
}
