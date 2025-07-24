package com.tie.dreamsquad.utils

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager

object SP {

    // --- All your existing and new constants ---
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
    const val USER_IS_VERIFIED = "USER_IS_VERIFIED" // Note: This key suggests a boolean, consider using the boolean functions for it
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

    // --- New constants for the ticket system ---
    const val IS_PAYOUT_VERIFIED = "is_payout_verified"
    const val PAYOUT_INFO_DISPLAY = "payout_info_display"

    // --- END OF NEW CONSTANTS ---


    private fun getSharedPrefs(context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }

    // =================================================================
    // == SAVE FUNCTIONS ===============================================
    // =================================================================

    /**
     * Saves a String value.
     */
    fun saveString(mContext: Context, key: String, value: String?) {
        getSharedPrefs(mContext).edit().putString(key, value).apply()
    }

    /**
     * Saves a Boolean value.
     */
    fun saveBoolean(mContext: Context, key: String, value: Boolean) {
        getSharedPrefs(mContext).edit().putBoolean(key, value).apply()
    }


    // =================================================================
    // == GET FUNCTIONS (WITH CLEAR, UNAMBIGUOUS NAMES) ================
    // =================================================================

    /**
     * Retrieves a String value, returning a nullable String?
     */
    fun getString(context: Context, key: String, defaultValue: String? = null): String? {
        return getSharedPrefs(context).getString(key, defaultValue)
    }

    /**
     * Retrieves a Boolean value, with a default of 'false'.
     */
    fun getBoolean(context: Context, key: String, defaultValue: Boolean = false): Boolean {
        return getSharedPrefs(context).getBoolean(key, defaultValue)
    }


    // =================================================================
    // == SPECIFIC HELPER FUNCTIONS (No change needed) =================
    // =================================================================

    fun saveInterestNames(mContext: Context, key: String, interestNames: List<String>?) {
        val interestNamesString = interestNames?.joinToString(",")
        saveString(mContext, key, interestNamesString) // Uses the new specific name
    }

    fun getInterestNames(context: Context, key: String): List<String> {
        val interestNamesString = getString(context, key, "") // Uses the new specific name
        return if (!interestNamesString.isNullOrEmpty()) interestNamesString.split(",") else emptyList()
    }

    fun removeAllSharedPreferences(mContext: Context) {
        getSharedPrefs(mContext).edit().clear().apply()
    }
}