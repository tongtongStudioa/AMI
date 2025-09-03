package com.tongtongstudio.ami.ui

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.tongtongstudio.ami.R

class SettingsFragment2 : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
    }
}