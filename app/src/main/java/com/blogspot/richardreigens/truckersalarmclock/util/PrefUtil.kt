/*
 * Copyright (c) Richard J Reigens / LiLRichy 2018
 */

package com.blogspot.richardreigens.truckersalarmclock.util

import android.content.Context
import android.preference.PreferenceManager
import com.blogspot.richardreigens.truckersalarmclock.AlarmActivity

class PrefUtil {
    companion object {

        private const val RINGTONE_SETTING = "com.blogspot.richardreigens.truckersalarmclock.ringtone_setting_id"

        fun getRingtonePreferenceValue(context: Context): String? {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            return preferences.getString(RINGTONE_SETTING, "")
        }

        fun setRingtonePreferenceValue(string: String, context: Context) {
            val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
            editor.putString(RINGTONE_SETTING, string)
            editor.apply()
        }


        private const val BREAK_CLOCK_ACTIVE = "com.blogspot.richardreigens.truckersalarmclock.break_clock_active_id"

        fun getBreakClockActive(context: Context): Boolean {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            return preferences.getBoolean(BREAK_CLOCK_ACTIVE, true)
        }

        fun setBreakClockActive(boolean: Boolean, context: Context) {
            val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
            editor.putBoolean(BREAK_CLOCK_ACTIVE, boolean)
            editor.apply()
        }





        private const val TIMER_LENGTH_ID = "com.blogspot.richardreigens.truckersalarmclock.timer_length_id"

        fun getTimerLength(context: Context): Long {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            return preferences.getLong(TIMER_LENGTH_ID, 0)
        }

        fun setTimerLength(length: Long, context: Context) {
            val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
            editor.putLong(TIMER_LENGTH_ID, length)
            editor.apply()
        }

        //App Preferences to run
        private const val PREVIOUS_TIMER_LENGTH_SECONDS_ID = "com.blogspot.richardreigens.truckersalarmclock.previous_timer_length_seconds_id"

        fun getPreviousTimerLengthSeconds(context: Context): Long {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            return preferences.getLong(PREVIOUS_TIMER_LENGTH_SECONDS_ID, 0)
        }

        fun setPreviousTimerLengthSeconds(seconds: Long, context: Context) {
            val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
            editor.putLong(PREVIOUS_TIMER_LENGTH_SECONDS_ID, seconds)
            editor.apply()
        }

        private const val TIMER_STATE_ID = "com.blogspot.richardreigens.truckersalarmclock.timer_state_id"

        fun getTimerState(context: Context): AlarmActivity.TimerState {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val ordinal = preferences.getInt(TIMER_STATE_ID, 0)
            return AlarmActivity.TimerState.values()[ordinal]
        }

        fun setTimerState(state: AlarmActivity.TimerState, context: Context) {
            val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
            val ordinal = state.ordinal
            editor.putInt(TIMER_STATE_ID, ordinal)
            editor.apply()
        }

        private const val SECONDS_REMAINING_ID = "com.blogspot.richardreigens.truckersalarmclock.seconds_remaining_id"

        fun getSecondsRemaining(context: Context): Long {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            return preferences.getLong(SECONDS_REMAINING_ID, 0)
        }

        fun setSecondsRemaining(seconds: Long, context: Context) {
            val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
            editor.putLong(SECONDS_REMAINING_ID, seconds)
            editor.apply()
        }

        private const val ALARM_SET_TIME_ID = "com.blogspot.richardreigens.truckersalarmclock.alarm_set_time_id"

        fun getAlarmSetTime(context: Context): Long {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            return preferences.getLong(ALARM_SET_TIME_ID, 0)
        }

        fun setAlarmSetTime(time: Long, context: Context) {
            val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
            editor.putLong(ALARM_SET_TIME_ID, time)
            editor.apply()
        }
    }
}