package com.hereliesaz.hg2gui.commands

import android.annotation.SuppressLint
import android.graphics.Color
import com.hereliesaz.hg2gui.commands.main.MainPack
import com.hereliesaz.hg2gui.commands.main.Param
import com.hereliesaz.hg2gui.commands.main.specific.ParamCommand
import com.hereliesaz.hg2gui.managers.AppsManager
import com.hereliesaz.hg2gui.managers.ContactManager
import com.hereliesaz.hg2gui.managers.FileManager
import com.hereliesaz.hg2gui.managers.FileManager.DirInfo
import com.hereliesaz.hg2gui.managers.HTMLExtractManager
import com.hereliesaz.hg2gui.managers.RssManager
import com.hereliesaz.hg2gui.managers.music.MusicManager2
import com.hereliesaz.hg2gui.managers.notifications.NotificationManager
import com.hereliesaz.hg2gui.managers.xml.XMLPrefsManager
import com.hereliesaz.hg2gui.managers.xml.classes.XMLPrefsSave
import com.hereliesaz.hg2gui.managers.xml.options.Apps
import com.hereliesaz.hg2gui.managers.xml.options.Notifications
import com.hereliesaz.hg2gui.managers.xml.options.Rss
import com.hereliesaz.hg2gui.tuils.Tuils
import java.io.File
import java.util.*

@SuppressLint("DefaultLocale")
object CommandTuils {

    private val extensionFileFilter = FileManager.SpecificExtensionFileFilter()
    private val nameFileFilter = FileManager.SpecificNameFileFilter()

    @JvmField
    var xmlPrefsEntrys: MutableList<XMLPrefsSave>? = null
    @JvmField
    var xmlPrefsFiles: MutableList<String>? = null

    //	parse a command
    @Throws(Exception::class)
    fun parse(input: String, info: ExecutePack): Command? {
        var inputVar = input
        val command = Command()

        val name = findName(inputVar)
        if (!Tuils.isAlpha(name))
            return null

        val cmd = (info.commandGroup as CommandGroup).getCommandByName(name)
            ?: return null
        command.cmd = cmd

        inputVar = inputVar.substring(name.length)
        inputVar = inputVar.trim { it <= ' ' }

        val args = ArrayList<Any?>()
        var nArgs = 0
        val types: IntArray?

        try {
            if (cmd is ParamCommand) {
                val arg = param(info as MainPack, cmd, inputVar)
                if (arg == null || !arg.found) {

                    command.indexNotFound = 0
                    args.add(inputVar)
                    command.nArgs = 1
                    command.mArgs = args.toArray()
                    return command
                }

                inputVar = arg.residualString ?: ""
                val p = arg.arg as Param
                types = p.args()

                nArgs++
                args.add(p)
            } else {
                types = cmd.argType()
            }

            if (types != null) {
                for (type in types) {
                    inputVar = inputVar.trim { it <= ' ' }
                    if (inputVar.isEmpty()) {
                        break
                    }

                    val arg = getArg(info, inputVar, type) ?: return null

                    if (!arg.found) {
                        command.indexNotFound = if (cmd is ParamCommand) args.size else args.size
                        // Original Java: command.indexNotFound = cmd instanceof ParamCommand ? count + 1 : count;
                        // wait, count + 1 if ParamCommand because Param is already in args.
                        // args.size would be 1 if Param was added.
                        // So if ParamCommand, args already has 1 element. count 0 -> index 1.
                        // if not ParamCommand, args has 0 elements. count 0 -> index 0.
                        // So args.size seems correct if I use the state of args list.
                        
                        args.add(inputVar)
                        command.mArgs = args.toArray()
                        command.nArgs = nArgs
                        return command
                    }

                    nArgs += arg.n
                    args.add(arg.arg)
                    inputVar = arg.residualString ?: ""
                }
            }
        } catch (e: Exception) {
            Tuils.log(e)
        }

        command.mArgs = args.toArray()
        command.nArgs = nArgs

        return command
    }

    //	find command name
    private fun findName(input: String): String {
        val space = input.indexOf(Tuils.SPACE)

        return if (space == -1) {
            input
        } else {
            input.substring(0, space)
        }
    }

