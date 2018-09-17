package com.blogspot.richardreigens.TruckersAlarmClock

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.blogspot.richardreigens.R
import kotlinx.android.synthetic.main.activity_timer.*

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Settings"
    }
}
