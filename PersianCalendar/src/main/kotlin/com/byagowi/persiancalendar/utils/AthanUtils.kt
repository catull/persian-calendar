package com.byagowi.persiancalendar.utils

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import androidx.annotation.RawRes
import androidx.annotation.StringRes
import androidx.core.app.ActivityCompat
import androidx.core.app.AlarmManagerCompat
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.byagowi.persiancalendar.ALARMS_BASE_ID
import com.byagowi.persiancalendar.ALARM_TAG
import com.byagowi.persiancalendar.ASR_KEY
import com.byagowi.persiancalendar.BROADCAST_ALARM
import com.byagowi.persiancalendar.DHUHR_KEY
import com.byagowi.persiancalendar.FAJR_KEY
import com.byagowi.persiancalendar.ISHA_KEY
import com.byagowi.persiancalendar.KEY_EXTRA_PRAYER
import com.byagowi.persiancalendar.KEY_EXTRA_PRAYER_TIME
import com.byagowi.persiancalendar.MAGHRIB_KEY
import com.byagowi.persiancalendar.PREF_ATHAN_ALARM
import com.byagowi.persiancalendar.PREF_ATHAN_GAP
import com.byagowi.persiancalendar.PREF_ATHAN_URI
import com.byagowi.persiancalendar.R
import com.byagowi.persiancalendar.entities.Clock
import com.byagowi.persiancalendar.entities.Jdn
import com.byagowi.persiancalendar.global.calculationMethod
import com.byagowi.persiancalendar.global.coordinates
import com.byagowi.persiancalendar.global.notificationAthan
import com.byagowi.persiancalendar.service.AlarmWorker
import com.byagowi.persiancalendar.service.AthanNotification
import com.byagowi.persiancalendar.service.BroadcastReceivers
import com.byagowi.persiancalendar.ui.athan.AthanActivity
import com.byagowi.persiancalendar.variants.debugLog
import io.github.persiancalendar.praytimes.PrayTimes
import java.util.GregorianCalendar
import java.util.concurrent.TimeUnit
import kotlin.math.abs

// https://stackoverflow.com/a/69505596
fun Resources.getRawUri(@RawRes rawRes: Int) = "%s://%s/%s/%s".format(
    ContentResolver.SCHEME_ANDROID_RESOURCE, this.getResourcePackageName(rawRes),
    this.getResourceTypeName(rawRes), this.getResourceEntryName(rawRes)
)

fun getAthanUri(context: Context): Uri =
    (context.preferences.getString(PREF_ATHAN_URI, null)?.takeIf { it.isNotEmpty() }
        ?: context.resources.getRawUri(R.raw.special)).toUri()

private var lastAthanKey = ""
private var lastAthanJdn: Jdn? = null
fun startAthan(context: Context, prayTimeKey: String, intendedTime: Long?) {
    debugLog("Alarms: startAthan for $prayTimeKey")
    if (intendedTime == null) return startAthanBody(context, prayTimeKey)
    // if alarm is off by 15 minutes, just skip
    if (abs(System.currentTimeMillis() - intendedTime) > FIFTEEN_MINUTES_IN_MILLIS) return

    // If at the of being is disabled by user, skip
    if (prayTimeKey !in getEnabledAlarms(context)) return

    // skips if already called through either WorkManager or AlarmManager
    val today = Jdn.today()
    if (lastAthanJdn == today && lastAthanKey == prayTimeKey) return
    lastAthanJdn = today; lastAthanKey = prayTimeKey

    startAthanBody(context, prayTimeKey)
}

private fun startAthanBody(context: Context, prayTimeKey: String) {
    runCatching {
        debugLog("Alarms: startAthanBody for $prayTimeKey")

        runCatching {
            context.getSystemService<PowerManager>()?.newWakeLock(
                PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.SCREEN_DIM_WAKE_LOCK,
                "persiancalendar:alarm"
            )?.acquire(THIRTY_SECONDS_IN_MILLIS)
        }.onFailure(logException)

        if (notificationAthan.value || ActivityCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) context.startService(
            Intent(context, AthanNotification::class.java)
                .putExtra(KEY_EXTRA_PRAYER, prayTimeKey)
        ) else startAthanActivity(context, prayTimeKey)
    }.onFailure(logException)
}

