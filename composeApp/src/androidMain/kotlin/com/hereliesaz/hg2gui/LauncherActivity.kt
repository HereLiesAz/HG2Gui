package com.hereliesaz.hg2gui

import android.Manifest
import android.content.*
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.*
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.hereliesaz.hg2gui.commands.main.MainPack
import com.hereliesaz.hg2gui.commands.tuixt.TuixtActivity
import com.hereliesaz.hg2gui.managers.ContactManager
import com.hereliesaz.hg2gui.managers.RegexManager
import com.hereliesaz.hg2gui.managers.TerminalManager
import com.hereliesaz.hg2gui.managers.TimeManager
import com.hereliesaz.hg2gui.managers.TuiLocationManager
import com.hereliesaz.hg2gui.managers.notifications.KeeperService
import com.hereliesaz.hg2gui.managers.notifications.NotificationManager
import com.hereliesaz.hg2gui.managers.notifications.NotificationMonitorService
import com.hereliesaz.hg2gui.managers.notifications.NotificationService
import com.hereliesaz.hg2gui.managers.suggestions.SuggestionsManager
import com.hereliesaz.hg2gui.managers.xml.XMLPrefsManager
import com.hereliesaz.hg2gui.managers.xml.options.Behavior
import com.hereliesaz.hg2gui.managers.xml.options.Notifications
import com.hereliesaz.hg2gui.managers.xml.options.Theme
import com.hereliesaz.hg2gui.managers.xml.options.Ui
import com.hereliesaz.hg2gui.tuils.*
import com.hereliesaz.hg2gui.tuils.interfaces.Inputable
import com.hereliesaz.hg2gui.tuils.interfaces.Outputable
import com.hereliesaz.hg2gui.tuils.interfaces.Reloadable
import com.hereliesaz.hg2gui.tuils.interfaces.Reloadable.ReloadMessageCategory
import java.util.*

/**
 * The main activity of the application.
 * <p>
 * This class serves as the entry point for the HG2Gui (formerly T-UI) launcher.
 * It is responsible for initializing the primary UI components, managing the application lifecycle,
 * handling runtime permissions, and coordinating between the [UIManager] (View) and [MainManager] (Controller).
 * </p>
 * <p>
 * As a Launcher activity, it is configured in the AndroidManifest with category android.intent.category.LAUNCHER
 * and android.intent.category.HOME.
 * </p>
 */
class LauncherActivity : AppCompatActivity(), Reloadable {

    companion object {
        /** Request code used when asking for permissions required by a specific command. */
        const val COMMAND_REQUEST_PERMISSION = 10
        /** Request code used for the initial startup permissions (Storage). */
        const val STARTING_PERMISSION = 11
        /** Request code used when suggestion engine needs permissions (e.g. Contacts). */
        const val COMMAND_SUGGESTION_REQUEST_PERMISSION = 12
        /** Request code used when requesting Location permissions. */
        const val LOCATION_REQUEST_PERMISSION = 13

        /** Request code for launching the internal text editor (Tuixt). */
        const val TUIXT_REQUEST = 10
    }

    /**
     * Manages the visual elements of the terminal (TextViews, Input, Layouts).
     * Acts as the 'View' in the architecture.
     */
    private var ui: UIManager? = null

    /**
     * Manages the logic and state of the terminal.
     * Acts as the 'Controller' in the architecture.
     */
    private var main: MainManager? = null

    /**
     * Receiver for internal broadcasts within the app (Private I/O).
     * Used for communication between services/components and the UI.
     */
    private var privateIOReceiver: PrivateIOReceiver? = null

    /**
     * Receiver for public broadcasts.
     * Allows external apps to send commands or output to this launcher.
     */
    private var publicIOReceiver: PublicIOReceiver? = null

    // Configuration flags loaded from preferences
    private var openKeyboardOnStart = false // Should keyboard open immediately on launch?
    private var canApplyTheme = false // Is it safe to apply theme (permissions granted)?
    private var backButtonEnabled = false // Is the back button allowed to close the keyboard/app?

