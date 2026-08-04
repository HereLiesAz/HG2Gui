package com.hereliesaz.hg2gui.tuils

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.ActivityManager
import android.app.ActivityManager.MemoryInfo
import android.app.admin.DevicePolicyManager
import android.content.*
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.location.Geocoder
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.telephony.TelephonyManager
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.text.style.AbsoluteSizeSpan
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.FileProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.hereliesaz.hg2gui.R
import com.hereliesaz.hg2gui.commands.main.MainPack
import com.hereliesaz.hg2gui.managers.TerminalManager
import com.hereliesaz.hg2gui.managers.music.MusicManager2
import com.hereliesaz.hg2gui.managers.music.Song
import com.hereliesaz.hg2gui.managers.notifications.NotificationService
import com.hereliesaz.hg2gui.managers.xml.XMLPrefsManager
import com.hereliesaz.hg2gui.managers.xml.classes.XMLPrefsSave
import com.hereliesaz.hg2gui.managers.xml.options.Behavior
import com.hereliesaz.hg2gui.managers.xml.options.Ui
import com.hereliesaz.hg2gui.tuils.interfaces.OnBatteryUpdate
import com.hereliesaz.hg2gui.tuils.stuff.FakeLauncherActivity
import dalvik.system.DexFile
import org.w3c.dom.Node
import org.xml.sax.SAXParseException
import java.io.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.HttpURLConnection
import java.net.URL
import java.nio.channels.Channels
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

object Tuils {

    const val SPACE = " "
    const val DOUBLE_SPACE = "  "
    const val NEWLINE = "\n"
    const val TRIBLE_SPACE = "   "
    const val DOT = "."
    const val EMPTYSTRING = ""
    private const val TUI_FOLDER = "t-ui"
    const val MINUS = "-"

    val patternNewline: Pattern = Pattern.compile("%n", Pattern.CASE_INSENSITIVE or Pattern.LITERAL)

    private var applicationContext: Context? = null

    fun init(context: Context) {
        applicationContext = context.applicationContext
    }

    private var globalTypeface: Typeface? = null
    var fontPath: String? = null

    private val calculusPattern = Pattern.compile("([+\\-*/^])(\\d+\\.?\\d*)")

    fun textCalculus(input: Double, text: String): Double {
        var result = input
        val m = calculusPattern.matcher(text)
        while (m.find()) {
            val operator = m.group(1)!![0]
            val value = m.group(2)!!.toDouble()

            when (operator) {
                '+' -> result += value
                '-' -> result -= value
                '*' -> result *= value
                '/' -> result /= value
                '^' -> result = Math.pow(result, value)
            }

            log("now im", result)
        }
        return result
    }

    fun getTypeface(context: Context): Typeface? {
        if (globalTypeface == null) {
            try {
                XMLPrefsManager.loadCommons(context)
            } catch (e: Exception) {
                return null
            }

            val systemFont = XMLPrefsManager.getBoolean(Ui.system_font)
            if (systemFont) {
                globalTypeface = Typeface.DEFAULT
            } else {
                val tui = getFolder()
                if (tui == null) {
                    return Typeface.createFromAsset(context.assets, "lucida_console.ttf")
                }

                val p = Pattern.compile(".[ot]tf$")
                var font: File? = null
                val files = tui.listFiles()
                if (files != null) {
                    for (f in files) {
                        val name = f.name
                        if (p.matcher(name).find()) {
                            font = f
                            fontPath = f.absolutePath
                            break
                        }
                    }
                }

                if (font != null) {
                    try {
                        globalTypeface = Typeface.createFromFile(font)
                        if (globalTypeface == null) throw UnsupportedOperationException()
                    } catch (e: Exception) {
                        globalTypeface = null
                    }
                }
            }

            if (globalTypeface == null) {
                globalTypeface = if (systemFont) Typeface.DEFAULT else Typeface.createFromAsset(context.assets, "lucida_console.ttf")
            }
        }
        return globalTypeface
    }

    fun cancelFont() {
        globalTypeface = null
        fontPath = null
    }

    fun locationName(context: Context, lat: Double, lng: Double): String? {
        val geocoder = Geocoder(context, Locale.getDefault())
        return try {
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            addresses?.get(0)?.getAddressLine(2)
        } catch (e: Exception) {
            null
        }
    }

    fun notificationServiceIsRunning(context: Context): Boolean {
        val collectorComponent = ComponentName(context, NotificationService::class.java)
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningServices = manager.getRunningServices(Int.MAX_VALUE) ?: return false

        for (service in runningServices) {
            if (service.service == collectorComponent) {
                if (service.pid == Process.myPid()) {
                    return true
                }
            }
        }

        return false
    }

    fun arrayContains(array: IntArray?, value: Int): Boolean {
        if (array == null) return false
        for (i in array) {
            if (i == value) return true
        }
        return false
    }

