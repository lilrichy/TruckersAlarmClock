package com.blogspot.richardreigens.truckersalarmclock.util

import android.content.Context
import android.preference.PreferenceManager
import com.blogspot.richardreigens.truckersalarmclock.AlarmActivity

class PrefUtil {
    companion object {

        fun getTimerLength(context: Context): Int {
            //placeholder
            return 1
        }

        private const val PREVIOUS_TIMER_LENGTH_SECONDS_ID = "com.blogspot.richardreigens.truckersalarmclock"

        fun getPreviousTimerLengthSeconds(context: Context): Long {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            return preferences.getLong(PREVIOUS_TIMER_LENGTH_SECONDS_ID, 0)
        }

        fun setPreviousTimerLengthSeconds(seconds: Long, context: Context) {
            val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
            editor.putLong(PREVIOUS_TIMER_LENGTH_SECONDS_ID, seconds)
            editor.apply()
        }

        private const val TIMER_STATE_ID = "com.blogspot.richardreigens.truckersalarmclock"

        fun getTimerState(context: Context): AlarmActivity.TimerState {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val ordinal = preferences.getLong(TIMER_STATE_ID, 0).toInt()
            System.out.println("getTimerState = $ordinal")

            return AlarmActivity.TimerState.values()[ordinal]
        }

        fun setTimerState(state: AlarmActivity.TimerState, context: Context) {
            val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
            val ordinal = state.ordinal.toLong()
            editor.putLong(TIMER_STATE_ID, ordinal)
            editor.apply()
            System.out.println("setTimerState = $ordinal")

        }

        private const val SECONDS_REMAINING_ID = "com.blogspot.richardreigens.truckersalarmclock"

        fun getSecondsRemaining(context: Context): Long {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            return preferences.getLong(SECONDS_REMAINING_ID, 0)
        }

        fun setSecondsRemaining(seconds: Long, context: Context) {
            val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
            editor.putLong(SECONDS_REMAINING_ID, seconds)
            editor.apply()
        }

        private const val ALARM_SET_TIME_ID = "com.blogspot.richardreigens.truckersalarmclock"

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