package com.hereliesaz.hg2gui.managers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// import com.hereliesaz.hg2gui.BuildConfig;
import com.hereliesaz.hg2gui.R;
import com.hereliesaz.hg2gui.managers.xml.XMLPrefsManager;
import com.hereliesaz.hg2gui.tuils.Tuils;
import com.hereliesaz.hg2gui.tuils.interfaces.Reloadable;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Manages the application and removal of themes.
 * <p>
 * Supports:
 * 1. Downloading themes from a repository.
 * 2. Parsing theme XML/JSON.
 * 3. Applying themes by replacing local configuration files (theme.xml, suggestions.xml).
 * 4. Reverting to previous themes.
 * 5. Parsing CSS-style rgba() colors to Android Hex codes.
 * </p>
 */
public class ThemeManager {

    // Actions for theme control broadcasts
    public static String ACTION_APPLY = "com.hereliesaz.hg2gui" + ".theme_apply";
    public static String ACTION_REVERT = "com.hereliesaz.hg2gui" + ".theme_revert";
    public static String ACTION_STANDARD = "com.hereliesaz.hg2gui" + ".theme_standard";

    public static String NAME = "name";

    OkHttpClient client;
    Context context;
    Reloadable reloadable; // Interface to restart the activity

    // Regex to split a theme file into suggestions config and theme config
    Pattern parser = Pattern.compile("(<SUGGESTIONS>.+<\\/SUGGESTIONS>).*(<THEME>.+<\\/THEME>)", Pattern.DOTALL);

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(ACTION_APPLY)) {
                String name = intent.getStringExtra(NAME);
                if(name == null) return;

                // name needs to be the absolute path if it's a file
                if(name.endsWith(".zip")) apply(new File(name));
                else apply(name);
            } else if(intent.getAction().equals(ACTION_REVERT)) {
                revert();
            } else if(intent.getAction().equals(ACTION_STANDARD)) {
                standard();
            }
        }
    };

    /**
     * Constructor. Registers receivers for theme commands.
     */
    public ThemeManager(OkHttpClient client, Context context, Reloadable reloadable) {
        this.client = client;
        this.context = context;
        this.reloadable = reloadable;

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_APPLY);
        filter.addAction(ACTION_REVERT);
        filter.addAction(ACTION_STANDARD);

        LocalBroadcastManager.getInstance(context.getApplicationContext()).registerReceiver(receiver, filter);
    }

    public void dispose() {
        LocalBroadcastManager.getInstance(context.getApplicationContext()).unregisterReceiver(receiver);
    }

    /**
     * Downloads and applies a theme by ID/Name from the online repository.
     */
    public void apply(final String themeName) {
        new Thread() {
            @Override
            public void run() {
                super.run();

                if(!Tuils.hasInternetAccess()) {
                    Tuils.sendOutput(Color.RED, context, R.string.no_internet);
                    return;
                }

                // TODO: This URL seems to be legacy. Verify availability or make configurable.
                String url = "https://tui.tarunshankerpandey.com/show_data.php?data_type=xml&theme_id=" + themeName;

                Request.Builder builder = new Request.Builder()
                        .url(url)
                        .get();

                Response response;
                try {
                    response = client.newCall(builder.build()).execute();
                } catch (IOException e) {
                    Tuils.sendOutput(context, e.toString());
                    return;
                }

                if(response.isSuccessful()) {
                    String string;
                    try {
                        string = response.body().string();
                    } catch (IOException e) {
                        string = Tuils.EMPTYSTRING;
                    }

                    if(string.length() == 0) {
                        Tuils.sendOutput(context, R.string.theme_not_found);
                        return;
                    }

                    // Parse the response
                    Matcher m = parser.matcher(string);
                    if(m.find()) {
                        String suggestions = m.group(1);
                        String theme = m.group(2);

                        applyTheme(theme, suggestions, true, themeName);
                    } else {
                        Tuils.sendOutput(context, R.string.theme_not_found);
                        return;
                    }
                }
            }
        }.start();
    }

    public void apply(File zip) {
        // Zip implementation appears to be missing or incomplete in original source
    }

    /**
     * Applies a theme from local files.
     */
    private void applyTheme(File theme, File suggestions, boolean keepOld) {
        if(theme == null || suggestions == null) {
            Tuils.sendOutput(context, R.string.theme_unable);
            return;
        }

        File oldTheme = new File(Tuils.getFolder(), XMLPrefsManager.XMLPrefsRoot.THEME.path);
        File oldSuggestions = new File(Tuils.getFolder(), XMLPrefsManager.XMLPrefsRoot.SUGGESTIONS.path);

        // Backup existing config
        if(keepOld) {
            Tuils.insertOld(oldTheme);
            Tuils.insertOld(oldSuggestions);
        }

        // Overwrite
        theme.renameTo(oldTheme);
        suggestions.renameTo(oldSuggestions);

        reloadable.reload();
    }

    /**
     * Applies a theme from string content (downloaded).
     */
    private void applyTheme(String theme, String suggestions, boolean keepOld, String themeName) {
        if(theme == null || suggestions == null) {
            Tuils.sendOutput(context, R.string.theme_unable);
            return;
        }

        // Convert RGBA colors to Hex before saving
        Matcher colorMatcher = colorParser.matcher(theme);
        while(colorMatcher.find()) {
            theme = Pattern.compile(Pattern.quote(colorMatcher.group())).matcher(theme).replaceAll(toHexColor(colorMatcher.group()));
        }

        colorMatcher = colorParser.matcher(suggestions);
        while(colorMatcher.find()) {
            suggestions = Pattern.compile(Pattern.quote(colorMatcher.group())).matcher(suggestions).replaceAll(toHexColor(colorMatcher.group()));
        }

        File oldTheme = new File(Tuils.getFolder(), XMLPrefsManager.XMLPrefsRoot.THEME.path);
        File oldSuggestions = new File(Tuils.getFolder(), XMLPrefsManager.XMLPrefsRoot.SUGGESTIONS.path);

        if(keepOld) {
            Tuils.insertOld(oldTheme);
            Tuils.insertOld(oldSuggestions);
        }
        oldTheme.delete();
        oldSuggestions.delete();

        try {
            FileOutputStream themeStream = new FileOutputStream(oldTheme);
            themeStream.write(theme.getBytes());
            themeStream.flush();
            themeStream.close();

            FileOutputStream suggestionsStream = new FileOutputStream(oldSuggestions);
            suggestionsStream.write(suggestions.getBytes());
            suggestionsStream.flush();
            suggestionsStream.close();

            reloadable.addMessage(context.getString(R.string.theme_applied) + Tuils.SPACE + themeName, null);
            reloadable.reload();
        } catch (IOException e) {
            Tuils.sendOutput(context, R.string.output_error);
        }
    }

    /**
     * Reverts to the previous theme.
     */
    private void revert() {
        applyTheme(Tuils.getOld(XMLPrefsManager.XMLPrefsRoot.THEME.path), Tuils.getOld(XMLPrefsManager.XMLPrefsRoot.SUGGESTIONS.path), false);
    }

    /**
     * Resets to the default standard theme.
     */
    private void standard() {
        File oldTheme = new File(Tuils.getFolder(), XMLPrefsManager.XMLPrefsRoot.THEME.path);
        File oldSuggestions = new File(Tuils.getFolder(), XMLPrefsManager.XMLPrefsRoot.SUGGESTIONS.path);

        Tuils.insertOld(oldTheme);
        Tuils.insertOld(oldSuggestions);

        oldTheme.delete();
        oldSuggestions.delete();

        reloadable.addMessage(context.getString(R.string.theme_applied) + Tuils.SPACE + "standard", null);
    }

    // Regex for parsing rgba(r, g, b, a)
    Pattern colorParser = Pattern.compile("rgba\\([\\s]*(\\d+),[\\s]*(\\d+),[\\s]*(\\d+),[\\s]*(\\d.*\\d*)[\\s]*\\)");

    /**
     * Converts a CSS-style rgba() string to Android Hex string (#AARRGGBB).
     */
    private String toHexColor(String color) {
        Matcher m = colorParser.matcher(color);
        if(m.find()) {
            int red = Integer.parseInt(m.group(1));
            int green = Integer.parseInt(m.group(2));
            int blue = Integer.parseInt(m.group(3));
            float alpha = Float.parseFloat(m.group(4));

            String redHex = Integer.toHexString(red);
            if(redHex.length() == 1) redHex = "0" + redHex;

            String greenHex = Integer.toHexString(green);
            if(greenHex.length() == 1) greenHex = "0" + greenHex;

            String blueHex = Integer.toHexString(blue);
            if(blueHex.length() == 1) blueHex = "0" + blueHex;

            String alphaHex = Integer.toHexString((int) alpha);
            if(alphaHex.length() == 1) alphaHex = "0" + alphaHex;

            return "#" + (alpha == 1 ? Tuils.EMPTYSTRING : alphaHex) + redHex + greenHex + blueHex;
        } else return Tuils.EMPTYSTRING;
    }
}
