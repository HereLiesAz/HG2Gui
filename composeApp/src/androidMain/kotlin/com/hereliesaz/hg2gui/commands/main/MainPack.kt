package com.hereliesaz.hg2gui.commands.main

import android.content.Context
import android.content.res.Resources
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import com.hereliesaz.hg2gui.AndroidPlatformContext
import com.hereliesaz.hg2gui.PlatformContext
import com.hereliesaz.hg2gui.commands.CommandGroup
import com.hereliesaz.hg2gui.commands.CommandRepository
import com.hereliesaz.hg2gui.commands.CommandsPreferences
import com.hereliesaz.hg2gui.commands.ExecutePack
import com.hereliesaz.hg2gui.managers.AliasManager
import com.hereliesaz.hg2gui.managers.AppsManager
import com.hereliesaz.hg2gui.managers.ContactManager
import com.hereliesaz.hg2gui.managers.RssManager
import com.hereliesaz.hg2gui.managers.TerminalManager
import com.hereliesaz.hg2gui.managers.flashlight.TorchManager
import com.hereliesaz.hg2gui.managers.music.MusicManager2
import com.hereliesaz.hg2gui.managers.xml.XMLPrefsManager
import com.hereliesaz.hg2gui.managers.xml.options.Behavior
import com.hereliesaz.hg2gui.tuils.interfaces.Redirectator
import com.hereliesaz.hg2gui.tuils.libsuperuser.ShellHolder
import okhttp3.OkHttpClient
import java.io.File
import java.lang.reflect.Method

/**
 * Created by francescoandreuzzi on 24/01/2017.
 */
class MainPack(
    @JvmField var androidContext: Context,
    override var commandGroup: CommandGroup,
    @JvmField var aliasManager: AliasManager,
    @JvmField var appsManager: AppsManager,
    @JvmField var player: MusicManager2?,
    @JvmField var contacts: ContactManager?,
    @JvmField var redirectator: Redirectator,
    @JvmField var rssManager: RssManager?,
    @JvmField var client: OkHttpClient,
    @JvmField var commandRepository: CommandRepository,
) : ExecutePack() {

    //	current directory
    @JvmField
    var currentDirectory: File? = XMLPrefsManager.get(File::class.java, Behavior.home_path)

    //	resources references
    @JvmField
    var res: Resources = androidContext.resources

    //	internet
    @JvmField
    var wifi: WifiManager? = null

    //	3g/data
    @JvmField
    var setMobileDataEnabledMethod: Method? = null

    @JvmField
    var connectivityMgr: ConnectivityManager? = null

    @JvmField
    var connectMgr: Any? = null

    @JvmField
    var cmdPrefs: CommandsPreferences = CommandsPreferences()

    @JvmField
    var lastCommand: String? = null

    @JvmField
    var shellHolder: ShellHolder? = null

    @JvmField
    var commandColor: Int = TerminalManager.NO_COLOR

    fun dispose() {
        val mgr = TorchManager.getInstance()
        if (mgr.isOn) mgr.turnOff()
    }

    fun destroy() {
        player?.destroy()
        appsManager.onDestroy()
        rssManager?.dispose()
        contacts?.destroy(androidContext)
    }

    override val context: PlatformContext
        get() = AndroidPlatformContext(androidContext)

    override fun getLaunchInfo(): AppsManager.LaunchInfo? {
        return get() as? AppsManager.LaunchInfo
    }

    override fun clear() {
        super.clear()
        commandColor = TerminalManager.NO_COLOR
    }
}
