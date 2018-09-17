package com.blogspot.richardreigens.truckersalarmclock

import android.os.Bundle
import android.support.v7.preference.PreferenceFragmentCompat


class SettingsActivityFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }
}