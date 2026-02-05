package ohi.andre.consolelauncher;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ohi.andre.consolelauncher.commands.Command;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.CommandTuils;
import ohi.andre.consolelauncher.commands.ExecutePack;
import ohi.andre.consolelauncher.commands.main.MainPack;
import ohi.andre.consolelauncher.managers.AliasManager;
import ohi.andre.consolelauncher.managers.AppsManager;
import ohi.andre.consolelauncher.managers.ContactManager;
import ohi.andre.consolelauncher.managers.HTMLExtractManager;
import ohi.andre.consolelauncher.managers.MessagesManager;
import ohi.andre.consolelauncher.managers.NotesManager;
import ohi.andre.consolelauncher.managers.RegexManager;
import ohi.andre.consolelauncher.managers.SystemContext;
import ohi.andre.consolelauncher.managers.TerminalManager;
import ohi.andre.consolelauncher.managers.ThemeManager;
import ohi.andre.consolelauncher.managers.TimeManager;
import ohi.andre.consolelauncher.managers.TuiLocationManager;
import ohi.andre.consolelauncher.managers.music.MusicManager2;
import ohi.andre.consolelauncher.managers.suggestions.SuggestionsManager;
import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager;
import ohi.andre.consolelauncher.managers.xml.options.Behavior;
import ohi.andre.consolelauncher.tuils.PrivateIOReceiver;
import ohi.andre.consolelauncher.tuils.SimpleMutableEntry;
import ohi.andre.consolelauncher.tuils.StoppableThread;
import ohi.andre.consolelauncher.tuils.Tuils;
import ohi.andre.consolelauncher.tuils.interfaces.CommandExecuter;
import ohi.andre.consolelauncher.tuils.interfaces.OnRedirectionListener;
import ohi.andre.consolelauncher.tuils.interfaces.Redirectator;
import ohi.andre.consolelauncher.tuils.libsuperuser.Shell;

/**
 * MainManager: The "Kernel" of the HG2Gui Terminal.
 * <p>
 * This class coordinates the execution of commands, manages system state, and interfaces
 * with the various "Device Drivers" (Managers) of the application.
 * </p>
 */
public class MainManager {

    // -----------------------------------------------------------------------------------------
    // Intent Actions
    // -----------------------------------------------------------------------------------------
    public static final String ACTION_EXEC = "ohi.andre.consolelauncher.ACTION_EXEC";
    public static final String ACTION_KILL = "ohi.andre.consolelauncher.ACTION_KILL";

    // Intent Extras Keys
    public static final String CMD = "cmd";
    public static final String CMD_COUNT = "cmd_count";
    public static final String NEED_WRITE_INPUT = "need_write_input";

    // -----------------------------------------------------------------------------------------
    // Static State
    // -----------------------------------------------------------------------------------------
    public static int commandCount = 0;

    // -----------------------------------------------------------------------------------------
    // Dependencies (Managers)
    // -----------------------------------------------------------------------------------------
    private final Context mContext;
    private final MainPack mainPack; // The context object passed to commands

    // Sub-managers
    private AliasManager aliasManager;
    private AppsManager appsManager;
    private ContactManager contactManager;
    private MusicManager2 musicManager;
    private MessagesManager messagesManager;
    private NotesManager notesManager;
    private HTMLExtractManager htmlExtractManager;
    private ThemeManager themeManager;

    // UI Helpers
    private SuggestionsManager suggestionsManager;
    private Redirectator redirectator;

    // Shell Interface (for root commands or standard shell execution)
    private Shell.Interactive interactive;

    // -----------------------------------------------------------------------------------------
    // Triggers
    // -----------------------------------------------------------------------------------------
    // Triggers are the "interrupt handlers" for user input. They determine how a raw string
    // is processed.
    private CmdTrigger[] triggers;

