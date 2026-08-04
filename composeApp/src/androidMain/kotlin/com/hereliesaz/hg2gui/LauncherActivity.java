package com.hereliesaz.hg2gui;

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

import com.hereliesaz.hg2gui.commands.main.MainPack;
import com.hereliesaz.hg2gui.commands.tuixt.TuixtActivity;
import com.hereliesaz.hg2gui.managers.ContactManager;
import com.hereliesaz.hg2gui.managers.RegexManager;
import com.hereliesaz.hg2gui.managers.TerminalManager;
import com.hereliesaz.hg2gui.managers.TimeManager;
import com.hereliesaz.hg2gui.managers.TuiLocationManager;
import com.hereliesaz.hg2gui.managers.notifications.KeeperService;
import com.hereliesaz.hg2gui.managers.notifications.NotificationManager;
import com.hereliesaz.hg2gui.managers.notifications.NotificationMonitorService;
import com.hereliesaz.hg2gui.managers.notifications.NotificationService;
import com.hereliesaz.hg2gui.managers.suggestions.SuggestionsManager;
import com.hereliesaz.hg2gui.managers.xml.XMLPrefsManager;
import com.hereliesaz.hg2gui.managers.xml.options.Behavior;
import com.hereliesaz.hg2gui.managers.xml.options.Notifications;
import com.hereliesaz.hg2gui.managers.xml.options.Theme;
import com.hereliesaz.hg2gui.managers.xml.options.Ui;
import com.hereliesaz.hg2gui.tuils.Assist;
import com.hereliesaz.hg2gui.tuils.CustomExceptionHandler;
import com.hereliesaz.hg2gui.tuils.LongClickableSpan;
import com.hereliesaz.hg2gui.tuils.PrivateIOReceiver;
import com.hereliesaz.hg2gui.tuils.PublicIOReceiver;
import com.hereliesaz.hg2gui.tuils.SimpleMutableEntry;
import com.hereliesaz.hg2gui.tuils.Tuils;
import com.hereliesaz.hg2gui.tuils.interfaces.Inputable;
import com.hereliesaz.hg2gui.tuils.interfaces.Outputable;
import com.hereliesaz.hg2gui.tuils.interfaces.Reloadable;

/**
 * The main activity of the application.
 * <p>
 * This class serves as the entry point for the HG2Gui (formerly T-UI) launcher.
 * It is responsible for initializing the primary UI components, managing the application lifecycle,
 * handling runtime permissions, and coordinating between the {@link UIManager} (View) and {@link MainManager} (Controller).
 * </p>
 * <p>
 * As a Launcher activity, it is configured in the AndroidManifest with category android.intent.category.LAUNCHER
 * and android.intent.category.HOME.
 * </p>
 */
public class LauncherActivity extends AppCompatActivity implements Reloadable {

    // Request codes for runtime permissions and activity results
    /** Request code used when asking for permissions required by a specific command. */
    public static final int COMMAND_REQUEST_PERMISSION = 10;
    /** Request code used for the initial startup permissions (Storage). */
    public static final int STARTING_PERMISSION = 11;
    /** Request code used when suggestion engine needs permissions (e.g. Contacts). */
    public static final int COMMAND_SUGGESTION_REQUEST_PERMISSION = 12;
    /** Request code used when requesting Location permissions. */
    public static final int LOCATION_REQUEST_PERMISSION = 13;

    /** Request code for launching the internal text editor (Tuixt). */
    public static final int TUIXT_REQUEST = 10;

    /**
     * Manages the visual elements of the terminal (TextViews, Input, Layouts).
     * Acts as the 'View' in the architecture.
     */
    private UIManager ui;

    /**
     * Manages the logic and state of the terminal.
     * Acts as the 'Controller' in the architecture.
     */
    private MainManager main;

    /**
     * Receiver for internal broadcasts within the app (Private I/O).
     * Used for communication between services/components and the UI.
     */
    private PrivateIOReceiver privateIOReceiver;

    /**
     * Receiver for public broadcasts.
     * Allows external apps to send commands or output to this launcher.
     */
    private PublicIOReceiver publicIOReceiver;

