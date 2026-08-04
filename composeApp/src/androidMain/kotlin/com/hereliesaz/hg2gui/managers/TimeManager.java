package com.hereliesaz.hg2gui.managers;

import android.content.Context;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hereliesaz.hg2gui.managers.xml.XMLPrefsManager;
import com.hereliesaz.hg2gui.managers.xml.options.Behavior;
import com.hereliesaz.hg2gui.managers.xml.options.Theme;
import com.hereliesaz.hg2gui.tuils.SimpleMutableEntry;
import com.hereliesaz.hg2gui.tuils.Tuils;

/**
 * Manages time formatting throughout the application.
 * <p>
 * This class interprets the %t[index] placeholder used in various UI elements (e.g. prompt, status bar).
 * Users can define multiple time formats (e.g. %t0 = "HH:mm", %t1 = "dd/MM") in the configuration XML.
 * This manager parses those formats and replaces placeholders with the current time.
 * </p>
 */
public class TimeManager {

    // Cache of DateFormats indexed by the integer specified in %t[index]
    // Key = Color (optional override), Value = SimpleDateFormat
    Map.Entry<Integer, SimpleDateFormat>[] dateFormatList;

    // Pattern to find %t followed by digits
    public static Pattern extractor = Pattern.compile("%t([0-9]*)", Pattern.CASE_INSENSITIVE);

    public static TimeManager instance;

    /**
     * Constructor. Loads time formats from XML preferences.
     */
    public TimeManager(Context context) {
        String format = XMLPrefsManager.get(Behavior.time_format);
        String separator = XMLPrefsManager.get(Behavior.time_format_separator);

        // Split multiple formats by separator
        String[] formats = format.split(separator);
        dateFormatList = new Map.Entry[formats.length];

        // Regex to extract Hex colors inside format string
        Pattern colorPattern = Pattern.compile("#(?:\\d|[a-fA-F]){6}");

        for(int c = 0; c < dateFormatList.length; c++) {
            try {
                // Normalize newlines
                formats[c] = Tuils.patternNewline.matcher(formats[c]).replaceAll(Tuils.NEWLINE);

                int color = XMLPrefsManager.getColor(Theme.time_color);

                // Check if format contains a color override (e.g. "#FF0000 HH:mm")
                Matcher m = colorPattern.matcher(formats[c]);
                if(m.find()) {
                    color = Color.parseColor(m.group());
                    formats[c] = m.replaceAll(Tuils.EMPTYSTRING);
                }

                dateFormatList[c] = new SimpleMutableEntry<>(color, new SimpleDateFormat(formats[c]));
            } catch (Exception e) {
                Tuils.sendOutput(Color.RED, context,"Invalid time format: " + formats[c]);
                // Fallback to first valid or null
                dateFormatList[c] = dateFormatList[0];
            }
        }

        instance = this;
    }

    /**
     * Retrieves the format for a given index.
     */
    private Map.Entry<Integer, SimpleDateFormat> get(int index) {
        if(dateFormatList == null) return null;
        if(index < 0 || index >= dateFormatList.length) index = 0;
        if(index == 0 && dateFormatList.length == 0) return null;

        return dateFormatList[index];
    }

    // --- Replacement Overloads ---

    /**
     * Replaces time placeholders in a CharSequence.
     * Uses current system time and default color.
     */
    public CharSequence replace(CharSequence cs) {
        return replace(cs, -1, TerminalManager.NO_COLOR);
    }

    public CharSequence replace(CharSequence cs, int color) {
        return replace(cs, -1, color);
    }

    public CharSequence replace(CharSequence cs, long tm, int color) {
        return replace(null, TerminalManager.NO_COLOR, cs, tm, color);
    }

    public CharSequence replace(CharSequence cs, long tm) {
        return replace(null, TerminalManager.NO_COLOR, cs, tm, TerminalManager.NO_COLOR);
    }

