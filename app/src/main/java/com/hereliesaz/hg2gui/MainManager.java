package com.hereliesaz.hg2gui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Parcelable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

import java.io.File;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hereliesaz.hg2gui.commands.Command;
import com.hereliesaz.hg2gui.commands.CommandGroup;
import com.hereliesaz.hg2gui.commands.CommandRepository;
import com.hereliesaz.hg2gui.commands.CommandTuils;
import com.hereliesaz.hg2gui.commands.main.MainPack;
import com.hereliesaz.hg2gui.commands.main.raw.location;
import com.hereliesaz.hg2gui.commands.main.specific.RedirectCommand;
import com.hereliesaz.hg2gui.managers.AliasManager;
import com.hereliesaz.hg2gui.managers.AppsManager;
import com.hereliesaz.hg2gui.managers.ChangelogManager;
import com.hereliesaz.hg2gui.managers.ContactManager;
import com.hereliesaz.hg2gui.managers.HTMLExtractManager;
import com.hereliesaz.hg2gui.managers.MessagesManager;
import com.hereliesaz.hg2gui.managers.RssManager;
import com.hereliesaz.hg2gui.managers.TerminalManager;
import com.hereliesaz.hg2gui.managers.ThemeManager;
import com.hereliesaz.hg2gui.managers.TimeManager;
import com.hereliesaz.hg2gui.managers.TuiLocationManager;
import com.hereliesaz.hg2gui.managers.music.MusicManager2;
import com.hereliesaz.hg2gui.managers.music.MusicService;
import com.hereliesaz.hg2gui.managers.notifications.KeeperService;
import com.hereliesaz.hg2gui.managers.xml.XMLPrefsManager;
import com.hereliesaz.hg2gui.managers.xml.options.Behavior;
import com.hereliesaz.hg2gui.managers.xml.options.Theme;
import com.hereliesaz.hg2gui.tuils.PrivateIOReceiver;
import com.hereliesaz.hg2gui.tuils.StoppableThread;
import com.hereliesaz.hg2gui.tuils.Tuils;
import com.hereliesaz.hg2gui.tuils.interfaces.CommandExecuter;
import com.hereliesaz.hg2gui.tuils.interfaces.OnRedirectionListener;
import com.hereliesaz.hg2gui.tuils.interfaces.Redirectator;
import com.hereliesaz.hg2gui.tuils.libsuperuser.Shell;
import com.hereliesaz.hg2gui.tuils.libsuperuser.ShellHolder;
import okhttp3.Cache;
import okhttp3.OkHttpClient;

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

/**
 * Core logic coordinator for the application.
 * <p>
 * This class acts as the central controller for the terminal environment. It is responsible for:
 * 1. Initializing and managing all other subsystems (Apps, Music, Theme, RSS, etc.).
 * 2. Receiving user input from the UI.
 * 3. Routing that input through a series of "Triggers" to determine the action (Alias, Command, App Launch, or Shell).
 * 4. Handling background service communication.
 * </p>
 */
public class MainManager {

    // Action strings for Intents used in local broadcasts
    public static String ACTION_EXEC = "com.hereliesaz.hg2gui" + ".main_exec";
    public static String CMD = "cmd", NEED_WRITE_INPUT = "writeInput", ALIAS_NAME = "aliasName", PARCELABLE = "parcelable", CMD_COUNT = "cmdCount", MUSIC_SERVICE = "musicService";

    // --- Redirection Logic ---
    // Handles commands that "redirect" flow, like 'alias' or commands expecting interactive input.
    private RedirectCommand redirect;
    private Redirectator redirectator = new Redirectator() {
        /**
         * Prepares a redirection. Called when a command requests control over subsequent inputs.
         * @param cmd The command requesting redirection.
         */
        @Override
        public void prepareRedirection(RedirectCommand cmd) {
            redirect = cmd;

            if(redirectionListener != null) {
                redirectionListener.onRedirectionRequest(cmd);
            }
        }

        /**
         * Cleans up after redirection ends.
         */
        @Override
        public void cleanup() {
            if(redirect != null) {
                // Clear any state stored in the redirect command
                redirect.beforeObjects.clear();
                redirect.afterObjects.clear();

                // Notify listener (usually UIManager) that redirection ended
                if(redirectionListener != null) {
                    redirectionListener.onRedirectionEnd(redirect);
                }

                redirect = null;
            }
        }
    };
    private OnRedirectionListener redirectionListener;
    public void setRedirectionListener(OnRedirectionListener redirectionListener) {
        this.redirectionListener = redirectionListener;
    }

