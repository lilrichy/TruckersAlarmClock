package com.blogspot.richardreigens.truckersalarmclock

import android.os.Bundle
import android.support.v7.app.AppCompatActivity


class SettingsActivity : AppCompatActivity() {

    companion object {
        val KEY_VIBRATE_SWITCH = "vibrate_switch"
        val KEY_RINGTONE_SELECTION = "ringtone_selection"
        val KEY_BREAK_BUTTON_SETTING = "break_button_setting"
        val KEY_REST_BUTTON_SETTING = "rest_button_setting"

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, SettingsActivityFragment())
                .commit()
    }
}