    @Throws(IOException::class)
    fun readerToString(initialReader: Reader): String {
        val arr = CharArray(8 * 1024)
        val buffer = StringBuilder()
        var numCharsRead: Int
        while (initialReader.read(arr, 0, arr.size).also { numCharsRead = it } != -1) {
            buffer.append(arr, 0, numCharsRead)
        }
        initialReader.close()
        return buffer.toString()
    }

    private var batteryUpdate: OnBatteryUpdate? = null
    private var batteryReceiver: BroadcastReceiver? = null

    fun registerBatteryReceiver(context: Context, listener: OnBatteryUpdate) {
        try {
            batteryReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    val update = batteryUpdate ?: return
                    when (intent.action) {
                        Intent.ACTION_BATTERY_CHANGED -> {
                            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
                            update.update(level.toFloat())
                        }
                        Intent.ACTION_POWER_CONNECTED -> update.onCharging()
                        Intent.ACTION_POWER_DISCONNECTED -> update.onNotCharging()
                    }
                }
            }

            val iFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            iFilter.addAction(Intent.ACTION_POWER_CONNECTED)
            iFilter.addAction(Intent.ACTION_POWER_DISCONNECTED)

            context.registerReceiver(batteryReceiver, iFilter)
            batteryUpdate = listener
        } catch (e: Exception) {
            toFile(e)
        }
    }

    fun unregisterBatteryReceiver(context: Context) {
        batteryReceiver?.let {
            context.unregisterReceiver(it)
            batteryReceiver = null
        }
    }

    fun containsExtension(array: Array<String>?, value: String?): Boolean {
        if (array == null || value == null) return false
        return try {
            val lowerValue = value.lowercase(Locale.getDefault()).trim()
            for (s in array) {
                if (lowerValue.endsWith(s)) return true
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    fun getSongsInFolder(folder: File): List<Song> {
        val songs = ArrayList<Song>()
        val files = folder.listFiles() ?: return songs

        for (file in files) {
            if (file.isDirectory) {
                songs.addAll(getSongsInFolder(file))
            } else if (containsExtension(MusicManager2.MUSIC_EXTENSIONS, file.name)) {
                songs.add(Song(file))
            }
        }
        return songs
    }

    fun convertStreamToString(inputStream: InputStream?): String {
        if (inputStream == null) return EMPTYSTRING
        val s = Scanner(inputStream).useDelimiter("\\A")
        return if (s.hasNext()) s.next() else EMPTYSTRING
    }

    @Throws(Exception::class)
    fun download(inputStream: InputStream, file: File): Long {
        val out = FileOutputStream(file, false)
        val data = ByteArray(1024)
        var bytes: Long = 0
        var count: Int
        while (inputStream.read(data).also { count = it } != -1) {
            out.write(data, 0, count)
            bytes += count.toLong()
        }
        out.flush()
        out.close()
        inputStream.close()
        return bytes
    }

    @Throws(Exception::class)
    fun write(file: File, separator: String, vararg ss: String) {
        val headerStream = FileOutputStream(file, false)
        for (c in 0 until ss.size - 1) {
            headerStream.write(ss[c].toByteArray())
            headerStream.write(separator.toByteArray())
        }
        headerStream.write(ss[ss.size - 1].toByteArray())
        headerStream.flush()
        headerStream.close()
    }

    fun dpToPx(context: Context, valueInDp: Float): Float {
        val metrics = context.resources.displayMetrics
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, valueInDp, metrics)
    }

    fun hasNotificationAccess(context: Context): Boolean {
        val pkgName = "com.hereliesaz.hg2gui"
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        if (!flat.isNullOrEmpty()) {
            val names = flat.split(":")
            for (name in names) {
                val cn = ComponentName.unflattenFromString(name)
                if (cn != null && TextUtils.equals(pkgName, cn.packageName)) {
                    return true
                }
            }
        }
        return false
    }

    fun resetPreferredLauncherAndOpenChooser(context: Context) {
        val packageManager = context.packageManager
        val componentName = ComponentName(context, FakeLauncherActivity::class.java)
        packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)

        val selector = Intent(Intent.ACTION_MAIN)
        selector.addCategory(Intent.CATEGORY_HOME)
        selector.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(selector)

        packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, PackageManager.DONT_KILL_APP)
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    fun openSettingsPage(c: Context, packageName: String) {
        val intent = Intent()
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        c.startActivity(intent)
    }

    fun requestAdmin(component: ComponentName, explanation: String): Intent {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component)
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, explanation)
        return intent
    }

    fun webPage(url: String): Intent {
        return Intent(Intent.ACTION_VIEW, Uri.parse(url))
    }

    fun getAvailableInternalMemorySize(unit: Int): Double {
        return getAvailableSpace(Environment.getDataDirectory(), unit)
    }

    fun getTotalInternalMemorySize(unit: Int): Double {
        return getTotalSpace(Environment.getDataDirectory(), unit)
    }

    fun getAvailableExternalMemorySize(unit: Int): Double {
        return try {
            getAvailableSpace(XMLPrefsManager.get(File::class.java, Behavior.external_storage_path), unit)
        } catch (e: Exception) {
            -1.0
        }
    }

    fun getTotalExternalMemorySize(unit: Int): Double {
        return try {
            getTotalSpace(XMLPrefsManager.get(File::class.java, Behavior.external_storage_path), unit)
        } catch (e: Exception) {
            -1.0
        }
    }

    fun getAvailableSpace(dir: File?, unit: Int): Double {
        if (dir == null) return -1.0
        val statFs = StatFs(dir.absolutePath)
        val blocks = statFs.availableBlocksLong
        return formatSize(blocks * statFs.blockSizeLong, unit)
    }

    fun getTotalSpace(dir: File?, unit: Int): Double {
        if (dir == null) return -1.0
        val statFs = StatFs(dir.absolutePath)
        val blocks = statFs.blockCountLong
        return formatSize(blocks * statFs.blockSizeLong, unit)
    }

    fun percentage(part: Double, total: Double): Double {
        return round(part * 100 / total, 2)
    }

    fun formatSize(bytes: Long, unit: Int): Double {
        val convert = 1048576.0
        val smallConvert = 1024.0
        val result = when (unit) {
            TERA -> bytes / convert / convert
            GIGA -> bytes / convert / smallConvert
            MEGA -> bytes / convert
            KILO -> bytes / smallConvert
            BYTE -> bytes.toDouble()
            else -> return -1.0
        }
        return round(result, 2)
    }

    fun isMyLauncherDefault(packageManager: PackageManager): Boolean {
        val filter = IntentFilter(Intent.ACTION_MAIN)
        filter.addCategory(Intent.CATEGORY_HOME)
        val filters = ArrayList<IntentFilter>()
        filters.add(filter)
        val myPackageName = "com.hereliesaz.hg2gui"
        val activities = ArrayList<ComponentName>()
        packageManager.getPreferredActivities(filters, activities, null)
        for (activity in activities) {
            if (myPackageName == activity.packageName) {
                return true
            }
        }
        return false
    }

    fun span(text: CharSequence?, color: Int): SpannableString {
        return span(null, text, color, Int.MAX_VALUE)
    }

    fun span(context: Context?, size: Int, text: CharSequence?): SpannableString {
        return span(context, text, Int.MAX_VALUE, size)
    }

    fun span(context: Context?, text: CharSequence?, color: Int, size: Int): SpannableString {
        return span(context, Int.MAX_VALUE, color, text, size)
    }

    fun span(bgColor: Int, foreColor: Int, text: CharSequence?): SpannableString {
        return span(null, bgColor, foreColor, text, Int.MAX_VALUE)
    }

    fun span(context: Context?, bgColor: Int, foreColor: Int, text: CharSequence?, size: Int): SpannableString {
        val txt = text ?: EMPTYSTRING
        val spannableString = if (txt is SpannableString) txt else SpannableString(txt)
        if (size != Int.MAX_VALUE && context != null) {
            spannableString.setSpan(AbsoluteSizeSpan(convertSpToPixels(size.toFloat(), context)), 0, txt.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        if (foreColor != Int.MAX_VALUE) {
            spannableString.setSpan(ForegroundColorSpan(foreColor), 0, txt.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        if (bgColor != Int.MAX_VALUE) {
            spannableString.setSpan(BackgroundColorSpan(bgColor), 0, txt.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return spannableString
    }

    fun span(bgColor: Int, text: SpannableString, section: String, fromIndex: Int): Int {
        val index = text.toString().indexOf(section, fromIndex)
        if (index == -1) return index
        text.setSpan(BackgroundColorSpan(bgColor), index, index + section.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        return index + section.length
    }

    fun convertSpToPixels(sp: Float, context: Context): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, context.resources.displayMetrics).toInt()
    }

    fun inputStreamToString(inputStream: InputStream): String {
        val s = Scanner(inputStream).useDelimiter("\\A")
        return if (s.hasNext()) s.next() else EMPTYSTRING
    }

    abstract class ArgsRunnable : Runnable {
        private var args: Array<out Any?>? = null

        fun setArgs(vararg args: Any?) {
            this.args = args
        }

        fun run(vararg args: Any?) {
            setArgs(*args)
            run()
        }

        fun <T> get(c: Class<T>, index: Int): T? {
            val currentArgs = args ?: return null
            return if (index < currentArgs.size) currentArgs[index] as? T else null
        }
    }

    fun deleteContentOnly(dir: File) {
        val files = dir.listFiles() ?: return
        for (f in files) {
            if (f.isDirectory) delete(f)
            f.delete()
        }
    }

    fun delete(dir: File) {
        val files = dir.listFiles() ?: return
        for (f in files) {
            if (f.isDirectory) delete(f)
            f.delete()
        }
        dir.delete()
    }

    fun insertOld(oldFile: File?): Boolean {
        if (oldFile == null || !oldFile.exists()) return false
        val oldPath = oldFile.absolutePath
        val oldFolder = File(getFolder(), "old")
        if (!oldFolder.exists()) oldFolder.mkdir()
        val dest = File(oldFolder, oldFile.name)
        if (dest.exists()) dest.delete()
        return oldFile.renameTo(dest) && File(oldPath).delete()
    }

    fun getOld(name: String): File? {
        val old = File(getFolder(), "old")
        val file = File(old, name)
        return if (file.exists()) file else null
    }

    fun deepView(v: View) {
        log(v.toString())
        if (v !is ViewGroup) return
        log(v.childCount)
        for (c in 0 until v.childCount) deepView(v.getChildAt(c))
        log("end of parents of: $v")
    }

    private val deepClickListener = View.OnClickListener { v -> log(v.toString()) }

    fun deepClickView(v: View) {
        v.setOnClickListener(deepClickListener)
        if (v !is ViewGroup) return
        for (c in 0 until v.childCount) deepClickView(v.getChildAt(c))
    }

    @Throws(NoSuchElementException::class)
    fun scaleImage(view: ImageView, newX: Int, newY: Int) {
        val bitmap = try {
            val drawing = view.drawable
            (drawing as BitmapDrawable).bitmap
        } catch (e: Exception) {
            throw NoSuchElementException("No drawable on given view")
        }

        val width = bitmap.width
        val height = bitmap.height
        val xBounding = dpToPx(view.context, newX)
        val yBounding = dpToPx(view.context, newY)

        val xScale = xBounding.toFloat() / width
        val yScale = yBounding.toFloat() / height
        val scale = if (xScale <= yScale) xScale else yScale

        val matrix = Matrix()
        matrix.postScale(scale, scale)

        val scaledBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true)
        val scaledWidth = scaledBitmap.width
        val scaledHeight = scaledBitmap.height
        val result = BitmapDrawable(scaledBitmap)

        view.setImageDrawable(result)

        val params = view.layoutParams as LinearLayout.LayoutParams
        params.width = scaledWidth
        params.height = scaledHeight
        view.layoutParams = params
    }

    fun dpToPx(context: Context, dp: Int): Int {
        val density = context.applicationContext.resources.displayMetrics.density
        return Math.round(dp.toFloat() * density)
    }

    fun sendOutput(context: Context, res: Int) {
        sendOutput(Int.MAX_VALUE, context, res)
    }

    fun sendOutput(color: Int, context: Context, res: Int) {
        sendOutput(color, context, context.getString(res))
    }

    fun sendOutput(context: Context, res: Int, type: Int) {
        sendOutput(Int.MAX_VALUE, context, res, type)
    }

    fun sendOutput(color: Int, context: Context, res: Int, type: Int) {
        sendOutput(color, context, context.getString(res), type)
    }

    fun sendOutput(context: Context, s: CharSequence) {
        sendOutput(Int.MAX_VALUE, context, s)
    }

    fun sendOutput(color: Int, context: Context, s: CharSequence) {
        sendOutput(color, context, s, TerminalManager.CATEGORY_OUTPUT)
    }

    fun sendOutput(context: Context, s: CharSequence, type: Int) {
        sendOutput(Int.MAX_VALUE, context, s, type)
    }

    fun sendOutput(color: Int, context: Context, s: CharSequence, type: Int) {
        val intent = Intent(PrivateIOReceiver.ACTION_OUTPUT)
        intent.putExtra(PrivateIOReceiver.TEXT, s)
        intent.putExtra(PrivateIOReceiver.COLOR, color)
        intent.putExtra(PrivateIOReceiver.TYPE, type)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    fun sendOutput(mainPack: MainPack, s: CharSequence, type: Int) {
        sendOutput(mainPack.commandColor, mainPack.context, s, type)
    }

    fun sendOutput(context: Context, s: CharSequence, type: Int, action: Any?) {
        sendOutput(Int.MAX_VALUE, context, s, type, action)
    }

    fun sendOutput(color: Int, context: Context, s: CharSequence, type: Int, action: Any?) {
        val intent = Intent(PrivateIOReceiver.ACTION_OUTPUT)
        intent.putExtra(PrivateIOReceiver.TEXT, s)
        intent.putExtra(PrivateIOReceiver.COLOR, color)
        intent.putExtra(PrivateIOReceiver.TYPE, type)

        when (action) {
            is String -> intent.putExtra(PrivateIOReceiver.ACTION, action)
            is Parcelable -> intent.putExtra(PrivateIOReceiver.ACTION, action)
        }

        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    fun sendOutput(context: Context, s: CharSequence, type: Int, action: Any?, longAction: Any?) {
        sendOutput(Int.MAX_VALUE, context, s, type, action, longAction)
    }

    fun sendOutput(color: Int, context: Context, s: CharSequence, type: Int, action: Any?, longAction: Any?) {
        val intent = Intent(PrivateIOReceiver.ACTION_OUTPUT)
        intent.putExtra(PrivateIOReceiver.TEXT, s)
        intent.putExtra(PrivateIOReceiver.COLOR, color)
        intent.putExtra(PrivateIOReceiver.TYPE, type)

        when (action) {
            is String -> intent.putExtra(PrivateIOReceiver.ACTION, action)
            is Parcelable -> intent.putExtra(PrivateIOReceiver.ACTION, action)
        }

        when (longAction) {
            is String -> intent.putExtra(PrivateIOReceiver.LONG_ACTION, longAction)
            is Parcelable -> intent.putExtra(PrivateIOReceiver.LONG_ACTION, longAction)
        }

        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    fun sendInput(context: Context, text: String) {
        val intent = Intent(PrivateIOReceiver.ACTION_INPUT)
        intent.putExtra(PrivateIOReceiver.TEXT, text)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    const val TERA = 0
    const val GIGA = 1
    const val MEGA = 2
    const val KILO = 3
    const val BYTE = 4

    private var total: Long = -1

    fun freeRam(mgr: ActivityManager, info: MemoryInfo): Double {
        mgr.getMemoryInfo(info)
        return info.availMem.toDouble()
    }

    fun totalRam(): Long {
        if (total > 0) return total
        try {
            val reader = BufferedReader(InputStreamReader(FileInputStream("/proc/meminfo")))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line?.startsWith("MemTotal") == true) {
                    line = line!!.replace("\\D+".toRegex(), EMPTYSTRING)
                    total = line!!.toLong()
                    return total
                }
            }
        } catch (e: Exception) {
        }
        return 0
    }

    fun round(value: Double, places: Int): Double {
        var p = places
        if (p < 0) p = 0
        return try {
            val bd = BigDecimal(value)
            bd.setScale(p, RoundingMode.HALF_UP).toDouble()
        } catch (e: Exception) {
            value
        }
    }

    @Throws(IOException::class)
    fun getClassesInPackage(packageName: String, c: Context): List<String> {
        val classes = ArrayList<String>()
        val packageCodePath = c.packageCodePath
        val df = DexFile(packageCodePath)
        val iter = df.entries()
        while (iter.hasMoreElements()) {
            val className = iter.nextElement()
            if (className.contains(packageName) && !className.contains("$")) {
                classes.add(className.substring(className.lastIndexOf(".") + 1))
            }
        }
        return classes
    }

    fun scale(from: IntArray, to: IntArray, n: Int): Int {
        return (to[1] - to[0]) * (n - from[0]) / (from[1] - from[0]) + to[0]
    }

    fun toString(enums: Array<Enum<*>>): Array<String> {
        return Array(enums.size) { i -> enums[i].name }
    }

    private fun getNicePath(filePath: String?): String {
        if (filePath == null) return "null"
        val home = XMLPrefsManager.get(File::class.java, Behavior.home_path).absolutePath
        return when {
            filePath == home -> "~"
            filePath.startsWith(home) -> "~" + filePath.replace(home, EMPTYSTRING)
            else -> filePath
        }
    }

    fun find(o: Any?, array: Array<Any?>): Int {
        return find(o, array.toList())
    }

    fun find(o: Any?, list: List<Any?>): Int {
        for (count in list.indices) {
            val x = list[count] ?: continue
            if (o === x) return count

            if (o is XMLPrefsSave) {
                try {
                    if (o.label() == x as? String) return count
                } catch (e: Exception) {
                }
            }

            if (o is String && x is XMLPrefsSave) {
                try {
                    if (x.label() == o) return count
                } catch (e: Exception) {
                }
            }

            try {
                if (o == x || x == o) return count
            } catch (e: Exception) {
                continue
            }
        }
        return -1
    }

    private val pd = Pattern.compile("%d", Pattern.CASE_INSENSITIVE or Pattern.LITERAL)
    private val pu = Pattern.compile("%u", Pattern.CASE_INSENSITIVE or Pattern.LITERAL)
    private val pp = Pattern.compile("%p", Pattern.CASE_INSENSITIVE or Pattern.LITERAL)

    fun getHint(currentPath: String): String? {
        if (!XMLPrefsManager.getBoolean(Ui.show_session_info)) return null
        var format = XMLPrefsManager.get(Behavior.session_info_format)
        if (format.isEmpty()) return null

        var deviceName = XMLPrefsManager.get(Ui.deviceName)
        if (deviceName.isNullOrEmpty()) {
            deviceName = Build.DEVICE
        }

        var username = XMLPrefsManager.get(Ui.username)
        if (username == null) username = EMPTYSTRING

        format = pd.matcher(format).replaceAll(Matcher.quoteReplacement(deviceName))
        format = pu.matcher(format).replaceAll(Matcher.quoteReplacement(username))
        format = pp.matcher(format).replaceAll(Matcher.quoteReplacement(getNicePath(currentPath)))

        return format
    }

    fun findPrefix(list: List<String>, prefix: String): Int {
        for (count in list.indices) {
            if (list[count].startsWith(prefix)) return count
        }
        return -1
    }

    fun mmToPx(metrics: DisplayMetrics, mm: Int): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_MM, mm.toFloat(), metrics).toInt()
    }

    fun insertHeaders(s: MutableList<String>, newLine: Boolean) {
        var current: Char = 0.toChar()
        var count = 0
        while (count < s.size) {
            val st = s[count].trim().uppercase(Locale.getDefault())
            if (st.isEmpty()) {
                count++
                continue
            }
            val c = st[0]
            if (current != c) {
                s.add(count, (if (newLine) NEWLINE else EMPTYSTRING) + c + (if (newLine) NEWLINE else EMPTYSTRING))
                current = c
                count++
            }
            count++
        }
    }

    fun addPrefix(list: MutableList<String>, prefix: String) {
        for (count in list.indices) {
            list[count] = prefix + list[count]
        }
    }

    fun addSeparator(list: MutableList<String>, separator: String) {
        for (count in list.indices) {
            list[count] = list[count] + separator
        }
    }

    fun toPlanString(strings: Array<String>?, separator: String): String {
        if (strings == null) return EMPTYSTRING
        val output = StringBuilder()
        for (count in strings.indices) {
            output.append(strings[count])
            if (count < strings.size - 1) output.append(separator)
        }
        return output.toString()
    }

    fun toPlanString(strings: Array<String>?): String {
        return if (strings != null) toPlanString(strings, NEWLINE) else EMPTYSTRING
    }

    fun toPlanString(separator: String, strings: List<*>?): String {
        if (strings == null) return EMPTYSTRING
        val output = StringBuilder()
        for (count in strings.indices) {
            output.append(strings[count].toString())
            if (count < strings.size - 1) output.append(separator)
        }
        return output.toString()
    }

    fun nodeToString(node: Node): String? {
        return try {
            val transfac = TransformerFactory.newInstance()
            val trans = transfac.newTransformer()
            trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
            trans.setOutputProperty(OutputKeys.INDENT, "yes")
            val sw = StringWriter()
            val result = StreamResult(sw)
            val source = DOMSource(node)
            trans.transform(source, result)
            sw.toString()
        } catch (e: TransformerException) {
            e.printStackTrace()
            null
        }
    }

    fun log(o: Any?) {
        if (o is Throwable) {
            Log.e("andre", "", o)
        } else {
            val text = if (o is Array<*>) o.contentToString() else o.toString()
            Log.e("andre", text)
        }
    }

    fun log(o: Any?, o2: Any?) {
        if (o is Array<*> && o2 is Array<*>) {
            Log.e("andre", o.contentToString() + " -- " + o2.contentToString())
        } else {
            Log.e("andre", o.toString() + " -- " + o2.toString())
        }
    }

    fun log(o: Any?, to: PrintStream) {
        if (o is Throwable) {
            o.printStackTrace(to)
        } else {
            val text = if (o is Array<*>) o.contentToString() else o.toString()
            try {
                to.write(text.toByteArray())
            } catch (e: IOException) {
                log(e)
            }
        }
    }

    fun log(o: Any?, o2: Any?, to: OutputStream) {
        try {
            if (o is Array<*> && o2 is Array<*>) {
                to.write((o.contentToString() + " -- " + o2.contentToString()).toByteArray())
            } else {
                to.write((o.toString() + " -- " + o2.toString()).toByteArray())
            }
        } catch (e: Exception) {
            log(e)
        }
    }

    fun hasInternetAccess(): Boolean {
        return try {
            val urlc = URL("http://clients3.google.com/generate_204").openConnection() as HttpURLConnection
            urlc.responseCode == 204 && urlc.contentLength == 0
        } catch (e: IOException) {
            false
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getDefaultValue(clazz: Class<T>): T {
        return java.lang.reflect.Array.get(java.lang.reflect.Array.newInstance(clazz, 1), 0) as T
    }

    fun toFile(s: String) {
        try {
            val f = RandomAccessFile(File(getFolder(), "crash.txt"), "rw")
            f.seek(0)
            f.write((Date().toString() + NEWLINE + NEWLINE).toByteArray())
            val isChannel = Channels.newOutputStream(f.channel)
            isChannel.write(s.toByteArray())
            f.write((NEWLINE + NEWLINE).toByteArray())
            isChannel.close()
            f.close()
        } catch (e1: Exception) {
        }
    }

    fun toFile(o: Any?) {
        if (o == null) return
        try {
            val stream = FileOutputStream(File(getFolder(), "crash.txt"))
            stream.write((NEWLINE + NEWLINE).toByteArray())
            if (o is Throwable) {
                val ps = PrintStream(stream)
                o.printStackTrace(ps)
            } else {
                stream.write(o.toString().toByteArray())
            }
            stream.write((NEWLINE + "----------------------------").toByteArray())
            stream.close()
        } catch (e1: Exception) {
        }
    }

    fun toPlanString(strings: List<String>?, separator: String): String {
        return if (strings != null) {
            toPlanString(strings.toTypedArray() as Array<String>, separator)
        } else EMPTYSTRING
    }

    fun filesToPlanString(files: List<File>?, separator: String): String? {
        if (files.isNullOrEmpty()) {
            return null
        }
        val builder = StringBuilder()
        val limit = files.size - 1
        for (count in files.indices) {
            builder.append(files[count].name)
            if (count < limit) {
                builder.append(separator)
            }
        }
        return builder.toString()
    }

    fun toPlanString(strings: List<String>): String {
        return toPlanString(strings, NEWLINE)
    }

    fun toPlanString(objs: Array<Any?>?, separator: String): String {
        if (objs == null) return EMPTYSTRING
        val output = StringBuilder()
        for (count in objs.indices) {
            output.append(objs[count])
            if (count < objs.size - 1) {
                output.append(separator)
            }
        }
        return output.toString()
    }

    private val unnecessarySpaces = Pattern.compile("\\s{2,}")
    fun removeUnnecessarySpaces(string: String): String {
        return unnecessarySpaces.matcher(string).replaceAll(SPACE)
    }

    fun getStackTrace(throwable: Throwable): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw, true)
        throwable.printStackTrace(pw)
        return sw.buffer.toString()
    }

    fun isAlpha(s: String?): Boolean {
        if (s == null) return false
        val chars = s.toCharArray()
        for (c in chars) if (!Character.isLetter(c)) return false
        return true
    }

    fun isPhoneNumber(s: String?): Boolean {
        if (s == null) return false
        val chars = s.toCharArray()
        for (c in chars) if (Character.isLetter(c)) return false
        return true
    }

    fun firstNonDigit(s: String?): Char {
        if (s == null) return 0.toChar()
        val chars = s.toCharArray()
        for (c in chars) if (!Character.isDigit(c)) return c
        return 0.toChar()
    }

    fun isNumber(s: String?): Boolean {
        if (s.isNullOrEmpty()) return false
        val chars = s.toCharArray()
        for (c in chars) if (!Character.isDigit(c)) return false
        return true
    }

    fun openFile(c: Context, f: File): Intent {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val u = buildFile(c, f)
        val mimetype = MimeTypes.getMimeType(f.absolutePath, f.isDirectory)
        intent.setDataAndType(u, mimetype)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
        } else {
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        }
        intent.addFlags(flags)
        return intent
    }

    fun shareFile(c: Context, f: File): Intent {
        val intent = Intent(Intent.ACTION_SEND)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val u = buildFile(c, f)
        val mimetype = MimeTypes.getMimeType(f.absolutePath, f.isDirectory)
        intent.setDataAndType(u, mimetype)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.putExtra(Intent.EXTRA_STREAM, u)
        return intent
    }

    private fun buildFile(context: Context, file: File): Uri {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            Uri.fromFile(file)
        } else {
            FileProvider.getUriForFile(context, GenericFileProvider.PROVIDER_NAME, file)
        }
    }

    private fun getTuiFolder(): File {
        var base: File? = null
        if (applicationContext != null) {
            base = applicationContext!!.getExternalFilesDir(null)
            if (base == null) base = applicationContext!!.filesDir
        }
        if (base == null) base = Environment.getExternalStorageDirectory()
        return File(base, TUI_FOLDER)
    }

    fun eval(str: String): Double {
        return object {
            var pos = -1
            var ch = 0

            fun nextChar() {
                ch = if (++pos < str.length) str[pos].code else -1
            }

            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.code) nextChar()
                if (ch == charToEat) {
                    nextChar()
                    return true
                }
                return false
            }

            fun parse(): Double {
                nextChar()
                val x = parseExpression()
                if (pos < str.length) throw RuntimeException("Unexpected: " + ch.toChar())
                return x
            }

            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    if (eat('+'.code)) x += parseTerm()
                    else if (eat('-'.code)) x -= parseTerm()
                    else return x
                }
            }

            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    if (eat('*'.code)) x *= parseFactor()
                    else if (eat('/'.code)) x /= parseFactor()
                    else return x
                }
            }

            fun parseFactor(): Double {
                if (eat('+'.code)) return parseFactor()
                if (eat('-'.code)) return -parseFactor()
                var x: Double
                val startPos = pos
                if (eat('('.code)) {
                    x = parseExpression()
                    eat(')'.code)
                } else if (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) {
                    while (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) nextChar()
                    x = str.substring(startPos, pos).toDouble()
                } else if (ch >= 'a'.code && ch <= 'z'.code) {
                    while (ch >= 'a'.code && ch <= 'z'.code) nextChar()
                    val func = str.substring(startPos, pos)
                    x = parseFactor()
                    x = when (func) {
                        "sqrt" -> Math.sqrt(x)
                        "sin" -> Math.sin(Math.toRadians(x))
                        "cos" -> Math.cos(Math.toRadians(x))
                        "tan" -> Math.tan(Math.toRadians(x))
                        else -> throw RuntimeException("Unknown function: $func")
                    }
                } else {
                    throw RuntimeException("Unexpected: " + ch.toChar())
                }
                if (eat('^'.code)) x = Math.pow(x, parseFactor())
                return x
            }
        }.parse()
    }

    fun getTextFromClipboard(context: Context): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
                val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val item = manager.primaryClip!!.getItemAt(0)
                item.text.toString()
            } else {
                val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.text.ClipboardManager
                manager.text.toString()
            }
        } catch (e: Exception) {
            null
        }
    }

    fun dpToPx(resources: Resources, dp: Int): Int {
        val displayMetrics = resources.displayMetrics
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT))
    }

    private const val FILEUPDATE_DELAY = 100
    private var folder: File? = null

    fun getFolder(): File? {
        if (folder != null) return folder
        var elapsedTime = 0
        while (elapsedTime < 1000) {
            val tuiFolder = getTuiFolder()
            if ((tuiFolder.exists() && tuiFolder.isDirectory) || tuiFolder.mkdir()) {
                folder = tuiFolder
                return folder
            }
            try {
                Thread.sleep(FILEUPDATE_DELAY.toLong())
            } catch (e: InterruptedException) {
            }
            elapsedTime += FILEUPDATE_DELAY
        }
        return null
    }

    fun alphabeticCompare(s1: String, s2: String): Int {
        val cmd1 = removeSpaces(s1).lowercase(Locale.getDefault())
        val cmd2 = removeSpaces(s2).lowercase(Locale.getDefault())
        var count = 0
        while (count < cmd1.length && count < cmd2.length) {
            val c1 = cmd1[count]
            val c2 = cmd2[count]
            if (c1 < c2) {
                return -1
            } else if (c1 > c2) {
                return 1
            }
            count++
        }
        if (s1.length > s2.length) {
            return 1
        } else if (s1.length < s2.length) {
            return -1
        }
        return 0
    }

    private const val SPACE_REGEXP = "\\s"
    fun removeSpaces(string: String): String {
        return string.replace(SPACE_REGEXP.toRegex(), EMPTYSTRING)
    }

    @SuppressLint("MissingPermission")
    fun getNetworkType(context: Context): String {
        val mTelephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val networkType = mTelephonyManager.networkType
        return when (networkType) {
            TelephonyManager.NETWORK_TYPE_GPRS, TelephonyManager.NETWORK_TYPE_EDGE, TelephonyManager.NETWORK_TYPE_CDMA, TelephonyManager.NETWORK_TYPE_1xRTT, TelephonyManager.NETWORK_TYPE_IDEN -> "2g"
            TelephonyManager.NETWORK_TYPE_UMTS, TelephonyManager.NETWORK_TYPE_EVDO_0, TelephonyManager.NETWORK_TYPE_EVDO_A, TelephonyManager.NETWORK_TYPE_HSDPA, TelephonyManager.NETWORK_TYPE_HSUPA, TelephonyManager.NETWORK_TYPE_HSPA, TelephonyManager.NETWORK_TYPE_EVDO_B, TelephonyManager.NETWORK_TYPE_EHRPD, TelephonyManager.NETWORK_TYPE_HSPAP -> "3g"
            TelephonyManager.NETWORK_TYPE_LTE -> "4g"
            else -> "unknown"
        }
    }

    @SuppressLint("DiscouragedPrivateApi", "SoonBlockedPrivateApi")
    fun setCursorDrawableColor(editText: EditText, color: Int) {
        try {
            val fCursorDrawableRes = TextView::class.java.getDeclaredField("mCursorDrawableRes")
            fCursorDrawableRes.isAccessible = true
            val mCursorDrawableRes = fCursorDrawableRes.getInt(editText)
            val fEditor = TextView::class.java.getDeclaredField("mEditor")
            fEditor.isAccessible = true
            val editor = fEditor.get(editText)
            val clazz = editor.javaClass
            val fCursorDrawable = clazz.getDeclaredField("mCursorDrawable")
            fCursorDrawable.isAccessible = true
            val drawables = arrayOfNulls<Drawable>(2)
            drawables[0] = editText.context.resources.getDrawable(mCursorDrawableRes)
            drawables[1] = editText.context.resources.getDrawable(mCursorDrawableRes)
            drawables[0]!!.setColorFilter(color, PorterDuff.Mode.SRC_IN)
            drawables[1]!!.setColorFilter(color, PorterDuff.Mode.SRC_IN)
            fCursorDrawable.set(editor, drawables)
        } catch (ignored: Throwable) {
        }
    }

    fun nOfBytes(file: File): Int {
        var count = 0
        try {
            val `in` = FileInputStream(file)
            while (`in`.read() != -1) count++
            `in`.close()
        } catch (e: IOException) {
            log(e)
        }
        return count
    }

    fun sendXMLParseError(context: Context, path: String, e: SAXParseException) {
        sendOutput(
            Color.RED,
            context, context.getString(R.string.output_xmlproblem1) + SPACE + path + context.getString(R.string.output_xmlproblem2) + NEWLINE + context.getString(R.string.output_errorlabel) +
                    "File: " + e.systemId + NEWLINE +
                    "Message" + e.message + NEWLINE +
                    "Line" + e.lineNumber + NEWLINE +
                    "Column" + e.columnNumber
        )
    }

    fun sendXMLParseError(context: Context, path: String) {
        sendOutput(Color.RED, context, context.getString(R.string.output_xmlproblem1) + SPACE + path + context.getString(R.string.output_xmlproblem2))
    }
}
