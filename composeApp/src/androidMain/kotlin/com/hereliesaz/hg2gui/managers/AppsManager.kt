package com.hereliesaz.hg2gui.managers

import android.content.*
import android.content.pm.*
import android.graphics.Color
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.os.Process
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.hereliesaz.hg2gui.MainManager
import com.hereliesaz.hg2gui.R
import com.hereliesaz.hg2gui.UIManager
import com.hereliesaz.hg2gui.commands.main.MainPack
import com.hereliesaz.hg2gui.managers.xml.XMLPrefsManager
import com.hereliesaz.hg2gui.managers.xml.XMLPrefsManager.Companion.VALUE_ATTRIBUTE
import com.hereliesaz.hg2gui.managers.xml.XMLPrefsManager.Companion.resetFile
import com.hereliesaz.hg2gui.managers.xml.XMLPrefsManager.Companion.set
import com.hereliesaz.hg2gui.managers.xml.XMLPrefsManager.Companion.writeTo
import com.hereliesaz.hg2gui.managers.xml.classes.XMLPrefsElement
import com.hereliesaz.hg2gui.managers.xml.classes.XMLPrefsList
import com.hereliesaz.hg2gui.managers.xml.classes.XMLPrefsSave
import com.hereliesaz.hg2gui.managers.xml.options.Apps
import com.hereliesaz.hg2gui.managers.xml.options.Behavior
import com.hereliesaz.hg2gui.managers.xml.options.Theme
import com.hereliesaz.hg2gui.managers.xml.options.Ui
import com.hereliesaz.hg2gui.tuils.StoppableThread
import com.hereliesaz.hg2gui.tuils.Tuils
import it.andreuzzi.comparestring2.StringableObject
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.SAXParseException
import java.io.File
import java.util.*
import java.util.regex.Pattern

class AppsManager(private val context: Context) : XMLPrefsElement {

    private val NAME = "APPS"
    private var file: File? = null

    private val SHOW_ATTRIBUTE = "show"
    private val APPS_ATTRIBUTE = "apps"
    private val BGCOLOR_ATTRIBUTE = "bgColor"
    private val FORECOLOR_ATTRIBUTE = "foreColor"

    private var appsHolder: AppsHolder? = null
    private var hiddenApps: MutableList<LaunchInfo> = ArrayList()

    private val PREFS = "apps"
    private val preferences: SharedPreferences = context.getSharedPreferences(PREFS, 0)
    private val editor: SharedPreferences.Editor = preferences.edit()

    private var prefsList: XMLPrefsList? = null

    var groups: MutableList<Group> = ArrayList()

    private var pp: Pattern? = null
    private var pl: Pattern? = null
    private var appInstalledFormat: String? = null
    private var appUninstalledFormat: String? = null
    private var appInstalledColor = 0
    private var appUninstalledColor = 0

    init {
        instance = this

        appInstalledFormat = if (XMLPrefsManager.getBoolean(Ui.show_app_installed)) XMLPrefsManager.get(Behavior.app_installed_format) else null
        appUninstalledFormat = if (XMLPrefsManager.getBoolean(Ui.show_app_uninstalled)) XMLPrefsManager.get(Behavior.app_uninstalled_format) else null

        if (appInstalledFormat != null || appUninstalledFormat != null) {
            pp = Pattern.compile("%p", Pattern.CASE_INSENSITIVE)
            pl = Pattern.compile("%l", Pattern.CASE_INSENSITIVE)

            appInstalledColor = XMLPrefsManager.getColor(Theme.app_installed_color)
            appUninstalledColor = XMLPrefsManager.getColor(Theme.app_uninstalled_color)
        } else {
            pp = null
            pl = null
        }

        val root = Tuils.getFolder()
        this.file = if (root == null) null else File(root, PATH)

        initAppListener(context)

        object : StoppableThread() {
            override fun run() {
                super.run()
                fill()
                LocalBroadcastManager.getInstance(context.applicationContext).sendBroadcast(Intent(UIManager.ACTION_UPDATE_SUGGESTIONS))
            }
        }.start()
    }

    override fun delete(): Array<String>? = null

    override fun write(save: XMLPrefsSave, value: String) {
        val f = File(Tuils.getFolder(), PATH)
        set(f, save.label(), arrayOf(VALUE_ATTRIBUTE), arrayOf(value))
    }

    override fun path(): String = PATH

    override fun getValues(): XMLPrefsList? = prefsList

