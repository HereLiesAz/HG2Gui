package com.hereliesaz.hg2gui.managers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// import com.hereliesaz.hg2gui.BuildConfig;
import com.hereliesaz.hg2gui.R;
import com.hereliesaz.hg2gui.managers.xml.XMLPrefsManager;
import com.hereliesaz.hg2gui.managers.xml.options.Behavior;
import com.hereliesaz.hg2gui.tuils.Tuils;

/**
 * Manages user-defined aliases for commands.
 * <p>
 * Aliases allow users to create shortcuts for long commands or chain commands.
 * They are stored in a simple text file (`alias.txt`) in the format `name=value`.
 * Supports parameter substitution (e.g., `search=google %` where `%` is replaced by the argument).
 * </p>
 */
public class AliasManager {

    // Actions for broadcasting alias operations
    public static String ACTION_LS = "com.hereliesaz.hg2gui" + ".alias_ls";
    public static String ACTION_ADD = "com.hereliesaz.hg2gui" + ".alias_add";
    public static String ACTION_RM = "com.hereliesaz.hg2gui" + ".alias_rm";

    public static String NAME = "name";

    public static final String PATH = "alias.txt";

    private List<Alias> aliases;
    private String paramSeparator, aliasLabelFormat;
    private boolean replaceAllMarkers;

    private Context context;

    private String paramMarker; // The character sequence used as a placeholder (e.g., "%")
    private Pattern parameterPattern;

    private BroadcastReceiver receiver;