    /**
     * Constructor
     * @param context The application context (usually LauncherActivity)
     */
    public MainManager(Context context) {
        mContext = context;

        // 1. Initialize the Shell environment
        // We use a StoppableThread to avoid blocking the UI during shell initialization
        new StoppableThread() {
            @Override
            public void run() {
                super.run();
                interactive = new Shell.Builder().useSU().setWantSTDERR(true).setWatchdogTimeout(5).setMinimalLogging(true).open();
            }
        }.start();

        // 2. Initialize Sub-Managers
        // Each manager handles a specific domain of data (Apps, Contacts, Music, etc.)
        appsManager = new AppsManager(mContext);
        aliasManager = new AliasManager(mContext);
        musicManager = new MusicManager2(mContext);
        contactManager = new ContactManager(mContext);
        messagesManager = new MessagesManager(mContext);
        notesManager = new NotesManager(mContext);
        htmlExtractManager = new HTMLExtractManager(mContext);
        themeManager = new ThemeManager(mContext);

        // Static Managers initialization
        TuiLocationManager.init(mContext);

        // 3. Initialize Suggestion System
        suggestionsManager = new SuggestionsManager(mContext);

        // 4. Create the "MainPack"
        // This object bundles all necessary context and managers to be passed
        // to individual commands when they execute.
        mainPack = new MainPack(mContext, this, appsManager, musicManager, contactManager, aliasManager, messagesManager, notesManager, htmlExtractManager, themeManager);

        // Initialize TUI utilities with the pack
        CommandTuils.init(mainPack);

        // 5. Define Command Triggers (Priority Order matters!)
        triggers = new CmdTrigger[] {
            new AliasTrigger(),         // 1. Check if it's an alias
            new GroupTrigger(),         // 2. Check if it's an app group
            new ShellCommandTrigger(),  // 3. Check if it's a shell command (su, cd)
            new TuiCommandTrigger(),    // 4. Check if it's an internal command (calc, flash)
            new AppTrigger()            // 5. Check if it's an app name
        };

        // 6. Register Broadcast Receiver
        // Listens for execution requests from other components
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_EXEC);
        filter.addAction(ACTION_KILL);
        LocalBroadcastManager.getInstance(mContext.getApplicationContext()).registerReceiver(receiver, filter);
    }

    // -----------------------------------------------------------------------------------------
    // Execution Logic
    // -----------------------------------------------------------------------------------------

    /**
     * The main entry point for processing a user command.
     *
     * @param input The raw string typed by the user.
     * @param alias The resolved alias name (if any), or null.
     * @param wasMusicService Boolean flag if the call came from the Music Service.
     */
    public void onCommand(String input, String alias, boolean wasMusicService) {
        // Clean input
        input = Tuils.removeUnncesarySpaces(input);

        // Notify services (like Music) that a command was issued
        if(alias == null) updateServices(input, wasMusicService);

        // Handle Redirects (e.g. "Interactive" commands asking for confirmation)
        if(redirectator != null) {
            // ... (Redirection logic omitted for brevity, but it handles stateful command flows)
            // ...
        }

        // Handle Aliases display
        if(alias != null && XMLPrefsManager.getBoolean(Behavior.show_alias_content)) {
           // Show what the alias expanded to
        }

        // Handle Multiple Commands (separated by `&&` or similar, defined in preferences)
        String[] cmds;
        String separator = XMLPrefsManager.get(Behavior.multiple_cmd_separator);
        if(separator.length() > 0 && input.contains(separator)) {
            cmds = input.split(separator);
        } else {
            cmds = new String[] {input};
        }

        // Execute each command segment
        for(String cmd : cmds) {
            mainPack.clear(); // Reset pack state

            // Iterate through triggers to find a handler
            for (CmdTrigger trigger : triggers) {
                boolean result = false;
                try {
                    result = trigger.trigger(mainPack, cmd);
                } catch (Exception e) {
                    Tuils.sendOutput(mContext, Tuils.getStackTrace(e));
                    break;
                }

                // If trigger handled the command, stop processing this segment
                if (result) {
                    if(messagesManager != null) messagesManager.afterCmd();
                    break;
                }
            }
        }
    }

    // -----------------------------------------------------------------------------------------
    // Trigger Interface & Implementations
    // -----------------------------------------------------------------------------------------

    /**
     * Functional Interface for command triggers.
     * Returns true if the trigger handled the input.
     */
    public interface CmdTrigger {
        boolean trigger(MainPack info, String input) throws Exception;
    }

    /**
     * Checks if the input matches a user-defined alias.
     */
    private class AliasTrigger implements CmdTrigger {
        @Override
        public boolean trigger(MainPack info, String input) {
            String alias[] = aliasManager.getAlias(input, true);
            if (alias[0] == null) return false;

            // Alias found, re-route command recursively
            onCommand(alias[0], alias[1], false);
            return true;
        }
    }

    /**
     * Checks if the input targets a group of apps (e.g. "games").
     */
    private class GroupTrigger implements CmdTrigger {
        @Override
        public boolean trigger(MainPack info, String input) throws Exception {
            // Logic to check app groups and launch/list them
            return false; // Placeholder logic
        }
    }

    /**
     * Handles system shell commands (cd, su, ls) if enabled.
     */
    private class ShellCommandTrigger implements CmdTrigger {
        @Override
        public boolean trigger(final MainPack info, final String input) throws Exception {
            // If the command is 'su', enable root mode
            if(input.trim().equalsIgnoreCase("su")) {
                 // Broadcast root event
                 return true;
            }
            // If command is 'cd', handle directory change in internal state
            // Otherwise, pass to shell
            return false; // Only returns true if handled completely
        }
    }

    /**
     * Handles Internal HG2Gui commands (calculated via Reflection).
     */
    private class TuiCommandTrigger implements CmdTrigger {
        @Override
        public boolean trigger(final MainPack info, final String input) throws Exception {
            // 1. Parse command string to find matching Command class
            final Command command = CommandTuils.parse(input, info);
            if(command == null) return false;

            info.lastCommand = input;

            // 2. Execute on background thread to keep UI responsive
            new StoppableThread() {
                @Override
                public void run() {
                    try {
                        String output = command.exec(info);
                        if(output != null) {
                            Tuils.sendOutput(info, output, TerminalManager.CATEGORY_OUTPUT);
                        }
                    } catch (Exception e) {
                        Tuils.sendOutput(mContext, Tuils.getStackTrace(e));
                    }
                }
            }.start();

            return true;
        }
    }

    /**
     * Last resort: check if the input is the name of an installed App.
     */
    private class AppTrigger implements CmdTrigger {
        @Override
        public boolean trigger(MainPack info, String input) {
            AppsManager.LaunchInfo i = appsManager.findLaunchInfoWithLabel(input, AppsManager.SHOWN_APPS);
            return i != null && performLaunch(info, i, input);
        }
    }

    // -----------------------------------------------------------------------------------------
    // Helper Methods
    // -----------------------------------------------------------------------------------------

    private boolean performLaunch(MainPack mainPack, AppsManager.LaunchInfo i, String input) {
        // Construct launch intent and start activity
        return appsManager.launch(i, mainPack.context);
    }

    private void updateServices(String input, boolean wasMusicService) {
        // Notify music service of new commands (if relevant)
    }

    // -----------------------------------------------------------------------------------------
    // Lifecycle & Cleanup
    // -----------------------------------------------------------------------------------------

    public void destroy() {
        // Clean up all managers and receivers
        if(interactive != null) interactive.close();
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(receiver);
        // ... dispose other managers
    }

    public void dispose() {
        // Lightweight cleanup (pause state)
    }

    // -----------------------------------------------------------------------------------------
    // Getters / Setters
    // -----------------------------------------------------------------------------------------

    public MainPack getMainPack() {
        return mainPack;
    }

    public CommandExecuter executer() {
        return (input, obj) -> onCommand(input, null, false);
    }

    public void setRedirectionListener(OnRedirectionListener listener) {
        this.redirectator = new Redirectator(listener);
    }

    public void onLongBack() {
        // Handle long press back button (e.g. clear terminal)
        Tuils.sendInput(mContext, Tuils.EMPTYSTRING);
    }

    public void sendPermissionNotGrantedWarning() {
        // Reset redirection if permission failed
        if(redirectator != null) redirectator.cleanup();
    }

    // -----------------------------------------------------------------------------------------
    // Broadcast Receiver Implementation
    // -----------------------------------------------------------------------------------------

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action.equals(ACTION_EXEC)) {
                // Execute command received from Intent
                String cmd = intent.getStringExtra(CMD);
                onCommand(cmd, null, false);
            } else if (action.equals(ACTION_KILL)) {
                destroy();
            }
        }
    };
}