    public CharSequence replace(Context context, int size, CharSequence cs) {
        return replace(context, size, cs, -1, TerminalManager.NO_COLOR);
    }

    public CharSequence replace(Context context, int size, CharSequence cs, int color) {
        return replace(context, size, cs, -1, color);
    }

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
    public CharSequence replace(Context context, int size, CharSequence cs, long tm, int color) {
        if(tm == -1) {
            tm = System.currentTimeMillis();
        }

        // Safety check: Cannot modify standard String with Spans, need CharSequence/Spannable logic handling
        if(cs instanceof String) {
            // Log warning but proceed (String will be converted during replace)
            // Tuils.log(Thread.currentThread().getStackTrace());
            // Tuils.log("cant span a string!", cs.toString());
        }

        Date date = new Date(tm);

        Matcher matcher = extractor.matcher(cs);
        while(matcher.find()) {
            String number = matcher.group(1);
            if(number == null || number.length() == 0) number = "0";

            Map.Entry<Integer, SimpleDateFormat> entry = get(Integer.parseInt(number));
            if(entry == null) continue;

            CharSequence s = span(context, entry, color, date, size);
            cs = TextUtils.replace(cs, new String[] {matcher.group(0)}, new CharSequence[] {s});
        }

        // Fallback for simple %t
        Map.Entry<Integer, SimpleDateFormat> entry = get(0);
        cs = TextUtils.replace(cs, new String[] {"%t"}, new CharSequence[] {span(context, entry, color, date, size)});

        return cs;
    }

    // --- CharSequence Generation Overloads ---

    public CharSequence getCharSequence(String s) {
        return getCharSequence(s, -1, TerminalManager.NO_COLOR);
    }

    public CharSequence getCharSequence(String s, int color) {
        return getCharSequence(s, -1, color);
    }

    public CharSequence getCharSequence(String s, long tm, int color) {
        return getCharSequence(null, TerminalManager.NO_COLOR, s, tm, color);
    }

    public CharSequence getCharSequence(String s, long tm) {
        return getCharSequence(null, TerminalManager.NO_COLOR, s, tm, TerminalManager.NO_COLOR);
    }

    public CharSequence getCharSequence(Context context, int size, String s) {
        return getCharSequence(context, size, s, -1, TerminalManager.NO_COLOR);
    }

    public CharSequence getCharSequence(Context context, int size, String s, int color) {
        return getCharSequence(context, size, s, -1, color);
    }

    /**
     * Generates a formatted time string for a specific single token (e.g. "%t0").
     */
    public CharSequence getCharSequence(Context context, int size, String s, long tm, int color) {
        if(tm == -1) {
            tm = System.currentTimeMillis();
        }

        Date date = new Date(tm);

        Matcher matcher = extractor.matcher(s);
        if(matcher.find()) {
            String number = matcher.group(1);
            if(number == null || number.length() == 0) number = "0";

            Map.Entry<Integer, SimpleDateFormat> entry = get(Integer.parseInt(number));
            if(entry == null) {
                return null;
            }

            return span(context, entry, color, date, size);
        } else return null;
    }

    /**
     * Helper to create a SpannableString with color and size.
     */
    private CharSequence span(Context context, Map.Entry<Integer, SimpleDateFormat> entry, int color, Date date, int size) {
        if(entry == null) return Tuils.EMPTYSTRING;

        String tf = entry.getValue().format(date);
        int clr = color != TerminalManager.NO_COLOR ? color : entry.getKey();

        SpannableString spannableString = new SpannableString(tf);
        spannableString.setSpan(new ForegroundColorSpan(clr), 0, tf.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        if(size != Integer.MAX_VALUE && context != null) {
            spannableString.setSpan(new AbsoluteSizeSpan(Tuils.convertSpToPixels(size, context)), 0, tf.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return spannableString;
    }

    public void dispose() {
        dateFormatList = null;
        instance = null;
    }
}
