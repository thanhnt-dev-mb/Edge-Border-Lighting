package com.merryblue.baseapplication.coredata.local

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.GsonBuilder
import com.merryblue.baseapplication.coredata.model.LanguageModel
import javax.inject.Inject

class AppPreferences @Inject constructor(context: Context) {
    
    companion object {
        private const val APP_PREFERENCES_NAME = "APP-NAME-Cache"
        private const val SESSION_PREFERENCES_NAME = "APP-NAME-UserCache"
        private const val MODE = Context.MODE_PRIVATE
        
        private const val KEY_CURRENT_LANGUAGE = "user_current_language"
        private const val KEY_REVIEW_TIME = "app_review_time"
        private const val KEY_RATE_STAR = "key_rate_star"
        private const val KEY_IS_SHOW_CAMERA_HOME = "show_camera_home"
        private val FIRST_TIME = Pair("FIRST_TIME", true)

        private const val KEY_SECURITY_SETTING = "user_security_setting"
        private const val KEY_PREVENT_UNINSTALL_SETTING = "user_prevent_uninstall_setting"

        private const val KEY_SERVICE_RUNNING = "key_service_is_running"
        private const val KEY_APP_OPEN_COUNT = "key_app_open_count"
        private const val KEY_APP_SELECTED_ALIAS = "key_app_selected_alias"
        private const val KEY_IS_FIRST_TUTORIAL = "key_is_first_tutorial"
    }
    
    private val appPreferences: SharedPreferences = context.getSharedPreferences(
        APP_PREFERENCES_NAME, MODE
    )
    private val sessionPreferences: SharedPreferences = context.getSharedPreferences(
        SESSION_PREFERENCES_NAME, MODE
    )
    private val gson = GsonBuilder().create()
    
    private inline fun SharedPreferences.edit(operation: (SharedPreferences.Editor) -> Unit) {
        val editor = edit()
        operation(editor)
        editor.apply()
    }
    
    var isFirstTime: Boolean
        get() {
            return appPreferences.getBoolean(FIRST_TIME.first, FIRST_TIME.second)
        }
        set(value) = appPreferences.edit {
            it.putBoolean(FIRST_TIME.first, value)
        }

    var isServiceRunning: Boolean
        get() = appPreferences.getBoolean(KEY_SERVICE_RUNNING,false)
        set(value) = appPreferences.edit {
            it.putBoolean(KEY_SERVICE_RUNNING, value)
        }
    
    var rated: Int
        get() {
            return appPreferences.getInt(KEY_RATE_STAR, 0)
        }
        set(value) = appPreferences.edit {
            it.putInt(KEY_RATE_STAR, value)
        }
    
    var reviewTime: Long
        get() = appPreferences.getLong(KEY_REVIEW_TIME, 0)
        set(value) = appPreferences.edit { it.putLong(KEY_REVIEW_TIME, value) }
    
    var currentLanguageModel: LanguageModel?
        get() = getObject(KEY_CURRENT_LANGUAGE, LanguageModel::class.java)
        set(value) = putObject(KEY_CURRENT_LANGUAGE, value)

    var isPreventUninstall: Boolean
        get() = appPreferences.getBoolean(KEY_PREVENT_UNINSTALL_SETTING, false)
        set(value) = appPreferences.edit { it.putBoolean(KEY_PREVENT_UNINSTALL_SETTING, value) }

    var openCount: Long
        get() = appPreferences.getLong(KEY_APP_OPEN_COUNT, 0)
        set(value) = appPreferences.edit { it.putLong(KEY_APP_OPEN_COUNT, value) }

    var currentAlias: String?
        get() = appPreferences.getString(KEY_APP_SELECTED_ALIAS, "")
        set(value) = appPreferences.edit { it.putString(KEY_APP_SELECTED_ALIAS, value) }

    var isFirstTutorial: Boolean
        get() = appPreferences.getBoolean(KEY_IS_FIRST_TUTORIAL, false)
        set(value) = appPreferences.edit { it.putBoolean(KEY_IS_FIRST_TUTORIAL, value) }

    fun clearPreferences() {
        sessionPreferences.edit {
            it.clear().apply()
        }
        appPreferences.edit {
            it.clear().apply()
        }
    }
    
    // Template for get/set object
    private fun <T> putObject(key: String?, value: T) {
        val editor: SharedPreferences.Editor = appPreferences.edit()
        editor.putString(key, gson.toJson(value))
        editor.apply()
    }
    
    private fun <T> getObject(key: String, clazz: Class<T>?): T? {
        val value = appPreferences.getString(key, null)
        return if (value != null) {
            try {
                return gson.fromJson(value, clazz)
            } catch (ex: Exception) {
                ex.printStackTrace()
                return null
            }
        } else {
            null
        }
    }
}