/*
 * Copyright (c) Richard J Reigens / LiLRichy 2018
 */

package com.blogspot.richardreigens.truckersalarmclock

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.*
import android.support.v7.app.AppCompatActivity
import android.support.v7.preference.PreferenceManager
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.blogspot.richardreigens.truckersalarmclock.util.NotificationUtil
import com.blogspot.richardreigens.truckersalarmclock.util.PrefUtil
import kotlinx.android.synthetic.main.activity_alarm.*
import kotlinx.android.synthetic.main.content_alarm.*
import java.text.SimpleDateFormat
import java.util.*

class AlarmActivity : AppCompatActivity() {
    //TODO: Setup Notification with snooze button to add time to timer "maybe start new 5min timer???"
    //TODO: Possibly Add 12/24 time selection option
    //TODO: Possibly add option to set alarm in Android alarm clock based on time picked. Maybe by a long press on button.

    companion object {
        fun setAlarm(context: Context, nowSeconds: Long, secondsRemaining: Long): Long {
            val wakeUpTime = (nowSeconds + secondsRemaining) * 1000
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, TimerExpiredReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0)
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, wakeUpTime, pendingIntent)
            PrefUtil.setAlarmSetTime(nowSeconds, context)
            return wakeUpTime
        }

        fun removeAlarm(context: Context) {
            val intent = Intent(context, TimerExpiredReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
            PrefUtil.setAlarmSetTime(0, context)
        }

        val nowSeconds: Long
            get() = Calendar.getInstance().timeInMillis / 1000
    }

    enum class TimerState {
        Stopped, Paused, Running
    }

    private var mp: MediaPlayer? = null
    private lateinit var notificationSound: Uri

    private lateinit var timer: CountDownTimer
    private var timerLengthSeconds: Long = 0
    private var timerState = TimerState.Stopped
    private var secondsRemaining: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm)
        setSupportActionBar(toolbar)
        supportActionBar?.setIcon(R.drawable.ic_timer)
        supportActionBar?.title = " Truckers Alarm Clock"

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)

        notificationSound = Uri.parse(PrefUtil.getRingtonePreferenceValue(this))

        fab_break.setOnClickListener { v ->
            // Timer setting for break
            val settingsUtil = PreferenceManager.getDefaultSharedPreferences(this)

            //Check for crash
            try {
                PrefUtil.setTimerLength(Integer.valueOf(settingsUtil.getString(
                        SettingsActivity.KEY_BREAK_BUTTON_SETTING, "30")).toLong(), this)
            } catch (e: Exception) {
                Toast.makeText(applicationContext, getString(R.string.break_setting_error), Toast.LENGTH_SHORT).show()
                PrefUtil.setTimerLength(0, this)
            }

            setNewTimerLength()

            secondsRemaining = PrefUtil.getTimerLength(this) * 60
            updateCountdownUI()
        }

        fab_start.setOnClickListener { v ->
            if (secondsRemaining > 0) {
                startTimer()
                timerState = TimerState.Running
            }
            updateButtons()
            updateCountdownUI()
        }

        fab_pause.setOnClickListener { v ->
            timer.cancel()
            timerState = TimerState.Paused

            updateButtons()
            updateCountdownUI()
        }

        fab_cancel.setOnClickListener { v ->
            PrefUtil.setTimerLength(0, this)
            setNewTimerLength()
            onTimerFinished()
            playAlarmSoundVibrate(false)
            NotificationUtil.hideTimerNotification(this)

            updateButtons()
            updateCountdownUI()

        }

        fab_rest.setOnClickListener { v ->
            // Timer setting for rest
            val settingsUtil = PreferenceManager.getDefaultSharedPreferences(this)

            //Check for crash
            try {
                PrefUtil.setTimerLength(Integer.valueOf(settingsUtil.getString(
                        SettingsActivity.KEY_REST_BUTTON_SETTING, "600")).toLong() * 60, this)
            } catch (e: Exception) {
                Toast.makeText(applicationContext, getString(R.string.rest_setting_error), Toast.LENGTH_SHORT).show()
                PrefUtil.setTimerLength(0, this)
            }

            setNewTimerLength()

            secondsRemaining = PrefUtil.getTimerLength(this) * 60
            updateCountdownUI()
        }
        clocksTimer()
    }

    override fun onResume() {
        super.onResume()
        try {
            initTimer()
        } catch (e: Exception) {
            Toast.makeText(applicationContext, getString(R.string.timer_init_error), Toast.LENGTH_SHORT).show()
        }
        removeAlarm(this)
        NotificationUtil.hideTimerNotification(this)
    }

    override fun onPause() {
        super.onPause()
        if (timerState == TimerState.Running) {
            timer.cancel()
            val wakeUpTime = setAlarm(this, nowSeconds, secondsRemaining)
            NotificationUtil.showTimerRunning(this, wakeUpTime)
        } else if (timerState == TimerState.Paused) {
            NotificationUtil.showTimerPaused(this)
        }

        PrefUtil.setPreviousTimerLengthSeconds(timerLengthSeconds, this)
        PrefUtil.setSecondsRemaining(secondsRemaining, this)
        PrefUtil.setTimerState(timerState, this)
    }

    private fun initTimer() {
        timerState = PrefUtil.getTimerState(this)

        //we don't want to change the length of the timer which is already running
        //if the length was changed in settings while it was backgrounded
        if (timerState == TimerState.Stopped)
            setNewTimerLength()
        else
            setPreviousTimerLength()

        secondsRemaining = if (timerState == TimerState.Running || timerState == TimerState.Paused)
            PrefUtil.getSecondsRemaining(this)
        else
            timerLengthSeconds

        val alarmSetTime = PrefUtil.getAlarmSetTime(this)
        if (alarmSetTime > 0)
            secondsRemaining -= nowSeconds - alarmSetTime

        if (secondsRemaining <= 0)
            onTimerFinished()
        else if (timerState == TimerState.Running)
            startTimer()

        updateButtons()
        updateCountdownUI()
    }

    private fun playAlarmSoundVibrate(soundMode: Boolean) {
        val settingsUtil = PreferenceManager.getDefaultSharedPreferences(this)

        //Alarm Sound Must edit here and in Notification.
        if (soundMode) {
            mp = MediaPlayer.create(applicationContext, Uri.parse(PrefUtil.getRingtonePreferenceValue(this)))
            mp?.start()

            //Vibrate
            if (settingsUtil.getBoolean(SettingsActivity.KEY_VIBRATE_SWITCH, true)) {
                val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                // Vibrate for 1000 milliseconds
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    //deprecated in API 26
                    @Suppress("DEPRECATION")
                    v.vibrate(1000)
                }
            }
        } else {
            mp?.stop()
        }
    }

    private fun onTimerFinished() {
        if (timerState == TimerState.Running) {
            playAlarmSoundVibrate(true)
            timer.cancel()
        } else {
            playAlarmSoundVibrate(false)
        }

        timerState = TimerState.Stopped

        setNewTimerLength()

        progress.progress = 0

        PrefUtil.setSecondsRemaining(timerLengthSeconds, this)
        secondsRemaining = timerLengthSeconds

        NotificationUtil.showTimerExpired(this)

        updateButtons()
        updateCountdownUI()
    }

    private fun startTimer() {
        timerState = TimerState.Running

        timer = object : CountDownTimer(secondsRemaining * 1000, 1000) {
            override fun onFinish() = onTimerFinished()

            override fun onTick(millisUntilFinished: Long) {
                secondsRemaining = millisUntilFinished / 1000
                updateCountdownUI()
            }
        }.start()
    }

    private fun setNewTimerLength() {
        val lengthInMinutes = PrefUtil.getTimerLength(this)
        timerLengthSeconds = (lengthInMinutes * 60L)
        progress.max = timerLengthSeconds.toInt()
    }

    private fun setPreviousTimerLength() {
        timerLengthSeconds = PrefUtil.getPreviousTimerLengthSeconds(this)
        progress.max = timerLengthSeconds.toInt()
    }

    private fun updateCountdownUI() {
        val hours = secondsRemaining / 3600
        val minutes = (secondsRemaining % 3600) / 60
        val seconds = secondsRemaining % 60
        val timeString = String.format("%02d:%02d:%02d", hours, minutes, seconds)

        textView_count.text = timeString
        textView_status.text = timerState.toString()
        progress.progress = (timerLengthSeconds - secondsRemaining).toInt()
    }

    private fun updateButtons() {
        when (timerState) {
            TimerState.Running -> {
                fab_break.isEnabled = false
                fab_start.isEnabled = false
                fab_pause.isEnabled = true
                fab_cancel.isEnabled = true
                fab_rest.isEnabled = false
            }
            TimerState.Stopped -> {
                fab_break.isEnabled = true
                fab_start.isEnabled = true
                fab_pause.isEnabled = false
                fab_cancel.isEnabled = true
                fab_rest.isEnabled = true
            }
            TimerState.Paused -> {
                fab_break.isEnabled = true
                fab_start.isEnabled = true
                fab_pause.isEnabled = false
                fab_cancel.isEnabled = true
                fab_rest.isEnabled = true
            }
        }
    }

    private fun clocksTimer() {
        val settingsUtil = PreferenceManager.getDefaultSharedPreferences(this)
        val dateFormat12 = SimpleDateFormat("hh:mm a", Locale.ENGLISH)//12 hour
        //   val dateFormat24 = SimpleDateFormat("HH:mm", Locale.ENGLISH)//24 hour

        val t = object : Thread() {
            override fun run() {
                try {
                    while (!isInterrupted) {
                        Thread.sleep(1000)
                        runOnUiThread {
                            val calAdd30min = Calendar.getInstance()
                            //catch crash if wrong value is entered in settings
                            try {
                                calAdd30min.add(Calendar.MINUTE, Integer.valueOf(
                                        settingsUtil.getString(
                                                SettingsActivity.KEY_BREAK_BUTTON_SETTING, "30")))
                            } catch (e: Exception) {
                                Toast.makeText(applicationContext, getString(R.string.break_setting_error), Toast.LENGTH_SHORT).show()
                                calAdd30min.add(Calendar.MINUTE, 0)
                            }

                            //TODO: Update if/when 12/24 option gets implemented
                            //If 12 hour
                            textView_30_min.text = dateFormat12.format(calAdd30min.time)
                            //Else 24 hour
                            // textView_30_min.text = dateFormat24.format(calAdd30min.time)

                            val calAdd10Hour = Calendar.getInstance()
                            //catch crash if wrong value is entered in settings
                            try {
                                calAdd10Hour.add(Calendar.HOUR, Integer.valueOf(
                                        settingsUtil.getString(
                                                SettingsActivity.KEY_REST_BUTTON_SETTING, "600")))
                            } catch (e: Exception) {
                                Toast.makeText(applicationContext, getString(R.string.rest_setting_error), Toast.LENGTH_SHORT).show()
                                calAdd10Hour.add(Calendar.HOUR, 0)
                            }

                            //If 12 hour
                            textView_10_hour.text = dateFormat12.format(calAdd10Hour.time)
                            //Else 24 hour
                            // textView_10_hour.text = dateFormat24.format(calAdd10Hour.time)
                        }
                    }
                } catch (e: InterruptedException) {
                }
            }
        }
        t.start()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_alarm, menu)
        return true
    }

    private inline fun consume(f: () -> Unit): Boolean {
        f()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        R.id.action_settings -> consume {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        R.id.menu_item_share -> consume {
            val sharingIntent = Intent(android.content.Intent.ACTION_SEND)
            sharingIntent.type = "text/plain"
            val shareBodyText = "Check it out. Your message goes here"
            sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Subject here")
            sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBodyText)
            startActivity(Intent.createChooser(sharingIntent, "Sharing Option"))
            println("Share Clicked")
            return true
        }
        else -> consume { super.onOptionsItemSelected(item) }
    }
}