    /**
     * Stores messages that need to be displayed after a reload/restart of the activity.
     * This allows preserving error messages or status updates across activity recreation.
     */
    private lateinit var categories: MutableSet<ReloadMessageCategory>

    /**
     * Runnable task to stop and restart the activity.
     * This is used to apply changes that require a full Activity recreation (e.g. theme changes).
     */
    private val stopActivity = Runnable {
        // Clean up resources before finishing
        dispose()
        finish()

        // Create an intent to restart the activity
        val startMain = Intent(Intent.ACTION_MAIN)
        startMain.addCategory(Intent.CATEGORY_HOME)

        // Aggregate reload messages to pass to the new instance
        var reloadMessage: CharSequence = Tuils.EMPTYSTRING
        for (c in categories) {
            reloadMessage = TextUtils.concat(reloadMessage, Tuils.NEWLINE, c.text())
        }
        startMain.putExtra(Reloadable.MESSAGE, reloadMessage)

        // Start the new instance
        startActivity(startMain)
    }

    /**
     * Implementation of the Inputable interface.
     * Defines how the Activity handles input-related actions requested by other components.
     */
    private val `in`: Inputable = object : Inputable {

        /**
         * Sets the text in the input field.
         * @param s The string to insert into the input field.
         */
        override fun `in`(s: String) {
            ui?.setInput(s)
        }

        /**
         * Changes the hint text displayed in the input field.
         * Runs on the UI thread to ensure thread safety.
         * @param s The new hint text.
         */
        override fun changeHint(s: String) {
            runOnUiThread { ui?.setHint(s) }
        }

        /**
         * Resets the hint text to the default.
         * Runs on the UI thread.
         */
        override fun resetHint() {
            runOnUiThread { ui?.resetHint() }
        }
    }

    /**
     * Implementation of the Outputable interface.
     * Defines how the Activity handles outputting text to the terminal screen.
     * Includes a buffering mechanism to handle output before the UI is fully ready.
     */
    private val out: Outputable = object : Outputable {

        private val DELAY = 500L // Delay in milliseconds for retrying UI access

        // Queues to buffer output if UI is not yet initialized
        var textColor: Queue<SimpleMutableEntry<CharSequence, Int>>? = LinkedList()
        var textCategory: Queue<SimpleMutableEntry<CharSequence, Int>>? = LinkedList()

        var charged = false // Flag to indicate if the retry handler is active
        var handler: Handler? = Handler(Looper.getMainLooper())

        // Runnable that attempts to flush the buffered output to the UI
        var r: Runnable? = object : Runnable {
            override fun run() {
                // If UI is still null, schedule another check later
                if (ui == null) {
                    handler?.postDelayed(this, DELAY)
                    return
                }

                // Flush textCategory queue
                while (textCategory?.peek() != null) {
                    val sm = textCategory!!.poll()!!
                    ui?.setOutput(sm.key, sm.value)
                }

                // Flush textColor queue
                while (textColor?.peek() != null) {
                    val sm = textColor!!.poll()!!
                    ui?.setOutput(sm.value, sm.key)
                }

                // Clear references
                textCategory = null
                textColor = null
                handler = null
                r = null
            }
        }

        /**
         * Output text with the default category.
         * @param output The text to display.
         */
        override fun onOutput(output: CharSequence) {
            if (ui != null) ui!!.setOutput(output, TerminalManager.CATEGORY_OUTPUT)
            else {
                // Buffer the output if UI is not ready
                textCategory?.add(SimpleMutableEntry(output, TerminalManager.CATEGORY_OUTPUT))

                if (!charged) {
                    charged = true
                    r?.let { handler?.postDelayed(it, DELAY) }
                }
            }
        }

        /**
         * Output text with a specific category (e.g., error, info).
         * @param output The text to display.
         * @param category The category ID.
         */
        override fun onOutput(output: CharSequence, category: Int) {
            if (ui != null) ui!!.setOutput(output, category)
            else {
                textCategory?.add(SimpleMutableEntry(output, category))

                if (!charged) {
                    charged = true
                    r?.let { handler?.postDelayed(it, DELAY) }
                }
            }
        }

        /**
         * Output text with a specific color.
         * @param color The color integer.
         * @param output The text to display.
         */
        override fun onOutput(color: Int, output: CharSequence) {
            if (ui != null) ui!!.setOutput(color, output)
            else {
                textColor?.add(SimpleMutableEntry(output, color))

                if (!charged) {
                    charged = true
                    r?.let { handler?.postDelayed(it, DELAY) }
                }
            }
        }

        /**
         * Cleans up handler callbacks to prevent memory leaks.
         */
        override fun dispose() {
            handler?.removeCallbacksAndMessages(null)
        }
    }