    //	find args
    fun getArg(info: ExecutePack, input: String, type: Int): ArgInfo? {
        return when {
            type == CommandAbstraction.FILE && info is MainPack -> file(input, info.currentDirectory)
            type == CommandAbstraction.CONTACTNUMBER && info is MainPack -> contactNumber(input, info.contacts)
            type == CommandAbstraction.PLAIN_TEXT -> plainText(input)
            type == CommandAbstraction.VISIBLE_PACKAGE && info is MainPack -> activityName(input, info.appsManager)
            type == CommandAbstraction.HIDDEN_PACKAGE && info is MainPack -> hiddenPackage(input, info.appsManager)
            type == CommandAbstraction.TEXTLIST -> textList(input)
            type == CommandAbstraction.SONG && info is MainPack -> {
                val player = info.player ?: return null
                song(input, player)
            }
            type == CommandAbstraction.COMMAND -> command(input, info.commandGroup as CommandGroup)
            type == CommandAbstraction.BOOLEAN -> bln(input)
            type == CommandAbstraction.COLOR -> color(input)
            type == CommandAbstraction.CONFIG_ENTRY -> configEntry(input)
            type == CommandAbstraction.CONFIG_FILE -> configFile(input)
            type == CommandAbstraction.INT -> integer(input)
            type == CommandAbstraction.DEFAULT_APP -> defaultApp(input, (info as MainPack).appsManager)
            type == CommandAbstraction.ALL_PACKAGES -> allPackages(input, (info as MainPack).appsManager)
            type == CommandAbstraction.NO_SPACE_STRING || type == CommandAbstraction.APP_GROUP || type == CommandAbstraction.BOUND_REPLY_APP -> noSpaceString(input)
            type == CommandAbstraction.APP_INSIDE_GROUP -> activityName(input, (info as MainPack).appsManager)
            type == CommandAbstraction.LONG -> numberLong(input)
            type == CommandAbstraction.DATASTORE_PATH_TYPE -> dataStoreType(input)
            else -> null
        }
    }

//	args extractors {

    private fun dataStoreType(input: String): ArgInfo {
        val a = noSpaceString(input)
        if (a.found) {
            val s = a.arg as String
            try {
                HTMLExtractManager.StoreableValue.Type.valueOf(s)
                return a
            } catch (_: Exception) {
                return ArgInfo(null, input, false, 0)
            }
        }

        a.found = false
        return a
    }

