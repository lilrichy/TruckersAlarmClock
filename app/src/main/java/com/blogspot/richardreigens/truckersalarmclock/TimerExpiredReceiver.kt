package com.blogspot.richardreigens.truckersalarmclock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.blogspot.richardreigens.truckersalarmclock.util.PrefUtil

class TimerExpiredReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
       //TODO: Show notification

        PrefUtil.setTimerState(AlarmActivity.TimerState.Stopped, context)
        PrefUtil.setAlarmSetTime(0, context)
    }
}
