package com.hereliesaz.hg2gui

import android.app.Activity
import android.app.ActivityManager
import android.app.KeyguardManager
import android.app.admin.DevicePolicyManager
import android.bluetooth.BluetoothAdapter
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.ConnectivityManager
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.util.DisplayMetrics
import android.view.*
import android.view.GestureDetector.OnDoubleTapListener
import android.view.View.OnTouchListener
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.core.view.GestureDetectorCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.hereliesaz.hg2gui.commands.main.MainPack
import com.hereliesaz.hg2gui.commands.main.specific.RedirectCommand
import com.hereliesaz.hg2gui.managers.*
import com.hereliesaz.hg2gui.managers.suggestions.SuggestionTextWatcher
import com.hereliesaz.hg2gui.managers.suggestions.SuggestionsManager
import com.hereliesaz.hg2gui.managers.xml.XMLPrefsManager
import com.hereliesaz.hg2gui.managers.xml.options.*
import com.hereliesaz.hg2gui.managers.xml.options.Toolbar as XMLToolbar
import com.hereliesaz.hg2gui.tuils.AllowEqualsSequence
import com.hereliesaz.hg2gui.tuils.NetworkUtils
import com.hereliesaz.hg2gui.tuils.OutlineTextView
import com.hereliesaz.hg2gui.tuils.Tuils
import com.hereliesaz.hg2gui.tuils.interfaces.*
import com.hereliesaz.hg2gui.tuils.stuff.PolicyReceiver
import java.io.File
import java.io.FileOutputStream
import java.lang.reflect.Method
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern

/*Copyright Francesco Andreuzzi

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.*/