fun startAthanActivity(context: Context, prayerKey: String?) {
    context.startActivity(
        Intent(context, AthanActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(KEY_EXTRA_PRAYER, prayerKey)
    )
}

fun getEnabledAlarms(context: Context): Set<String> {
    if (coordinates.value == null) return emptySet()
    return (context.preferences.getString(PREF_ATHAN_ALARM, null)?.trim() ?: return emptySet())
        .splitFilterNotEmpty(",")
        .toSet()
}

fun scheduleAlarms(context: Context) {
    val enabledAlarms = getEnabledAlarms(context).takeIf { it.isNotEmpty() } ?: return
    val athanGap =
        ((context.preferences.getString(PREF_ATHAN_GAP, null)?.toDoubleOrNull()
            ?: .0) * 60.0 * 1000.0).toLong()

    val prayTimes = coordinates.value?.calculatePrayTimes() ?: return
    // convert spacedComma separated string to a set
    enabledAlarms.forEachIndexed { i, name ->
        scheduleAlarm(context, name, GregorianCalendar().also {
            // if (name == ISHA_KEY) return@also it.add(Calendar.SECOND, 5)
            val alarmTime = prayTimes.getFromStringId(getPrayTimeName(name))
            it[GregorianCalendar.HOUR_OF_DAY] = alarmTime.hours
            it[GregorianCalendar.MINUTE] = alarmTime.minutes
            it[GregorianCalendar.SECOND] = 0
        }.timeInMillis - athanGap, i)
    }
}

private fun scheduleAlarm(context: Context, alarmTimeName: String, timeInMillis: Long, i: Int) {
    val remainedMillis = timeInMillis - System.currentTimeMillis()
    debugLog("Alarms: $alarmTimeName in ${remainedMillis / 60000} minutes")
    if (remainedMillis < 0) return // Don't set alarm in past

    run { // Schedule in both alarmmanager and workmanager, startAthan has the logic to skip duplicated calls
        val workerInputData = Data.Builder().putLong(KEY_EXTRA_PRAYER_TIME, timeInMillis)
            .putString(KEY_EXTRA_PRAYER, alarmTimeName).build()
        val alarmWorker = OneTimeWorkRequest.Builder(AlarmWorker::class.java)
            .setInitialDelay(remainedMillis, TimeUnit.MILLISECONDS)
            .setInputData(workerInputData)
            .build()
        WorkManager.getInstance(context)
            .beginUniqueWork(ALARM_TAG + i, ExistingWorkPolicy.REPLACE, alarmWorker)
            .enqueue()
    }

    val am = context.getSystemService<AlarmManager>() ?: return
    val pendingIntent = PendingIntent.getBroadcast(
        context, ALARMS_BASE_ID + i,
        Intent(context, BroadcastReceivers::class.java)
            .putExtra(KEY_EXTRA_PRAYER, alarmTimeName)
            .putExtra(KEY_EXTRA_PRAYER_TIME, timeInMillis)
            .setAction(BROADCAST_ALARM),
        PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
    )
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms())
        AlarmManagerCompat.setExactAndAllowWhileIdle(
            am, AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent
        )
}

private val prayTimesNames = mapOf(
    FAJR_KEY to R.string.fajr,
    DHUHR_KEY to R.string.dhuhr,
    ASR_KEY to R.string.asr,
    MAGHRIB_KEY to R.string.maghrib,
    ISHA_KEY to R.string.isha
)

@StringRes
fun getPrayTimeName(athanKey: String?): Int = prayTimesNames[athanKey] ?: R.string.fajr

fun PrayTimes.getFractionFromStringId(@StringRes stringId: Int): Double {
    return when (stringId) {
        R.string.imsak -> imsak
        R.string.fajr -> fajr
        R.string.sunrise -> sunrise
        R.string.dhuhr -> dhuhr
        R.string.asr -> asr
        R.string.sunset -> sunset
        R.string.maghrib -> maghrib
        R.string.isha -> isha
        R.string.midnight -> midnight
        else -> .0
    }
}

fun PrayTimes.getFromStringId(@StringRes stringId: Int): Clock =
    Clock.fromHoursFraction(getFractionFromStringId(stringId))

private val TIME_NAMES = listOf(
    R.string.imsak, R.string.fajr, R.string.sunrise, R.string.dhuhr, R.string.asr,
    R.string.sunset, R.string.maghrib, R.string.isha, R.string.midnight
)

fun getTimeNames(): List<Int> {
    return if (calculationMethod.value.isJafari) TIME_NAMES else TIME_NAMES - R.string.sunset
}
