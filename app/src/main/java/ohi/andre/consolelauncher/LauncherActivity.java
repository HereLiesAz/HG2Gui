package ohi.andre.consolelauncher;

/*
 * Copyright (C) 2024 HG2Gui Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import ohi.andre.consolelauncher.commands.main.MainPack;
import ohi.andre.consolelauncher.commands.tuixt.TuixtActivity;
import ohi.andre.consolelauncher.managers.ContactManager;
import ohi.andre.consolelauncher.managers.RegexManager;
import ohi.andre.consolelauncher.managers.TerminalManager;
import ohi.andre.consolelauncher.managers.TimeManager;
import ohi.andre.consolelauncher.managers.TuiLocationManager;
import ohi.andre.consolelauncher.managers.notifications.KeeperService;
import ohi.andre.consolelauncher.managers.notifications.NotificationManager;
import ohi.andre.consolelauncher.managers.notifications.NotificationMonitorService;
import ohi.andre.consolelauncher.managers.notifications.NotificationService;
import ohi.andre.consolelauncher.managers.suggestions.SuggestionsManager;
import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager;
import ohi.andre.consolelauncher.managers.xml.options.Behavior;
import ohi.andre.consolelauncher.managers.xml.options.Notifications;
import ohi.andre.consolelauncher.managers.xml.options.Theme;
import ohi.andre.consolelauncher.managers.xml.options.Ui;
import ohi.andre.consolelauncher.tuils.Assist;
import ohi.andre.consolelauncher.tuils.CustomExceptionHandler;
import ohi.andre.consolelauncher.tuils.LongClickableSpan;
import ohi.andre.consolelauncher.tuils.PrivateIOReceiver;
import ohi.andre.consolelauncher.tuils.PublicIOReceiver;
import ohi.andre.consolelauncher.tuils.SimpleMutableEntry;
import ohi.andre.consolelauncher.tuils.Tuils;
import ohi.andre.consolelauncher.tuils.interfaces.Inputable;
import ohi.andre.consolelauncher.tuils.interfaces.Outputable;
import ohi.andre.consolelauncher.tuils.interfaces.Reloadable;

/**
 * LauncherActivity
 * <p>
 * This is the primary entry point for the HG2Gui Android Terminal.
 * While technically registered as an Android "Launcher" (HOME category),
 * its primary function is to host the Terminal Interface.
 * <p>
 * Architecture:
 * - <b>UIManager</b>: Handles the presentation layer (EditText, TextView, Suggestions).
 * - <b>MainManager</b>: Handles the logic layer (Command processing, System Context).
 * <p>
 * This activity manages the lifecycle of these components and handles system-level
 * interactions (Permissions, Back Press, Configuration Changes).
 */
public class LauncherActivity extends AppCompatActivity implements Reloadable {

    // -----------------------------------------------------------------------------------------
    // Constants: Permission Request Codes
    // -----------------------------------------------------------------------------------------

    /** Request code for generic runtime permissions needed by commands. */
    public static final int COMMAND_REQUEST_PERMISSION = 10;

    /** Request code for critical permissions requested at app startup (Storage). */
    public static final int STARTING_PERMISSION = 11;

    /** Request code for suggestion-related permissions (e.g., Contacts). */
    public static final int COMMAND_SUGGESTION_REQUEST_PERMISSION = 12;

    /** Request code for location permissions (used by weather/location commands). */
    public static final int LOCATION_REQUEST_PERMISSION = 13;

    // -----------------------------------------------------------------------------------------
    // Constants: Activity Request Codes
    // -----------------------------------------------------------------------------------------

    /** Request code for launching the internal text editor (Tuixt). */
    public static final int TUIXT_REQUEST = 10;

    // -----------------------------------------------------------------------------------------
    // Core Managers
    // -----------------------------------------------------------------------------------------

    /**
     * Handles all UI updates (Input/Output/Suggestions).
     * It is the "View" controller in the MVC-like pattern.
     */
    private UIManager ui;

    /**
     * Handles command logic and system context.
     * It is the "Controller" in the MVC-like pattern.
     */
    private MainManager main;

    // -----------------------------------------------------------------------------------------
    // Broadcast Receivers
    // -----------------------------------------------------------------------------------------

    /** Listens for internal broadcasts (Intents sent within the app process). */
    private PrivateIOReceiver privateIOReceiver;

    /** Listens for public broadcasts (Intents sent from other apps). */
    private PublicIOReceiver publicIOReceiver;

    // -----------------------------------------------------------------------------------------
    // State Flags
    // -----------------------------------------------------------------------------------------

    /** If true, the keyboard will be shown immediately when the activity starts. */
    private boolean openKeyboardOnStart;