    /**
     * Constructor. Initializes the alias list and registers receivers.
     */
    public AliasManager(Context c) {
        this.context = c;

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_ADD);
        filter.addAction(ACTION_LS);
        filter.addAction(ACTION_RM);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if(action.equals(ACTION_ADD)) {
                    add(context, intent.getStringExtra(NAME), intent.getStringExtra(XMLPrefsManager.VALUE_ATTRIBUTE));
                } else if(action.equals(ACTION_RM)) {
                    remove(context, intent.getStringExtra(NAME));
                } else if(action.equals(ACTION_LS)) {
                    Tuils.sendOutput(context, printAliases());
                }
            }
        };

        // Load configuration
        paramMarker = XMLPrefsManager.get(Behavior.alias_param_marker);
        parameterPattern = Pattern.compile(Pattern.quote(paramMarker));
        paramSeparator = XMLPrefsManager.get(Behavior.alias_param_separator);
        aliasLabelFormat = XMLPrefsManager.get(Behavior.alias_content_format);
        replaceAllMarkers = XMLPrefsManager.getBoolean(Behavior.alias_replace_all_markers);

        // Load aliases from file
        reload();
    }

    /**
     * Returns a string representation of all aliases for display.
     */
    public String printAliases() {
        String output = Tuils.EMPTYSTRING;
        for (Alias a : aliases) {
            output = output.concat(a.name + " --> " + a.value + Tuils.NEWLINE);
        }

        return output.trim();
    }

    /**
     * Resolves an alias from a command string.
     * @param alias The input string that might contain an alias.
     * @param supportSpaces Whether to attempt matching aliases with arguments separated by spaces.
     * @return String array: [0]=Resolved Value, [1]=Alias Name, [2]=Arguments/Residual
     */
    public String[] getAlias(String alias, boolean supportSpaces) {
        if(supportSpaces) {
            String args = Tuils.EMPTYSTRING;

            String aliasValue = null;
            // Iterate backwards to find longest matching alias name
            while (true) {
                aliasValue = getALias(alias);
                if(aliasValue != null) break;
                else {
                    int index = alias.lastIndexOf(Tuils.SPACE);
                    if(index == -1) return new String[] {null, null, alias};

                    args = alias.substring(index + 1) + Tuils.SPACE + args;
                    args = args.trim();
                    alias = alias.substring(0,index);
                }
            }

            return new String[] {aliasValue, alias, args};
        } else {
            return new String[] {getALias(alias), alias, Tuils.EMPTYSTRING};
        }
    }

    // Temporary placeholder for parameter substitution to avoid conflicts
    private final String SECURITY_REPLACEMENT = "{#@";
    private Pattern securityPattern = Pattern.compile(Pattern.quote(SECURITY_REPLACEMENT));

    /**
     * Formats the alias value by injecting parameters.
     * @param aliasValue The raw alias value (e.g., "echo %")
     * @param params The parameters to inject (e.g., "hello")
     * @return The formatted string (e.g., "echo hello")
     */
    public String format(String aliasValue, String params) {
        params = params.trim();
        if(params.length() == 0) return aliasValue;

        // Count markers
        int before = aliasValue.length();
        aliasValue = parameterPattern.matcher(aliasValue).replaceAll(SECURITY_REPLACEMENT);
        int replaced = (aliasValue.length() - before) / Math.abs(SECURITY_REPLACEMENT.length() - paramMarker.length());

        // Split parameters
        String[] split = params.split(Pattern.quote(paramSeparator), replaced);

        // Replace markers sequentially
        for(String s : split) {
            aliasValue = securityPattern.matcher(aliasValue).replaceFirst(s);
        }

        // If configured, replace all remaining markers with the first param
        if(replaceAllMarkers) aliasValue = securityPattern.matcher(aliasValue).replaceAll(split[0]);

        return aliasValue;
    }

    /**
     * Retrieves the value of a specific alias by name.
     */
    private String getALias(String name) {
        for(Alias a : aliases) {
            if(name.equals(a.name)) return a.value;
        }
        return null;
    }

    /**
     * Removes an alias from the memory list.
     */
    private boolean removeAlias(String name) {
        for(int c = 0; c < aliases.size(); c++) {
            Alias a = aliases.get(c);
            if(name.equals(a.name)) {
                aliases.remove(c);
                return true;
            }
        }
        return false;
    }

    private final Pattern pv = Pattern.compile("%v", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
    private final Pattern pa = Pattern.compile("%a", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);

    /**
     * Formats the display label for an alias (used in UI feedback).
     */
    public String formatLabel(String aliasName, String aliasValue) {
        String a = aliasLabelFormat;
        a = Tuils.patternNewline.matcher(a).replaceAll(Matcher.quoteReplacement(Tuils.NEWLINE));
        a = pv.matcher(a).replaceAll(Matcher.quoteReplacement(aliasValue));
        a = pa.matcher(a).replaceAll(Matcher.quoteReplacement(aliasName));
        return a;
    }

    /**
     * Reloads aliases from the `alias.txt` file.
     */
    public void reload() {
        if(aliases != null) aliases.clear();
        else aliases = new ArrayList<>();

        File root = Tuils.getFolder();
        if(root == null) return;

        File file = new File(root, PATH);

        try {
            if(!file.exists()) file.createNewFile();

            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));

            String line;
            while((line = reader.readLine()) != null) {
                String[] splatted = line.split("=");
                if(splatted.length < 2) continue;

                String name, value = Tuils.EMPTYSTRING;
                name = splatted[0];

                // Reconstruct value if it contained '=' characters
                for(int c = 1; c < splatted.length; c++) {
                    value += splatted[c];
                    if(c != splatted.length - 1) value += "=";
                }

                name = name.trim();
                value = value.trim();

                // Prevent recursive loops
                if(name.equalsIgnoreCase(value)) {
                    Tuils.sendOutput(Color.RED, context,
                            context.getString(R.string.output_notaddingalias1) + Tuils.SPACE + name + Tuils.SPACE + context.getString(R.string.output_notaddingalias2));
                } else if(value.startsWith(name + Tuils.SPACE)) {
                    Tuils.sendOutput(Color.RED, context,
                            context.getString(R.string.output_notaddingalias1) + Tuils.SPACE + name + Tuils.SPACE + context.getString(R.string.output_notaddingalias3));
                } else {
                    aliases.add(new Alias(name, value, parameterPattern));
                }
            }
        } catch (Exception e) {
            Tuils.log(e);
        }
    }

    public void dispose() {
        LocalBroadcastManager.getInstance(context.getApplicationContext()).unregisterReceiver(receiver);
    }

    /**
     * Adds a new alias to the file and memory.
     */
    public void add(Context context, String name, String value) {
        for(Alias a : aliases) {
            if(name.equals(a.name)) {
                Tuils.sendOutput(context, R.string.unavailable_name);
                return;
            }
        }

        FileOutputStream fos;
        try {
            fos = new FileOutputStream(new File(Tuils.getFolder(), PATH), true);
            fos.write((Tuils.NEWLINE + name + "=" + value).getBytes());
            fos.close();

            aliases.add(new Alias(name, value, parameterPattern));
        } catch (Exception e) {
            Tuils.sendOutput(context, e.toString());
        }

    }

    /**
     * Removes an alias from the file and memory.
     */
    public void remove(Context context, String name) {
        reload(); // Ensure we are up to date

        if(!removeAlias(name)) {
            Tuils.sendOutput(context, R.string.invalid_name);
            return;
        }

        try {
            File inputFile = new File(Tuils.getFolder(), PATH);
            File tempFile = new File(Tuils.getFolder(), PATH + "2");

            BufferedReader reader = new BufferedReader(new FileReader(inputFile));
            BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));

            String prefix = name + "=";
            String line;
            while((line = reader.readLine()) != null) {
                if(line.startsWith(prefix)) continue;
                writer.write(line + Tuils.NEWLINE);
            }
            writer.close();
            reader.close();

            tempFile.renameTo(inputFile);
        } catch (Exception e) {
            Tuils.sendOutput(context, e.toString());
        }
    }

    public List<Alias> getAliases(boolean excludeEmtpy) {
        List<Alias> l = new ArrayList<>(aliases);
        if(excludeEmtpy) {
            for(int c = 0; c < l.size(); c++) {
                if(l.get(c).name.length() == 0) {
                    l.remove(c);
                    break;
                }
            }
        }

        return l;
    }

    /**
     * Internal representation of an alias.
     */
    public static class Alias {
        public String name, value;
        public boolean isParametrized;

        public Alias(String name, String value, Pattern parameterPattern) {
            this.name = name;
            this.value = value;

            isParametrized = parameterPattern.matcher(value).find();
        }

        @Override
        public boolean equals(Object obj) {
            return (obj instanceof Alias && ((Alias) obj).name.equals(name)) || obj.equals(name);
        }
    }
}