    /**
     * Called when the activity is first created.
     * This is the entry point for initialization.
     * @param savedInstanceState Bundle containing the activity's previously saved state.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Disable default transition animations for a snappier feel
        overridePendingTransition(0, 0)

        // Safety check: if activity is already finishing, do nothing
        if (isFinishing) {
            return
        }

        // Check for essential storage permissions on Android M+
        // These are required for loading config files and themes.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            !(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
        ) {

            // Request permissions if not granted
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE),
                STARTING_PERMISSION
            )
        } else {
            // Permissions are granted (or not needed on older Android), proceed with initialization
            canApplyTheme = true
            finishOnCreate()
        }
    }

    /**
     * Completes the initialization process after permissions are confirmed.
     * Sets up managers, receivers, UI, and services.
     */
    private fun finishOnCreate() {

        // Set a global exception handler to catch crashes and log them to a file
        Thread.currentThread().uncaughtExceptionHandler = CustomExceptionHandler()

        // Load common preferences (XML configuration)
        XMLPrefsManager.loadCommons(this)
        // Initialize regex manager for parsing commands
        RegexManager(this@LauncherActivity)
        // Initialize time manager
        TimeManager(this)

        // Register the PrivateIOReceiver for internal app communication
        val filter = IntentFilter()
        filter.addAction(PrivateIOReceiver.ACTION_INPUT)
        filter.addAction(PrivateIOReceiver.ACTION_OUTPUT)
        filter.addAction(PrivateIOReceiver.ACTION_REPLY)

        privateIOReceiver = PrivateIOReceiver(this, out, `in`)
        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(privateIOReceiver!!, filter)

        // Register the PublicIOReceiver for external communication
        val filter1 = IntentFilter()
        filter1.addAction(PublicIOReceiver.ACTION_CMD)
        filter1.addAction(PublicIOReceiver.ACTION_OUTPUT)

        publicIOReceiver = PublicIOReceiver()
        ContextCompat.registerReceiver(applicationContext, publicIOReceiver!!, filter1, ContextCompat.RECEIVER_EXPORTED)

        // Handle screen orientation settings
        val requestedOrientation = XMLPrefsManager.getInt(Behavior.orientation)
        if (requestedOrientation >= 0 && requestedOrientation != 2) {
            val orientation = resources.configuration.orientation
            if (orientation != requestedOrientation) setRequestedOrientation(requestedOrientation)
            // Lock orientation on Jelly Bean MR2 and above
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED)
            }
        }

        // Configure Status Bar and Navigation Bar colors on Lollipop+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !XMLPrefsManager.getBoolean(Ui.ignore_bar_color)) {
            val window = window

            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.statusBarColor = XMLPrefsManager.getColor(Theme.statusbar_color)
            window.navigationBarColor = XMLPrefsManager.getColor(Theme.navigationbar_color)
        }

        // Check if back button should be enabled
        backButtonEnabled = XMLPrefsManager.getBoolean(Behavior.back_button_enabled)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (backButtonEnabled && main != null) {
                    ui?.onBackPressed()
                }
            }
        })

        // Manage the notification listener service (KeeperService) based on preferences
        val showNotification = XMLPrefsManager.getBoolean(Behavior.tui_notification)
        val keeperIntent = Intent(this, KeeperService::class.java)
        if (showNotification) {
            keeperIntent.putExtra(KeeperService.PATH_KEY, XMLPrefsManager.get(Behavior.home_path))
            startService(keeperIntent)
        } else {
            try {
                stopService(keeperIntent)
            } catch (e: Exception) {
            }
        }

        // Configure fullscreen mode
        val fullscreen = XMLPrefsManager.getBoolean(Ui.fullscreen)
        if (fullscreen) {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }

        // Apply wallpaper theme (transparent/system wallpaper vs solid background)
        val useSystemWP = XMLPrefsManager.getBoolean(Ui.system_wallpaper)
        if (useSystemWP) {
            setTheme(R.style.Custom_SystemWP)
        } else {
            setTheme(R.style.Custom_Solid)
        }

        // Initialize Notification Manager
        try {
            NotificationManager.create(this)
        } catch (e: Exception) {
            Tuils.toFile(e)
        }

        // Handle advanced notification features (listening to system notifications)
        val notificationsVal = XMLPrefsManager.get(Notifications.show_notifications)
        val notifications = XMLPrefsManager.getBoolean(Notifications.show_notifications) || notificationsVal.equals("enabled", ignoreCase = true)
        if (notifications) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                try {
                    // Enable the NotificationService component
                    val notificationComponent = ComponentName(this, NotificationService::class.java)
                    val pm = packageManager
                    pm.setComponentEnabledSetting(notificationComponent, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)

                    // Check if the user has granted Notification Access permission
                    if (!Tuils.hasNotificationAccess(this)) {
                        val i = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                        if (i.resolveActivity(packageManager) == null) {
                            Toast.makeText(this, R.string.no_notification_access, Toast.LENGTH_LONG).show()
                        } else {
                            startActivity(i)
                        }
                    }

                    // Start monitoring services
                    val monitor = Intent(this, NotificationMonitorService::class.java)
                    startService(monitor)

                    val notificationIntent = Intent(this, NotificationService::class.java)
                    startService(notificationIntent)
                } catch (er: NoClassDefFoundError) {
                    val intent = Intent(PrivateIOReceiver.ACTION_OUTPUT)
                    intent.putExtra(PrivateIOReceiver.TEXT, getString(R.string.output_notification_error) + Tuils.SPACE + er.toString())
                }
            } else {
                // Notify user if API level is too low for notifications
                Tuils.sendOutput(Color.RED, this, R.string.notification_low_api)
            }
        }

        // Set vibration duration for long clicks
        LongClickableSpan.longPressVibrateDuration = XMLPrefsManager.getInt(Behavior.long_click_vibration_duration)

        // Determine if keyboard should open automatically
        openKeyboardOnStart = XMLPrefsManager.getBoolean(Behavior.auto_show_keyboard)
        if (!openKeyboardOnStart) {
            this.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN or WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }

        // Set the content view to the main layout
        setContentView(R.layout.base_view)

        // Display restart message if one exists (passed from previous instance)
        if (XMLPrefsManager.getBoolean(Ui.show_restart_message)) {
            val s = intent.getCharSequenceExtra(Reloadable.MESSAGE)
            if (s != null) out.onOutput(Tuils.span(s, XMLPrefsManager.getColor(Theme.restart_message_color)))
        }

        categories = HashSet<ReloadMessageCategory>()

        // Initialize the MainManager (Logic Controller)
        main = MainManager(this, this)

        // Get reference to the main container view
        val mainView = findViewById<ViewGroup>(R.id.mainview)

        // Handle Light Status Bar icons (dark icons on light background) for Android M+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !XMLPrefsManager.getBoolean(Ui.ignore_bar_color) && !XMLPrefsManager.getBoolean(Ui.statusbar_light_icons)) {
            mainView.systemUiVisibility = mainView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        // Initialize the UIManager (View Controller)
        ui = UIManager(this, mainView, main!!.mainPack, canApplyTheme, main!!.executer())

        // Connect UIManager and MainManager via RedirectionListener
        main!!.setRedirectionListener(ui!!.buildRedirectionListener())
        ui!!.pack = main!!.mainPack

        // Initialize input and focus
        `in`.`in`(Tuils.EMPTYSTRING)
        ui!!.focusTerminal()

        // Set up Assist gesture if in fullscreen
        if (fullscreen) Assist.assistActivity(this)

        // Run garbage collector to clean up initialization objects
        System.gc()
    }

    /**
     * Called when the activity is becoming visible to the user.
     */
    override fun onStart() {
        super.onStart()

        // Notify UI to start (e.g. show keyboard if needed)
        ui?.onStart(openKeyboardOnStart)
    }

    /**
     * Called after the activity has been stopped, prior to it being started again.
     */
    override fun onRestart() {
        super.onRestart()

        // Send a broadcast to update command suggestions
        LocalBroadcastManager.getInstance(this.applicationContext).sendBroadcast(Intent(UIManager.ACTION_UPDATE_SUGGESTIONS))
    }

    /**
     * Called when the system is about to start resuming another activity.
     */
    override fun onPause() {
        super.onPause()

        // Pause UI and dispose of MainManager resources temporarily
        if (ui != null && main != null) {
            ui!!.pause()
            main!!.dispose()
        }
    }

    // Flag to track if the activity has been disposed
    private var disposed = false

    /**
     * Clean up all resources, receivers, and services.
     * Called during onDestroy or when the activity needs to reload.
     */
    private fun dispose() {
        if (disposed) return

        // Unregister receivers
        try {
            LocalBroadcastManager.getInstance(this.applicationContext).unregisterReceiver(privateIOReceiver!!)
            applicationContext.unregisterReceiver(publicIOReceiver)
        } catch (e: Exception) {
        }

        // Stop background services
        try {
            stopService(Intent(this, NotificationMonitorService::class.java))
        } catch (e: NoClassDefFoundError) {
            Tuils.log(e)
        } catch (e: Exception) {
            Tuils.log(e)
        }

        try {
            stopService(Intent(this, KeeperService::class.java))
        } catch (e: NoClassDefFoundError) {
            Tuils.log(e)
        } catch (e: Exception) {
            Tuils.log(e)
        }

        // Send destroy signal to NotificationService
        try {
            val notificationIntent = Intent(this, NotificationService::class.java)
            notificationIntent.putExtra(NotificationService.DESTROY, true)
            startService(notificationIntent)
        } catch (e: Throwable) {
            Tuils.log(e)
        }

        overridePendingTransition(0, 0)

        // Destroy managers
        main?.destroy()
        ui?.dispose()

        // Dispose singleton managers
        XMLPrefsManager.dispose()
        RegexManager.instance.dispose()
        TimeManager.instance?.dispose()

        disposed = true
    }

    /**
     * Called when the activity is destroying.
     */
    override fun onDestroy() {
        super.onDestroy()

        dispose()
    }

    /**
     * Called when the back button is pressed.
     */


    /**
     * Handle long key presses (specifically Back button).
     */
    @Suppress("BackPress")
    override fun onKeyLongPress(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode != KeyEvent.KEYCODE_BACK)
            return super.onKeyLongPress(keyCode, event)

        // Notify MainManager of long back press (often used to kill app or show menu)
        main?.onLongBack()
        return true
    }

    /**
     * Implementation of Reloadable interface.
     * Triggers a restart of the activity.
     */
    override fun reload() {
        runOnUiThread(stopActivity)
    }

    /**
     * Adds a message to be displayed after reload.
     * @param header The header/title of the message category.
     * @param message The message content.
     */
    override fun addMessage(header: String, message: String) {
        // Check if category already exists
        for (cs in categories) {
            Tuils.log(cs.header, header)
            if (cs.header == header) {
                cs.lines.add(message)
                return
            }
        }

        // Create new category if not found
        val c = ReloadMessageCategory(header)
        c.lines.add(message)
        categories.add(c)
    }

    /**
     * Called when the window focus changes.
     */
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        // Refocus the terminal input when window gains focus
        if (hasFocus && ui != null) {
            ui!!.focusTerminal()
        }
    }

    // Temporary storage for the context menu suggestion item
    private var suggestion: SuggestionsManager.Suggestion? = null

    /**
     * Creates a context menu (e.g. long press on a suggestion).
     */
    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)

        // Retrieve the suggestion object from the view tag
        suggestion = v.getTag(R.id.suggestion_id) as? SuggestionsManager.Suggestion

        // If it's a contact, show available numbers
        suggestion?.let {
            if (it.type == SuggestionsManager.Suggestion.TYPE_CONTACT) {
                val contact = it.`object` as ContactManager.Contact

                menu.setHeaderTitle(contact.name)
                for (count in contact.numbers.indices) {
                    menu.add(0, count, count, contact.numbers[count])
                }
            }
        }
    }

    /**
     * Handles context menu item selection.
     */
    override fun onContextItemSelected(item: MenuItem): Boolean {
        suggestion?.let {
            if (it.type == SuggestionsManager.Suggestion.TYPE_CONTACT) {
                // If a number was selected for a contact, use that number
                val contact = it.`object` as ContactManager.Contact
                contact.setSelectedNumber(item.itemId)

                // Send the contact name to input
                Tuils.sendInput(this, it.text)

                return true
            }
        }

        return false
    }

    /**
     * Handles results from launched activities (e.g. Tuixt).
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == TUIXT_REQUEST && resultCode != 0) {
            if (resultCode == TuixtActivity.BACK_PRESSED) {
                Tuils.sendOutput(this, R.string.tuixt_back_pressed)
            } else if (data != null) {
                // Show error if Tuixt failed
                Tuils.sendOutput(this, data.getStringExtra(TuixtActivity.ERROR_KEY) as CharSequence)
            }
        }
    }

    /**
     * Handles the result of permission requests.
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Refresh contacts if READ_CONTACTS was granted
        if (permissions.isNotEmpty() && permissions[0] == Manifest.permission.READ_CONTACTS && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            LocalBroadcastManager.getInstance(this.applicationContext).sendBroadcast(Intent(ContactManager.ACTION_REFRESH))
        }

        try {
            when (requestCode) {
                COMMAND_REQUEST_PERMISSION -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Retry the command
                    val info = main!!.mainPack
                    main!!.onCommand(input = info.lastCommand ?: "", alias = null, wasMusicService = false)
                } else {
                    // Permission denied
                    ui!!.setOutput(getString(R.string.output_nopermissions), TerminalManager.CATEGORY_OUTPUT)
                    main!!.sendPermissionNotGrantedWarning()
                }
                STARTING_PERMISSION -> {
                    // Initial storage permissions
                    var count = 0
                    while (count < permissions.size && count < grantResults.size) {
                        if (grantResults[count] == PackageManager.PERMISSION_DENIED) {
                            Toast.makeText(this, R.string.permissions_toast, Toast.LENGTH_LONG).show()
                            // If denied, show toast and restart activity (essentially a loop until granted or quit)
                            object : Thread() {
                                override fun run() {
                                    super.run()

                                    try {
                                        sleep(2000)
                                    } catch (e: InterruptedException) {
                                    }

                                    runOnUiThread(stopActivity)
                                }
                            }.start()
                            return
                        }
                        count++
                    }
                    // All granted, proceed to init
                    canApplyTheme = false
                    finishOnCreate()
                }
                COMMAND_SUGGESTION_REQUEST_PERMISSION -> if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    ui!!.setOutput(getString(R.string.output_nopermissions), TerminalManager.CATEGORY_OUTPUT)
                }
                LOCATION_REQUEST_PERMISSION -> {
                    // Location permission result
                    val i = Intent(TuiLocationManager.ACTION_GOT_PERMISSION)
                    i.putExtra(XMLPrefsManager.VALUE_ATTRIBUTE, grantResults[0])
                    LocalBroadcastManager.getInstance(this.applicationContext).sendBroadcast(i)
                }
            }
        } catch (e: Exception) {
        }
    }

    /**
     * Called when the activity receives a new Intent (e.g. from a shortcut or another app).
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        // Check for text input passed via the intent
        val cmd = intent.getStringExtra(PrivateIOReceiver.TEXT)
        if (cmd != null) {
            // Broadcast the command to be executed
            val i = Intent(MainManager.ACTION_EXEC)
            i.putExtra(MainManager.CMD_COUNT, MainManager.commandCount)
            i.putExtra(MainManager.CMD, cmd)
            i.putExtra(MainManager.NEED_WRITE_INPUT, true)
            LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(i)
        }
    }

    /**
     * Called when configuration changes (e.g. screen rotation).
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }
}
