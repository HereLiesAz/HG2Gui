package com.hereliesaz.hg2gui.managers

import android.content.Context
import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import com.hereliesaz.hg2gui.managers.xml.XMLPrefsManager
import com.hereliesaz.hg2gui.managers.xml.options.Behavior
import com.hereliesaz.hg2gui.managers.xml.options.Theme
import com.hereliesaz.hg2gui.tuils.SimpleMutableEntry
import com.hereliesaz.hg2gui.tuils.Tuils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.regex.Pattern

/**
 * Manages time formatting throughout the application.
 *
 * This class interprets the %t[index] placeholder used in various UI elements (e.g. prompt, status bar).
 * Users can define multiple time formats (e.g. %t0 = "HH:mm", %t1 = "dd/MM") in the configuration XML.
 * This manager parses those formats and replaces placeholders with the current time.
 */
class TimeManager(context: Context) {

    // Cache of DateFormats indexed by the integer specified in %t[index]
    // Key = Color (optional override), Value = SimpleDateFormat
    private var dateFormatList: Array<MutableMap.MutableEntry<Int, SimpleDateFormat>?>? = null

    init {
        val format = XMLPrefsManager.get(Behavior.time_format) ?: ""
        val separator = XMLPrefsManager.get(Behavior.time_format_separator) ?: ""

        // Split multiple formats by separator
        val formats = if (format.isNotEmpty() && separator.isNotEmpty()) {
            format.split(separator.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        } else if (format.isNotEmpty()) {
            arrayOf(format)
        } else {
            emptyArray()
        }
        
        dateFormatList = arrayOfNulls(formats.size)

        // Regex to extract Hex colors inside format string
        val colorPattern = Pattern.compile("#(?:\\d|[a-fA-F]){6}")

        for (c in formats.indices) {
            try {
                var currentFormat = formats[c]
                // Normalize newlines
                currentFormat = Tuils.patternNewline.matcher(currentFormat).replaceAll(Tuils.NEWLINE)

                var color = XMLPrefsManager.getColor(Theme.time_color)

                // Check if format contains a color override (e.g. "#FF0000 HH:mm")
                val m = colorPattern.matcher(currentFormat)
                if (m.find()) {
                    color = Color.parseColor(m.group())
                    currentFormat = m.replaceAll(Tuils.EMPTYSTRING)
                }

                dateFormatList!![c] = SimpleMutableEntry(color, SimpleDateFormat(currentFormat))
            } catch (e: Exception) {
                Tuils.sendOutput(Color.RED, context, "Invalid time format: " + formats[c])
                // Fallback to first valid or null
                if (c > 0 && (dateFormatList!![0] != null)) {
                    dateFormatList!![c] = dateFormatList!![0]
                }
            }
        }

        instance = this
    }

    /**
     * Retrieves the format for a given index.
     */
    private fun get(index: Int): MutableMap.MutableEntry<Int, SimpleDateFormat>? {
        var idx = index
        val list = dateFormatList ?: return null
        if (idx < 0 || idx >= list.size) idx = 0
        if (idx == 0 && list.isEmpty()) return null

        return list[idx]
    }

    // --- Replacement Overloads ---

    fun replace(cs: CharSequence): CharSequence = replace(null, Int.MAX_VALUE, cs, -1, TerminalManager.NO_COLOR)

    fun replace(cs: CharSequence, color: Int): CharSequence = replace(null, Int.MAX_VALUE, cs, -1, color)

    fun replace(cs: CharSequence, tm: Long, color: Int): CharSequence = replace(null, Int.MAX_VALUE, cs, tm, color)

    fun replace(cs: CharSequence, tm: Long): CharSequence = replace(null, Int.MAX_VALUE, cs, tm, TerminalManager.NO_COLOR)

    fun replace(context: Context?, size: Int, cs: CharSequence): CharSequence = replace(context, size, cs, -1, TerminalManager.NO_COLOR)

    fun replace(context: Context?, size: Int, cs: CharSequence, color: Int): CharSequence = replace(context, size, cs, -1, color)

    /**
     * Core replacement logic.
     * Finds all %t tokens and replaces them with formatted time strings.
     * @param context Context for resource access (optional)
     * @param size Font size override (optional)
     * @param cs Input text containing placeholders
     * @param tm Time in millis (-1 for current time)
     * @param color Color override (-1 to use format default)
     * @return Text with time replaced
     */
    fun replace(context: Context?, size: Int, cs: CharSequence, tm: Long, color: Int): CharSequence {
        var result = cs
        val time = if (tm == -1L) System.currentTimeMillis() else tm

        val date = Date(time)

        val matcher = extractor.matcher(result)
        while (matcher.find()) {
            var number = matcher.group(1)
            if (number.isNullOrEmpty()) number = "0"

            val entry = get(number.toInt()) ?: continue

            val s = span(context, entry, color, date, size)
            result = TextUtils.replace(result, arrayOf(matcher.group(0)), arrayOf(s))
        }

        // Fallback for simple %t
        val entry = get(0)
        entry?.let {
            result = TextUtils.replace(result, arrayOf("%t"), arrayOf(span(context, it, color, date, size)))
        }

        return result
    }

    // --- CharSequence Generation Overloads ---

    fun getCharSequence(s: String): CharSequence? = getCharSequence(null, Int.MAX_VALUE, s, -1, TerminalManager.NO_COLOR)

    fun getCharSequence(s: String, color: Int): CharSequence? = getCharSequence(null, Int.MAX_VALUE, s, -1, color)

    fun getCharSequence(s: String, tm: Long, color: Int): CharSequence? = getCharSequence(null, Int.MAX_VALUE, s, tm, color)

    fun getCharSequence(s: String, tm: Long): CharSequence? = getCharSequence(null, Int.MAX_VALUE, s, tm, TerminalManager.NO_COLOR)

    fun getCharSequence(context: Context?, size: Int, s: String): CharSequence? = getCharSequence(context, size, s, -1, TerminalManager.NO_COLOR)

    fun getCharSequence(context: Context?, size: Int, s: String, color: Int): CharSequence? = getCharSequence(context, size, s, -1, color)

    /**
     * Generates a formatted time string for a specific single token (e.g. "%t0").
     */
    fun getCharSequence(context: Context?, size: Int, s: String, tm: Long, color: Int): CharSequence? {
        val time = if (tm == -1L) System.currentTimeMillis() else tm

        val date = Date(time)

        val matcher = extractor.matcher(s)
        return if (matcher.find()) {
            var number = matcher.group(1)
            if (number.isNullOrEmpty()) number = "0"

            val entry = get(number.toInt()) ?: return null

            span(context, entry, color, date, size)
        } else null
    }

    /**
     * Helper to create a SpannableString with color and size.
     */
    private fun span(
        context: Context?,
        entry: MutableMap.MutableEntry<Int, SimpleDateFormat>?,
        color: Int,
        date: Date,
        size: Int,
    ): CharSequence {
        if (entry == null) return Tuils.EMPTYSTRING

        val tf = entry.value.format(date)
        val clr = if (color != TerminalManager.NO_COLOR) color else entry.key

        val spannableString = SpannableString(tf)
        spannableString.setSpan(ForegroundColorSpan(clr), 0, tf.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        if (size != Int.MAX_VALUE && context != null) {
            spannableString.setSpan(
                AbsoluteSizeSpan(Tuils.convertSpToPixels(size.toFloat(), context)),
                0,
                tf.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        return spannableString
    }

    fun dispose() {
        dateFormatList = null
        instance = null
    }

    companion object {
        // Pattern to find %t followed by digits
        @JvmField
        val extractor: Pattern = Pattern.compile("%t([0-9]*)", Pattern.CASE_INSENSITIVE)

        @JvmField
        var instance: TimeManager? = null
    }
}