    // Package path where raw command classes are located.
    private final String COMMANDS_PKG = "com.hereliesaz.hg2gui.commands.main.raw";

    // --- Triggers ---
    // The order of triggers determines the precedence of interpretation.
    // 1. Groups (e.g. folder-like structures for apps)
    // 2. Aliases (user-defined shortcuts)
    // 3. TUI Commands (internal commands like 'clear', 'config')
    // 4. App Launch (if input matches an app name)
    // 5. Shell Command (fallback to system shell)
    private CmdTrigger[] triggers = new CmdTrigger[] {
            new GroupTrigger(),
            new AliasTrigger(),
            new TuiCommandTrigger(),
            new AppTrigger(),
            new ShellCommandTrigger()
    };

    // MainPack holds references to all managers, passed to commands so they can access system resources.
    private MainPack mainPack;

    private LauncherActivity mContext;

    // Preferences cached for performance
    private boolean showAliasValue;
    private boolean showAppHistory;
    private int aliasContentColor;

    private String multipleCmdSeparator;

    // Static interactive shell session (shared across the app)
    public static Shell.Interactive interactive;

    // Sub-Managers
    private AliasManager aliasManager;
    private RssManager rssManager;
    private AppsManager appsManager;
    private ContactManager contactManager;
    private MusicManager2 musicManager2;
    private ThemeManager themeManager;
    private HTMLExtractManager htmlExtractManager;
    private CommandRepository commandRepository;

    MessagesManager messagesManager;

    private BroadcastReceiver receiver;

    // Counter to keep track of command order and avoid race conditions
    public static int commandCount = 0;

    private boolean keeperServiceRunning;

