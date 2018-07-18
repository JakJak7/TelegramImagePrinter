package me.jakjak.telegramimagereceiver

import android.content.Context
import android.preference.PreferenceManager

class Preferences() {

    companion object {
        public val PHONE_NUMBER = "phonenumber"

        fun getPreferences(context: Context, name: String): String? {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            return preferences.getString(name, null)
        }

        fun setPreference(context: Context, name: String, value: String?) {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val editor = preferences.edit()
            editor.putString(name, value)
            editor.apply()
        }
    }
}