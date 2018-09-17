package com.blogspot.richardreigens.truckersalarmclock


import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.icu.util.Calendar
import android.os.Bundle
import android.os.CountDownTimer
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.blogspot.richardreigens.truckersalarmclock.util.PrefUtil
import kotlinx.android.synthetic.main.activity_alarm.*
import kotlinx.android.synthetic.main.content_alarm.*
import java.text.SimpleDateFormat


class AlarmActivity : AppCompatActivity() {
    //TODO: Add project to Github
    //TODO: Setup Notification with snooze button and cancel button
    //TODO: Add Alarm sound and vibrate when finished
    //TODO: Add Settings Page Integration

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

    private lateinit var timer: CountDownTimer
    private var timerLengthSeconds = 0L
    private var timerState = TimerState.Stopped
    private var secondsRemaining = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm)
        setSupportActionBar(toolbar)
        supportActionBar?.setIcon(R.drawable.ic_timer)
        supportActionBar?.title = "     Truckers Alarm Clock"

        fab_30_min.setOnClickListener { v ->
            //TODO: testing out timer code
            // PrefUtil.setAlarmSetTime(30,this)
            startTimer()
            timerState = TimerState.Running
            updateButtons()
        }

        fab_cancel.setOnClickListener { v ->
            timer.cancel()
            timerState = TimerState.Stopped
            onTimerFinished()
        }

        fab_10_hour.setOnClickListener { v ->
            startTimer()
            timerState = TimerState.Running
            updateButtons()
        }
    }

    override fun onResume() {
        super.onResume()
        initTimer()
        removeAlarm(this)

        //TODO: hide notification
    }

    override fun onPause() {
        super.onPause()
        if (timerState == TimerState.Running) {
            timer.cancel()
            val wakeUpTime = setAlarm(this, nowSeconds, secondsRemaining)
            //TODO: show notification

        } else if (timerState == TimerState.Paused) {
            //TODO: show notification
        }

        PrefUtil.setPreviousTimerLengthSeconds(timerLengthSeconds, this)
        PrefUtil.setSecondsRemaining(secondsRemaining, this)
        PrefUtil.setTimerState(timerState, this)
    }

    private fun initTimer() {
        timerState = PrefUtil.getTimerState(this)


        if (timerState == TimerState.Stopped)
            setNewTimerLength()
        else
            setPreviousTimerLength()

        secondsRemaining = if (timerState == TimerState.Running || timerState == TimerState.Paused)
            PrefUtil.getSecondsRemaining(this)
        else
            timerLengthSeconds

        val alarmSetTime = PrefUtil.getAlarmSetTime(this)
        System.out.println("alarmSetTime = $alarmSetTime")

        if (alarmSetTime > 0)
            secondsRemaining -= nowSeconds - alarmSetTime

        System.out.println("nowSeconds = $nowSeconds")
        System.out.println("secondsRemaining = $secondsRemaining")

        if (secondsRemaining <= 0)
            onTimerFinished()
        else if (timerState == TimerState.Running) {
            System.out.println("timerState = $timerState")
            startTimer()
            //TODO: BUG in code start timer not running when onResume - Need to fix
        }

        updateButtons()
        updateCountdownUI()
        updateClocks()
    }

    private fun onTimerFinished() {
        timerState = TimerState.Stopped
        setNewTimerLength()
        progress.progress = 0
        PrefUtil.setSecondsRemaining(timerLengthSeconds, this)
        secondsRemaining = timerLengthSeconds

        updateButtons()
        updateCountdownUI()
    }

    private fun startTimer() {
        //TODO: Add clock times when button pressed
        timerState = TimerState.Running
        timer = object : CountDownTimer(secondsRemaining * 1000, 1000) {
            override fun onFinish() = onTimerFinished()
            override fun onTick(millisUntilFinished: Long) {
                secondsRemaining = millisUntilFinished / 1000
                updateCountdownUI()
                updateClocks()
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
        val minutesUntilFinished = secondsRemaining / 60
        val secondsInMinuteUntilFinished = secondsRemaining - minutesUntilFinished * 60
        val secondsStr = secondsInMinuteUntilFinished.toString()
        textView_count.text = "$minutesUntilFinished:${if (secondsStr.length == 2) secondsStr else "0" + secondsStr}"
        progress.progress = (timerLengthSeconds - secondsRemaining).toInt()
        updateClocks()
    }

    private fun updateButtons() {
        when (timerState) {
            TimerState.Running -> {
                fab_30_min.isEnabled = false
                fab_10_hour.isEnabled = true
                fab_cancel.isEnabled = true
            }
            TimerState.Stopped -> {
                fab_30_min.isEnabled = true
                fab_10_hour.isEnabled = false
                fab_cancel.isEnabled = false
            }
            TimerState.Paused -> {
                fab_30_min.isEnabled = true
                fab_10_hour.isEnabled = false
                fab_cancel.isEnabled = true
            }
        }
    }

    //Test code for clock adjustments
    private fun updateClocks() {
        val dateFormat = SimpleDateFormat("HH:mm")

        val calAdd30min = Calendar.getInstance()
        calAdd30min.add(Calendar.MINUTE, 30)
        textView_30_min.text = dateFormat.format(calAdd30min.time)

        val calAdd10hr = Calendar.getInstance()
        calAdd10hr.add(Calendar.HOUR, 10)
        textView_10_hour.text = dateFormat.format(calAdd10hr.time)

        //TODO: Change values to a setting
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
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }
}