    /**
     * Constructor. Initializes the environment.
     * @param c The LauncherActivity context.
     */
    protected MainManager(LauncherActivity c) {
        mContext = c;

        // Load preferences
        keeperServiceRunning = XMLPrefsManager.getBoolean(Behavior.tui_notification);

        showAliasValue = XMLPrefsManager.getBoolean(Behavior.show_alias_content);
        showAppHistory = XMLPrefsManager.getBoolean(Behavior.show_launch_history);
        aliasContentColor = XMLPrefsManager.getColor(Theme.alias_content_color);

        multipleCmdSeparator = XMLPrefsManager.get(Behavior.multiple_cmd_separator);

        // CommandGroup manages categorization of commands
        CommandGroup group = new CommandGroup(mContext, COMMANDS_PKG);

        try {
            contactManager = new ContactManager(mContext);
        } catch (NullPointerException e) {
            Tuils.log(e);
        }

        appsManager = new AppsManager(c);
        aliasManager = new AliasManager(mContext);

        // HTTP Client for network operations (Weather, RSS)
        final OkHttpClient client = new OkHttpClient.Builder()
                .cache(new Cache(mContext.getCacheDir(), 10*1024*1024))
                .build();

        // Initialize other managers
        rssManager = new RssManager(mContext, client);
        themeManager = new ThemeManager(client, mContext, c);
        musicManager2 = XMLPrefsManager.getBoolean(Behavior.enable_music) ? new MusicManager2(mContext) : null;
        ChangelogManager.printLog(mContext, client);
        htmlExtractManager = new HTMLExtractManager(mContext, client);

        if(XMLPrefsManager.getBoolean(Behavior.show_hints)) {
            messagesManager = new MessagesManager(mContext);
        }

        // Initialize Command Repository (indexes available commands)
        commandRepository = new CommandRepository();
        // Create the MainPack data transfer object
        mainPack = new MainPack(mContext, group, aliasManager, appsManager, musicManager2, contactManager, redirectator, rssManager, client, commandRepository);
        // Populate command repository with available commands
        commandRepository.update(mainPack);

        // Initialize Shell
        ShellHolder shellHolder = new ShellHolder(mContext);
        interactive = shellHolder.build();
        mainPack.shellHolder = shellHolder;

        // Register BroadcastReceiver for internal events
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_EXEC); // Execute command
        filter.addAction(location.ACTION_LOCATION_CMD_GOT); // Location received
        filter.addAction(UIManager.ACTION_UPDATE_SUGGESTIONS); // Update suggestions UI

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if(action.equals(UIManager.ACTION_UPDATE_SUGGESTIONS)) {
                    // Update command repository when suggestions need refresh (e.g. new app installed)
                    if(commandRepository != null && mainPack != null) commandRepository.update(mainPack);
                } else if (action.equals(ACTION_EXEC)) {
                    // Execute a command received via Intent
                    String cmd = intent.getStringExtra(CMD);
                    if (cmd == null) cmd = intent.getStringExtra(PrivateIOReceiver.TEXT);

                    if (cmd == null) {
                        return;
                    }

                    // Check for stale commands
                    int cmdCount = intent.getIntExtra(CMD_COUNT, -1);
                    if (cmdCount < commandCount) return;
                    commandCount++;

                    String aliasName = intent.getStringExtra(ALIAS_NAME);
                    boolean needWriteInput = intent.getBooleanExtra(NEED_WRITE_INPUT, false);
                    Parcelable p = intent.getParcelableExtra(PARCELABLE);

                    // If requested, echo the command to the input field
                    if(needWriteInput) {
                        Intent i = new Intent(PrivateIOReceiver.ACTION_INPUT);
                        i.putExtra(PrivateIOReceiver.TEXT, cmd);
                        LocalBroadcastManager.getInstance(context.getApplicationContext()).sendBroadcast(i);
                    }

                    // Execute based on type
                    if(p != null && p instanceof AppsManager.LaunchInfo) {
                        onCommand(cmd, (AppsManager.LaunchInfo) p, intent.getBooleanExtra(MainManager.MUSIC_SERVICE, false));
                    } else {
                        onCommand(cmd, aliasName, intent.getBooleanExtra(MainManager.MUSIC_SERVICE, false));
                    }
                } else if(action.equals(location.ACTION_LOCATION_CMD_GOT)) {
                    // Handle async location result
                    Tuils.sendOutput(context, "Lat: " + intent.getDoubleExtra(TuiLocationManager.LATITUDE, 0) + "; Long: " + intent.getDoubleExtra(TuiLocationManager.LONGITUDE, 0));
                    TuiLocationManager.instance(context).rm(location.ACTION_LOCATION_CMD_GOT);
                }
            }
        };

        LocalBroadcastManager.getInstance(mContext.getApplicationContext()).registerReceiver(receiver, filter);
    }

    /**
     * Updates background services when a command is executed.
     * This informs the KeeperService (notification listener) of the current context.
     */
    private void updateServices(String cmd, boolean wasMusicService) {

        if(keeperServiceRunning) {
            Intent i = new Intent(mContext, KeeperService.class);
            i.putExtra(KeeperService.CMD_KEY, cmd);
            i.putExtra(KeeperService.PATH_KEY, mainPack.currentDirectory.getAbsolutePath());
            mContext.startService(i);
        }

        if(wasMusicService) {
            Intent i = new Intent(mContext, MusicService.class);
            mContext.startService(i);
        }
    }

    /**
     * Handles a command that is explicitly an App Launch.
     */
    public void onCommand(String input, AppsManager.LaunchInfo launchInfo, boolean wasMusicService) {
        if(launchInfo == null) {
            onCommand(input, (String) null, wasMusicService);
            return;
        }

        updateServices(input, wasMusicService);

        // Verify if the input matches the app label
        if(launchInfo.unspacedLowercaseLabel.equals(Tuils.removeSpaces(input.toLowerCase()))) {
            performLaunch(mainPack, launchInfo, input);
        } else {
            // Fallback to standard processing
            onCommand(input, (String) null, wasMusicService);
        }
    }

    // Pattern to extract color codes from input: #RRGGBB[text]
    Pattern colorExtractor = Pattern.compile("(#[^(]{6})\\[([^\\)]*)\\]", Pattern.CASE_INSENSITIVE);

    /**
     * Main command processing entry point.
     * @param input The raw command string.
     * @param alias The alias name if this command came from an alias expansion.
     * @param wasMusicService Whether this command originated from the music service.
     */
    public void onCommand(String input, String alias, boolean wasMusicService) {
        input = Tuils.removeUnncesarySpaces(input);

        if(alias == null) updateServices(input, wasMusicService);

        // --- Redirection Handling ---
        // If a command like 'tuixt' or 'alias' requested redirection, all input goes to it.
        if(redirect != null) {
            if(!redirect.isWaitingPermission()) {
                redirect.afterObjects.add(input);
            }
            String output = redirect.onRedirect(mainPack);
            Tuils.sendOutput(mContext, output);

            return;
        }

        // Show alias expansion if enabled
        if(alias != null && showAliasValue) {
           Tuils.sendOutput(aliasContentColor, mContext, aliasManager.formatLabel(alias, input));
        }

        // --- Multiple Commands ---
        // Split input by separator (e.g., '&&' or ';')
        String[] cmds;
        if(multipleCmdSeparator.length() > 0) {
            cmds = input.split(multipleCmdSeparator);
        } else {
            cmds = new String[] {input};
        }

        // --- Color Extraction ---
        // Check if user specified a color for the output
        int[] colors = new int[cmds.length];
        for(int c = 0; c < colors.length; c++) {
            Matcher m = colorExtractor.matcher(cmds[c]);
            if(m.matches()) {
                try {
                    colors[c] = Color.parseColor(m.group(1));
                    cmds[c] = m.group(2); // The actual text content
                } catch (Exception e) {
                    colors[c] = TerminalManager.NO_COLOR;
                }
            } else colors[c] = TerminalManager.NO_COLOR;
        }

        // --- Execution Loop ---
        for(int c = 0; c < cmds.length; c++) {
            mainPack.clear();
            mainPack.commandColor = colors[c];

            // Iterate through triggers until one handles the command
            for (CmdTrigger trigger : triggers) {
                boolean r;
                try {
                    r = trigger.trigger(mainPack, cmds[c]);
                } catch (Exception e) {
                    Tuils.sendOutput(mContext, Tuils.getStackTrace(e));
                    break;
                }
                if (r) {
                    // If triggered successfully, check for hint messages
                    if(messagesManager != null) messagesManager.afterCmd();
                    break;
                }
            }
        }
    }

    /**
     * Handle long press on back button.
     */
    public void onLongBack() {
        // Clear input
        Tuils.sendInput(mContext, Tuils.EMPTYSTRING);
    }

    /**
     * Called when a command requires permissions that were denied.
     */
    public void sendPermissionNotGrantedWarning() {
        redirectator.cleanup();
    }

    /**
     * Dispose of resources.
     */
    public void dispose() {
        mainPack.dispose();
    }

    /**
     * Permanent destruction of the manager.
     */
    public void destroy() {
        mainPack.destroy();
        TuiLocationManager.disposeStatic();

        if(messagesManager != null) messagesManager.onDestroy();

        themeManager.dispose();
        htmlExtractManager.dispose(mContext);
        aliasManager.dispose();
        LocalBroadcastManager.getInstance(mContext.getApplicationContext()).unregisterReceiver(receiver);

        // Kill the shell session in background
        new StoppableThread() {
            @Override
            public void run() {
                super.run();

                try {
                    interactive.kill();
                    interactive.close();
                } catch (Exception e) {
                    Tuils.log(e);
                    Tuils.toFile(e);
                }
            }
        }.start();
    }

    public MainPack getMainPack() {
        return mainPack;
    }

    /**
     * Returns an executor for functional interfaces.
     */
    public CommandExecuter executer() {
        return (input, obj) -> {
            AppsManager.LaunchInfo li = obj instanceof AppsManager.LaunchInfo ? (AppsManager.LaunchInfo) obj : null;

            onCommand(input, li, false);
        };
    }