    /** If true, the theme is ready to be applied. False if permissions are pending. */
    private boolean canApplyTheme, backButtonEnabled;

    // -----------------------------------------------------------------------------------------
    // Reload / Restart Logic
    // -----------------------------------------------------------------------------------------

    /**
     * Container for messages that need to be displayed after a reload
     * (e.g., error messages that caused the crash/restart).
     */
    private Set<ReloadMessageCategory> categories;

    /**
     * Initiating a Kernel Panic / Reboot Sequence. Cycles the activity to apply new themes or recover from errors.
     * Used when themes change or a critical error requires a reset.
     */
    private Runnable stopActivity = () -> {
            // 1. Clean up resources
            dispose();

            // 2. Finish the current activity
            finish();

            // 3. Create a fresh Intent to restart the app
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);

            // 4. Compile any reload messages into a single string
            CharSequence reloadMessage = Tuils.EMPTYSTRING;
            for (ReloadMessageCategory c : categories) {
                reloadMessage = TextUtils.concat(reloadMessage, Tuils.NEWLINE, c.text());
            }

            // 5. Pass messages to the new instance via Intent extras
            startMain.putExtra(Reloadable.MESSAGE, reloadMessage);

            // 6. Launch
            startActivity(startMain);
    };

    // -----------------------------------------------------------------------------------------
    // IO Interface Implementations
    // -----------------------------------------------------------------------------------------

    /**
     * Inputable: Defines how the system sends text *into* the input field.
     */
    private Inputable in = new Inputable() {

        @Override
        public void in(String s) {
            // Delegate to UIManager (View Controller) to set text in EditText, strictly enforcing MVC separation.
            if(ui != null) ui.setInput(s);
        }

        @Override
        public void changeHint(final String s) {
            // Update hint on UI thread
            runOnUiThread(() -> ui.setHint(s));
        }

        @Override
        public void resetHint() {
            // Reset hint to default on UI thread
            runOnUiThread(() -> ui.resetHint());
        }
    };

    /**
     * Outputable: Defines how the system sends text *out* to the terminal display.
     * Includes a queuing mechanism to handle rapid output bursts or initialization delays.
     */
    private Outputable out = new Outputable() {

        private final int DELAY = 500;

        // Queues for holding output if UI is not yet ready
        Queue<SimpleMutableEntry<CharSequence,Integer>> textColor = new LinkedList<>();
        Queue<SimpleMutableEntry<CharSequence,Integer>> textCategory = new LinkedList<>();

        boolean charged = false;
        Handler handler = new Handler();

        // Worker to flush queues
        Runnable r = new Runnable() {
            @Override
            public void run() {
                if(ui == null) {
                    // If UI still not ready, check again later
                    handler.postDelayed(this, DELAY);
                    return;
                }

                SimpleMutableEntry<CharSequence,Integer> sm;

                // Flush category-based messages
                while ((sm = textCategory.poll()) != null) {
                    ui.setOutput(sm.getKey(), sm.getValue());
                }

                // Flush color-based messages
                while ((sm = textColor.poll()) != null) {
                    ui.setOutput(sm.getValue(), sm.getKey());
                }

                // Cleanup
                textCategory = null;
                textColor = null;
                handler = null;
                r = null;
            }
        };

        @Override
        public void onOutput(CharSequence output) {
            if(ui != null) ui.setOutput(output, TerminalManager.CATEGORY_OUTPUT);
            else {
                textCategory.add(new SimpleMutableEntry<>(output, TerminalManager.CATEGORY_OUTPUT));
                scheduleQueueProcessing();
            }
        }

        @Override
        public void onOutput(CharSequence output, int category) {
            if(ui != null) ui.setOutput(output, category);
            else {
                textCategory.add(new SimpleMutableEntry<>(output, category));
                scheduleQueueProcessing();
            }
        }

        @Override
        public void onOutput(int color, CharSequence output) {
            if(ui != null) ui.setOutput(color, output);
            else {
                textColor.add(new SimpleMutableEntry<>(output, color));
                scheduleQueueProcessing();
            }
        }

        private void scheduleQueueProcessing() {
            if(!charged) {
                charged = true;
                handler.postDelayed(r, DELAY);
            }
        }

        @Override
        public void dispose() {
            if(handler != null) handler.removeCallbacksAndMessages(null);
        }
    };

    // -----------------------------------------------------------------------------------------
    // Activity Lifecycle Methods
    // -----------------------------------------------------------------------------------------

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Remove standard transitions for a "static" terminal feel
        overridePendingTransition(0,0);

        if (isFinishing()) {
            return;
        }

        // On Android M (6.0) and above, we must check for Storage permissions at runtime.
        // This is critical for reading config files and the 'Guide' data.
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED  &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)) {

            // Request necessary permissions
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, LauncherActivity.STARTING_PERMISSION);
        } else {
            // Permissions exist, proceed to full initialization
            canApplyTheme = true;
            finishOnCreate();
        }
    }

    /**
     * Completes the initialization process. Separated from onCreate to handle the asynchronous Android Runtime Permission model (Marshmallow+).
     * Called either directly from onCreate (if permissions exist) or after permissions are granted.
     */
    private void finishOnCreate() {

        // 1. Install Global Exception Handler
        // Captures uncaught crashes and logs them to a file for debugging.
        Thread.currentThread().setUncaughtExceptionHandler(new CustomExceptionHandler());

        // 2. Load Core Managers
        XMLPrefsManager.loadCommons(this); // Load preferences
        new RegexManager(LauncherActivity.this); // Initialize Regex engine
        new TimeManager(this); // Initialize Time formatter

        // 3. Setup Internal Communication (Private Receiver)
        IntentFilter filter = new IntentFilter();
        filter.addAction(PrivateIOReceiver.ACTION_INPUT);
        filter.addAction(PrivateIOReceiver.ACTION_OUTPUT);
        filter.addAction(PrivateIOReceiver.ACTION_REPLY);

        privateIOReceiver = new PrivateIOReceiver(this, out, in);
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(privateIOReceiver, filter);

        // 4. Setup External Communication (Public Receiver)
        // Allows other apps to send commands to the terminal
        IntentFilter filter1 = new IntentFilter();
        filter1.addAction(PublicIOReceiver.ACTION_CMD);
        filter1.addAction(PublicIOReceiver.ACTION_OUTPUT);

        publicIOReceiver = new PublicIOReceiver();
        getApplicationContext().registerReceiver(publicIOReceiver, filter1);

        // 5. Handle Orientation Locking
        int requestedOrientation = XMLPrefsManager.getInt(Behavior.orientation);
        if(requestedOrientation >= 0 && requestedOrientation != 2) {
            int orientation = getResources().getConfiguration().orientation;
            if(orientation != requestedOrientation) setRequestedOrientation(requestedOrientation);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
            }
        }

        // 6. Theme System Bars (Lollipop+)
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !XMLPrefsManager.getBoolean(Ui.ignore_bar_color)) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(XMLPrefsManager.getColor(Theme.statusbar_color));
            window.setNavigationBarColor(XMLPrefsManager.getColor(Theme.navigationbar_color));
        }

        backButtonEnabled = XMLPrefsManager.getBoolean(Behavior.back_button_enabled);

        // 7. Setup Persistent Notification (KeeperService)
        // Keeps the app alive in memory
        boolean showNotification = XMLPrefsManager.getBoolean(Behavior.tui_notification);
        Intent keeperIntent = new Intent(this, KeeperService.class);
        if (showNotification) {
            keeperIntent.putExtra(KeeperService.PATH_KEY, XMLPrefsManager.get(Behavior.home_path));
            startService(keeperIntent);
        } else {
            try {
                stopService(keeperIntent);
            } catch (Exception e) {}
        }

        // 8. Handle Fullscreen Mode
        boolean fullscreen = XMLPrefsManager.getBoolean(Ui.fullscreen);
        if(fullscreen) {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        // 9. Apply Window Theme
        boolean useSystemWP = XMLPrefsManager.getBoolean(Ui.system_wallpaper);
        if (useSystemWP) {
            setTheme(R.style.Custom_SystemWP);
        } else {
            setTheme(R.style.Custom_Solid);
        }

        // 10. Initialize Notifications System
        try {
            NotificationManager.create(this);
        } catch (Exception e) {
            Tuils.toFile(e);
        }

        // Handle Notification Listener Service
        boolean notifications = XMLPrefsManager.getBoolean(Notifications.show_notifications) || XMLPrefsManager.get(Notifications.show_notifications).equalsIgnoreCase("enabled");
        if(notifications) {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                try {
                    ComponentName notificationComponent = new ComponentName(this, NotificationService.class);
                    PackageManager pm = getPackageManager();
                    pm.setComponentEnabledSetting(notificationComponent, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

                    // Request Notification Access if missing
                    if (!Tuils.hasNotificationAccess(this)) {
                        Intent i = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
                        if (i.resolveActivity(getPackageManager()) == null) {
                            Toast.makeText(this, R.string.no_notification_access, Toast.LENGTH_LONG).show();
                        } else {
                            startActivity(i);
                        }
                    }

                    // Start Monitoring
                    Intent monitor = new Intent(this, NotificationMonitorService.class);
                    startService(monitor);

                    Intent notificationIntent = new Intent(this, NotificationService.class);
                    startService(notificationIntent);
                } catch (NoClassDefFoundError er) {
                    Intent intent = new Intent(PrivateIOReceiver.ACTION_OUTPUT);
                    intent.putExtra(PrivateIOReceiver.TEXT, getString(R.string.output_notification_error) + Tuils.SPACE + er.toString());
                }
            } else {
                Tuils.sendOutput(Color.RED, this, R.string.notification_low_api);
            }
        }

        LongClickableSpan.longPressVibrateDuration = XMLPrefsManager.getInt(Behavior.long_click_vibration_duration);

        // 11. Keyboard Behavior
        openKeyboardOnStart = XMLPrefsManager.getBoolean(Behavior.auto_show_keyboard);
        if (!openKeyboardOnStart) {
            this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        // 12. Set Layout
        setContentView(R.layout.base_view);

        // 13. Show restart message if needed
        if(XMLPrefsManager.getBoolean(Ui.show_restart_message)) {
            CharSequence s = getIntent().getCharSequenceExtra(Reloadable.MESSAGE);
            if(s != null) out.onOutput(Tuils.span(s, XMLPrefsManager.getColor(Theme.restart_message_color)));
        }

        categories = new HashSet<>();

        // 14. Initialize Logic Manager (MainManager)
        main = new MainManager(this);

        ViewGroup mainView = (ViewGroup) findViewById(R.id.mainview);

        // Ensure status bar icons are visible on light backgrounds (Android M+)
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !XMLPrefsManager.getBoolean(Ui.ignore_bar_color) && !XMLPrefsManager.getBoolean(Ui.statusbar_light_icons)) {
            mainView.setSystemUiVisibility(mainView.getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        // 15. Initialize UI Manager
        ui = new UIManager(this, mainView, main.getMainPack(), canApplyTheme, main.executer());

        // Connect Redirector (allows commands to ask for follow-up input)
        main.setRedirectionListener(ui.buildRedirectionListener());
        ui.pack = main.getMainPack();

        // 16. Finalize Setup
        in.in(Tuils.EMPTYSTRING); // Clear input
        ui.focusTerminal(); // Focus cursor

        if(fullscreen) Assist.assistActivity(this); // Setup Assistant

        System.gc(); // Clean house
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (ui != null) ui.onStart(openKeyboardOnStart);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        // Refresh suggestions when returning to the app
        LocalBroadcastManager.getInstance(this.getApplicationContext()).sendBroadcast(new Intent(UIManager.ACTION_UPDATE_SUGGESTIONS));
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (ui != null && main != null) {
            ui.pause();
            main.dispose();
        }
    }

    // Flag to prevent double-disposal
    private boolean disposed = false;

    /**
     * Cleanly shuts down the activity and its managers.
     */
    private void dispose() {
        if(disposed) return;

        try {
            LocalBroadcastManager.getInstance(this.getApplicationContext()).unregisterReceiver(privateIOReceiver);
            getApplicationContext().unregisterReceiver(publicIOReceiver);
        } catch (Exception e) {}

        try {
            stopService(new Intent(this, NotificationMonitorService.class));
        } catch (NoClassDefFoundError | Exception e) {
            Tuils.log(e);
        }

        try {
            stopService(new Intent(this, KeeperService.class));
        } catch (NoClassDefFoundError | Exception e) {
            Tuils.log(e);
        }

        try {
            Intent notificationIntent = new Intent(this, NotificationService.class);
            notificationIntent.putExtra(NotificationService.DESTROY, true);
            startService(notificationIntent);
        } catch (Throwable e) {
            Tuils.log(e);
        }

        overridePendingTransition(0,0);

        if(main != null) main.destroy();
        if(ui != null) ui.dispose();

        XMLPrefsManager.dispose();
        RegexManager.instance.dispose();
        TimeManager.instance.dispose();

        disposed = true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dispose();
    }

    @Override
    public void onBackPressed() {
        if (backButtonEnabled && main != null) {
            ui.onBackPressed();
        }
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (keyCode != KeyEvent.KEYCODE_BACK)
            return super.onKeyLongPress(keyCode, event);

        // Long press back typically clears input
        if (main != null)
            main.onLongBack();
        return true;
    }

    @Override
    public void reload() {
        runOnUiThread(stopActivity);
    }

    @Override
    public void addMessage(String header, String message) {
        for(ReloadMessageCategory cs : categories) {
            Tuils.log(cs.header, header);
            if(cs.header.equals(header)) {
                cs.lines.add(message);
                return;
            }
        }

        ReloadMessageCategory c = new ReloadMessageCategory(header);
        if(message != null) c.lines.add(message);
        categories.add(c);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && ui != null) {
            ui.focusTerminal();
        }
    }

    // Helper class for formatting restart messages
    private class ReloadMessageCategory {
        String header;
        Queue<String> lines = new LinkedList<>();

        public ReloadMessageCategory(String header) {
            this.header = header;
        }

        public CharSequence text() {
            CharSequence text = Tuils.span(header, Color.RED);
            for(String s : lines) {
                text = TextUtils.concat(text, Tuils.NEWLINE, s);
            }
            return text;
        }
    }

    SuggestionsManager.Suggestion suggestion;

    // Context Menu Logic (for Long Press on items)
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        suggestion = (SuggestionsManager.Suggestion) v.getTag(R.id.suggestion_id);

        if(suggestion.type == SuggestionsManager.Suggestion.TYPE_CONTACT) {
            ContactManager.Contact contact = (ContactManager.Contact) suggestion.object;

            menu.setHeaderTitle(contact.name);
            for(int count = 0; count < contact.numbers.size(); count++) {
                menu.add(0, count, count, contact.numbers.get(count));
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if(suggestion != null) {
            if(suggestion.type == SuggestionsManager.Suggestion.TYPE_CONTACT) {
                ContactManager.Contact contact = (ContactManager.Contact) suggestion.object;
                contact.setSelectedNumber(item.getItemId());

                Tuils.sendInput(this, suggestion.getText());

                return true;
            }
        }

        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Handle return from Tuixt (Text Editor)
        if(requestCode == TUIXT_REQUEST && resultCode != 0) {
            if(resultCode == TuixtActivity.BACK_PRESSED) {
                Tuils.sendOutput(this, R.string.tuixt_back_pressed);
            } else {
                Tuils.sendOutput(this, data.getStringExtra(TuixtActivity.ERROR_KEY));
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        // Auto-refresh contacts if permission is granted
        if(permissions.length > 0 && permissions[0].equals(Manifest.permission.READ_CONTACTS) && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            LocalBroadcastManager.getInstance(this.getApplicationContext()).sendBroadcast(new Intent(ContactManager.ACTION_REFRESH));
        }

        try {
            switch (requestCode) {
                case COMMAND_REQUEST_PERMISSION:
                    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        // Retry last command
                        MainPack info = main.getMainPack();
                        main.onCommand(info.lastCommand, (String) null, false);
                    } else {
                        ui.setOutput(getString(R.string.output_nopermissions), TerminalManager.CATEGORY_OUTPUT);
                        main.sendPermissionNotGrantedWarning();
                    }
                    break;
                case STARTING_PERMISSION:
                    // Critical permissions check loop
                    int count = 0;
                    while(count < permissions.length && count < grantResults.length) {
                        if(grantResults[count] == PackageManager.PERMISSION_DENIED) {
                            Toast.makeText(this, R.string.permissions_toast, Toast.LENGTH_LONG).show();
                            // Restart to force permission check again
                            new Thread() {
                                @Override
                                public void run() {
                                    super.run();
                                    try { sleep(2000); } catch (InterruptedException e) {}
                                    runOnUiThread(stopActivity);
                                }
                            }.start();
                            return;
                        }
                        count++;
                    }
                    canApplyTheme = false;
                    finishOnCreate();
                    break;
                case COMMAND_SUGGESTION_REQUEST_PERMISSION:
                    if (grantResults.length == 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                        ui.setOutput(getString(R.string.output_nopermissions), TerminalManager.CATEGORY_OUTPUT);
                    }
                    break;
                case LOCATION_REQUEST_PERMISSION:
                    Intent i = new Intent(TuiLocationManager.ACTION_GOT_PERMISSION);
                    i.putExtra(XMLPrefsManager.VALUE_ATTRIBUTE, grantResults[0]);
                    LocalBroadcastManager.getInstance(this.getApplicationContext()).sendBroadcast(i);
                    break;
            }
        } catch (Exception e) {}
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        // Handle external commands via Intent
        String cmd = intent.getStringExtra(PrivateIOReceiver.TEXT);
        if(cmd != null) {
            Intent i = new Intent(MainManager.ACTION_EXEC);
            i.putExtra(MainManager.CMD_COUNT, MainManager.commandCount);
            i.putExtra(MainManager.CMD, cmd);
            i.putExtra(MainManager.NEED_WRITE_INPUT, true);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(i);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Handled manually to prevent Activity restart on rotation if configured
    }
}
