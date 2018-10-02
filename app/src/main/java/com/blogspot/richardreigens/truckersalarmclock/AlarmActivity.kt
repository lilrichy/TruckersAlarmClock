package com.blogspot.richardreigens.truckersalarmclock

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.*
import android.support.v7.app.AppCompatActivity
import android.support.v7.preference.PreferenceManager
import android.view.Menu
import android.view.MenuItem
import com.blogspot.richardreigens.truckersalarmclock.util.NotificationUtil
import com.blogspot.richardreigens.truckersalarmclock.util.PrefUtil
import kotlinx.android.synthetic.main.activity_alarm.*
import kotlinx.android.synthetic.main.content_alarm.*
import java.text.SimpleDateFormat
import java.util.*

class AlarmActivity : AppCompatActivity() {
    //TODO: Setup Notification with snooze button to add time to timer "maybe start new 5min timer???"
    //TODO: Setting for picking alarm ringtone
    //TODO: Possibly Add 12/24 time selection option

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
        Stopped, Paused, Running, Finished
    }

    //Set to true to display debug text usage: db("String to display")
    private var debugText: Boolean = true

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
        supportActionBar?.title = "     Truckers Alarm Clock"

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)

        notificationSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)


        fab_30_min.setOnClickListener { v ->
            // Timer setting for break
            val settingsUtil = PreferenceManager.getDefaultSharedPreferences(this)

            PrefUtil.setTimerLength(Integer.parseInt(settingsUtil.getString(
                    SettingsActivity.KEY_BREAK_BUTTON_SETTING, "30")).toLong(), this)

            setNewTimerLength()

            secondsRemaining = PrefUtil.getTimerLength(this) * 60
            updateCountdownUI()

            //TODO debug text
            db("30 min Clicked!")
        }

        fab_start.setOnClickListener { v ->
            if (secondsRemaining > 0) {
                startTimer()
                timerState = TimerState.Running
            }
            updateButtons()
            updateCountdownUI()

            //TODO debug text
            db("Start Clicked!")
        }

        fab_pause.setOnClickListener { v ->
            timer.cancel()
            timerState = TimerState.Paused

            updateButtons()
            updateCountdownUI()

            //TODO debug text
            db("Pause Clicked!")
        }

        fab_cancel.setOnClickListener { v ->
            PrefUtil.setTimerLength(0, this)
            setNewTimerLength()
            onTimerFinished()

            updateButtons()
            updateCountdownUI()

            //TODO debug text
            db("Cancel Clicked!")
        }

        fab_10_hour.setOnClickListener { v ->
            // Timer setting for rest
            val settingsUtil = PreferenceManager.getDefaultSharedPreferences(this)

            PrefUtil.setTimerLength(Integer.parseInt(settingsUtil.getString(
                    SettingsActivity.KEY_REST_BUTTON_SETTING, "600")).toLong() * 60, this)

            setNewTimerLength()

            secondsRemaining = PrefUtil.getTimerLength(this) * 60
            updateCountdownUI()

            //TODO debug text
            db("10 hr Clicked!")
        }
        clocksTimer()
    }

    override fun onResume() {
        super.onResume()
        initTimer()
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

        //Alarm Sound
        //TODO: Update to setting to pick what sound to Play when finished
        //TODO: Fix notification sound playing / wakeup?

        if (soundMode) {
            mp = MediaPlayer.create(applicationContext, notificationSound)
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
            // mp?.release()

            db("mp.stop???")
        }
    }

    private fun onTimerFinished() {
        if (timerState == TimerState.Running) {
            playAlarmSoundVibrate(true)
            timer.cancel()
            db("onTimerFinished running")
        } else {
            playAlarmSoundVibrate(false)
            db("onTimerFinished else")
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
                fab_30_min.isEnabled = false
                fab_start.isEnabled = false
                fab_pause.isEnabled = true
                fab_cancel.isEnabled = true
                fab_10_hour.isEnabled = false
            }
            TimerState.Stopped -> {
                fab_30_min.isEnabled = true
                fab_start.isEnabled = true
                fab_pause.isEnabled = false
                fab_cancel.isEnabled = true
                fab_10_hour.isEnabled = true
            }
            TimerState.Paused -> {
                fab_30_min.isEnabled = true
                fab_start.isEnabled = true
                fab_pause.isEnabled = false
                fab_cancel.isEnabled = true
                fab_10_hour.isEnabled = true
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
                            //If to catch crash if nothing is entered in settings
                            if (settingsUtil.getString(SettingsActivity.KEY_BREAK_BUTTON_SETTING, "30") != "")
                                calAdd30min.add(Calendar.MINUTE, Integer.parseInt(
                                        settingsUtil.getString(
                                                SettingsActivity.KEY_BREAK_BUTTON_SETTING, "30")))
                            else
                                calAdd30min.add(Calendar.MINUTE, 1)

                            //TODO: Update if/when 12/24 option gets implemented
                            //If 12 hour
                            textView_30_min.text = dateFormat12.format(calAdd30min.time)
                            //Else 24 hour
                            // textView_30_min.text = dateFormat24.format(calAdd30min.time)

                            val calAdd10Hour = Calendar.getInstance()
                            //If to catch crash if nothing is entered in settings
                            if (settingsUtil.getString(SettingsActivity.KEY_REST_BUTTON_SETTING, "10") != "")
                                calAdd10Hour.add(Calendar.HOUR, Integer.parseInt(
                                        settingsUtil.getString(
                                                SettingsActivity.KEY_REST_BUTTON_SETTING, "600")))
                            else
                                calAdd10Hour.add(Calendar.HOUR, 10)

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

    //Debug text displayed if debugText at top = true
    fun db(message: String) {
        if (debugText)
            println(message)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_alarm, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}