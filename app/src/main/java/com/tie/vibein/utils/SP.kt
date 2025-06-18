package com.tie.dreamsquad.utils

import android.content.Context
import android.preference.PreferenceManager

object SP {

    // Constants for SharedPreferences keys
    const val FCM_TEMP_TOKEN = "fcm_temp_token"
    const val SP_TRUE = "SP_TRUE"
    const val SP_FALSE = "SP_FALSE"
    const val USER_ID = "USER_ID"
    const val USER_MOBILE = "USER_MOBILE"
    const val FULL_NAME = "FULL_NAME"
    const val USER_NAME = "USER_NAME"
    const val LOGIN_STATUS = "LOGIN_STATUS"
    const val USER_EMAIL = "USER_EMAIL"
    const val USER_PROFILE_PIC = "USER_PROFILE_PIC"
    const val USER_ABOUT_YOU = "USER_ABOUT_YOU"
    const val USER_COUNTRY_CODE = "USER_COUNTRY_CODE"
    const val USER_COUNTRY_SHORT_NAME = "USER_COUNTRY_SHORT_NAME"
    const val USER_IS_VERIFIED = "USER_IS_VERIFIED"
    const val USER_STATUS = "USER_STATUS"
    const val USER_DELETED = "USER_DELETED"
    const val USER_CREATED_AT = "USER_CREATED_AT"
    const val USER_INTEREST_NAMES = "USER_INTEREST_NAMES"
    const val CACHED_CITY = "CACHED_CITY"
    const val CACHED_COUNTRY = "CACHED_COUNTRY"
    const val CACHED_PINCODE = "CACHED_PINCODE"
    const val CACHED_LATITUDE = "CACHED_LATITUDE"
    const val CACHED_LONGITUDE = "CACHED_LONGITUDE"
    const val CACHED_ADDRESS_LINE = "CACHED_ADDRESS_LINE"
    const val CACHED_STATE = "CACHED_STATE"
    const val CACHED_SUBLOCALITY = "CACHED_SUBLOCALITY"




    // Save String data in SharedPreferences
    fun savePreferences(mContext: Context, key: String, value: String?) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext)
        val editor = sharedPreferences.edit()
        editor.putString(key, value).apply()
    }

    // Save List of Strings as a comma-separated String in SharedPreferences
    fun saveInterestNames(mContext: Context, key: String, interestNames: List<String>?) {
        val interestNamesString = interestNames?.joinToString(",") ?: ""
        savePreferences(mContext, key, interestNamesString)
    }

    // Retrieve String data from SharedPreferences
    fun getPreferences(context: Context, key: String, defaultValue: String = ""): String? {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        return sharedPreferences.getString(key, defaultValue)
    }

    // Retrieve List of Strings from SharedPreferences (as a comma-separated String)
    fun getInterestNames(context: Context, key: String, defaultValue: String = ""): List<String> {
        val interestNamesString = getPreferences(context, key, defaultValue)
        return interestNamesString?.split(",") ?: emptyList()
    }

    // Remove all data from SharedPreferences (for logout, clear user data)
    fun removeAllSharedPreferences(mContext: Context) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext)
        val editor = sharedPreferences.edit()
        editor.clear().apply()
    }
}
