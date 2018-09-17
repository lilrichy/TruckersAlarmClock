package com.blogspot.richardreigens.TruckersAlarmClock

import android.os.Bundle
import android.support.v7.preference.PreferenceFragmentCompat
import com.blogspot.richardreigens.R


class SettingsActivityFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)
    }
}