//
    String appFormat;
    int outputColor;

    Pattern pa = Pattern.compile("%a", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
    Pattern pp = Pattern.compile("%p", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
    Pattern pl = Pattern.compile("%l", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);

    /**
     * Launches an application.
     * @param mainPack The main data pack.
     * @param i The LaunchInfo of the app.
     * @param input The raw input.
     * @return True if successful.
     */
    public boolean performLaunch(MainPack mainPack, AppsManager.LaunchInfo i, String input) {
        Intent intent = appsManager.getIntent(i);
        if (intent == null) {
            return false;
        }

        // Show launch history message if enabled
        if(showAppHistory) {
            if(appFormat == null) {
                appFormat = XMLPrefsManager.get(Behavior.app_launch_format);
                outputColor = XMLPrefsManager.getColor(Theme.output_color);
            }

            String a = new String(appFormat);
            // Replace placeholders in format string (%a=Activity, %p=Package, %l=Label)
            a = pa.matcher(a).replaceAll(Matcher.quoteReplacement(intent.getComponent().getClassName()));
            a = pp.matcher(a).replaceAll(Matcher.quoteReplacement(intent.getComponent().getPackageName()));
            a = pl.matcher(a).replaceAll(Matcher.quoteReplacement(i.publicLabel));
            a = Tuils.patternNewline.matcher(a).replaceAll(Matcher.quoteReplacement(Tuils.NEWLINE));

            SpannableString text = new SpannableString(a);
            text.setSpan(new ForegroundColorSpan(outputColor), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            CharSequence s = TimeManager.instance.replace(text);

            Tuils.sendOutput(mainPack, s, TerminalManager.CATEGORY_OUTPUT);
        }

        // Start the app activity
        mainPack.context.startActivity(intent);

        return true;
    }
//

    /**
     * Interface for command triggers.
     */
    public interface CmdTrigger {
        boolean trigger(MainPack info, String input) throws Exception;
    }

    /**
     * Trigger for Aliases.
     */
    private class AliasTrigger implements CmdTrigger {

        @Override
        public boolean trigger(MainPack info, String input) {
            // Check if input matches an alias
            String alias[] = aliasManager.getAlias(input, true);

            String aliasValue = alias[0];
            if (alias[0] == null) {
                return false;
            }

            String aliasName = alias[1];
            String residual = alias[2];

            // Format alias with arguments
            aliasValue = aliasManager.format(aliasValue, residual);

            // Execute the expanded command
            onCommand(aliasValue, aliasName, false);

            return true;
        }
    }

    /**
     * Trigger for App Groups (folders).
     */
    private class GroupTrigger implements CmdTrigger {

        @Override
        public boolean trigger(MainPack info, String input) throws Exception {
            int index = input.indexOf(Tuils.SPACE);
            String name;

            // Separate group name from arguments
            if(index != -1) {
                name = input.substring(0,index);
                input = input.substring(index + 1);
            } else {
                name = input;
                input = null;
            }

            List<? extends Group> appGroups = info.appsManager.groups;
            if(appGroups != null) {
                for(Group g : appGroups) {
                    if(name.equals(g.name())) {
                        if(input == null) {
                            // List members if no argument
                            Tuils.sendOutput(mContext, AppsManager.AppUtils.printApps(AppsManager.AppUtils.labelList((List<AppsManager.LaunchInfo>) g.members(), false)));
                            return true;
                        } else {
                            // Execute action within group
                            return g.use(mainPack, input);
                        }
                    }
                }
            }

            return false;
        }
    }

    /**
     * Trigger for Shell commands.
     */
    private class ShellCommandTrigger implements CmdTrigger {

        final int CD_CODE = 10;
        final int PWD_CODE = 11;

        // Listener for shell command results
        final Shell.OnCommandResultListener result = new Shell.OnCommandResultListener() {
            @Override
            public void onCommandResult(int commandCode, int exitCode, List<String> output) {
                if(commandCode == CD_CODE) {
                    // After changing directory, get the new path
                    interactive.addCommand("pwd", PWD_CODE, result);
                } else if(commandCode == PWD_CODE && output.size() == 1) {
                    // Update current directory in MainPack
                    File f = new File(output.get(0));
                    if(f.exists()) {
                        mainPack.currentDirectory = f;

                        LocalBroadcastManager.getInstance(mContext.getApplicationContext()).sendBroadcast(new Intent(UIManager.ACTION_UPDATE_HINT));
                    }
                }
            }
        };

        @Override
        public boolean trigger(final MainPack info, final String input) throws Exception {
            new StoppableThread() {
                @Override
                public void run() {
                    // Handle 'su' specially to toggle root indicator
                    if(input.trim().equalsIgnoreCase("su")) {
                        if(Shell.SU.available()) LocalBroadcastManager.getInstance(mContext.getApplicationContext()).sendBroadcast(new Intent(UIManager.ACTION_ROOT));
                        interactive.addCommand("su");

                    } else if(input.contains("cd ")) {
                        // Handle directory change with callback
                        interactive.addCommand(input, CD_CODE, result);
                    } else interactive.addCommand(input);

                }
            }.start();

            return true;
        }
    }

    /**
     * Trigger for launching apps by name.
     */
    private class AppTrigger implements CmdTrigger {

        @Override
        public boolean trigger(MainPack info, String input) {
            // Find app with matching label
            AppsManager.LaunchInfo i = appsManager.findLaunchInfoWithLabel(input, AppsManager.SHOWN_APPS);
            // Launch it
            return i != null && performLaunch(info, i, input);
        }
    }

    /**
     * Trigger for internal TUI commands.
     */
    private class TuiCommandTrigger implements CmdTrigger {

        @Override
        public boolean trigger(final MainPack info, final String input) throws Exception {

            // Parse input into a Command object
            final Command command = CommandTuils.parse(input, info);
            if(command == null) return false;

            mainPack.lastCommand = input;

            // Execute in background thread
            new StoppableThread() {
                @Override
                public void run() {
                    super.run();

                    try {
                        String output = command.exec(info);
                        if(output != null) {
                            Tuils.sendOutput(info, output, TerminalManager.CATEGORY_OUTPUT);
                        }
                    } catch (Exception e) {
                        Tuils.sendOutput(mContext, Tuils.getStackTrace(e));
                        Tuils.log(e);
                    }
                }
            }.start();

            return true;
        }
    }

    /**
     * Interface for groupable items.
     */
    public interface Group {
        List<? extends Object> members();
        boolean use(MainPack mainPack, String input);
        String name();
    }
}