open class UIManager(
    protected val mContext: Context,
    rootView: ViewGroup,
    mainPack: MainPack,
    canApplyTheme: Boolean,
    executer: CommandExecuter
) : OnTouchListener {

    companion object {
        const val ACTION_UPDATE_SUGGESTIONS = "com.hereliesaz.hg2gui.ui_update_suggestions"
        const val ACTION_UPDATE_HINT = "com.hereliesaz.hg2gui.ui_update_hint"
        const val ACTION_ROOT = "com.hereliesaz.hg2gui.ui_root"
        const val ACTION_NOROOT = "com.hereliesaz.hg2gui.ui_noroot"
        const val ACTION_LOGTOFILE = "com.hereliesaz.hg2gui.ui_log"
        const val ACTION_CLEAR = "com.hereliesaz.hg2gui.ui_clear"
        const val ACTION_WEATHER = "com.hereliesaz.hg2gui.ui_weather"
        const val ACTION_WEATHER_GOT_LOCATION = "com.hereliesaz.hg2gui.ui_weather_location"
        const val ACTION_WEATHER_DELAY = "com.hereliesaz.hg2gui.ui_weather_delay"
        const val ACTION_WEATHER_MANUAL_UPDATE = "com.hereliesaz.hg2gui.ui_weather_update"
        const val ACTION_PANIC = "com.hereliesaz.hg2gui.ui_panic"

        const val FILE_NAME = "fileName"
        const val PREFS_NAME = "ui"

        const val UNLOCK_KEY = "unlockTimes"
        const val NEXT_UNLOCK_CYCLE_RESTART = "nextUnlockRestart"

        fun getListOfIntValues(values: String, length: Int, defaultValue: Int): IntArray {
            val `is` = IntArray(length)
            val cleanValues = removeSquareBrackets(values)
            val split = cleanValues.split(",").toTypedArray()
            var c = 0
            while (c < split.size && c < length) {
                try {
                    `is`[c] = split[c].trim().toInt()
                } catch (e: Exception) {
                    `is`[c] = defaultValue
                }
                c++
            }
            while (c < length) {
                `is`[c] = defaultValue
                c++
            }
            return `is`
        }

        fun getListOfStringValues(values: String, length: Int, defaultValue: String): Array<String> {
            val `is` = Array(length) { defaultValue }
            val split = values.split(",").toTypedArray()
            val len = split.size.coerceAtMost(`is`.size)
            System.arraycopy(split, 0, `is`, 0, len)
            return `is`
        }

        private val sbPattern = Pattern.compile("[\\[\\]\\s]")
        private fun removeSquareBrackets(s: String): String {
            return sbPattern.matcher(s).replaceAll(Tuils.EMPTYSTRING)
        }

        private fun applyBgRect(v: View, strokeColor: String, bgColor: String, spaces: IntArray, strokeWidth: Int, cornerRadius: Int) {
            try {
                val d = GradientDrawable()
                d.shape = GradientDrawable.RECTANGLE
                d.cornerRadius = cornerRadius.toFloat()
                if (!(strokeColor.startsWith("#00") && strokeColor.length == 9)) {
                    d.setStroke(strokeWidth, Color.parseColor(strokeColor))
                }
                applyMargins(v, spaces)
                d.setColor(Color.parseColor(bgColor))
                v.background = d
            } catch (e: Exception) {
                Tuils.toFile(e)
                Tuils.log(e)
            }
        }

        private fun applyMargins(v: View, margins: IntArray) {
            v.setPadding(margins[2], margins[3], margins[2], margins[3])
            val params = v.layoutParams
            if (params is RelativeLayout.LayoutParams) {
                params.setMargins(margins[0], margins[1], margins[0], margins[1])
            } else if (params is LinearLayout.LayoutParams) {
                params.setMargins(margins[0], margins[1], margins[0], margins[1])
            }
        }

        private fun applyShadow(v: TextView, color: String, x: Int, y: Int, radius: Float) {
            if (!(color.startsWith("#00") && color.length == 9)) {
                v.setShadowLayer(radius, x.toFloat(), y.toFloat(), Color.parseColor(color))
                v.tag = OutlineTextView.SHADOW_TAG
            }
        }
    }

    private enum class Label {
        ram, device, time, battery, storage, network, notes, weather, unlock
    }

    private val RAM_DELAY = 3000
    private val TIME_DELAY = 1000
    private val STORAGE_DELAY = 60 * 1000

    private var handler: Handler? = Handler(Looper.getMainLooper())
    private var policy: DevicePolicyManager? = null
    private var component: ComponentName? = null
    private var gestureDetector: GestureDetectorCompat? = null

    private val preferences: SharedPreferences = mContext.getSharedPreferences(PREFS_NAME, 0)
    private val imm: InputMethodManager = mContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    private var mTerminalAdapter: TerminalManager? = null

    private var mediumPercentage = 0
    private var lowPercentage = 0
    private var batteryFormat: String? = null

    private var hideToolbarNoInput = false
    private var toolbarView: View? = null

    private var labelViews: Array<TextView?> = arrayOfNulls(Label.entries.size)
    private val labelIndexes = FloatArray(labelViews.size)
    private val labelSizes = IntArray(labelViews.size)
    private val labelTexts: Array<CharSequence?> = arrayOfNulls(labelViews.size)

    private val A_DAY = (1000 * 60 * 60 * 24).toLong()
    private var unlockColor = 0
    private var unlockTimeOrder = 0
    private var unlockTimes = 0
    private var unlockHour = 0
    private var unlockMinute = 0
    private val cycleDuration = A_DAY.toInt()
    private var lastUnlockTime = -1L
    private var nextUnlockCycleRestart = 0L
    private var unlockFormat: String? = null
    private var notAvailableText: String? = null
    private var unlockTimeDivider: String? = null
    private val UP_DOWN = 1
    private var lastUnlocks: LongArray? = null

    private val unlockCount = Pattern.compile("%c", Pattern.CASE_INSENSITIVE)
    private val advancement = Pattern.compile("%a(\\d+)(.)")
    private val timePattern = Pattern.compile("(%t\\d*)(?:\\(([^\\)]*)\\))?(\\d+)?")
    private val indexPattern = Pattern.compile("%i", Pattern.CASE_INSENSITIVE)
    private val whenPattern = "%w"

    private val unlockTimeRunnable: Runnable = object : Runnable {
        override fun run() {
            var delay = nextUnlockCycleRestart - System.currentTimeMillis()
            if (delay <= 0) {
                unlockTimes = 0
                lastUnlocks?.let { unlocks ->
                    for (c in unlocks.indices) {
                        unlocks[c] = -1
                    }
                }
                val now = Calendar.getInstance()
                val hour = now[Calendar.HOUR_OF_DAY]
                val minute = now[Calendar.MINUTE]
                if (unlockHour < hour || unlockHour == hour && unlockMinute <= minute) {
                    now.set(Calendar.DAY_OF_YEAR, now[Calendar.DAY_OF_YEAR] + 1)
                }
                val nextRestart = now
                nextRestart[Calendar.HOUR_OF_DAY] = unlockHour
                nextRestart[Calendar.MINUTE] = unlockMinute
                nextRestart[Calendar.SECOND] = 0
                nextUnlockCycleRestart = nextRestart.timeInMillis
                preferences.edit().putLong(NEXT_UNLOCK_CYCLE_RESTART, nextUnlockCycleRestart).putInt(UNLOCK_KEY, 0).apply()
                delay = nextUnlockCycleRestart - System.currentTimeMillis()
                if (delay < 0) delay = 0
            }
            invalidateUnlockText()
            delay = delay.coerceAtMost(cycleDuration / 24L)
            handler?.postDelayed(this, delay)
        }
    }

    private fun getLabelView(l: Label): TextView? {
        return labelViews[labelIndexes[l.ordinal].toInt()]
    }

    private var notesMaxLines = 0
    private var notesManager: NotesManager? = null
    private var notesRunnable: NotesRunnable? = null

    private inner class NotesRunnable : Runnable {
        var updateTime = 2000
        override fun run() {
            notesManager?.let { manager ->
                if (manager.hasChanged) {
                    updateText(Label.notes, Tuils.span(mContext, labelSizes[Label.notes.ordinal], manager.notes))
                }
                handler?.postDelayed(this, updateTime.toLong())
            }
        }
    }

    private var batteryUpdate: BatteryUpdate? = null

    private inner class BatteryUpdate : OnBatteryUpdate {
        var optionalCharging: Pattern? = null
        val value: Pattern = Pattern.compile("%v", Pattern.LITERAL or Pattern.CASE_INSENSITIVE)
        var manyStatus = false
        var loaded = false
        var colorHigh = 0
        var colorMedium = 0
        var colorLow = 0
        var charging = false
        var last = -1f

        override fun update(p: Float) {
            var percentageFloat = p
            if (batteryFormat == null) {
                batteryFormat = XMLPrefsManager.get(Behavior.battery_format)
                val intent = mContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                if (intent == null) {
                    charging = false
                } else {
                    val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
                    charging = plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB
                }
                val optionalSeparator = "\\" + XMLPrefsManager.get(Behavior.optional_values_separator)
                val optional = "%\\(([^$optionalSeparator]*)$optionalSeparator([^)]*)\\)"
                optionalCharging = Pattern.compile(optional, Pattern.CASE_INSENSITIVE)
            }
            if (percentageFloat == -1f) percentageFloat = last
            last = percentageFloat
            if (!loaded) {
                loaded = true
                manyStatus = XMLPrefsManager.getBoolean(Ui.enable_battery_status)
                colorHigh = XMLPrefsManager.getColor(Theme.battery_color_high)
                colorMedium = XMLPrefsManager.getColor(Theme.battery_color_medium)
                colorLow = XMLPrefsManager.getColor(Theme.battery_color_low)
            }
            val percentage = percentageFloat.toInt()
            val color = if (manyStatus) {
                if (percentage > mediumPercentage) colorHigh else if (percentage > lowPercentage) colorMedium else colorLow
            } else {
                colorHigh
            }
            var cp = batteryFormat ?: ""
            val m = optionalCharging?.matcher(cp)
            while (m?.find() == true) {
                cp = cp.replace(m.group(0)!!, if (m.groupCount() == 2) m.group(if (charging) 1 else 2)!! else Tuils.EMPTYSTRING)
            }
            cp = value.matcher(cp).replaceAll(percentage.toString())
            cp = Tuils.patternNewline.matcher(cp).replaceAll(Tuils.NEWLINE)
            updateText(Label.battery, Tuils.span(mContext, cp, color, labelSizes[Label.battery.ordinal]))
        }

        override fun onCharging() {
            charging = true
            update(-1f)
        }

        override fun onNotCharging() {
            charging = false
            update(-1f)
        }
    }

    private var storageRunnable: StorageRunnable? = null

    private inner class StorageRunnable : Runnable {
        private val INT_AV = "%iav"
        private val INT_TOT = "%itot"
        private val EXT_AV = "%eav"
        private val EXT_TOT = "%etot"
        private var storagePatterns: MutableList<Pattern>? = null
        private var storageFormat: String? = null
        var color = 0

        override fun run() {
            if (storageFormat == null) {
                storageFormat = XMLPrefsManager.get(Behavior.storage_format)
                color = XMLPrefsManager.getColor(Theme.storage_color)
            }
            if (storagePatterns == null) {
                storagePatterns = mutableListOf()
                storagePatterns!!.add(Pattern.compile(INT_AV + "tb", Pattern.CASE_INSENSITIVE or Pattern.LITERAL))
                storagePatterns!!.add(Pattern.compile(INT_AV + "gb", Pattern.CASE_INSENSITIVE or Pattern.LITERAL))
                storagePatterns!!.add(Pattern.compile(INT_AV + "mb", Pattern.CASE_INSENSITIVE or Pattern.LITERAL))
                storagePatterns!!.add(Pattern.compile(INT_AV + "kb", Pattern.CASE_INSENSITIVE or Pattern.LITERAL))
                storagePatterns!!.add(Pattern.compile(INT_AV + "b", Pattern.CASE_INSENSITIVE or Pattern.LITERAL))
                storagePatterns!!.add(Pattern.compile(INT_AV + "%", Pattern.CASE_INSENSITIVE or Pattern.LITERAL))
                storagePatterns!!.add(Pattern.compile(INT_TOT + "tb", Pattern.CASE_INSENSITIVE or Pattern.LITERAL))
                storagePatterns!!.add(Pattern.compile(INT_TOT + "gb", Pattern.CASE_INSENSITIVE or Pattern.LITERAL))
                storagePatterns!!.add(Pattern.compile(INT_TOT + "mb", Pattern.CASE_INSENSITIVE or Pattern.LITERAL))
                storagePatterns!!.add(Pattern.compile(INT_TOT + "kb", Pattern.CASE_INSENSITIVE or Pattern.LITERAL))
                storagePatterns!!.add(Pattern.compile(INT_TOT + "b", Pattern.CASE_INSENSITIVE or Pattern.LITERAL))
                storagePatterns!!.add(Pattern.compile(EXT_AV + "tb", Pattern.CASE_INSENSITIVE or Pattern.LITERAL))
                storagePatterns!!.add(Pattern.compile(EXT_AV + "gb", Pattern.CASE_INSENSITIVE or Pattern.LITERAL))
                storagePatterns!!.add(Pattern.compile(EXT_AV + "mb", Pattern.CASE_INSENSITIVE or Pattern.LITERAL))
                storagePatterns!!.add(Pattern.compile(EXT_AV + "kb", Pattern.CASE_INSENSITIVE or Pattern.LITERAL))
                storagePatterns!!.add(Pattern.compile(EXT_AV + "b", Pattern.CASE_INSENSITIVE or Pattern.LITERAL))
                storagePatterns!!.add(Pattern.compile(EXT_AV + "%", Pattern.CASE_INSENSITIVE or Pattern.LITERAL))
                storagePatterns!!.add(Pattern.compile(EXT_TOT + "tb", Pattern.CASE_INSENSITIVE or Pattern.LITERAL))
                storagePatterns!!.add(Pattern.compile(EXT_TOT + "gb", Pattern.CASE_INSENSITIVE or Pattern.LITERAL))
                storagePatterns!!.add(Pattern.compile(EXT_TOT + "mb", Pattern.CASE_INSENSITIVE or Pattern.LITERAL))
                storagePatterns!!.add(Pattern.compile(EXT_TOT + "kb", Pattern.CASE_INSENSITIVE or Pattern.LITERAL))
                storagePatterns!!.add(Pattern.compile(EXT_TOT + "b", Pattern.CASE_INSENSITIVE or Pattern.LITERAL))
                storagePatterns!!.add(Tuils.patternNewline)
                storagePatterns!!.add(Pattern.compile(INT_AV, Pattern.CASE_INSENSITIVE or Pattern.LITERAL))
                storagePatterns!!.add(Pattern.compile(INT_TOT, Pattern.CASE_INSENSITIVE or Pattern.LITERAL))
                storagePatterns!!.add(Pattern.compile(EXT_AV, Pattern.CASE_INSENSITIVE or Pattern.LITERAL))
                storagePatterns!!.add(Pattern.compile(EXT_TOT, Pattern.CASE_INSENSITIVE or Pattern.LITERAL))
            }
            val iav = Tuils.getAvailableInternalMemorySize(Tuils.BYTE).toDouble()
            val itot = Tuils.getTotalInternalMemorySize(Tuils.BYTE).toDouble()
            val eav = Tuils.getAvailableExternalMemorySize(Tuils.BYTE).toDouble()
            val etot = Tuils.getTotalExternalMemorySize(Tuils.BYTE).toDouble()
            var copy = storageFormat ?: ""
            val patterns = storagePatterns!!
            copy = patterns[0].matcher(copy).replaceAll(Matcher.quoteReplacement(Tuils.formatSize(iav.toLong(), Tuils.TERA).toString()))
            copy = patterns[1].matcher(copy).replaceAll(Matcher.quoteReplacement(Tuils.formatSize(iav.toLong(), Tuils.GIGA).toString()))
            copy = patterns[2].matcher(copy).replaceAll(Matcher.quoteReplacement(Tuils.formatSize(iav.toLong(), Tuils.MEGA).toString()))
            copy = patterns[3].matcher(copy).replaceAll(Matcher.quoteReplacement(Tuils.formatSize(iav.toLong(), Tuils.KILO).toString()))
            copy = patterns[4].matcher(copy).replaceAll(Matcher.quoteReplacement(Tuils.formatSize(iav.toLong(), Tuils.BYTE).toString()))
            copy = patterns[5].matcher(copy).replaceAll(Matcher.quoteReplacement(Tuils.percentage(iav, itot).toString()))
            copy = patterns[6].matcher(copy).replaceAll(Matcher.quoteReplacement(Tuils.formatSize(itot.toLong(), Tuils.TERA).toString()))
            copy = patterns[7].matcher(copy).replaceAll(Matcher.quoteReplacement(Tuils.formatSize(itot.toLong(), Tuils.GIGA).toString()))
            copy = patterns[8].matcher(copy).replaceAll(Matcher.quoteReplacement(Tuils.formatSize(itot.toLong(), Tuils.MEGA).toString()))
            copy = patterns[9].matcher(copy).replaceAll(Matcher.quoteReplacement(Tuils.formatSize(itot.toLong(), Tuils.KILO).toString()))
            copy = patterns[10].matcher(copy).replaceAll(Matcher.quoteReplacement(Tuils.formatSize(itot.toLong(), Tuils.BYTE).toString()))
            copy = patterns[11].matcher(copy).replaceAll(Matcher.quoteReplacement(Tuils.formatSize(eav.toLong(), Tuils.TERA).toString()))
            copy = patterns[12].matcher(copy).replaceAll(Matcher.quoteReplacement(Tuils.formatSize(eav.toLong(), Tuils.GIGA).toString()))
            copy = patterns[13].matcher(copy).replaceAll(Matcher.quoteReplacement(Tuils.formatSize(eav.toLong(), Tuils.MEGA).toString()))
            copy = patterns[14].matcher(copy).replaceAll(Matcher.quoteReplacement(Tuils.formatSize(eav.toLong(), Tuils.KILO).toString()))
            copy = patterns[15].matcher(copy).replaceAll(Matcher.quoteReplacement(Tuils.formatSize(eav.toLong(), Tuils.BYTE).toString()))
            copy = patterns[16].matcher(copy).replaceAll(Matcher.quoteReplacement(Tuils.percentage(eav, etot).toString()))
            copy = patterns[17].matcher(copy).replaceAll(Matcher.quoteReplacement(Tuils.formatSize(etot.toLong(), Tuils.TERA).toString()))
            copy = patterns[18].matcher(copy).replaceAll(Matcher.quoteReplacement(Tuils.formatSize(etot.toLong(), Tuils.GIGA).toString()))
            copy = patterns[19].matcher(copy).replaceAll(Matcher.quoteReplacement(Tuils.formatSize(etot.toLong(), Tuils.MEGA).toString()))
            copy = patterns[20].matcher(copy).replaceAll(Matcher.quoteReplacement(Tuils.formatSize(etot.toLong(), Tuils.KILO).toString()))
            copy = patterns[21].matcher(copy).replaceAll(Matcher.quoteReplacement(Tuils.formatSize(etot.toLong(), Tuils.BYTE).toString()))
            copy = patterns[22].matcher(copy).replaceAll(Matcher.quoteReplacement(Tuils.NEWLINE))
            copy = patterns[23].matcher(copy).replaceAll(Matcher.quoteReplacement(Tuils.formatSize(iav.toLong(), Tuils.GIGA).toString()))
            copy = patterns[24].matcher(copy).replaceAll(Matcher.quoteReplacement(Tuils.formatSize(itot.toLong(), Tuils.GIGA).toString()))
            copy = patterns[25].matcher(copy).replaceAll(Matcher.quoteReplacement(Tuils.formatSize(eav.toLong(), Tuils.GIGA).toString()))
            copy = patterns[26].matcher(copy).replaceAll(Matcher.quoteReplacement(Tuils.formatSize(etot.toLong(), Tuils.GIGA).toString()))
            updateText(Label.storage, Tuils.span(mContext, copy, color, labelSizes[Label.storage.ordinal]))
            handler?.postDelayed(this, STORAGE_DELAY.toLong())
        }
    }

    private var timeRunnable: TimeRunnable? = null

    private inner class TimeRunnable : Runnable {
        var active = false
        override fun run() {
            if (!active) active = true
            updateText(Label.time, TimeManager.instance!!.getCharSequence(mContext, labelSizes[Label.time.ordinal], "%t0"))
            handler?.postDelayed(this, TIME_DELAY.toLong())
        }
    }

    private var memory: ActivityManager.MemoryInfo? = null
    private var activityManager: ActivityManager? = null
    private var ramRunnable: RamRunnable? = null

    private inner class RamRunnable : Runnable {
        private val AV = "%av"
        private val TOT = "%tot"
        var ramPatterns: MutableList<Pattern>? = null
        var ramFormat: String? = null
        var color = 0

        override fun run() {
            if (ramFormat == null) {
                ramFormat = XMLPrefsManager.get(Behavior.ram_format)
                color = XMLPrefsManager.getColor(Theme.ram_color)
            }
            if (ramPatterns == null) {
                ramPatterns = mutableListOf()
                ramPatterns!!.add(Pattern.compile(AV + "tb", Pattern.CASE_INSENSITIVE or Pattern.LITERAL))
                ramPatterns!!.add(Pattern.compile(AV + "gb", Pattern.CASE_INSENSITIVE or Pattern.LITERAL))
                ramPatterns!!.add(Pattern.compile(AV + "mb", Pattern.CASE_INSENSITIVE or Pattern.LITERAL))
                ramPatterns!!.add(Pattern.compile(AV + "kb", Pattern.CASE_INSENSITIVE or Pattern.LITERAL))
                ramPatterns!!.add(Pattern.compile(AV + "b", Pattern.CASE_INSENSITIVE or Pattern.LITERAL))
                ramPatterns!!.add(Pattern.compile(AV + "%", Pattern.CASE_INSENSITIVE or Pattern.LITERAL))
                ramPatterns!!.add(Pattern.compile(TOT + "tb", Pattern.CASE_INSENSITIVE or Pattern.LITERAL))
                ramPatterns!!.add(Pattern.compile(TOT + "gb", Pattern.CASE_INSENSITIVE or Pattern.LITERAL))
                ramPatterns!!.add(Pattern.compile(TOT + "mb", Pattern.CASE_INSENSITIVE or Pattern.LITERAL))
                ramPatterns!!.add(Pattern.compile(TOT + "kb", Pattern.CASE_INSENSITIVE or Pattern.LITERAL))
                ramPatterns!!.add(Pattern.compile(TOT + "b", Pattern.CASE_INSENSITIVE or Pattern.LITERAL))
                ramPatterns!!.add(Tuils.patternNewline)
            }
            var copy = ramFormat ?: ""
            val av = Tuils.freeRam(activityManager!!, memory!!).toDouble()
            val tot = (Tuils.totalRam() * 1024L).toDouble()
            val patterns = ramPatterns!!
            copy = patterns[0].matcher(copy).replaceAll(Matcher.quoteReplacement(Tuils.formatSize(av.toLong(), Tuils.TERA).toString()))
            copy = patterns[1].matcher(copy).replaceAll(Matcher.quoteReplacement(Tuils.formatSize(av.toLong(), Tuils.GIGA).toString()))
            copy = patterns[2].matcher(copy).replaceAll(Matcher.quoteReplacement(Tuils.formatSize(av.toLong(), Tuils.MEGA).toString()))
            copy = patterns[3].matcher(copy).replaceAll(Matcher.quoteReplacement(Tuils.formatSize(av.toLong(), Tuils.KILO).toString()))
            copy = patterns[4].matcher(copy).replaceAll(Matcher.quoteReplacement(Tuils.formatSize(av.toLong(), Tuils.BYTE).toString()))
            copy = patterns[5].matcher(copy).replaceAll(Matcher.quoteReplacement(Tuils.percentage(av, tot).toString()))
            copy = patterns[6].matcher(copy).replaceAll(Matcher.quoteReplacement(Tuils.formatSize(tot.toLong(), Tuils.TERA).toString()))
            copy = patterns[7].matcher(copy).replaceAll(Matcher.quoteReplacement(Tuils.formatSize(tot.toLong(), Tuils.GIGA).toString()))
            copy = patterns[8].matcher(copy).replaceAll(Matcher.quoteReplacement(Tuils.formatSize(tot.toLong(), Tuils.MEGA).toString()))
            copy = patterns[9].matcher(copy).replaceAll(Matcher.quoteReplacement(Tuils.formatSize(tot.toLong(), Tuils.KILO).toString()))
            copy = patterns[10].matcher(copy).replaceAll(Matcher.quoteReplacement(Tuils.formatSize(tot.toLong(), Tuils.BYTE).toString()))
            copy = patterns[11].matcher(copy).replaceAll(Matcher.quoteReplacement(Tuils.NEWLINE))
            updateText(Label.ram, Tuils.span(mContext, copy, color, labelSizes[Label.ram.ordinal]))
            handler?.postDelayed(this, RAM_DELAY.toLong())
        }
    }

    private var networkRunnable: NetworkRunnable? = null

    private inner class NetworkRunnable : Runnable {
        val zero = "0"
        val one = "1"
        val on = "on"
        val off = "off"
        val ON = on.uppercase()
        val OFF = off.uppercase()
        val _true = "true"
        val _false = "false"
        val TRUE = _true.uppercase()
        val FALSE = _false.uppercase()

        val w0: Pattern = Pattern.compile("%w0", Pattern.CASE_INSENSITIVE or Pattern.LITERAL)
        val w1: Pattern = Pattern.compile("%w1", Pattern.CASE_INSENSITIVE or Pattern.LITERAL)
        val w2: Pattern = Pattern.compile("%w2", Pattern.CASE_INSENSITIVE or Pattern.LITERAL)
        val w3: Pattern = Pattern.compile("%w3", Pattern.CASE_INSENSITIVE or Pattern.LITERAL)
        val w4: Pattern = Pattern.compile("%w4", Pattern.CASE_INSENSITIVE or Pattern.LITERAL)
        val wn: Pattern = Pattern.compile("%wn", Pattern.CASE_INSENSITIVE or Pattern.LITERAL)
        val d0: Pattern = Pattern.compile("%d0", Pattern.CASE_INSENSITIVE or Pattern.LITERAL)
        val d1: Pattern = Pattern.compile("%d1", Pattern.CASE_INSENSITIVE or Pattern.LITERAL)
        val d2: Pattern = Pattern.compile("%d2", Pattern.CASE_INSENSITIVE or Pattern.LITERAL)
        val d3: Pattern = Pattern.compile("%d3", Pattern.CASE_INSENSITIVE or Pattern.LITERAL)
        val d4: Pattern = Pattern.compile("%d4", Pattern.CASE_INSENSITIVE or Pattern.LITERAL)
        val b0: Pattern = Pattern.compile("%b0", Pattern.CASE_INSENSITIVE or Pattern.LITERAL)
        val b1: Pattern = Pattern.compile("%b1", Pattern.CASE_INSENSITIVE or Pattern.LITERAL)
        val b2: Pattern = Pattern.compile("%b2", Pattern.CASE_INSENSITIVE or Pattern.LITERAL)
        val b3: Pattern = Pattern.compile("%b3", Pattern.CASE_INSENSITIVE or Pattern.LITERAL)
        val b4: Pattern = Pattern.compile("%b4", Pattern.CASE_INSENSITIVE or Pattern.LITERAL)
        val ip4: Pattern = Pattern.compile("%ip4", Pattern.CASE_INSENSITIVE or Pattern.LITERAL)
        val ip6: Pattern = Pattern.compile("%ip6", Pattern.CASE_INSENSITIVE or Pattern.LITERAL)
        val dt: Pattern = Pattern.compile("%dt", Pattern.CASE_INSENSITIVE or Pattern.LITERAL)

        var optionalWifi: Pattern? = null
        var optionalData: Pattern? = null
        var optionalBluetooth: Pattern? = null
        var format: String? = null
        var optionalValueSeparator: String? = null
        var color = 0
        var wifiManager: WifiManager? = null
        var mBluetoothAdapter: BluetoothAdapter? = null
        var connectivityManager: ConnectivityManager? = null
        var method: Method? = null
        var maxDepth = 0
        var updateTime = 0

        override fun run() {
            if (format == null) {
                format = XMLPrefsManager.get(Behavior.network_info_format)
                color = XMLPrefsManager.getColor(Theme.network_info_color)
                maxDepth = XMLPrefsManager.getInt(Behavior.max_optional_depth)
                updateTime = XMLPrefsManager.getInt(Behavior.network_info_update_ms)
                if (updateTime < 1000) updateTime = Behavior.network_info_update_ms.defaultValue().toInt()
                connectivityManager = mContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                wifiManager = mContext.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                optionalValueSeparator = "\\" + XMLPrefsManager.get(Behavior.optional_values_separator)
                val wifiRegex = "%\\(([^$optionalValueSeparator]*)$optionalValueSeparator([^)]*)\\)"
                val dataRegex = "%\\[([^$optionalValueSeparator]*)$optionalValueSeparator([^\\]]*)\\]"
                val bluetoothRegex = "%\\{([^$optionalValueSeparator]*)$optionalValueSeparator([^}]*)\\}"
                optionalWifi = Pattern.compile(wifiRegex, Pattern.CASE_INSENSITIVE)
                optionalBluetooth = Pattern.compile(bluetoothRegex, Pattern.CASE_INSENSITIVE)
                optionalData = Pattern.compile(dataRegex, Pattern.CASE_INSENSITIVE)
                try {
                    val cmClass = Class.forName(connectivityManager!!.javaClass.name)
                    method = cmClass.getDeclaredMethod("getMobileDataEnabled")
                    method!!.isAccessible = true
                } catch (e: Exception) {
                    method = null
                }
            }
            val wifiOn = connectivityManager?.getNetworkInfo(ConnectivityManager.TYPE_WIFI)?.isConnected == true
            var wifiName: String? = null
            if (wifiOn) {
                val connectionInfo = wifiManager?.connectionInfo
                if (connectionInfo != null) {
                    wifiName = connectionInfo.ssid
                }
            }
            var mobileOn = false
            try {
                if (method != null && connectivityManager != null) {
                    mobileOn = method!!.invoke(connectivityManager) as Boolean
                }
            } catch (e: Exception) {
            }
            val mobileType = if (mobileOn) Tuils.getNetworkType(mContext) else "unknown"
            val bluetoothOn = mBluetoothAdapter?.isEnabled == true
            var copy = format ?: ""
            if (maxDepth > 0) {
                copy = apply(1, copy, booleanArrayOf(wifiOn, mobileOn, bluetoothOn), optionalWifi!!, optionalData!!, optionalBluetooth!!)
                copy = apply(1, copy, booleanArrayOf(mobileOn, wifiOn, bluetoothOn), optionalData!!, optionalWifi!!, optionalBluetooth!!)
                copy = apply(1, copy, booleanArrayOf(bluetoothOn, wifiOn, mobileOn), optionalBluetooth!!, optionalWifi!!, optionalData!!)
            }
            copy = w0.matcher(copy).replaceAll(if (wifiOn) one else zero)
            copy = w1.matcher(copy).replaceAll(if (wifiOn) on else off)
            copy = w2.matcher(copy).replaceAll(if (wifiOn) ON else OFF)
            copy = w3.matcher(copy).replaceAll(if (wifiOn) _true else _false)
            copy = w4.matcher(copy).replaceAll(if (wifiOn) TRUE else FALSE)
            copy = wn.matcher(copy).replaceAll(wifiName?.replace("\"", Tuils.EMPTYSTRING) ?: "null")
            copy = d0.matcher(copy).replaceAll(if (mobileOn) one else zero)
            copy = d1.matcher(copy).replaceAll(if (mobileOn) on else off)
            copy = d2.matcher(copy).replaceAll(if (mobileOn) ON else OFF)
            copy = d3.matcher(copy).replaceAll(if (mobileOn) _true else _false)
            copy = d4.matcher(copy).replaceAll(if (mobileOn) TRUE else FALSE)
            copy = b0.matcher(copy).replaceAll(if (bluetoothOn) one else zero)
            copy = b1.matcher(copy).replaceAll(if (bluetoothOn) on else off)
            copy = b2.matcher(copy).replaceAll(if (bluetoothOn) ON else OFF)
            copy = b3.matcher(copy).replaceAll(if (bluetoothOn) _true else _false)
            copy = b4.matcher(copy).replaceAll(if (bluetoothOn) TRUE else FALSE)
            copy = ip4.matcher(copy).replaceAll(NetworkUtils.getIPAddress(true))
            copy = ip6.matcher(copy).replaceAll(NetworkUtils.getIPAddress(false))
            copy = dt.matcher(copy).replaceAll(mobileType)
            copy = Tuils.patternNewline.matcher(copy).replaceAll(Tuils.NEWLINE)
            updateText(Label.network, Tuils.span(mContext, copy, color, labelSizes[Label.network.ordinal]))
            handler?.postDelayed(this, updateTime.toLong())
        }

        private fun apply(depth: Int, s: String, on: BooleanArray, vararg ps: Pattern): String {
            var res = s
            if (ps.isEmpty()) return res
            val m = ps[0].matcher(res)
            while (m.find()) {
                if (m.groupCount() < 2) {
                    res = res.replace(m.group(0)!!, Tuils.EMPTYSTRING)
                    continue
                }
                var g1 = m.group(1)!!
                var g2 = m.group(2)!!
                if (depth < maxDepth) {
                    for (c in 0 until ps.size - 1) {
                        val subOn = BooleanArray(on.size - 1)
                        subOn[0] = on[c + 1]
                        val subPs = arrayOfNulls<Pattern>(ps.size - 1)
                        subPs[0] = ps[c + 1]
                        var j = 1
                        var k = 1
                        while (j < subOn.size) {
                            if (k == c + 1) {
                                k++
                                continue
                            }
                            subOn[j] = on[k]
                            subPs[j] = ps[k]
                            j++
                            k++
                        }
                        @Suppress("UNCHECKED_CAST")
                        g1 = apply(depth + 1, g1, subOn, *(subPs as Array<out Pattern>))
                        @Suppress("UNCHECKED_CAST")
                        g2 = apply(depth + 1, g2, subOn, *(subPs as Array<out Pattern>))
                    }
                }
                res = res.replace(m.group(0)!!, if (on[0]) g1 else g2)
            }
            return res
        }
    }

    private var weatherDelay = 0
    private var lastLatitude = 0.0
    private var lastLongitude = 0.0
    private var location: String? = null
    private var fixedLocation = false
    private var weatherPerformedStartupRun = false
    private var weatherRunnable: WeatherRunnable? = null
    private var weatherColor = 0
    var showWeatherUpdate = false

    private inner class WeatherRunnable : Runnable {
        var key: String? = null
        var url: String? = null

        init {
            if (XMLPrefsManager.wasChanged(Behavior.weather_key, false)) {
                weatherDelay = XMLPrefsManager.getInt(Behavior.weather_update_time)
                key = XMLPrefsManager.get(Behavior.weather_key)
            } else {
                key = Behavior.weather_key.defaultValue()
                weatherDelay = 60 * 60
            }
            weatherDelay *= 1000
            val where = XMLPrefsManager.get(Behavior.weather_location)
            if (where == null || where.isEmpty() || !Tuils.isNumber(where) && !where.contains(",")) {
                TuiLocationManager.instance(mContext).add(ACTION_WEATHER_GOT_LOCATION)
            } else {
                fixedLocation = true
                var w = where
                if (w.contains(",")) {
                    val split = w.split(",").toTypedArray()
                    w = "lat=" + split[0] + "&lon=" + split[1]
                } else {
                    w = "id=$w"
                }
                updateUrl(w)
            }
        }

        override fun run() {
            weatherPerformedStartupRun = true
            if (!fixedLocation) updateUrl(lastLatitude, lastLongitude)
            send()
            handler?.postDelayed(this, weatherDelay.toLong())
        }

        private fun send() {
            if (url == null) return
            val i = Intent(HTMLExtractManager.ACTION_WEATHER)
            i.putExtra(XMLPrefsManager.VALUE_ATTRIBUTE, url)
            i.putExtra(HTMLExtractManager.BROADCAST_COUNT, HTMLExtractManager.broadcastCount)
            LocalBroadcastManager.getInstance(mContext.applicationContext).sendBroadcast(i)
        }

        private fun updateUrl(where: String) {
            url = "http://api.openweathermap.org/data/2.5/weather?$where&appid=$key&units=${XMLPrefsManager.get(Behavior.weather_temperature_measure)}"
        }

        private fun updateUrl(latitude: Double, longitude: Double) {
            url = "http://api.openweathermap.org/data/2.5/weather?lat=$latitude&lon=$longitude&appid=$key&units=${XMLPrefsManager.get(Behavior.weather_temperature_measure)}"
        }
    }

    private fun updateText(l: Label, s: CharSequence?) {
        labelTexts[l.ordinal] = s
        val base = labelIndexes[l.ordinal].toInt()
        val indexs = mutableListOf<Float>()
        for (count in Label.entries.indices) {
            if (labelIndexes[count].toInt() == base && labelTexts[count] != null) {
                indexs.add(labelIndexes[count])
            }
        }
        indexs.sort()
        var sequence: CharSequence = Tuils.EMPTYSTRING
        for (c in indexs.indices) {
            val i = indexs[c]
            for (a in Label.entries.indices) {
                if (i == labelIndexes[a] && labelTexts[a] != null) {
                    sequence = TextUtils.concat(sequence, labelTexts[a])
                }
            }
        }
        val view = labelViews[base]
        if (sequence.isEmpty()) {
            view?.visibility = View.GONE
        } else {
            view?.visibility = View.VISIBLE
            view?.text = sequence
        }
    }

    private var suggestionsManager: SuggestionsManager? = null
    private var terminalView: TextView? = null
    private var panicView: TextView? = null
    private var doubleTapCmd: String? = null
    private var lockOnDbTap = false
    private var receiver: BroadcastReceiver? = null
    var pack: MainPack? = null
    private var clearOnLock = false

    init {
        val filter = IntentFilter().apply {
            addAction(ACTION_UPDATE_SUGGESTIONS)
            addAction(ACTION_UPDATE_HINT)
            addAction(ACTION_ROOT)
            addAction(ACTION_NOROOT)
            addAction(ACTION_LOGTOFILE)
            addAction(ACTION_CLEAR)
            addAction(ACTION_WEATHER)
            addAction(ACTION_WEATHER_GOT_LOCATION)
            addAction(ACTION_WEATHER_DELAY)
            addAction(ACTION_WEATHER_MANUAL_UPDATE)
            addAction(ACTION_PANIC)
        }

        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val action = intent.action ?: return
                when (action) {
                    ACTION_PANIC -> {
                        panicView?.let {
                            it.visibility = View.VISIBLE
                            handler?.postDelayed({ it.visibility = View.GONE }, 3000)
                        }
                    }
                    ACTION_UPDATE_SUGGESTIONS -> suggestionsManager?.requestSuggestion(Tuils.EMPTYSTRING)
                    ACTION_UPDATE_HINT -> mTerminalAdapter?.setDefaultHint()
                    ACTION_ROOT -> mTerminalAdapter?.onRoot()
                    ACTION_NOROOT -> mTerminalAdapter?.onStandard()
                    ACTION_LOGTOFILE -> {
                        val fileName = intent.getStringExtra(FILE_NAME)
                        if (fileName == null || fileName.contains(File.separator)) return
                        val file = File(Tuils.getFolder(), fileName)
                        if (file.exists()) file.delete()
                        try {
                            file.createNewFile()
                            val fos = FileOutputStream(file)
                            fos.write(mTerminalAdapter?.getTerminalText()?.toByteArray() ?: ByteArray(0))
                            fos.close()
                            Tuils.sendOutput(context, "Logged to ${file.absolutePath}")
                        } catch (e: Exception) {
                            Tuils.sendOutput(Color.RED, context, e.toString())
                        }
                    }
                    ACTION_CLEAR -> {
                        mTerminalAdapter?.clear()
                        suggestionsManager?.requestSuggestion(Tuils.EMPTYSTRING)
                    }
                    ACTION_WEATHER -> {
                        val c = Calendar.getInstance()
                        var s = intent.getCharSequenceExtra(XMLPrefsManager.VALUE_ATTRIBUTE)
                        if (s == null) s = intent.getStringExtra(XMLPrefsManager.VALUE_ATTRIBUTE)
                        if (s == null) return
                        s = Tuils.span(context, s, weatherColor, labelSizes[Label.weather.ordinal])
                        updateText(Label.weather, s)
                        if (showWeatherUpdate) {
                            val message = context.getString(R.string.weather_updated) + Tuils.SPACE + c[Calendar.HOUR_OF_DAY] + "." + c[Calendar.MINUTE] + Tuils.SPACE + "(" + lastLatitude + ", " + lastLongitude + ")"
                            Tuils.sendOutput(context, message, TerminalManager.CATEGORY_OUTPUT)
                        }
                    }
                    ACTION_WEATHER_GOT_LOCATION -> {
                        if (intent.getBooleanExtra(TuiLocationManager.FAIL, false)) {
                            weatherRunnable?.let { handler?.removeCallbacks(it) }
                            weatherRunnable = null
                            val s = Tuils.span(context, context.getString(R.string.location_error), weatherColor, labelSizes[Label.weather.ordinal])
                            updateText(Label.weather, s)
                        } else {
                            lastLatitude = intent.getDoubleExtra(TuiLocationManager.LATITUDE, 0.0)
                            lastLongitude = intent.getDoubleExtra(TuiLocationManager.LONGITUDE, 0.0)
                            location = Tuils.locationName(context, lastLatitude, lastLongitude)
                            if (!weatherPerformedStartupRun || XMLPrefsManager.wasChanged(Behavior.weather_key, false)) {
                                weatherRunnable?.let { 
                                    handler?.removeCallbacks(it)
                                    handler?.post(it)
                                }
                            }
                        }
                    }
                    ACTION_WEATHER_DELAY -> {
                        val c = Calendar.getInstance()
                        c.timeInMillis = System.currentTimeMillis() + 1000 * 10
                        if (showWeatherUpdate) {
                            val message = context.getString(R.string.weather_error) + Tuils.SPACE + c[Calendar.HOUR_OF_DAY] + "." + c[Calendar.MINUTE]
                            Tuils.sendOutput(context, message, TerminalManager.CATEGORY_OUTPUT)
                        }
                        weatherRunnable?.let {
                            handler?.removeCallbacks(it)
                            handler?.postDelayed(it, 1000 * 60)
                        }
                    }
                    ACTION_WEATHER_MANUAL_UPDATE -> {
                        weatherRunnable?.let {
                            handler?.removeCallbacks(it)
                            handler?.post(it)
                        }
                    }
                }
            }
        }

        LocalBroadcastManager.getInstance(mContext.applicationContext).registerReceiver(receiver!!, filter)
        policy = mContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        component = ComponentName(mContext, PolicyReceiver::class.java)

        if (!XMLPrefsManager.getBoolean(Ui.system_wallpaper) || !canApplyTheme) {
            rootView.setBackgroundColor(XMLPrefsManager.getColor(Theme.bg_color))
        } else {
            rootView.setBackgroundColor(XMLPrefsManager.getColor(Theme.overlay_color))
        }

        if (XMLPrefsManager.getBoolean(Behavior.auto_scroll)) {
            rootView.viewTreeObserver.addOnGlobalLayoutListener {
                val heightDiff = rootView.rootView.height - rootView.height
                if (heightDiff > Tuils.dpToPx(mContext, 200)) {
                    mTerminalAdapter?.scrollToEnd()
                }
            }
        }

        clearOnLock = XMLPrefsManager.getBoolean(Behavior.clear_on_lock)
        lockOnDbTap = XMLPrefsManager.getBoolean(Behavior.double_tap_lock)
        doubleTapCmd = XMLPrefsManager.get(Behavior.double_tap_cmd)

        if (!lockOnDbTap && doubleTapCmd.isNullOrEmpty()) {
            policy = null
            component = null
            gestureDetector = null
        } else {
            gestureDetector = GestureDetectorCompat(mContext, object : GestureDetector.OnGestureListener {
                override fun onDown(e: MotionEvent): Boolean = false
                override fun onShowPress(e: MotionEvent) {}
                override fun onSingleTapUp(e: MotionEvent): Boolean = false
                override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean = false
                override fun onLongPress(e: MotionEvent) {}
                override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean = false
            }).apply {
                setOnDoubleTapListener(object : OnDoubleTapListener {
                    override fun onSingleTapConfirmed(e: MotionEvent): Boolean = false
                    override fun onDoubleTapEvent(e: MotionEvent): Boolean = true
                    override fun onDoubleTap(e: MotionEvent): Boolean {
                        if (!doubleTapCmd.isNullOrEmpty()) {
                            val input = mTerminalAdapter?.getInput() ?: ""
                            mTerminalAdapter?.setInput(doubleTapCmd!!)
                            mTerminalAdapter?.simulateEnter()
                            mTerminalAdapter?.setInput(input)
                        }
                        if (lockOnDbTap) {
                            val admin = policy?.isAdminActive(component!!) ?: false
                            if (!admin) {
                                val i = Tuils.requestAdmin(component!!, mContext.getString(R.string.admin_permission))
                                mContext.startActivity(i)
                            } else {
                                policy?.lockNow()
                            }
                        }
                        return true
                    }
                })
            }
        }

        val displayMargins = getListOfIntValues(XMLPrefsManager.get(Ui.display_margin_mm) ?: "", 4, 0)
        val metrics = mContext.resources.displayMetrics
        rootView.setPadding(Tuils.mmToPx(metrics, displayMargins[0]), Tuils.mmToPx(metrics, displayMargins[1]), Tuils.mmToPx(metrics, displayMargins[2]), Tuils.mmToPx(metrics, displayMargins[3]))

        labelSizes[Label.time.ordinal] = XMLPrefsManager.getInt(Ui.time_size)
        labelSizes[Label.ram.ordinal] = XMLPrefsManager.getInt(Ui.ram_size)
        labelSizes[Label.battery.ordinal] = XMLPrefsManager.getInt(Ui.battery_size)
        labelSizes[Label.storage.ordinal] = XMLPrefsManager.getInt(Ui.storage_size)
        labelSizes[Label.network.ordinal] = XMLPrefsManager.getInt(Ui.network_size)
        labelSizes[Label.notes.ordinal] = XMLPrefsManager.getInt(Ui.notes_size)
        labelSizes[Label.device.ordinal] = XMLPrefsManager.getInt(Ui.device_size)
        labelSizes[Label.weather.ordinal] = XMLPrefsManager.getInt(Ui.weather_size)
        labelSizes[Label.unlock.ordinal] = XMLPrefsManager.getInt(Ui.unlock_size)

        labelViews = arrayOf(
            rootView.findViewById(R.id.tv0),
            rootView.findViewById(R.id.tv1),
            rootView.findViewById(R.id.tv2),
            rootView.findViewById(R.id.tv3),
            rootView.findViewById(R.id.tv4),
            rootView.findViewById(R.id.tv5),
            rootView.findViewById(R.id.tv6),
            rootView.findViewById(R.id.tv7),
            rootView.findViewById(R.id.tv8)
        )

        val show = BooleanArray(Label.entries.size).apply {
            this[Label.notes.ordinal] = XMLPrefsManager.getBoolean(Ui.show_notes)
            this[Label.ram.ordinal] = XMLPrefsManager.getBoolean(Ui.show_ram)
            this[Label.device.ordinal] = XMLPrefsManager.getBoolean(Ui.show_device_name)
            this[Label.time.ordinal] = XMLPrefsManager.getBoolean(Ui.show_time)
            this[Label.battery.ordinal] = XMLPrefsManager.getBoolean(Ui.show_battery)
            this[Label.network.ordinal] = XMLPrefsManager.getBoolean(Ui.show_network_info)
            this[Label.storage.ordinal] = XMLPrefsManager.getBoolean(Ui.show_storage_info)
            this[Label.weather.ordinal] = XMLPrefsManager.getBoolean(Ui.show_weather)
            this[Label.unlock.ordinal] = XMLPrefsManager.getBoolean(Ui.show_unlock_counter)
        }

        val indexes = FloatArray(Label.entries.size).apply {
            this[Label.notes.ordinal] = if (show[Label.notes.ordinal]) XMLPrefsManager.getFloat(Ui.notes_index) else Int.MAX_VALUE.toFloat()
            this[Label.ram.ordinal] = if (show[Label.ram.ordinal]) XMLPrefsManager.getFloat(Ui.ram_index) else Int.MAX_VALUE.toFloat()
            this[Label.device.ordinal] = if (show[Label.device.ordinal]) XMLPrefsManager.getFloat(Ui.device_index) else Int.MAX_VALUE.toFloat()
            this[Label.time.ordinal] = if (show[Label.time.ordinal]) XMLPrefsManager.getFloat(Ui.time_index) else Int.MAX_VALUE.toFloat()
            this[Label.battery.ordinal] = if (show[Label.battery.ordinal]) XMLPrefsManager.getFloat(Ui.battery_index) else Int.MAX_VALUE.toFloat()
            this[Label.network.ordinal] = if (show[Label.network.ordinal]) XMLPrefsManager.getFloat(Ui.network_index) else Int.MAX_VALUE.toFloat()
            this[Label.storage.ordinal] = if (show[Label.storage.ordinal]) XMLPrefsManager.getFloat(Ui.storage_index) else Int.MAX_VALUE.toFloat()
            this[Label.weather.ordinal] = if (show[Label.weather.ordinal]) XMLPrefsManager.getFloat(Ui.weather_index) else Int.MAX_VALUE.toFloat()
            this[Label.unlock.ordinal] = if (show[Label.unlock.ordinal]) XMLPrefsManager.getFloat(Ui.unlock_index) else Int.MAX_VALUE.toFloat()
        }

        val statusLineAlignments = getListOfIntValues(XMLPrefsManager.get(Ui.status_lines_alignment) ?: "", 9, -1)
        val statusLinesBgRectColors = getListOfStringValues(XMLPrefsManager.get(Theme.status_lines_bgrectcolor) ?: "", 9, "#ff000000")
        val otherBgRectColors = arrayOf(
            XMLPrefsManager.get(Theme.input_bgrectcolor) ?: "",
            XMLPrefsManager.get(Theme.output_bgrectcolor) ?: "",
            XMLPrefsManager.get(Theme.suggestions_bgrectcolor) ?: "",
            XMLPrefsManager.get(Theme.toolbar_bgrectcolor) ?: ""
        )
        val bgRectColors = Array(statusLinesBgRectColors.size + otherBgRectColors.size) { "" }
        System.arraycopy(statusLinesBgRectColors, 0, bgRectColors, 0, statusLinesBgRectColors.size)
        System.arraycopy(otherBgRectColors, 0, bgRectColors, statusLinesBgRectColors.size, otherBgRectColors.size)

        val statusLineBgColors = getListOfStringValues(XMLPrefsManager.get(Theme.status_lines_bg) ?: "", 9, "#00000000")
        val otherBgColors = arrayOf(
            XMLPrefsManager.get(Theme.input_bg) ?: "",
            XMLPrefsManager.get(Theme.output_bg) ?: "",
            XMLPrefsManager.get(Theme.suggestions_bg) ?: "",
            XMLPrefsManager.get(Theme.toolbar_bg) ?: ""
        )
        val bgColors = Array(statusLineBgColors.size + otherBgColors.size) { "" }
        System.arraycopy(statusLineBgColors, 0, bgColors, 0, statusLineBgColors.size)
        System.arraycopy(otherBgColors, 0, bgColors, statusLineBgColors.size, otherBgColors.size)

        val statusLineOutlineColors = getListOfStringValues(XMLPrefsManager.get(Theme.status_lines_shadow_color) ?: "", 9, "#00000000")
        val otherOutlineColors = arrayOf(
            XMLPrefsManager.get(Theme.input_shadow_color) ?: "",
            XMLPrefsManager.get(Theme.output_shadow_color) ?: ""
        )
        val outlineColors = Array(statusLineOutlineColors.size + otherOutlineColors.size) { "" }
        System.arraycopy(statusLineOutlineColors, 0, outlineColors, 0, statusLineOutlineColors.size)
        System.arraycopy(otherOutlineColors, 0, outlineColors, 9, otherOutlineColors.size)

        val shadowParams = getListOfStringValues(XMLPrefsManager.get(Ui.shadow_params) ?: "", 3, "0")
        val shadowXOffset = shadowParams[0].toInt()
        val shadowYOffset = shadowParams[1].toInt()
        val shadowRadius = shadowParams[2].toFloat()

        val INPUT_BGCOLOR_INDEX = 9
        val OUTPUT_BGCOLOR_INDEX = 10
        val SUGGESTIONS_BGCOLOR_INDEX = 11
        val TOOLBAR_BGCOLOR_INDEX = 12

        val rectParams = getListOfStringValues(XMLPrefsManager.get(Ui.bgrect_params) ?: "", 2, "0")
        val strokeWidth = rectParams[0].toInt()
        val cornerRadius = rectParams[1].toInt()

        val OUTPUT_MARGINS_INDEX = 1
        val INPUTAREA_MARGINS_INDEX = 2
        val INPUTFIELD_MARGINS_INDEX = 3
        val TOOLBAR_MARGINS_INDEX = 4
        val SUGGESTIONS_MARGINS_INDEX = 5

        val margins = Array(6) { IntArray(4) }
        margins[0] = getListOfIntValues(XMLPrefsManager.get(Ui.status_lines_margins) ?: "", 4, 0)
        margins[1] = getListOfIntValues(XMLPrefsManager.get(Ui.output_field_margins) ?: "", 4, 0)
        margins[2] = getListOfIntValues(XMLPrefsManager.get(Ui.input_area_margins) ?: "", 4, 0)
        margins[3] = getListOfIntValues(XMLPrefsManager.get(Ui.input_field_margins) ?: "", 4, 0)
        margins[4] = getListOfIntValues(XMLPrefsManager.get(Ui.toolbar_margins) ?: "", 4, 0)
        margins[5] = getListOfIntValues(XMLPrefsManager.get(Ui.suggestions_area_margin) ?: "", 4, 0)

        val sequence = AllowEqualsSequence(indexes, Label.entries.toTypedArray())
        val lViewsParent = labelViews[0]?.parent as? LinearLayout

        var effectiveCount = 0
        for (count in labelViews.indices) {
            labelViews[count]?.setOnTouchListener(this)
            val os = sequence.get(count)
            for (j in os.indices) {
                val i = (os[j] as Label).ordinal
                val v = count.toFloat() + j.toFloat() * 0.1f
                labelIndexes[i] = v
            }
            if (count >= sequence.minKey && count <= sequence.maxKey && os.isNotEmpty()) {
                labelViews[count]?.typeface = Tuils.getTypeface(mContext)
                val ec = effectiveCount++
                val p = statusLineAlignments[ec]
                if (p >= 0) {
                    labelViews[count]?.gravity = if (p == 0) Gravity.CENTER_HORIZONTAL else Gravity.RIGHT
                }
                if (count.toFloat() != labelIndexes[Label.notes.ordinal]) {
                    labelViews[count]?.isVerticalScrollBarEnabled = false
                }
                labelViews[count]?.let { 
                    applyBgRect(it, bgRectColors[count], bgColors[count], margins[0], strokeWidth, cornerRadius)
                    applyShadow(it, outlineColors[count], shadowXOffset, shadowYOffset, shadowRadius)
                }
            } else {
                lViewsParent?.removeView(labelViews[count])
                labelViews[count] = null
            }
        }

        if (show[Label.ram.ordinal]) {
            ramRunnable = RamRunnable()
            memory = ActivityManager.MemoryInfo()
            activityManager = mContext.getSystemService(Activity.ACTIVITY_SERVICE) as ActivityManager
            handler?.post(ramRunnable!!)
        }
        if (show[Label.storage.ordinal]) {
            storageRunnable = StorageRunnable()
            handler?.post(storageRunnable!!)
        }
        if (show[Label.device.ordinal]) {
            val USERNAME = Pattern.compile("%u", Pattern.CASE_INSENSITIVE or Pattern.LITERAL)
            val DV = Pattern.compile("%d", Pattern.CASE_INSENSITIVE or Pattern.LITERAL)
            var deviceFormat = XMLPrefsManager.get(Behavior.device_format)
            val username = XMLPrefsManager.get(Ui.username)
            var deviceName = XMLPrefsManager.get(Ui.deviceName)
            if (deviceName.isNullOrEmpty()) {
                deviceName = Build.DEVICE
            }
            deviceFormat = USERNAME.matcher(deviceFormat).replaceAll(Matcher.quoteReplacement(username ?: "null"))
            deviceFormat = DV.matcher(deviceFormat).replaceAll(Matcher.quoteReplacement(deviceName))
            deviceFormat = Tuils.patternNewline.matcher(deviceFormat).replaceAll(Matcher.quoteReplacement(Tuils.NEWLINE))
            updateText(Label.device, Tuils.span(mContext, deviceFormat, XMLPrefsManager.getColor(Theme.device_color), labelSizes[Label.device.ordinal]))
        }
        if (show[Label.time.ordinal]) {
            timeRunnable = TimeRunnable()
            handler?.post(timeRunnable!!)
        }
        if (show[Label.battery.ordinal]) {
            batteryUpdate = BatteryUpdate()
            mediumPercentage = XMLPrefsManager.getInt(Behavior.battery_medium)
            lowPercentage = XMLPrefsManager.getInt(Behavior.battery_low)
            Tuils.registerBatteryReceiver(mContext, batteryUpdate!!)
        }
        if (show[Label.network.ordinal]) {
            networkRunnable = NetworkRunnable()
            handler?.post(networkRunnable!!)
        }

        val notesView = getLabelView(Label.notes)
        notesManager = NotesManager(mContext, notesView)
        if (show[Label.notes.ordinal]) {
            notesRunnable = NotesRunnable()
            handler?.post(notesRunnable!!)
            notesView?.movementMethod = LinkMovementMethod()
            notesMaxLines = XMLPrefsManager.getInt(Ui.notes_max_lines)
            if (notesMaxLines > 0) {
                notesView?.maxLines = notesMaxLines
                notesView?.ellipsize = TextUtils.TruncateAt.MARQUEE
                if (XMLPrefsManager.getBoolean(Ui.show_scroll_notes_message)) {
                    notesView?.viewTreeObserver?.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                        var linesBefore = Int.MIN_VALUE
                        override fun onGlobalLayout() {
                            if (notesView!!.lineCount > notesMaxLines && linesBefore <= notesMaxLines) {
                                Tuils.sendOutput(Color.RED, mContext, R.string.note_max_reached)
                            }
                            linesBefore = notesView!!.lineCount
                        }
                    })
                }
            }
        }

        if (show[Label.weather.ordinal]) {
            weatherRunnable = WeatherRunnable()
            weatherColor = XMLPrefsManager.getColor(Theme.weather_color)
            val where = XMLPrefsManager.get(Behavior.weather_location)
            if (where != null && (where.contains(",") || Tuils.isNumber(where))) handler?.post(weatherRunnable!!)
            showWeatherUpdate = XMLPrefsManager.getBoolean(Behavior.show_weather_updates)
        }

        if (show[Label.unlock.ordinal]) {
            unlockTimes = preferences.getInt(UNLOCK_KEY, 0)
            unlockColor = XMLPrefsManager.getColor(Theme.unlock_counter_color)
            unlockFormat = XMLPrefsManager.get(Behavior.unlock_counter_format)
            notAvailableText = XMLPrefsManager.get(Behavior.not_available_text)
            unlockTimeDivider = XMLPrefsManager.get(Behavior.unlock_time_divider)
            unlockTimeDivider = Tuils.patternNewline.matcher(unlockTimeDivider).replaceAll(Tuils.NEWLINE)
            val start = XMLPrefsManager.get(Behavior.unlock_counter_cycle_start)
            val p = Pattern.compile("(\\d{1,2}).(\\d{1,2})")
            var m = p.matcher(start)
            if (!m.find()) {
                m = p.matcher(Behavior.unlock_counter_cycle_start.defaultValue())
                m.find()
            }
            unlockHour = m.group(1).toInt()
            unlockMinute = m.group(2).toInt()
            unlockTimeOrder = XMLPrefsManager.getInt(Behavior.unlock_time_order)
            nextUnlockCycleRestart = preferences.getLong(NEXT_UNLOCK_CYCLE_RESTART, 0)
            m = timePattern.matcher(unlockFormat)
            if (m.find()) {
                val s = m.group(3)
                val count = if (s.isNullOrEmpty()) 1 else s.toInt()
                lastUnlocks = LongArray(count) { -1L }
                registerLockReceiver()
                handler?.post(unlockTimeRunnable)
            } else {
                lastUnlocks = null
            }
        }

        val inputBottom = XMLPrefsManager.getBoolean(Ui.input_bottom)
        val layoutId = if (inputBottom) R.layout.input_down_layout else R.layout.input_up_layout
        val inflater = mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val inputOutputView = inflater.inflate(layoutId, null)
        rootView.addView(inputOutputView)
        panicView = rootView.findViewById(R.id.panic_overlay)
        terminalView = inputOutputView.findViewById<TextView>(R.id.terminal_view).apply {
            setOnTouchListener(this@UIManager)
            (parent.parent as? View)?.setOnTouchListener(this@UIManager)
            applyBgRect(this, bgRectColors[OUTPUT_BGCOLOR_INDEX], bgColors[OUTPUT_BGCOLOR_INDEX], margins[OUTPUT_MARGINS_INDEX], strokeWidth, cornerRadius)
            applyShadow(this, outlineColors[OUTPUT_BGCOLOR_INDEX], shadowXOffset, shadowYOffset, shadowRadius)
        }

        val inputView = inputOutputView.findViewById<EditText>(R.id.input_view)
        val prefixView = inputOutputView.findViewById<TextView>(R.id.prefix_view)
        applyBgRect(inputOutputView.findViewById(R.id.input_group), bgRectColors[INPUT_BGCOLOR_INDEX], bgColors[INPUT_BGCOLOR_INDEX], margins[INPUTAREA_MARGINS_INDEX], strokeWidth, cornerRadius)
        applyShadow(inputView, outlineColors[INPUT_BGCOLOR_INDEX], shadowXOffset, shadowYOffset, shadowRadius)
        applyShadow(prefixView, outlineColors[INPUT_BGCOLOR_INDEX], shadowXOffset, shadowYOffset, shadowRadius)
        applyMargins(inputView, margins[INPUTFIELD_MARGINS_INDEX])
        applyMargins(prefixView, margins[INPUTFIELD_MARGINS_INDEX])

        var submitView: ImageView? = inputOutputView.findViewById(R.id.submit_tv)
        if (!XMLPrefsManager.getBoolean(Ui.show_enter_button)) {
            submitView?.visibility = View.GONE
            submitView = null
        }

        var backView: ImageButton? = null
        var nextView: ImageButton? = null
        var deleteView: ImageButton? = null
        var pasteView: ImageButton? = null
        if (!XMLPrefsManager.getBoolean(XMLToolbar.show_toolbar)) {
            inputOutputView.findViewById<View>(R.id.tools_view).visibility = View.GONE
            toolbarView = null
        } else {
            backView = inputOutputView.findViewById(R.id.back_view)
            nextView = inputOutputView.findViewById(R.id.next_view)
            deleteView = inputOutputView.findViewById(R.id.delete_view)
            pasteView = inputOutputView.findViewById(R.id.paste_view)
            toolbarView = inputOutputView.findViewById(R.id.tools_view)
            hideToolbarNoInput = XMLPrefsManager.getBoolean(XMLToolbar.hide_toolbar_no_input)
            toolbarView?.let { applyBgRect(it, bgRectColors[TOOLBAR_BGCOLOR_INDEX], bgColors[TOOLBAR_BGCOLOR_INDEX], margins[TOOLBAR_MARGINS_INDEX], strokeWidth, cornerRadius) }
        }

        mTerminalAdapter = TerminalManager(
            terminalView!!, inputView, prefixView, submitView, backView, nextView, deleteView, pasteView,
            mContext, mainPack, executer
        )

        inputOutputView.findViewById<LinearLayout>(R.id.cli_tabs_group)?.let { group ->
            val tabs = arrayOf("./script.sh", "brew install", "code .", "clear", "ls -la", "switch-os macos", "switch-os windows")
            for (tabCmd in tabs) {
                val tabView = inflater.inflate(R.layout.item_cli_tab, group, false) as TextView
                tabView.text = tabCmd
                tabView.setOnClickListener {
                    mTerminalAdapter?.let { adapter ->
                        adapter.setInput(tabCmd)
                        adapter.simulateEnter()
                    }
                }
                group.addView(tabView)
            }
        }

        if (XMLPrefsManager.getBoolean(Suggestions.show_suggestions)) {
            rootView.findViewById<HorizontalScrollView>(R.id.suggestions_container)?.apply {
                isFocusable = false
                onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus -> if (hasFocus) v.clearFocus() }
                applyBgRect(this, bgRectColors[SUGGESTIONS_BGCOLOR_INDEX], bgColors[SUGGESTIONS_BGCOLOR_INDEX], margins[SUGGESTIONS_MARGINS_INDEX], strokeWidth, cornerRadius)
            }
            val suggestionsView = rootView.findViewById<LinearLayout>(R.id.suggestions_group)
            suggestionsManager = SuggestionsManager(suggestionsView, mainPack, mTerminalAdapter)
            inputView.addTextChangedListener(SuggestionTextWatcher(suggestionsManager, object : OnTextChanged {
                override fun textChanged(currentText: String, before: Int) {
                    if (!hideToolbarNoInput) return
                    if (currentText.isEmpty()) toolbarView?.visibility = View.GONE
                    else if (before == 0) toolbarView?.visibility = View.VISIBLE
                }
            }))
        } else {
            rootView.findViewById<View>(R.id.suggestions_group).visibility = View.GONE
        }

        OutlineTextView.redrawTimes = XMLPrefsManager.getInt(Ui.text_redraw_times).coerceAtLeast(1)
    }

    fun dispose() {
        handler?.removeCallbacksAndMessages(null)
        handler = null
        suggestionsManager?.dispose()
        notesManager?.dispose(mContext)
        LocalBroadcastManager.getInstance(mContext.applicationContext).unregisterReceiver(receiver!!)
        Tuils.unregisterBatteryReceiver(mContext)
        Tuils.cancelFont()
        unregisterLockReceiver()
    }

    fun openKeyboard() {
        mTerminalAdapter?.let {
            it.requestInputFocus()
            imm.showSoftInput(it.getInputView(), InputMethodManager.SHOW_IMPLICIT)
        }
    }

    fun closeKeyboard() {
        mTerminalAdapter?.let { imm.hideSoftInputFromWindow(it.getInputWindowToken(), 0) }
    }

    fun onStart(openKeyboardOnStart: Boolean) {
        if (openKeyboardOnStart) openKeyboard()
    }

    fun setInput(s: String?) {
        if (s == null) return
        mTerminalAdapter?.setInput(s)
        mTerminalAdapter?.focusInputEnd()
    }

    fun setHint(hint: String) {
        mTerminalAdapter?.setHint(hint)
    }

    fun resetHint() {
        mTerminalAdapter?.setDefaultHint()
    }

    fun setOutput(s: CharSequence?, category: Int) {
        mTerminalAdapter?.setOutput(s, category)
    }

    fun setOutput(color: Int, output: CharSequence?) {
        mTerminalAdapter?.setOutput(color, output)
    }

    fun disableSuggestions() {
        suggestionsManager?.disable()
    }

    fun enableSuggestions() {
        suggestionsManager?.enable()
    }

    fun onBackPressed() {
        mTerminalAdapter?.onBackPressed()
    }

    fun focusTerminal() {
        mTerminalAdapter?.requestInputFocus()
    }

    fun pause() {
        closeKeyboard()
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        gestureDetector?.onTouchEvent(event)
        return v.onTouchEvent(event)
    }

    fun buildRedirectionListener(): OnRedirectionListener {
        return object : OnRedirectionListener {
            override fun onRedirectionRequest(cmdObj: Any?) {
                if (cmdObj !is RedirectCommand) return
                (mContext as Activity).runOnUiThread {
                    mTerminalAdapter?.setHint(mContext.getString(cmdObj.hint))
                    disableSuggestions()
                }
            }
            override fun onRedirectionEnd(cmdObj: Any?) {
                if (cmdObj !is RedirectCommand) return
                (mContext as Activity).runOnUiThread {
                    mTerminalAdapter?.setDefaultHint()
                    enableSuggestions()
                }
            }
        }
    }

    private var lockReceiver: BroadcastReceiver? = null
    private fun registerLockReceiver() {
        if (lockReceiver != null) return
        val theFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        lockReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val strAction = intent.action
                val myKM = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
                if (strAction == Intent.ACTION_USER_PRESENT || strAction == Intent.ACTION_SCREEN_OFF || strAction == Intent.ACTION_SCREEN_ON) {
                    if (myKM.inKeyguardRestrictedInputMode()) onLock() else onUnlock()
                }
            }
        }
        mContext.applicationContext.registerReceiver(lockReceiver, theFilter)
    }

    private fun unregisterLockReceiver() {
        lockReceiver?.let { mContext.applicationContext.unregisterReceiver(it) }
    }

    private fun onLock() {
        if (clearOnLock) {
            mTerminalAdapter?.clear()
        }
    }

    private fun onUnlock() {
        if (System.currentTimeMillis() - lastUnlockTime < 1000 || lastUnlocks == null) return
        lastUnlockTime = System.currentTimeMillis()
        unlockTimes++
        val unlocks = lastUnlocks!!
        System.arraycopy(unlocks, 0, unlocks, 1, unlocks.size - 1)
        unlocks[0] = lastUnlockTime
        preferences.edit().putInt(UNLOCK_KEY, unlockTimes).apply()
        invalidateUnlockText()
    }

    private fun invalidateUnlockText() {
        var cp = unlockFormat ?: ""
        cp = unlockCount.matcher(cp).replaceAll(unlockTimes.toString())
        cp = Tuils.patternNewline.matcher(cp).replaceAll(Tuils.NEWLINE)
        val m = advancement.matcher(cp)
        if (m.find()) {
            val denominator = m.group(1)!!.toInt()
            val divider = m.group(2)!!
            val lastCycleStart = nextUnlockCycleRestart - cycleDuration
            val elapsed = (System.currentTimeMillis() - lastCycleStart).toInt()
            val numerator = denominator * elapsed / cycleDuration
            cp = m.replaceAll(numerator.toString() + divider + denominator)
        }
        var s: CharSequence = Tuils.span(mContext, cp, unlockColor, labelSizes[Label.unlock.ordinal])
        val timeMatcher = timePattern.matcher(cp)
        if (timeMatcher.find()) {
            val timeGroup = timeMatcher.group(1)!!
            var text = timeMatcher.group(2)
            if (text == null) text = whenPattern
            var cs: CharSequence = Tuils.EMPTYSTRING
            var c: Int
            val change: Int
            if (unlockTimeOrder == UP_DOWN) {
                c = 0
                change = +1
            } else {
                c = lastUnlocks!!.size - 1
                change = -1
            }
            val unlocks = lastUnlocks!!
            for (counter in unlocks.indices) {
                var t = text!!
                t = indexPattern.matcher(t).replaceAll((c + 1).toString())
                cs = TextUtils.concat(cs, t)
                val time: CharSequence = if (unlocks[c] > 0) {
                    TimeManager.instance!!.getCharSequence(timeGroup, unlocks[c]) ?: ""
                } else {
                    notAvailableText ?: ""
                }
                cs = TextUtils.replace(cs, arrayOf(whenPattern), arrayOf(time))
                if (counter != unlocks.size - 1) cs = TextUtils.concat(cs, unlockTimeDivider)
                c += change
            }
            s = TextUtils.replace(s, arrayOf(timeMatcher.group(0)), arrayOf(cs))
        }
        updateText(Label.unlock, s)
    }
}