    private val appsBroadcast: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            val data = intent.data?.schemeSpecificPart ?: return
            if (action == Intent.ACTION_PACKAGE_ADDED) {
                appInstalled(data)
            } else {
                appUninstalled(data)
            }
        }
    }

    private fun initAppListener(c: Context) {
        val intentFilter = IntentFilter()
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED)
        intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED)
        intentFilter.addDataScheme("package")
        c.registerReceiver(appsBroadcast, intentFilter)
    }

    fun fill() {
        val allApps = createAppMap(context.packageManager)
        hiddenApps = ArrayList()

        groups.clear()

        try {
            prefsList = XMLPrefsList()

            val f = file
            if (f != null) {
                if (!f.exists()) {
                    resetFile(f, NAME)
                }

                val o: Array<Any?>?
                try {
                    o = XMLPrefsManager.buildDocument(f, NAME)
                    if (o == null) {
                        Tuils.sendXMLParseError(context, PATH)
                        return
                    }
                } catch (e: SAXParseException) {
                    Tuils.sendXMLParseError(context, PATH, e)
                    return
                } catch (e: Exception) {
                    Tuils.log(e)
                    return
                }

                val d = o[0] as Document
                val root = o[1] as Element

                val enums: MutableList<Apps> = ArrayList(listOf(*Apps.values()))
                val nodes = root.getElementsByTagName("*")

                for (count in 0 until nodes.length) {
                    val node = nodes.item(count)
                    val nn = node.nodeName
                    val nodeIndex = Tuils.find(nn, enums as List<Any>)
                    if (nodeIndex != -1) {
                        if (nn.startsWith("d")) {
                            prefsList!!.add(nn, node.attributes.getNamedItem(VALUE_ATTRIBUTE).nodeValue)
                        } else {
                            prefsList!!.add(nn, XMLPrefsManager.getStringAttribute(node as Element, VALUE_ATTRIBUTE))
                        }

                        for (en in enums.indices) {
                            if (enums[en].label() == nn) {
                                enums.removeAt(en)
                                break
                            }
                        }
                    } else {
                        if (node.nodeType == Node.ELEMENT_NODE) {
                            val e = node as Element

                            if (e.hasAttribute(APPS_ATTRIBUTE)) {
                                val name = e.nodeName
                                if (name.contains(Tuils.SPACE)) {
                                    Tuils.sendOutput(Color.RED, context, PATH + ": " + context.getString(R.string.output_groupspace) + ": " + name)
                                    continue
                                }

                                object : StoppableThread() {
                                    override fun run() {
                                        super.run()
                                        val g = Group(name)
                                        val appsStr = e.getAttribute(APPS_ATTRIBUTE)
                                        val split = appsStr.split(APPS_SEPARATOR.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                                        val asList: MutableList<LaunchInfo> = ArrayList(allApps)

                                        external@ for (s in split) {
                                            for (c in asList.indices) {
                                                if (asList[c].isApp(s)) {
                                                    g.add(asList.removeAt(c), false)
                                                    continue@external
                                                }
                                            }
                                        }

                                        g.sort()

                                        if (e.hasAttribute(BGCOLOR_ATTRIBUTE)) {
                                            val c = e.getAttribute(BGCOLOR_ATTRIBUTE)
                                            if (c.isNotEmpty()) {
                                                try {
                                                    g.setBgColor(Color.parseColor(c))
                                                } catch (e2: Exception) {
                                                    Tuils.sendOutput(Color.RED, context, PATH + ": " + context.getString(R.string.output_invalidcolor) + ": " + c)
                                                }
                                            }
                                        }

                                        if (e.hasAttribute(FORECOLOR_ATTRIBUTE)) {
                                            val c = e.getAttribute(FORECOLOR_ATTRIBUTE)
                                            if (c.isNotEmpty()) {
                                                try {
                                                    g.setForeColor(Color.parseColor(c))
                                                } catch (e2: Exception) {
                                                    Tuils.sendOutput(Color.RED, context, PATH + ": " + context.getString(R.string.output_invalidcolor) + ": " + c)
                                                }
                                            }
                                        }

                                        groups.add(g)
                                    }
                                }.start()
                            } else {
                                val shown = !e.hasAttribute(SHOW_ATTRIBUTE) || e.getAttribute(SHOW_ATTRIBUTE).toBoolean()
                                if (!shown) {
                                    var name: ComponentName? = null
                                    val split = nn.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                                    if (split.size >= 2) {
                                        name = ComponentName(split[0], split[1])
                                    } else if (split.size == 1) {
                                        if (split[0].contains("Activity")) {
                                            for (i in allApps) {
                                                if (i.componentName.className == split[0]) name = i.componentName
                                            }
                                        } else {
                                            for (i in allApps) {
                                                if (i.componentName.packageName == split[0]) name = i.componentName
                                            }
                                        }
                                    }

                                    if (name == null) continue

                                    val removed = AppUtils.findLaunchInfoWithComponent(allApps, name)
                                    if (removed != null) {
                                        allApps.remove(removed)
                                        hiddenApps.add(removed)
                                    }
                                }
                            }
                        }
                    }
                }

                if (enums.size > 0) {
                    for (s in enums) {
                        val value = s.defaultValue()
                        val em = d.createElement(s.label())
                        em.setAttribute(VALUE_ATTRIBUTE, value)
                        root.appendChild(em)
                        prefsList!!.add(s.label(), value)
                    }
                    writeTo(d, file!!)
                }
            } else {
                Tuils.sendOutput(Color.RED, context, R.string.tuinotfound_app)
            }

            for ((key, value1) in preferences.all) {
                if (value1 is Int) {
                    var name: ComponentName? = null
                    val split = key.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    if (split.size >= 2) {
                        name = ComponentName(split[0], split[1])
                    } else if (split.size == 1) {
                        if (split[0].contains("Activity")) {
                            for (i in allApps) {
                                if (i.componentName.className == split[0]) name = i.componentName
                            }
                        } else {
                            for (i in allApps) {
                                if (i.componentName.packageName == split[0]) name = i.componentName
                            }
                        }
                    }

                    if (name == null) continue

                    val info = AppUtils.findLaunchInfoWithComponent(allApps, name)
                    if (info != null) info.launchedTimes = value1
                }
            }

        } catch (e1: Exception) {
            Tuils.toFile(e1)
        }

        appsHolder = AppsHolder(allApps, prefsList!!)
        AppUtils.checkEquality(hiddenApps)

        Group.sorting = XMLPrefsManager.getInt(Apps.app_groups_sorting)
        for (g in groups) g.sort()
        Collections.sort(groups) { o1, o2 -> Tuils.alphabeticCompare(o1.name(), o2.name()) }
    }

    private fun createAppMap(mgr: PackageManager): MutableList<LaunchInfo> {
        val infos: MutableList<LaunchInfo> = ArrayList()
        val i = Intent(Intent.ACTION_MAIN)
        i.addCategory(Intent.CATEGORY_LAUNCHER)

        val main: List<ResolveInfo> = try {
            mgr.queryIntentActivities(i, 0)
        } catch (e: Exception) {
            return infos
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1 && Tuils.isMyLauncherDefault(context.packageManager)) {
            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            for (ri in main) {
                val li = LaunchInfo(ri.activityInfo.packageName, ri.activityInfo.name, ri.loadLabel(mgr).toString())
                try {
                    val pi = mgr.getPackageInfo(li.componentName.packageName, 0)
                    li.lastUpdateTime = pi.firstInstallTime
                } catch (e: Exception) {}

                try {
                    val query = LauncherApps.ShortcutQuery()
                    query.setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC)
                    query.setPackage(li.componentName.packageName)
                    li.shortcuts = launcherApps.getShortcuts(query, Process.myUserHandle())
                } catch (e: Throwable) {
                    Tuils.log(e)
                }
                infos.add(li)
            }
        } else {
            for (ri in main) {
                val li = LaunchInfo(ri.activityInfo.packageName, ri.activityInfo.name, ri.loadLabel(mgr).toString())
                try {
                    val pi = mgr.getPackageInfo(li.componentName.packageName, 0)
                    li.lastUpdateTime = pi.firstInstallTime
                } catch (e: Exception) {}
                infos.add(li)
            }
        }

        return infos
    }

    private fun appInstalled(packageName: String) {
        try {
            val manager = context.packageManager
            val packageInfo = manager.getPackageInfo(packageName, 0)

            if (appInstalledFormat != null) {
                var cp = appInstalledFormat!!
                cp = pp!!.matcher(cp).replaceAll(packageName)
                if (packageInfo != null) {
                    val sequence = packageInfo.applicationInfo?.loadLabel(manager)
                    if (sequence != null) cp = pl!!.matcher(cp).replaceAll(sequence.toString())
                } else {
                    val index = packageName.lastIndexOf(Tuils.DOT)
                    cp = if (index == -1) pl!!.matcher(cp).replaceAll(Tuils.EMPTYSTRING) else {
                        pl!!.matcher(cp).replaceAll(packageName.substring(index + 1))
                    }
                }
                cp = Tuils.patternNewline.matcher(cp).replaceAll(Tuils.NEWLINE)
                Tuils.sendOutput(appInstalledColor, context, cp)
            }

            val intent = manager.getLaunchIntentForPackage(packageName) ?: return
            val name = intent.component!!
            val activity = name.className
            val label = manager.getActivityInfo(name, 0).loadLabel(manager).toString()

            val app = LaunchInfo(packageName, activity, label)
            try {
                val pi = manager.getPackageInfo(packageName, 0)
                app.lastUpdateTime = pi.firstInstallTime
            } catch (e: Exception) {}

            appsHolder!!.add(app)
        } catch (e: Exception) {}
    }

    private fun appUninstalled(packageName: String) {
        if (appsHolder == null) return
        val infos = AppUtils.findLaunchInfosWithPackage(packageName, appsHolder!!.getApps())

        if (appUninstalledFormat != null) {
            var cp = appUninstalledFormat!!
            cp = pp!!.matcher(cp).replaceAll(packageName)
            if (infos.isNotEmpty()) {
                cp = pl!!.matcher(cp).replaceAll(infos[0].publicLabel ?: "")
            } else {
                val index = packageName.lastIndexOf(Tuils.DOT)
                cp = if (index == -1) pl!!.matcher(cp).replaceAll(Tuils.EMPTYSTRING) else {
                    pl!!.matcher(cp).replaceAll(packageName.substring(index + 1))
                }
            }
            cp = Tuils.patternNewline.matcher(cp).replaceAll(Tuils.NEWLINE)
            Tuils.sendOutput(appUninstalledColor, context, cp)
        }

        for (i in infos) appsHolder!!.remove(i)
    }

    fun findLaunchInfoWithLabel(label: String, type: Int): LaunchInfo? {
        if (appsHolder == null) return null

        val appList = if (type == SHOWN_APPS) {
            appsHolder!!.getApps()
        } else {
            hiddenApps
        }

        val i = AppUtils.findLaunchInfoWithLabel(appList, label)
        if (i != null) return i

        val isList = AppUtils.findLaunchInfosWithPackage(label, appList)
        return if (isList.isEmpty()) null else isList[0]
    }

    fun writeLaunchTimes(info: LaunchInfo) {
        editor.putInt(info.write(), info.launchedTimes)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            editor.apply()
        } else {
            editor.commit()
        }

        appsHolder?.update(true)
    }

    fun getIntent(info: LaunchInfo): Intent {
        info.launchedTimes++
        object : StoppableThread() {
            override fun run() {
                super.run()
                appsHolder!!.requestSuggestionUpdate(info)
                writeLaunchTimes(info)
            }
        }.start()

        return Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(info.componentName)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
    }

    fun hideActivity(info: LaunchInfo): String? {
        set(file!!, info.write(), arrayOf(SHOW_ATTRIBUTE), arrayOf(false.toString() + Tuils.EMPTYSTRING))

        appsHolder!!.remove(info)
        appsHolder!!.update(true)
        hiddenApps.add(info)
        AppUtils.checkEquality(hiddenApps)

        return info.publicLabel
    }

    fun showActivity(info: LaunchInfo): String? {
        set(file!!, info.write(), arrayOf(SHOW_ATTRIBUTE), arrayOf(true.toString() + Tuils.EMPTYSTRING))

        hiddenApps.remove(info)
        appsHolder!!.add(info)
        appsHolder!!.update(false)

        return info.publicLabel
    }

    fun createGroup(name: String): String? {
        val index = Tuils.find(name, groups as List<Any>)
        if (index == -1) {
            groups.add(Group(name))
            return set(file!!, name, arrayOf(APPS_ATTRIBUTE), arrayOf(Tuils.EMPTYSTRING))
        }

        return context.getString(R.string.output_groupexists)
    }

    fun groupBgColor(name: String, color: String): String? {
        val index = Tuils.find(name, groups as List<Any>)
        if (index == -1) {
            return context.getString(R.string.output_groupnotfound)
        }

        groups[index].setBgColor(Color.parseColor(color))
        return set(file!!, name, arrayOf(BGCOLOR_ATTRIBUTE), arrayOf(color))
    }

    fun groupForeColor(name: String, color: String): String? {
        val index = Tuils.find(name, groups as List<Any>)
        if (index == -1) {
            return context.getString(R.string.output_groupnotfound)
        }

        groups[index].setForeColor(Color.parseColor(color))
        return set(file!!, name, arrayOf(FORECOLOR_ATTRIBUTE), arrayOf(color))
    }

    fun removeGroup(name: String): String? {
        val output = XMLPrefsManager.removeNode(file!!, name)

        if (output == null) return null
        if (output.isEmpty()) return context.getString(R.string.output_groupnotfound)

        val index = Tuils.find(name, groups as List<Any>)
        if (index != -1) groups.removeAt(index)

        return output
    }

    fun addAppToGroup(group: String, app: LaunchInfo): String? {
        val o: Array<Any?>?
        try {
            o = XMLPrefsManager.buildDocument(file!!, null)
            if (o == null) {
                Tuils.sendXMLParseError(context, PATH)
                return null
            }
        } catch (e: Exception) {
            return e.toString()
        }

        val d = o[0] as Document
        val root = o[1] as Element

        val node = XMLPrefsManager.findNode(root, group) ?: return context.getString(R.string.output_groupnotfound)

        val e = node as Element
        var appsStr = e.getAttribute(APPS_ATTRIBUTE)

        if (appsStr != null && app.isInside(appsStr)) return null

        appsStr = appsStr + APPS_SEPARATOR + app.write()
        if (appsStr.startsWith(APPS_SEPARATOR)) appsStr = appsStr.substring(1)

        e.setAttribute(APPS_ATTRIBUTE, appsStr)

        writeTo(d, file!!)

        val index = Tuils.find(group, groups as List<Any>)
        if (index != -1) groups[index].add(app, true)

        return null
    }

    fun removeAppFromGroup(group: String, app: LaunchInfo): String? {
        val o: Array<Any?>?
        try {
            o = XMLPrefsManager.buildDocument(file!!, null)
            if (o == null) {
                Tuils.sendXMLParseError(context, PATH)
                return null
            }
        } catch (e: Exception) {
            return e.toString()
        }

        val d = o[0] as Document
        val root = o[1] as Element

        val node = XMLPrefsManager.findNode(root, group) ?: return context.getString(R.string.output_groupnotfound)

        val e = node as Element

        var appsStr = e.getAttribute(APPS_ATTRIBUTE) ?: return null

        if (!app.isInside(appsStr)) return null

        val temp = appsStr.replace(app.write().toRegex(), Tuils.EMPTYSTRING)
        if (temp.length < appsStr.length) {
            appsStr = temp
            appsStr = appsStr.replace((APPS_SEPARATOR + APPS_SEPARATOR).toRegex(), APPS_SEPARATOR)
            if (appsStr.startsWith(APPS_SEPARATOR)) appsStr = appsStr.substring(1)
            if (appsStr.endsWith(APPS_SEPARATOR)) appsStr = appsStr.substring(0, appsStr.length - 1)

            e.setAttribute(APPS_ATTRIBUTE, appsStr)

            writeTo(d, file!!)

            val index = Tuils.find(group, groups as List<Any>)
            if (index != -1) groups[index].remove(app)
        }

        return null
    }

    fun listGroup(group: String): String? {
        val o: Array<Any?>?
        try {
            o = XMLPrefsManager.buildDocument(file!!, null)
            if (o == null) {
                Tuils.sendXMLParseError(context, PATH)
                return null
            }
        } catch (e: Exception) {
            return e.toString()
        }

        val root = o[1] as Element

        val node = XMLPrefsManager.findNode(root, group) ?: return context.getString(R.string.output_groupnotfound)

        val e = node as Element

        val appsStr = e.getAttribute(APPS_ATTRIBUTE) ?: return "[]"

        var labels = Tuils.EMPTYSTRING

        val manager = context.packageManager
        val split = appsStr.split(APPS_SEPARATOR.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (s in split) {
            if (s.isEmpty()) continue

            val label: String
            val name = LaunchInfo.componentInfo(s)
            if (name == null) {
                try {
                    label = manager.getApplicationInfo(s, 0).loadLabel(manager).toString()
                } catch (e1: Exception) {
                    continue
                }
            } else {
                try {
                    label = manager.getActivityInfo(name, 0).loadLabel(manager).toString()
                } catch (e1: Exception) {
                    continue
                }
            }

            labels = labels + Tuils.NEWLINE + label
        }

        return labels.trim { it <= ' ' }
    }

    fun listGroups(): String? {
        val o: Array<Any?>?
        try {
            o = XMLPrefsManager.buildDocument(file!!, null)
            if (o == null) {
                Tuils.sendXMLParseError(context, PATH)
                return null
            }
        } catch (e: Exception) {
            return e.toString()
        }

        val root = o[1] as Element

        var groupsStr = Tuils.EMPTYSTRING

        val list = root.getElementsByTagName("*")
        for (count in 0 until list.length) {
            val node = list.item(count)
            if (node !is Element) continue

            val e = node
            if (!e.hasAttribute(APPS_ATTRIBUTE)) continue

            groupsStr = groupsStr + Tuils.NEWLINE + e.nodeName
        }

        if (groupsStr.isEmpty()) return "[]"
        return groupsStr.trim { it <= ' ' }
    }

    fun shownApps(): List<LaunchInfo> {
        return appsHolder?.getApps() ?: ArrayList()
    }

    fun hiddenApps(): List<LaunchInfo> {
        return hiddenApps
    }

    fun getSuggestedApps(): Array<LaunchInfo> {
        return appsHolder?.getSuggestedApps() ?: emptyArray()
    }

    fun printApps(type: Int): String {
        return printNApps(type, -1)
    }

    fun printApps(type: Int, text: String): String {
        var ok: Boolean
        var length = 0
        try {
            length = text.toInt()
            ok = true
        } catch (exc: NumberFormatException) {
            ok = false
        }

        return if (ok) {
            printNApps(type, length)
        } else {
            printAppsThatBegins(type, text)
        }
    }

    private fun printNApps(type: Int, n: Int): String {
        return try {
            val labels = AppUtils.labelList(if (type == SHOWN_APPS) appsHolder!!.getApps() else hiddenApps, true)

            if (n >= 0) {
                val toRemove = labels.size - n
                if (toRemove <= 0) return "[]"

                for (c in 0 until toRemove) {
                    labels.removeAt(labels.size - 1)
                }
            }

            AppUtils.printApps(labels)
        } catch (e: NullPointerException) {
            "[]"
        }
    }

    private fun printAppsThatBegins(type: Int, with: String?): String {
        return try {
            val labels = AppUtils.labelList(if (type == SHOWN_APPS) appsHolder!!.getApps() else hiddenApps, true)

            if (with != null && with.isNotEmpty()) {
                val withLower = with.lowercase(Locale.getDefault())

                val it = labels.iterator()
                while (it.hasNext()) {
                    if (!it.next().lowercase(Locale.getDefault()).startsWith(withLower)) it.remove()
                }
            }

            AppUtils.printApps(labels)
        } catch (e: NullPointerException) {
            "[]"
        }
    }

    fun unregisterReceiver(context: Context) {
        context.unregisterReceiver(appsBroadcast)
    }

    fun onDestroy() {
        unregisterReceiver(context)
    }

    class Group(private val name: String) : MainManager.Group, StringableObject {

        private val lowercaseName: String = name.lowercase(Locale.getDefault())
        private val apps: MutableList<GroupLaunchInfo> = ArrayList()

        private var bgColor = Int.MAX_VALUE
        private var foreColor = Int.MAX_VALUE

        fun add(info: LaunchInfo, sort: Boolean) {
            apps.add(GroupLaunchInfo(info, apps.size))
            if (sort) sort()
        }

        fun remove(info: LaunchInfo) {
            val iterator = apps.iterator()
            while (iterator.hasNext()) {
                if (iterator.next().componentName == info.componentName) {
                    iterator.remove()
                    return
                }
            }
        }

        fun remove(app: String) {
            val iterator = apps.iterator()
            while (iterator.hasNext()) {
                if (iterator.next().componentName.packageName == app) {
                    iterator.remove()
                    return
                }
            }
        }

        fun sort() {
            Collections.sort(apps, comparator)
        }

        fun contains(info: LaunchInfo): Boolean {
            return apps.contains(info)
        }

        fun getBgColor(): Int = bgColor

        fun setBgColor(color: Int) {
            this.bgColor = color
        }

        fun getForeColor(): Int = foreColor

        fun setForeColor(foreColor: Int) {
            this.foreColor = foreColor
        }

        override fun members(): List<Any> = apps

        override fun use(mainPack: MainPack, input: String): Boolean {
            val info = AppUtils.findLaunchInfoWithLabel(apps, input) ?: return false

            info.launchedTimes++

            val intent = Intent(Intent.ACTION_MAIN)
            intent.component = info.componentName
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            Tuils.getContext(mainPack).startActivity(intent)

            return true
        }

        override fun name(): String = name

        override fun equals(other: Any?): Boolean {
            return if (other is Group) {
                name == other.name()
            } else if (other is String) {
                other == name
            } else {
                false
            }
        }

        override fun getLowercaseString(): String = lowercaseName

        override fun getString(): String = name()

        inner class GroupLaunchInfo(info: LaunchInfo, val initialIndex: Int) : LaunchInfo(info.componentName.packageName, info.componentName.className, info.publicLabel ?: "") {
            init {
                launchedTimes = info.launchedTimes
                unspacedLowercaseLabel = info.unspacedLowercaseLabel
            }
        }

        companion object {
            const val ALPHABETIC_UP_DOWN = 0
            const val ALPHABETIC_DOWN_UP = 1
            const val TIME_UP_DOWN = 2
            const val TIME_DOWN_UP = 3
            const val MOSTUSED_UP_DOWN = 4
            const val MOSTUSED_DOWN_UP = 5

            @JvmField
            var sorting = 0

            @JvmField
            val comparator = Comparator<GroupLaunchInfo> { o1, o2 ->
                when (sorting) {
                    ALPHABETIC_UP_DOWN -> Tuils.alphabeticCompare(o1.publicLabel ?: "", o2.publicLabel ?: "")
                    ALPHABETIC_DOWN_UP -> Tuils.alphabeticCompare(o2.publicLabel ?: "", o1.publicLabel ?: "")
                    TIME_UP_DOWN -> o1.initialIndex - o2.initialIndex
                    TIME_DOWN_UP -> o2.initialIndex - o1.initialIndex
                    MOSTUSED_UP_DOWN -> o2.launchedTimes - o1.launchedTimes
                    MOSTUSED_DOWN_UP -> o1.launchedTimes - o2.launchedTimes
                    else -> 0
                }
            }
        }

        override fun hashCode(): Int {
            return name.hashCode()
        }
    }

    open class LaunchInfo : Parcelable, StringableObject, Comparable<LaunchInfo> {
        @JvmField
        var componentName: ComponentName
        @JvmField
        var publicLabel: String? = null
        @JvmField
        var unspacedLowercaseLabel: String? = null
        @JvmField
        var lowercaseLabel: String? = null
        @JvmField
        var launchedTimes = 0
        @JvmField
        var lastUpdateTime: Long = 0
        @JvmField
        var shortcuts: List<ShortcutInfo>? = null

        constructor(packageName: String, activityName: String, label: String) {
            this.componentName = ComponentName(packageName, activityName)
            setLabel(label)
        }

        protected constructor(`in`: Parcel) {
            componentName = `in`.readParcelable(ComponentName::class.java.classLoader)!!
            setLabel(`in`.readString()!!)
            launchedTimes = `in`.readInt()
            lastUpdateTime = `in`.readLong()
        }

        fun setLabel(s: String) {
            this.publicLabel = s
            this.lowercaseLabel = s.lowercase(Locale.getDefault())
            this.unspacedLowercaseLabel = Tuils.removeSpaces(lowercaseLabel ?: "")
        }

        fun isInside(appsStr: String): Boolean {
            val split = appsStr.split(APPS_SEPARATOR.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            for (s in split) {
                if (isApp(s)) return true
            }
            return false
        }

        fun isApp(app: String): Boolean {
            val split2 = app.split(COMPONENT_SEPARATOR.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            return if (split2.size == 1) {
                componentName.packageName == split2[0]
            } else {
                componentName.packageName == split2[0] && componentName.className == split2[1]
            }
        }

        override fun equals(other: Any?): Boolean {
            if (other == null) return false
            if (other is LaunchInfo) {
                return try {
                    this.componentName == other.componentName
                } catch (e: Exception) {
                    false
                }
            } else if (other is ComponentName) {
                return this.componentName == other
            } else if (other is String) {
                return isApp(other) || this.componentName.className == other
            }
            return false
        }

        override fun toString(): String {
            return componentName.packageName + " - " + componentName.className + " --> " + publicLabel + ", n=" + launchedTimes
        }

        override fun getLowercaseString(): String? = lowercaseLabel

        override fun getString(): String? = publicLabel

        fun write(): String {
            return this.componentName.packageName + COMPONENT_SEPARATOR + this.componentName.className
        }

        override fun describeContents(): Int = 0

        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeParcelable(componentName, flags)
            dest.writeString(publicLabel)
            dest.writeInt(launchedTimes)
            dest.writeLong(lastUpdateTime)
        }



        override fun compareTo(other: LaunchInfo): Int {
            return other.launchedTimes - launchedTimes
        }

        override fun hashCode(): Int {
            return componentName.hashCode()
        }

        companion object {
            const val COMPONENT_SEPARATOR = "-"

            @JvmField
            val CREATOR: Parcelable.Creator<LaunchInfo> = object : Parcelable.Creator<LaunchInfo> {
                override fun createFromParcel(`in`: Parcel): LaunchInfo = LaunchInfo(`in`)
                override fun newArray(size: Int): Array<LaunchInfo?> = arrayOfNulls(size)
            }

            fun componentInfo(app: String): ComponentName? {
                val split2 = app.split(COMPONENT_SEPARATOR.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                return if (split2.size == 1) {
                    null
                } else {
                    ComponentName(split2[0], split2[1])
                }
            }
        }
    }

    private inner class AppsHolder(private val infos: MutableList<LaunchInfo>, private val values: XMLPrefsList) {
        val MOST_USED = 10
        val NULL = 11
        val USER_DEFINIED = 12

        private var suggestedAppMgr: SuggestedAppMgr? = null

        private val mostUsedComparator = Comparator<LaunchInfo> { lhs, rhs ->
            if (rhs.launchedTimes > lhs.launchedTimes) -1 else if (rhs.launchedTimes == lhs.launchedTimes) 0 else 1
        }

        init {
            update(true)
        }

        fun add(info: LaunchInfo) {
            if (!infos.contains(info)) {
                infos.add(info)
                update(false)
            }
        }

        fun remove(info: LaunchInfo) {
            infos.remove(info)
            update(true)
        }

        private fun sort() {
            try {
                Collections.sort(infos, mostUsedComparator)
            } catch (e: NullPointerException) {}
        }

        private fun fillSuggestions() {
            suggestedAppMgr = SuggestedAppMgr(values, getApps())
            for (info in infos) {
                suggestedAppMgr!!.attemptInsertSuggestion(info)
            }
        }

        fun requestSuggestionUpdate(info: LaunchInfo) {
            suggestedAppMgr!!.attemptInsertSuggestion(info)
        }

        fun update(refreshSuggestions: Boolean) {
            AppUtils.checkEquality(infos)
            sort()
            if (refreshSuggestions) {
                fillSuggestions()
            }
        }

        fun getApps(): MutableList<LaunchInfo> = infos

        fun getSuggestedApps(): Array<LaunchInfo> {
            val apps = suggestedAppMgr!!.apps()
            return apps.toTypedArray()
        }

        private inner class SuggestedAppMgr(values: XMLPrefsList, apps: List<LaunchInfo>) {
            private val suggested: MutableList<SuggestedApp> = ArrayList()
            private var lastWriteable = -1

            init {
                val PREFIX = "default_app_n"
                for (count in 0 until 5) {
                    val entry = values.get(Apps.valueOf(PREFIX + (count + 1)))
                    val vl = entry?.value ?: Apps.NULL

                    if (vl == Apps.NULL) continue
                    if (vl == Apps.MOST_USED) suggested.add(SuggestedApp(MOST_USED, count + 1)) else {
                        var name: ComponentName? = null
                        val split = vl.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        if (split.size >= 2) {
                            name = ComponentName(split[0], split[1])
                        } else if (split.size == 1) {
                            if (split[0].contains("Activity")) {
                                for (i in apps) {
                                    if (i.componentName.className == split[0]) name = i.componentName
                                }
                            } else {
                                for (i in apps) {
                                    if (i.componentName.packageName == split[0]) name = i.componentName
                                }
                            }
                        }

                        if (name == null) continue

                        val info = AppUtils.findLaunchInfoWithComponent(infos, name) ?: continue
                        suggested.add(SuggestedApp(info, USER_DEFINIED, count + 1))
                    }
                }
                sort()
            }

            fun size(): Int = suggested.size

            private fun sort() {
                Collections.sort(suggested)
                for (count in suggested.indices) {
                    if (suggested[count].type != MOST_USED) {
                        lastWriteable = count - 1
                        return
                    }
                }
                lastWriteable = suggested.size - 1
            }

            operator fun get(index: Int): SuggestedApp = suggested[index]

            fun set(index: Int, info: LaunchInfo) {
                suggested[index].change(info)
            }

            fun attemptInsertSuggestion(info: LaunchInfo) {
                if (info.launchedTimes == 0 || lastWriteable == -1) {
                    return
                }

                val index = Tuils.find(info, suggested as List<Any>)
                if (index == -1) {
                    for (count in 0..lastWriteable) {
                        val app = get(count)

                        if (app.app == null || info.launchedTimes > app.app!!.launchedTimes) {
                            val s = suggested[count]
                            val before = s.app
                            s.change(info)
                            if (before != null) {
                                attemptInsertSuggestion(before)
                            }
                            break
                        }
                    }
                }
                sort()
            }

            fun apps(): List<LaunchInfo> {
                val list: MutableList<LaunchInfo> = ArrayList()
                val cp: MutableList<SuggestedApp> = ArrayList(suggested)
                Collections.sort(cp) { o1, o2 -> o1.index - o2.index }

                for (count in cp.indices) {
                    val app = cp[count]
                    if (app.type != NULL && app.app != null) list.add(app.app!!)
                }
                return list
            }

            private inner class SuggestedApp : Comparable<Any> {
                var type: Int
                var app: LaunchInfo? = null
                var index: Int

                constructor(type: Int, index: Int) {
                    this.type = type
                    this.index = index
                }

                constructor(info: LaunchInfo?, type: Int, index: Int) {
                    this.app = info
                    this.type = type
                    this.index = index
                }

                fun change(info: LaunchInfo): SuggestedApp {
                    this.app = info
                    return this
                }

                override fun equals(other: Any?): Boolean {
                    return if (other is SuggestedApp) {
                        try {
                            (app == null && other.app == null) || (app != null && app!!.equals(other.app))
                        } catch (e: NullPointerException) {
                            false
                        }
                    } else if (other is LaunchInfo) {
                        if (app == null) false else app!!.equals(other)
                    } else {
                        false
                    }
                }

                override fun compareTo(other: Any): Int {
                    val otherApp = other as SuggestedApp

                    if (this.type == USER_DEFINIED || otherApp.type == USER_DEFINIED) {
                        if (this.type == USER_DEFINIED && otherApp.type == USER_DEFINIED) return otherApp.app!!.launchedTimes - this.app!!.launchedTimes
                        return if (this.type == USER_DEFINIED) 1 else -1
                    }

                    if (this.app == null || otherApp.app == null) {
                        if (this.app == null && otherApp.app == null) return 0
                        return if (this.app == null) 1 else -1
                    }
                    return this.app!!.launchedTimes - otherApp.app!!.launchedTimes
                }

                override fun hashCode(): Int {
                    var result = type
                    result = 31 * result + (app?.hashCode() ?: 0)
                    result = 31 * result + index
                    return result
                }
            }
        }
    }

    object AppUtils {
        @JvmStatic
        fun findLaunchInfoWithComponent(appList: List<LaunchInfo>, name: ComponentName?): LaunchInfo? {
            if (name == null) return null
            for (i in appList) {
                if (i.equals(name)) return i
            }
            return null
        }

        @JvmStatic
        fun findLaunchInfoWithLabel(appList: List<out LaunchInfo>, label: String?): LaunchInfo? {
            val cleanLabel = Tuils.removeSpaces(label ?: "")
            for (i in appList) if (i.unspacedLowercaseLabel.equals(cleanLabel, ignoreCase = true)) return i
            return null
        }

        @JvmStatic
        fun findLaunchInfosWithPackage(packageName: String, infos: List<LaunchInfo>): List<LaunchInfo> {
            val result: MutableList<LaunchInfo> = ArrayList()
            for (info in infos) if (info.componentName.packageName == packageName) result.add(info)
            return result
        }

        @JvmStatic
        fun checkEquality(list: MutableList<LaunchInfo>) {
            for (info in list) {
                if (info.publicLabel == null) {
                    continue
                }

                for (count in list.indices) {
                    val info2 = list[count]
                    if (info2.publicLabel == null) {
                        continue
                    }
                    if (info === info2) {
                        continue
                    }
                    if (info.unspacedLowercaseLabel == info2.unspacedLowercaseLabel) {
                        if (info.componentName.packageName == info2.componentName.packageName) {
                            info.setLabel(insertActivityName(info.publicLabel!!, info.componentName.className))
                            info2.setLabel(insertActivityName(info2.publicLabel!!, info2.componentName.className))
                        } else {
                            info2.setLabel(getNewLabel(info2.publicLabel!!, info2.componentName.packageName))
                        }
                    }
                }
            }
        }

        var activityPattern: Pattern = Pattern.compile("activity", Pattern.CASE_INSENSITIVE or Pattern.LITERAL)
        @JvmStatic
        fun insertActivityName(oldLabel: String, activityName: String): String {
            var name: String
            val lastDot = activityName.lastIndexOf(".")
            name = if (lastDot == -1) {
                activityName
            } else {
                activityName.substring(lastDot + 1)
            }
            name = activityPattern.matcher(name).replaceAll(Tuils.EMPTYSTRING)
            name = name.substring(0, 1).uppercase(Locale.getDefault()) + name.substring(1)
            return oldLabel + Tuils.SPACE + "-" + Tuils.SPACE + name
        }

        @JvmStatic
        fun getNewLabel(oldLabel: String, packageName: String): String {
            try {
                var firstDot = packageName.indexOf(Tuils.DOT)
                if (firstDot == -1) {
                    return packageName
                }
                firstDot++
                val secondDot = packageName.substring(firstDot).indexOf(Tuils.DOT)
                var prefix: String
                return if (secondDot == -1) {
                    prefix = packageName.substring(0, firstDot - 1)
                    prefix = prefix.substring(0, 1).uppercase(Locale.getDefault()) + prefix.substring(1).lowercase(Locale.getDefault())
                    prefix + Tuils.SPACE + oldLabel
                } else {
                    prefix = packageName.substring(firstDot, secondDot + firstDot)
                    prefix = prefix.substring(0, 1).uppercase(Locale.getDefault()) + prefix.substring(1).lowercase(Locale.getDefault())
                    prefix + Tuils.SPACE + oldLabel
                }
            } catch (e: Exception) {
                return packageName
            }
        }

        @JvmStatic
        fun format(app: LaunchInfo, info: PackageInfo): String {
            val builder = StringBuilder()
            builder.append(info.packageName).append(Tuils.NEWLINE)
            builder.append("vrs: ").append(info.versionCode).append(" - ").append(info.versionName).append(Tuils.NEWLINE).append(Tuils.NEWLINE)
            builder.append("launched_times: ").append(app.launchedTimes).append(Tuils.NEWLINE).append(Tuils.NEWLINE)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                builder.append("Install: ").append(TimeManager.instance?.replace(null, Int.MAX_VALUE, "%t0", info.firstInstallTime, -1) ?: "").append(Tuils.NEWLINE).append(Tuils.NEWLINE)
            }

            val a = info.activities
            if (a != null && a.isNotEmpty()) {
                val asList: MutableList<String> = ArrayList()
                for (activity in a) asList.add(activity.name.replace(info.packageName, Tuils.EMPTYSTRING))
                builder.append("Activities: ").append(Tuils.NEWLINE).append(Tuils.toPlanString(asList, Tuils.NEWLINE)).append(Tuils.NEWLINE).append(Tuils.NEWLINE)
            }

            val s = info.services
            if (s != null && s.isNotEmpty()) {
                val ss: MutableList<String> = ArrayList()
                for (service in s) ss.add(service.name.replace(info.packageName, Tuils.EMPTYSTRING))
                builder.append("Services: ").append(Tuils.NEWLINE).append(Tuils.toPlanString(ss, Tuils.NEWLINE)).append(Tuils.NEWLINE).append(Tuils.NEWLINE)
            }

            val r = info.receivers
            if (r != null && r.isNotEmpty()) {
                val rs: MutableList<String> = ArrayList()
                for (receiver in r) rs.add(receiver.name.replace(info.packageName, Tuils.EMPTYSTRING))
                builder.append("Receivers: ").append(Tuils.NEWLINE).append(Tuils.toPlanString(rs, Tuils.NEWLINE)).append(Tuils.NEWLINE).append(Tuils.NEWLINE)
            }

            val p = info.requestedPermissions
            if (p != null && p.isNotEmpty()) {
                val ps: MutableList<String> = ArrayList()
                for (perm in p) ps.add(perm.substring(perm.lastIndexOf(".") + 1))
                builder.append("Permissions: ").append(Tuils.NEWLINE).append(Tuils.toPlanString(ps, ", "))
            }

            return builder.toString()
        }

        @JvmStatic
        fun printApps(apps: List<String>): String {
            if (apps.isEmpty()) {
                return apps.toString()
            }
            val list: MutableList<String> = ArrayList(apps)
            Collections.sort(list) { s1, s2 -> Tuils.alphabeticCompare(s1, s2) }
            Tuils.addPrefix(list, Tuils.DOUBLE_SPACE)
            Tuils.insertHeaders(list, false)
            return Tuils.toPlanString(list)
        }

        @JvmStatic
        fun labelList(infos: List<LaunchInfo>, sort: Boolean): MutableList<String> {
            val labels: MutableList<String> = ArrayList()
            for (info in infos) {
                labels.add(info.publicLabel ?: "")
            }
            if (sort) labels.sort()
            return labels
        }
    }

    companion object {
        const val SHOWN_APPS = 10
        const val HIDDEN_APPS = 11
        const val PATH = "apps.xml"
        private const val APPS_SEPARATOR = ";"

        @JvmField
        var instance: XMLPrefsElement? = null
    }
}