    // Configuration flags loaded from preferences
    private boolean openKeyboardOnStart; // Should keyboard open immediately on launch?
    private boolean canApplyTheme; // Is it safe to apply theme (permissions granted)?
    private boolean backButtonEnabled; // Is the back button allowed to close the keyboard/app?

    /**
     * Stores messages that need to be displayed after a reload/restart of the activity.
     * This allows preserving error messages or status updates across activity recreation.
     */
    private Set<ReloadMessageCategory> categories;

    /**
     * Runnable task to stop and restart the activity.
     * This is used to apply changes that require a full Activity recreation (e.g. theme changes).
     */
    private Runnable stopActivity = () -> {
            // Clean up resources before finishing
            dispose();
            finish();

            // Create an intent to restart the activity
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);

            // Aggregate reload messages to pass to the new instance
            CharSequence reloadMessage = Tuils.EMPTYSTRING;
            for (ReloadMessageCategory c : categories) {
                reloadMessage = TextUtils.concat(reloadMessage, Tuils.NEWLINE, c.text());
            }
            startMain.putExtra(Reloadable.MESSAGE, reloadMessage);

            // Start the new instance
            startActivity(startMain);
    };

    /**
     * Implementation of the Inputable interface.
     * Defines how the Activity handles input-related actions requested by other components.
     */
    private Inputable in = new Inputable() {

        /**
         * Sets the text in the input field.
         * @param s The string to insert into the input field.
         */
        @Override
        public void in(String s) {
            if(ui != null) ui.setInput(s);
        }

        /**
         * Changes the hint text displayed in the input field.
         * Runs on the UI thread to ensure thread safety.
         * @param s The new hint text.
         */
        @Override
        public void changeHint(final String s) {
            runOnUiThread(() -> ui.setHint(s));
        }

        /**
         * Resets the hint text to the default.
         * Runs on the UI thread.
         */
        @Override
        public void resetHint() {
            runOnUiThread(() -> ui.resetHint());
        }
    };

    /**
     * Implementation of the Outputable interface.
     * Defines how the Activity handles outputting text to the terminal screen.
     * Includes a buffering mechanism to handle output before the UI is fully ready.
     */
    private Outputable out = new Outputable() {

        private final int DELAY = 500; // Delay in milliseconds for retrying UI access

        // Queues to buffer output if UI is not yet initialized
        Queue<SimpleMutableEntry<CharSequence,Integer>> textColor = new LinkedList<>();
        Queue<SimpleMutableEntry<CharSequence,Integer>> textCategory = new LinkedList<>();

        boolean charged = false; // Flag to indicate if the retry handler is active
        Handler handler = new Handler();

        // Runnable that attempts to flush the buffered output to the UI
        Runnable r = new Runnable() {
            @Override
            public void run() {
                // If UI is still null, schedule another check later
                if(ui == null) {
                    handler.postDelayed(this, DELAY);
                    return;
                }

                // Flush textCategory queue
                SimpleMutableEntry<CharSequence,Integer> sm;
                while ((sm = textCategory.poll()) != null) {
                    ui.setOutput(sm.getKey(), sm.getValue());
                }

                // Flush textColor queue
                while ((sm = textColor.poll()) != null) {
                    ui.setOutput(sm.getValue(), sm.getKey());
                }

                // Clear references
                textCategory = null;
                textColor = null;
                handler = null;
                r = null;
            }
        };

        /**
         * Output text with the default category.
         * @param output The text to display.
         */
        @Override
        public void onOutput(CharSequence output) {
            if(ui != null) ui.setOutput(output, TerminalManager.CATEGORY_OUTPUT);
            else {
                // Buffer the output if UI is not ready
                textCategory.add(new SimpleMutableEntry<>(output, TerminalManager.CATEGORY_OUTPUT));

                if(!charged) {
                    charged = true;
                    handler.postDelayed(r, DELAY);
                }
            }
        }

        /**
         * Output text with a specific category (e.g., error, info).
         * @param output The text to display.
         * @param category The category ID.
         */
        @Override
        public void onOutput(CharSequence output, int category) {
            if(ui != null) ui.setOutput(output, category);
            else {
                textCategory.add(new SimpleMutableEntry<>(output, category));

                if(!charged) {
                    charged = true;
                    handler.postDelayed(r, DELAY);
                }
            }
        }

        /**
         * Output text with a specific color.
         * @param color The color integer.
         * @param output The text to display.
         */
        @Override
        public void onOutput(int color, CharSequence output) {
            if(ui != null) ui.setOutput(color, output);
            else {
                textColor.add(new SimpleMutableEntry<>(output, color));

                if(!charged) {
                    charged = true;
                    handler.postDelayed(r, DELAY);
                }
            }
        }

        /**
         * Cleans up handler callbacks to prevent memory leaks.
         */
        @Override
        public void dispose() {
            if(handler != null) handler.removeCallbacksAndMessages(null);
        }
    };

    /**
     * Called when the activity is first created.
     * This is the entry point for initialization.
     * @param savedInstanceState Bundle containing the activity's previously saved state.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Disable default transition animations for a snappier feel
        overridePendingTransition(0,0);

        // Safety check: if activity is already finishing, do nothing
        if (isFinishing()) {
            return;
        }

        // Check for essential storage permissions on Android M+
        // These are required for loading config files and themes.
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED  &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)) {

            // Request permissions if not granted
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, LauncherActivity.STARTING_PERMISSION);
        } else {
            // Permissions are granted (or not needed on older Android), proceed with initialization
            canApplyTheme = true;
            finishOnCreate();
        }
    }

    /**
     * Completes the initialization process after permissions are confirmed.
     * Sets up managers, receivers, UI, and services.
     */
    private void finishOnCreate() {

        // Set a global exception handler to catch crashes and log them to a file
        Thread.currentThread().setUncaughtExceptionHandler(new CustomExceptionHandler());

        // Load common preferences (XML configuration)
        XMLPrefsManager.loadCommons(this);
        // Initialize regex manager for parsing commands
        new RegexManager(LauncherActivity.this);
        // Initialize time manager
        new TimeManager(this);

        // Register the PrivateIOReceiver for internal app communication
        IntentFilter filter = new IntentFilter();
        filter.addAction(PrivateIOReceiver.ACTION_INPUT);
        filter.addAction(PrivateIOReceiver.ACTION_OUTPUT);
        filter.addAction(PrivateIOReceiver.ACTION_REPLY);

        privateIOReceiver = new PrivateIOReceiver(this, out, in);
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(privateIOReceiver, filter);

        // Register the PublicIOReceiver for external communication
        IntentFilter filter1 = new IntentFilter();
        filter1.addAction(PublicIOReceiver.ACTION_CMD);
        filter1.addAction(PublicIOReceiver.ACTION_OUTPUT);

        publicIOReceiver = new PublicIOReceiver();
        getApplicationContext().registerReceiver(publicIOReceiver, filter1);

        // Handle screen orientation settings
        int requestedOrientation = XMLPrefsManager.getInt(Behavior.orientation);
        if(requestedOrientation >= 0 && requestedOrientation != 2) {
            int orientation = getResources().getConfiguration().orientation;
            if(orientation != requestedOrientation) setRequestedOrientation(requestedOrientation);
            // Lock orientation on Jelly Bean MR2 and above
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
            }
        }

        // Configure Status Bar and Navigation Bar colors on Lollipop+
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !XMLPrefsManager.getBoolean(Ui.ignore_bar_color)) {
            Window window = getWindow();

            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(XMLPrefsManager.getColor(Theme.statusbar_color));
            window.setNavigationBarColor(XMLPrefsManager.getColor(Theme.navigationbar_color));
        }

        // Check if back button should be enabled
        backButtonEnabled = XMLPrefsManager.getBoolean(Behavior.back_button_enabled);

        // Manage the notification listener service (KeeperService) based on preferences
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

        // Configure fullscreen mode
        boolean fullscreen = XMLPrefsManager.getBoolean(Ui.fullscreen);
        if(fullscreen) {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        // Apply wallpaper theme (transparent/system wallpaper vs solid background)
        boolean useSystemWP = XMLPrefsManager.getBoolean(Ui.system_wallpaper);
        if (useSystemWP) {
            setTheme(R.style.Custom_SystemWP);
        } else {
            setTheme(R.style.Custom_Solid);
        }

        // Initialize Notification Manager
        try {
            NotificationManager.create(this);
        } catch (Exception e) {
            Tuils.toFile(e);
        }

        // Handle advanced notification features (listening to system notifications)
        boolean notifications = XMLPrefsManager.getBoolean(Notifications.show_notifications) || XMLPrefsManager.get(Notifications.show_notifications).equalsIgnoreCase("enabled");
        if(notifications) {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                try {
                    // Enable the NotificationService component
                    ComponentName notificationComponent = new ComponentName(this, NotificationService.class);
                    PackageManager pm = getPackageManager();
                    pm.setComponentEnabledSetting(notificationComponent, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

                    // Check if the user has granted Notification Access permission
                    if (!Tuils.hasNotificationAccess(this)) {
                        Intent i = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
                        if (i.resolveActivity(getPackageManager()) == null) {
                            Toast.makeText(this, R.string.no_notification_access, Toast.LENGTH_LONG).show();
                        } else {
                            startActivity(i);
                        }
                    }

                    // Start monitoring services
                    Intent monitor = new Intent(this, NotificationMonitorService.class);
                    startService(monitor);

                    Intent notificationIntent = new Intent(this, NotificationService.class);
                    startService(notificationIntent);
                } catch (NoClassDefFoundError er) {
                    Intent intent = new Intent(PrivateIOReceiver.ACTION_OUTPUT);
                    intent.putExtra(PrivateIOReceiver.TEXT, getString(R.string.output_notification_error) + Tuils.SPACE + er.toString());
                }
            } else {
                // Notify user if API level is too low for notifications
                Tuils.sendOutput(Color.RED, this, R.string.notification_low_api);
            }
        }

        // Set vibration duration for long clicks
        LongClickableSpan.longPressVibrateDuration = XMLPrefsManager.getInt(Behavior.long_click_vibration_duration);

        // Determine if keyboard should open automatically
        openKeyboardOnStart = XMLPrefsManager.getBoolean(Behavior.auto_show_keyboard);
        if (!openKeyboardOnStart) {
            this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        // Set the content view to the main layout
        setContentView(R.layout.base_view);

        // Display restart message if one exists (passed from previous instance)
        if(XMLPrefsManager.getBoolean(Ui.show_restart_message)) {
            CharSequence s = getIntent().getCharSequenceExtra(Reloadable.MESSAGE);
            if(s != null) out.onOutput(Tuils.span(s, XMLPrefsManager.getColor(Theme.restart_message_color)));
        }

        categories = new HashSet<>();

        // Initialize the MainManager (Logic Controller)
        main = new MainManager(this);

        // Get reference to the main container view
        ViewGroup mainView = (ViewGroup) findViewById(R.id.mainview);

        // Handle Light Status Bar icons (dark icons on light background) for Android M+
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !XMLPrefsManager.getBoolean(Ui.ignore_bar_color) && !XMLPrefsManager.getBoolean(Ui.statusbar_light_icons)) {
            mainView.setSystemUiVisibility(mainView.getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        // Initialize the UIManager (View Controller)
        ui = new UIManager(this, mainView, main.getMainPack(), canApplyTheme, main.executer());

        // Connect UIManager and MainManager via RedirectionListener
        main.setRedirectionListener(ui.buildRedirectionListener());
        ui.pack = main.getMainPack();

        // Initialize input and focus
        in.in(Tuils.EMPTYSTRING);
        ui.focusTerminal();

        // Set up Assist gesture if in fullscreen
        if(fullscreen) Assist.assistActivity(this);

        // Run garbage collector to clean up initialization objects
        System.gc();
    }

    /**
     * Called when the activity is becoming visible to the user.
     */
    @Override
    protected void onStart() {
        super.onStart();

        // Notify UI to start (e.g. show keyboard if needed)
        if (ui != null) ui.onStart(openKeyboardOnStart);
    }

    /**
     * Called after the activity has been stopped, prior to it being started again.
     */
    @Override
    protected void onRestart() {
        super.onRestart();

        // Send a broadcast to update command suggestions
        LocalBroadcastManager.getInstance(this.getApplicationContext()).sendBroadcast(new Intent(UIManager.ACTION_UPDATE_SUGGESTIONS));
    }

    /**
     * Called when the system is about to start resuming another activity.
     */
    @Override
    protected void onPause() {
        super.onPause();

        // Pause UI and dispose of MainManager resources temporarily
        if (ui != null && main != null) {
            ui.pause();
            main.dispose();
        }
    }

    // Flag to track if the activity has been disposed
    private boolean disposed = false;

    /**
     * Clean up all resources, receivers, and services.
     * Called during onDestroy or when the activity needs to reload.
     */
    private void dispose() {
        if(disposed) return;

        // Unregister receivers
        try {
            LocalBroadcastManager.getInstance(this.getApplicationContext()).unregisterReceiver(privateIOReceiver);
            getApplicationContext().unregisterReceiver(publicIOReceiver);
        } catch (Exception e) {}

        // Stop background services
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

        // Send destroy signal to NotificationService
        try {
            Intent notificationIntent = new Intent(this, NotificationService.class);
            notificationIntent.putExtra(NotificationService.DESTROY, true);
            startService(notificationIntent);
        } catch (Throwable e) {
            Tuils.log(e);
        }

        overridePendingTransition(0,0);

        // Destroy managers
        if(main != null) main.destroy();
        if(ui != null) ui.dispose();

        // Dispose singleton managers
        XMLPrefsManager.dispose();
        RegexManager.instance.dispose();
        TimeManager.instance.dispose();

        disposed = true;
    }

    /**
     * Called when the activity is destroying.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();

        dispose();
    }

    /**
     * Called when the back button is pressed.
     */
    @Override
    public void onBackPressed() {
        // Delegate to UI manager if back button is enabled
        if (backButtonEnabled && main != null) {
            ui.onBackPressed();
        }
    }

    /**
     * Handle long key presses (specifically Back button).
     */
    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (keyCode != KeyEvent.KEYCODE_BACK)
            return super.onKeyLongPress(keyCode, event);

        // Notify MainManager of long back press (often used to kill app or show menu)
        if (main != null)
            main.onLongBack();
        return true;
    }

    /**
     * Implementation of Reloadable interface.
     * Triggers a restart of the activity.
     */
    @Override
    public void reload() {
        runOnUiThread(stopActivity);
    }

    /**
     * Adds a message to be displayed after reload.
     * @param header The header/title of the message category.
     * @param message The message content.
     */
    @Override
    public void addMessage(String header, String message) {
        // Check if category already exists
        for(ReloadMessageCategory cs : categories) {
            Tuils.log(cs.header, header);
            if(cs.header.equals(header)) {
                cs.lines.add(message);
                return;
            }
        }

        // Create new category if not found
        ReloadMessageCategory c = new ReloadMessageCategory(header);
        if(message != null) c.lines.add(message);
        categories.add(c);
    }

    /**
     * Called when the window focus changes.
     */
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        // Refocus the terminal input when window gains focus
        if (hasFocus && ui != null) {
            ui.focusTerminal();
        }
    }

    // Temporary storage for the context menu suggestion item
    SuggestionsManager.Suggestion suggestion;

    /**
     * Creates a context menu (e.g. long press on a suggestion).
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        // Retrieve the suggestion object from the view tag
        suggestion = (SuggestionsManager.Suggestion) v.getTag(R.id.suggestion_id);

        // If it's a contact, show available numbers
        if(suggestion.type == SuggestionsManager.Suggestion.TYPE_CONTACT) {
            ContactManager.Contact contact = (ContactManager.Contact) suggestion.object;

            menu.setHeaderTitle(contact.name);
            for(int count = 0; count < contact.numbers.size(); count++) {
                menu.add(0, count, count, contact.numbers.get(count));
            }
        }
    }

    /**
     * Handles context menu item selection.
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if(suggestion != null) {
            if(suggestion.type == SuggestionsManager.Suggestion.TYPE_CONTACT) {
                // If a number was selected for a contact, use that number
                ContactManager.Contact contact = (ContactManager.Contact) suggestion.object;
                contact.setSelectedNumber(item.getItemId());

                // Send the contact name to input
                Tuils.sendInput(this, suggestion.getText());

                return true;
            }
        }

        return false;
    }

    /**
     * Handles results from launched activities (e.g. Tuixt).
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == TUIXT_REQUEST && resultCode != 0) {
            if(resultCode == TuixtActivity.BACK_PRESSED) {
                Tuils.sendOutput(this, R.string.tuixt_back_pressed);
            } else {
                // Show error if Tuixt failed
                Tuils.sendOutput(this, data.getStringExtra(TuixtActivity.ERROR_KEY));
            }
        }
    }

    /**
     * Handles the result of permission requests.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        // Refresh contacts if READ_CONTACTS was granted
        if(permissions.length > 0 && permissions[0].equals(Manifest.permission.READ_CONTACTS) && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            LocalBroadcastManager.getInstance(this.getApplicationContext()).sendBroadcast(new Intent(ContactManager.ACTION_REFRESH));
        }

        try {
            switch (requestCode) {
                case COMMAND_REQUEST_PERMISSION:
                    // Permission requested by a command execution
                    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        // Retry the command
                        MainPack info = main.getMainPack();
                        main.onCommand(info.lastCommand, (String) null, false);
                    } else {
                        // Permission denied
                        ui.setOutput(getString(R.string.output_nopermissions), TerminalManager.CATEGORY_OUTPUT);
                        main.sendPermissionNotGrantedWarning();
                    }
                    break;
                case STARTING_PERMISSION:
                    // Initial storage permissions
                    int count = 0;
                    while(count < permissions.length && count < grantResults.length) {
                        if(grantResults[count] == PackageManager.PERMISSION_DENIED) {
                            Toast.makeText(this, R.string.permissions_toast, Toast.LENGTH_LONG).show();
                            // If denied, show toast and restart activity (essentially a loop until granted or quit)
                            new Thread() {
                                @Override
                                public void run() {
                                    super.run();

                                    try {
                                        sleep(2000);
                                    } catch (InterruptedException e) {}

                                    runOnUiThread(stopActivity);
                                }
                            }.start();
                            return;
                        }
                        count++;
                    }
                    // All granted, proceed to init
                    canApplyTheme = false;
                    finishOnCreate();
                    break;
                case COMMAND_SUGGESTION_REQUEST_PERMISSION:
                    // Permission requested by suggestion engine
                    if (grantResults.length == 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                        ui.setOutput(getString(R.string.output_nopermissions), TerminalManager.CATEGORY_OUTPUT);
                    }
                    break;
                case LOCATION_REQUEST_PERMISSION:
                    // Location permission result
                    Intent i = new Intent(TuiLocationManager.ACTION_GOT_PERMISSION);
                    i.putExtra(XMLPrefsManager.VALUE_ATTRIBUTE, grantResults[0]);
                    LocalBroadcastManager.getInstance(this.getApplicationContext()).sendBroadcast(i);

                    break;
            }
        } catch (Exception e) {}
    }

    /**
     * Called when the activity receives a new Intent (e.g. from a shortcut or another app).
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        // Check for text input passed via the intent
        String cmd = intent.getStringExtra(PrivateIOReceiver.TEXT);
        if(cmd != null) {
            // Broadcast the command to be executed
            Intent i = new Intent(MainManager.ACTION_EXEC);
            i.putExtra(MainManager.CMD_COUNT, MainManager.commandCount);
            i.putExtra(MainManager.CMD, cmd);
            i.putExtra(MainManager.NEED_WRITE_INPUT, true);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(i);
        }
    }

    /**
     * Called when configuration changes (e.g. screen rotation).
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
}
