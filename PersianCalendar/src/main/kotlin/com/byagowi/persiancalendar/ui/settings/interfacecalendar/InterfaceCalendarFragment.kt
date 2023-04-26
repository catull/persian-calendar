package com.byagowi.persiancalendar.ui.settings.interfacecalendar

import android.Manifest
import android.animation.ValueAnimator
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.app.ActivityCompat
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.RecyclerView
import com.byagowi.persiancalendar.DEFAULT_ISLAMIC_OFFSET
import com.byagowi.persiancalendar.DEFAULT_THEME_GRADIENT
import com.byagowi.persiancalendar.PREF_APP_LANGUAGE
import com.byagowi.persiancalendar.PREF_ASTRONOMICAL_FEATURES
import com.byagowi.persiancalendar.PREF_EASTERN_GREGORIAN_ARABIC_MONTHS
import com.byagowi.persiancalendar.PREF_HOLIDAY_TYPES
import com.byagowi.persiancalendar.PREF_ISLAMIC_OFFSET
import com.byagowi.persiancalendar.PREF_LOCAL_DIGITS
import com.byagowi.persiancalendar.PREF_SHOW_DEVICE_CALENDAR_EVENTS
import com.byagowi.persiancalendar.PREF_SHOW_WEEK_OF_YEAR_NUMBER
import com.byagowi.persiancalendar.PREF_THEME
import com.byagowi.persiancalendar.PREF_THEME_GRADIENT
import com.byagowi.persiancalendar.PREF_WEEK_ENDS
import com.byagowi.persiancalendar.PREF_WEEK_START
import com.byagowi.persiancalendar.R
import com.byagowi.persiancalendar.entities.Theme
import com.byagowi.persiancalendar.global.language
import com.byagowi.persiancalendar.global.weekDays
import com.byagowi.persiancalendar.ui.settings.SettingsScreen
import com.byagowi.persiancalendar.ui.settings.build
import com.byagowi.persiancalendar.ui.settings.clickable
import com.byagowi.persiancalendar.ui.settings.interfacecalendar.calendarsorder.showCalendarPreferenceDialog
import com.byagowi.persiancalendar.ui.settings.multiSelect
import com.byagowi.persiancalendar.ui.settings.section
import com.byagowi.persiancalendar.ui.settings.singleSelect
import com.byagowi.persiancalendar.ui.settings.summary
import com.byagowi.persiancalendar.ui.settings.switch
import com.byagowi.persiancalendar.ui.settings.title
import com.byagowi.persiancalendar.ui.utils.askForCalendarPermission
import com.byagowi.persiancalendar.utils.appPrefs
import com.byagowi.persiancalendar.utils.formatNumber
import com.byagowi.persiancalendar.utils.isIslamicOffsetExpired

class InterfaceCalendarFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val activity = activity ?: return
        val destination = arguments?.getString(SettingsScreen.PREF_DESTINATION)
        if (destination == PREF_HOLIDAY_TYPES) {
            showHolidaysTypesDialog(activity)
            arguments?.remove(SettingsScreen.PREF_DESTINATION)
        }

        preferenceScreen = preferenceManager.createPreferenceScreen(activity).build {
            section(R.string.pref_interface) {
                clickable(onClick = { showLanguagePreferenceDialog(activity) }) {
                    if (destination == PREF_APP_LANGUAGE) title = "Language"
                    else title(R.string.language)
                    summary = language.nativeName
                }
                switch(PREF_EASTERN_GREGORIAN_ARABIC_MONTHS, false) {
                    if (language.isArabic) {
                        title = "السنة الميلادية بالاسماء الشرقية"
                        summary = "كانون الثاني، شباط، آذار، …"
                    } else isVisible = false
                }
                switch(PREF_LOCAL_DIGITS, true) {
                    title(R.string.native_digits)
                    summary(R.string.enable_native_digits)
                    if (!language.canHaveLocalDigits) isVisible = false
                }
                singleSelect(
                    PREF_THEME,
                    enumValues<Theme>().map { getString(it.title) },
                    enumValues<Theme>().map { it.key },
                    Theme.SYSTEM_DEFAULT.key,
                    R.string.select_skin
                ) { title(R.string.select_skin) }
                switch(PREF_THEME_GRADIENT, DEFAULT_THEME_GRADIENT) {
                    title(R.string.color_gradient)
                    summary(R.string.color_gradient_summary)
                }
            }
            section(R.string.calendar) {
                // Mark the rest of options as advanced
                initialExpandedChildrenCount = 5
                clickable(onClick = { showHolidaysTypesDialog(activity) }) {
                    title(R.string.events)
                    summary(R.string.events_summary)
                }
                switch(PREF_SHOW_DEVICE_CALENDAR_EVENTS, false) {
                    title(R.string.show_device_calendar_events)
                    summary(R.string.show_device_calendar_events_summary)
                    this.setOnPreferenceChangeListener { _, _ ->
                        isChecked = if (ActivityCompat.checkSelfPermission(
                                activity, Manifest.permission.READ_CALENDAR
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            activity.askForCalendarPermission()
                            false
                        } else {
                            !isChecked
                        }
                        false
                    }
                }
                clickable(onClick = {
                    showCalendarPreferenceDialog(activity, onEmpty = {
                        // Easter egg when empty result is rejected
                        val view = view?.rootView ?: return@showCalendarPreferenceDialog
                        ValueAnimator.ofFloat(0f, 360f).also {
                            it.duration = 3000L
                            it.interpolator = AccelerateDecelerateInterpolator()
                            it.addUpdateListener { value ->
                                view.rotation = value.animatedValue as? Float ?: 0f
                            }
                        }.start()
                    })
                }) {
                    title(R.string.calendars_priority)
                    summary(R.string.calendars_priority_summary)
                }
                switch(PREF_ASTRONOMICAL_FEATURES, false) {
                    title(R.string.astronomy)
                    summary(R.string.astronomical_info_summary)
                }
                switch(PREF_SHOW_WEEK_OF_YEAR_NUMBER, false) {
                    title(R.string.week_of_year)
                    summary(R.string.week_of_year_summary)
                }
                run { // reset Islamic offset if is already expired
                    val appPrefs = context.appPrefs
                    if (PREF_ISLAMIC_OFFSET in appPrefs && appPrefs.isIslamicOffsetExpired)
                        appPrefs.edit { putString(PREF_ISLAMIC_OFFSET, DEFAULT_ISLAMIC_OFFSET) }
                }
                singleSelect(
                    PREF_ISLAMIC_OFFSET,
                    // One is formatted with locale's numerals and the other used for keys isn't
                    (-2..2).map { formatNumber(it.toString()) }, (-2..2).map { it.toString() },
                    DEFAULT_ISLAMIC_OFFSET, R.string.islamic_offset,
                    R.string.islamic_offset_summary
                ) { title(R.string.islamic_offset) }
                val weekDaysValues = (0..6).map { it.toString() }
                singleSelect(
                    PREF_WEEK_START, weekDays, weekDaysValues, language.defaultWeekStart,
                    R.string.week_start_summary
                ) { title(R.string.week_start) }
                multiSelect(
                    PREF_WEEK_ENDS, weekDays, weekDaysValues, language.defaultWeekEnds,
                    R.string.week_ends_summary
                ) {
                    title(R.string.week_ends)
                    summary(R.string.week_ends_summary)
                }
            }
        }
    }

    override fun onCreateRecyclerView(
        inflater: LayoutInflater,
        parent: ViewGroup,
        savedInstanceState: Bundle?
    ): RecyclerView {
        val view = super.onCreateRecyclerView(inflater, parent, savedInstanceState)
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(bottom = insets.bottom)
            windowInsets
        }
        return view
    }
}