    private fun numberLong(input: String): ArgInfo {
        val split = input.split(Tuils.SPACE.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

        return try {
            val l = split[0].toLong()

            val builder = StringBuilder()
            for (c in 1 until split.size) {
                builder.append(split[c]).append(Tuils.SPACE)
            }
            ArgInfo(l, builder.toString().trim { it <= ' ' }, true, 1)
        } catch (_: Exception) {
            ArgInfo(null, input, false, 0)
        }
    }

    private fun color(input: String): ArgInfo {
        val inputVar = input.trim { it <= ' ' }

        val space = inputVar.indexOf(Tuils.SPACE)
        val cl = inputVar.substring(0, if (space == -1) inputVar.length else space)
        val residual = if (space == -1) Tuils.EMPTYSTRING else inputVar.substring(space + 1)

        return try {
            Color.parseColor(cl)
            ArgInfo(cl, residual, true, 1)
        } catch (e: Exception) {
            ArgInfo(null, residual, false, 0)
        }
    }

    private fun bln(input: String): ArgInfo {
        val used: String
        val notUsed: String?
        if (input.contains(Tuils.SPACE)) {
            val space = input.indexOf(Tuils.SPACE)
            used = input.substring(0, space)
            notUsed = input.substring(space + 1)
        } else {
            used = input
            notUsed = null
        }

        val result = used.toBoolean()
        return ArgInfo(result, notUsed, used.isNotEmpty(), if (used.isNotEmpty()) 1 else 0)
    }

    private fun plainText(input: String): ArgInfo {
        return ArgInfo(input, "", true, 1)
    }

    private fun textList(input: String?): ArgInfo? {
        if (input == null) {
            return null
        }

        val strings = input.split((Tuils.SPACE + "+").toRegex()).dropLastWhile { it.isEmpty() }
        val arg = ArrayList(strings)

        return ArgInfo(arg, null, true, arg.size)
    }

    private fun noSpaceString(input: String?): ArgInfo {
        if (input == null) return ArgInfo(null, null, false, 0)

        var index = input.indexOf(Tuils.SPACE)
        if (index == -1) index = input.length

        return ArgInfo(input.substring(0, index), if (input.length > index) input.substring(index + 1) else null, true, 1)
    }

    private fun command(string: String, active: CommandGroup): ArgInfo {
        val i = noSpaceString(string)
        val cmdName = i.arg as String

        var abstraction: CommandAbstraction? = null
        try {
            abstraction = active.getCommandByName(cmdName)
        } catch (e: Exception) {
            Tuils.log(e)
            Tuils.toFile(e)
        }

        return ArgInfo(abstraction, i.residualString, abstraction != null, 1)
    }

    @Suppress("UNCHECKED_CAST")
    private fun file(input: String, cd: File?): ArgInfo {
        val inputVar = input.trim { it <= ' ' }
        if ((inputVar.startsWith("\"") || inputVar.startsWith("'")) && (inputVar.substring(1).contains("\"") || inputVar.substring(1).contains("'"))) {
            val afterFirst = inputVar.substring(1)

            var endIndex = afterFirst.indexOf("\"")
            if (endIndex == -1) endIndex = afterFirst.indexOf("'")

            if (endIndex != -1) {
                val file = afterFirst.substring(0, endIndex)
                val residual = afterFirst.substring(endIndex + 1)

                val f: File = if (file.startsWith("/")) /*absolute*/ File(file)
                else if (cd != null) File(cd, file)
                else File(file)

                return ArgInfo(f, residual, true, 1)
            }
        }

        val textListInfo = textList(inputVar) ?: return ArgInfo(null, inputVar, false, 0)
        val strings = textListInfo.arg as MutableList<String>

        var toVerify = Tuils.EMPTYSTRING
        while (strings.isNotEmpty()) {
            val s = strings.removeAt(0)
            toVerify = if (toVerify.isEmpty()) s else toVerify + Tuils.SPACE + s

            val info = getFile(toVerify, cd)
            if (info.file != null && info.notFound == null) {
                val residual = Tuils.toPlanString(strings, Tuils.SPACE)
                return ArgInfo(info.file, residual, true, 1)
            }
        }

        return ArgInfo(null, inputVar, false, 0)
    }

    private fun getFile(path: String, cd: File?): DirInfo {
        return FileManager.cd(cd, path)
    }

    private fun attemptWildcard(dir: DirInfo): List<File>? {
        val files: List<File>?

        val info = FileManager.wildcard(dir.notFound) ?: return null

        val cd = dir.file ?: return null
        if (!cd.isDirectory) {
            return null
        }

        if (info.allExtensions && info.allNames) {
            files = cd.listFiles()?.toList()
        } else if (info.allNames) {
            extensionFileFilter.setExtension(info.extension)
            files = cd.listFiles(extensionFileFilter)?.toList()
        } else if (info.allExtensions) {
            nameFileFilter.setName(info.name)
            files = cd.listFiles(nameFileFilter)?.toList()
        } else {
            return null
        }

        return if (files != null && files.isNotEmpty()) {
            files
        } else {
            null
        }
    }

    private fun param(pack: MainPack, cmd: ParamCommand, input: String?): ArgInfo? {
        if (input == null || input.trim { it <= ' ' }.isEmpty()) return null

        var indexOfFirstSpace = input.indexOf(Tuils.SPACE)
        if (indexOfFirstSpace == -1) {
            indexOfFirstSpace = input.length
        }

        var param = input.substring(0, indexOfFirstSpace).trim { it <= ' ' }
        if (!param.startsWith("-")) param = "-$param"

        val sm = cmd.getParam(pack, param)
        val p = sm.value
        val df = sm.key

        return ArgInfo(p, if (df) input else input.substring(indexOfFirstSpace), p != null, if (p != null) 1 else 0)
    }

    private fun activityName(input: String, apps: AppsManager): ArgInfo {
        val info = apps.findLaunchInfoWithLabel(input, AppsManager.SHOWN_APPS)
        return ArgInfo(info, null, info != null, if (info != null) 1 else 0)
    }

    private fun hiddenPackage(input: String, apps: AppsManager): ArgInfo {
        val info = apps.findLaunchInfoWithLabel(input, AppsManager.HIDDEN_APPS)
        return ArgInfo(info, null, info != null, if (info != null) 1 else 0)
    }

    private fun allPackages(input: String, apps: AppsManager): ArgInfo {
        var info = apps.findLaunchInfoWithLabel(input, AppsManager.SHOWN_APPS)
        if (info == null) {
            info = apps.findLaunchInfoWithLabel(input, AppsManager.HIDDEN_APPS)
        }

        return ArgInfo(info, null, info != null, if (info != null) 1 else 0)
    }

    private fun defaultApp(input: String, apps: AppsManager): ArgInfo {
        val info = apps.findLaunchInfoWithLabel(input, AppsManager.SHOWN_APPS)
        return if (info == null) {
            ArgInfo(input, null, true, 1)
        } else {
            ArgInfo(info, null, true, 1)
        }
    }

    private fun contactNumber(input: String, contacts: ContactManager?): ArgInfo {
        val number: String? = if (Tuils.isPhoneNumber(input))
            input
        else
            contacts?.findNumber(input)

        return ArgInfo(number, null, number != null, 1)
    }

    private fun song(input: String, music: MusicManager2): ArgInfo {
        return ArgInfo(input, null, true, 1)
    }

    private fun configEntry(input: String): ArgInfo {
        val index = input.indexOf(Tuils.SPACE)

        if (xmlPrefsEntrys == null) {
            xmlPrefsEntrys = ArrayList()

            for (element in XMLPrefsManager.XMLPrefsRoot.entries) {
                xmlPrefsEntrys!!.addAll(element.enums)
            }
            Collections.addAll(xmlPrefsEntrys!!, *Apps.entries.toTypedArray())
            Collections.addAll(xmlPrefsEntrys!!, *Notifications.entries.toTypedArray())
            Collections.addAll(xmlPrefsEntrys!!, *Rss.entries.toTypedArray())
        }

        val candidate = if (index == -1) input else input.substring(0, index)
        for (xs in xmlPrefsEntrys!!) {
            if (xs.label() == candidate) {
                return ArgInfo(xs, if (index == -1) null else input.substring(index + 1), true, 1)
            }
        }
        return ArgInfo(null, input, false, 0)
    }

    private fun configFile(input: String): ArgInfo {
        if (xmlPrefsFiles == null) {
            xmlPrefsFiles = ArrayList()
            for (element in XMLPrefsManager.XMLPrefsRoot.entries)
                xmlPrefsFiles!!.add(element.path)
            xmlPrefsFiles!!.add(AppsManager.PATH)
            xmlPrefsFiles!!.add(NotificationManager.PATH)
            xmlPrefsFiles!!.add(RssManager.PATH)
        }

        for (xs in xmlPrefsFiles!!) {
            if (xs.equals(input, ignoreCase = true)) return ArgInfo(xs, null, true, 1)
        }
        return ArgInfo(null, input, false, 0)
    }

    private fun integer(input: String): ArgInfo {
        val n: Int
        val s: String

        val index = input.indexOf(Tuils.SPACE)
        if (index == -1) s = input
        else s = input.substring(0, index)

        try {
            n = s.toInt()
        } catch (e: NumberFormatException) {
            return ArgInfo(null, input, false, 0)
        }

        return ArgInfo(n, if (index == -1) null else input.substring(index + 1), true, 1)
    }

    fun isSuRequest(input: String): Boolean {
        return input == "su"
    }

    fun isSuCommand(input: String): Boolean {
        return input.startsWith("su ")
    }

    class ArgInfo(var arg: Any?, var residualString: String?, var found: Boolean, var n: Int)

}
