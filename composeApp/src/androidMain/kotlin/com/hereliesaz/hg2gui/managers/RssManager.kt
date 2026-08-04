package com.hereliesaz.hg2gui.managers

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.ConnectivityManager
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.hereliesaz.hg2gui.MainManager
import com.hereliesaz.hg2gui.R
import com.hereliesaz.hg2gui.managers.xml.XMLPrefsManager
import com.hereliesaz.hg2gui.managers.xml.classes.XMLPrefsElement
import com.hereliesaz.hg2gui.managers.xml.classes.XMLPrefsList
import com.hereliesaz.hg2gui.managers.xml.classes.XMLPrefsSave
import com.hereliesaz.hg2gui.tuils.StoppableThread
import com.hereliesaz.hg2gui.tuils.Tuils
import com.hereliesaz.hg2gui.tuils.html_escape.HtmlEscape
import okhttp3.OkHttpClient
import okhttp3.Request
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.xml.sax.SAXParseException
import java.io.BufferedInputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern
import javax.xml.parsers.DocumentBuilderFactory

class RssManager(private val context: Context, private val client: OkHttpClient) : XMLPrefsElement {

    private val RSS_CHECK_DELAY = 5000
    private val RSS_FOLDER = "rss"

    private val defaultRSSDateFormat = SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.US)

    private var defaultColor: Int = 0
    private var downloadMessageColor: Int = 0
    private var defaultFormat: String? = null
    private var timeFormat: String? = null
    private var downloadFormat: String? = null

    private var includeRssDefault: Boolean = false
    private var showDownloadMessage: Boolean = false
    private var click: Boolean = false

    private val handler: Handler = Handler(Looper.getMainLooper())
    private val root: File = File(Tuils.getFolder(), RSS_FOLDER)
    private val rssIndexFile: File = File(Tuils.getFolder(), PATH)

    private val feeds: MutableList<Rss> = ArrayList()
    private val formats: MutableList<XMLPrefsManager.IdValue> = ArrayList()
    private val cmdRegexes: MutableList<CmdableRegex> = ArrayList()

    private var hideTagPatterns: Array<Pattern>? = null
    private var urlPattern: Pattern? = null
    private var idPattern: Pattern? = null
    private var bPattern: Pattern? = null
    private var kbPattern: Pattern? = null
    private var mbPattern: Pattern? = null
    private var gbPattern: Pattern? = null

    private val connectivityManager: ConnectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    init {
        instance = this
        prepare()
        refresh()
    }

    override fun delete(): Array<String>? = null

    override fun getValues(): XMLPrefsList = values

    override fun write(save: XMLPrefsSave, value: String) {
        XMLPrefsManager.set(File(Tuils.getFolder(), PATH), save.label(), arrayOf(XMLPrefsManager.VALUE_ATTRIBUTE), arrayOf(value))
    }

    override fun path(): String = PATH

    fun refresh() {
        handler.removeCallbacksAndMessages(null)
        feeds.clear()
        formats.clear()
        cmdRegexes.clear()

        object : StoppableThread() {
            override fun run() {
                super.run()
                val o = try {
                    XMLPrefsManager.buildDocument(rssIndexFile, NAME)
                } catch (e: SAXParseException) {
                    Tuils.sendXMLParseError(context, PATH, e)
                    return
                } catch (e: Exception) {
                    Tuils.log(e)
                    return
                } ?: run {
                    Tuils.sendOutput(context, "Error loading RSS config")
                    return
                }

                try {
                    val document = o[0] as Document
                    val rootElement = o[1] as Element
                    val nodes = rootElement.getElementsByTagName("*")
                    val enums = ArrayList(listOf(*com.hereliesaz.hg2gui.managers.xml.options.Rss.values()))
                    val deleted = delete()
                    var needToWrite = false

                    for (count in 0 until nodes.length) {
                        val node = nodes.item(count)
                        val nn = node.nodeName
                        val enumIndex = enums.indexOfFirst { it.label() == nn }
                        if (enumIndex != -1) {
                            values.add(nn, node.attributes.getNamedItem(XMLPrefsManager.VALUE_ATTRIBUTE).nodeValue)
                            enums.removeAt(enumIndex)
                        } else {
                            val delIndex = deleted?.let { Tuils.find(nn, it as Array<out Any?>) } ?: -1
                            if (delIndex != -1) {
                                deleted!![delIndex] = null!! // Java behavior emulation
                                rootElement.removeChild(node)
                                needToWrite = true
                            } else {
                                when (nn) {
                                    RSS_LABEL -> feeds.add(Rss.fromElement(node as Element))
                                    FORMAT_LABEL -> {
                                        val e = node as Element
                                        val id = e.getAttribute(ID_ATTRIBUTE).toIntOrNull() ?: -1
                                        if (id != -1) {
                                            val format = XMLPrefsManager.getStringAttribute(e, XMLPrefsManager.VALUE_ATTRIBUTE) ?: ""
                                            formats.add(XMLPrefsManager.IdValue(format, id))
                                        }
                                    }
                                    REGEX_CMD_LABEL -> {
                                        val e = node as Element
                                        val id = e.getAttribute(ID_ATTRIBUTE).toIntOrNull() ?: continue
                                        val regex = XMLPrefsManager.getStringAttribute(e, XMLPrefsManager.VALUE_ATTRIBUTE) ?: continue
                                        val on = XMLPrefsManager.getStringAttribute(e, ON_ATTRIBUTE) ?: continue
                                        val cmd = XMLPrefsManager.getStringAttribute(e, CMD_ATTRIBUTE) ?: continue
                                        cmdRegexes.add(CmdableRegex(id, on, regex, cmd))
                                    }
                                }
                            }
                        }
                    }

                    if (enums.isNotEmpty()) {
                        for (s in enums) {
                            val value = s.defaultValue()
                            val em = document.createElement(s.label())
                            em.setAttribute(XMLPrefsManager.VALUE_ATTRIBUTE, value)
                            rootElement.appendChild(em)
                            values.add(s.label(), value)
                        }
                        XMLPrefsManager.writeTo(document, rssIndexFile)
                    } else if (needToWrite) {
                        XMLPrefsManager.writeTo(document, rssIndexFile)
                    }
                } catch (e: Exception) {
                    Tuils.log(e)
                }

                click = XMLPrefsManager.getBoolean(com.hereliesaz.hg2gui.managers.xml.options.Rss.click_rss)
                defaultFormat = XMLPrefsManager.get(com.hereliesaz.hg2gui.managers.xml.options.Rss.rss_default_format)
                defaultColor = XMLPrefsManager.getColor(com.hereliesaz.hg2gui.managers.xml.options.Rss.rss_default_color)
                includeRssDefault = XMLPrefsManager.getBoolean(com.hereliesaz.hg2gui.managers.xml.options.Rss.include_rss_default)
                timeFormat = XMLPrefsManager.get(com.hereliesaz.hg2gui.managers.xml.options.Rss.rss_time_format)
                showDownloadMessage = XMLPrefsManager.getBoolean(com.hereliesaz.hg2gui.managers.xml.options.Rss.show_rss_download)

                if (showDownloadMessage) {
                    downloadFormat = XMLPrefsManager.get(com.hereliesaz.hg2gui.managers.xml.options.Rss.rss_download_format)
                    val size = "%s"
                    idPattern = Pattern.compile("%id", Pattern.CASE_INSENSITIVE)
                    urlPattern = Pattern.compile("%url", Pattern.CASE_INSENSITIVE)
                    gbPattern = Pattern.compile(size + "gb", Pattern.CASE_INSENSITIVE)
                    mbPattern = Pattern.compile(size + "mb", Pattern.CASE_INSENSITIVE)
                    kbPattern = Pattern.compile(size + "kb", Pattern.CASE_INSENSITIVE)
                    bPattern = Pattern.compile(size + "b", Pattern.CASE_INSENSITIVE)
                    downloadMessageColor = XMLPrefsManager.getColor(com.hereliesaz.hg2gui.managers.xml.options.Rss.rss_download_message_color)
                }

                val hiddenTags = (XMLPrefsManager.get(com.hereliesaz.hg2gui.managers.xml.options.Rss.rss_hidden_tags) ?: "").replace(Tuils.SPACE, Tuils.EMPTYSTRING)
                var split: Array<String>? = null
                for (c in hiddenTags) {
                    if (c.isLetter()) continue
                    split = hiddenTags.split(c.toString()).toTypedArray()
                    break
                }
                if (split == null) split = arrayOf(hiddenTags)

                hideTagPatterns = Array(split.size * 2) { i ->
                    val s = split[i / 2]
                    if (i % 2 == 0) Pattern.compile("<$s[^>]*>[^<]*</$s>")
                    else Pattern.compile("<$s[^>]*/>")
                }

                for (rss in feeds) {
                    rss.updateFormat(formats)
                    rss.updateIncludeIfMatches()
                    rss.updateExcludeIfMatches()
                    if (rss.color == Int.MAX_VALUE) rss.color = defaultColor
                }

                for (rg in cmdRegexes) {
                    try {
                        val id = rg.literalPattern.toInt()
                        rg.regex = RegexManager.instance.get(id).regex
                    } catch (exc: Exception) {
                        try {
                            rg.regex = Pattern.compile(rg.literalPattern)
                        } catch (e: Exception) {
                            Tuils.sendOutput(Color.RED, context, context.getString(R.string.invalid_regex) + Tuils.SPACE + rg.literalPattern)
                            rg.regex = null
                        }
                    }
                }
                handler.post(updateRunnable)
            }
        }.start()
    }

    fun dispose() {
        handler.removeCallbacksAndMessages(null)
    }

    fun add(id: Int, timeInSeconds: Long, url: String): String? {
        val output = XMLPrefsManager.add(rssIndexFile, RSS_LABEL, arrayOf(ID_ATTRIBUTE, TIME_ATTRIBUTE, SHOW_ATTRIBUTE, URL_ATTRIBUTE),
            arrayOf(id.toString(), timeInSeconds.toString(), "true", url))
        if (output == null) {
            try {
                val r = Rss(url, timeInSeconds, id, true)
                r.lastShownItem = System.currentTimeMillis()
                r.format = defaultFormat
                updateRss(r, true)
                feeds.add(r)
            } catch (e: Exception) {
                Tuils.log(e)
                return e.toString()
            }
            return null
        } else return output
    }

    fun addFormat(id: Int, value: String): String? {
        val output = XMLPrefsManager.add(rssIndexFile, FORMAT_LABEL, arrayOf(ID_ATTRIBUTE, XMLPrefsManager.VALUE_ATTRIBUTE), arrayOf(id.toString(), value))
        if (output == null) {
            formats.add(XMLPrefsManager.IdValue(value, id))
            return null
        } else return output
    }

    fun removeFormat(id: Int): String? {
        val output = XMLPrefsManager.removeNode(rssIndexFile, FORMAT_LABEL, arrayOf(ID_ATTRIBUTE), arrayOf(id.toString()))
        return if (output == null) null else if (output.isNotEmpty()) output else context.getString(R.string.id_notfound)
    }

    fun rm(id: Int): String? {
        val output = XMLPrefsManager.removeNode(rssIndexFile, RSS_LABEL, arrayOf(ID_ATTRIBUTE), arrayOf(id.toString()))
        if (output == null) {
            val rss = File(root, "${RSS_LABEL}${id}.xml")
            if (rss.exists()) rss.delete()
            removeId(id)
            handler.sendEmptyMessage(id)
            return null
        } else return if (output.isNotEmpty()) output else context.getString(R.string.rss_not_found)
    }

    fun list(): String {
        val builder = StringBuilder()
        for (r in feeds) {
            builder.append(Rss.ID_LABEL).append(":").append(Tuils.SPACE).append(r.id).append(Tuils.NEWLINE).append(r.url).append(Tuils.NEWLINE)
        }
        val output = builder.toString().trim()
        return if (output.isEmpty()) "[]" else output
    }

    fun l(id: Int): String? {
        val feed = feeds.find { it.id == id } ?: return context.getString(R.string.rss_not_found)
        return try {
            parse(feed, false)
            null
        } catch (e: Exception) {
            Tuils.log(e)
            e.toString()
        }
    }

    fun setShow(id: Int, show: Boolean): String? {
        val output = XMLPrefsManager.set(rssIndexFile, RSS_LABEL, arrayOf(ID_ATTRIBUTE), arrayOf(id.toString()), arrayOf(SHOW_ATTRIBUTE), arrayOf(show.toString()), false)
        if (output == null) {
            findId(id)?.show = show
            return null
        } else return if (output.isNotEmpty()) output else context.getString(R.string.rss_not_found)
    }

    fun setTime(id: Int, timeSeconds: Long): String? {
        val output = XMLPrefsManager.set(rssIndexFile, RSS_LABEL, arrayOf(ID_ATTRIBUTE), arrayOf(id.toString()), arrayOf(TIME_ATTRIBUTE), arrayOf(timeSeconds.toString()), false)
        if (output == null) {
            findId(id)?.updateTimeSeconds = timeSeconds
            return null
        } else return if (output.isNotEmpty()) output else context.getString(R.string.rss_not_found)
    }

    fun setTimeFormat(id: Int, format: String): String? {
        val output = XMLPrefsManager.set(rssIndexFile, RSS_LABEL, arrayOf(ID_ATTRIBUTE), arrayOf(id.toString()), arrayOf(TIME_FORMAT_ATTRIBUTE), arrayOf(format), false)
        if (output == null) {
            findId(id)?.timeFormat = SimpleDateFormat(format, Locale.US)
            return null
        } else return if (output.isNotEmpty()) output else context.getString(R.string.rss_not_found)
    }

    fun setFormat(id: Int, format: String): String? {
        val output = XMLPrefsManager.set(rssIndexFile, RSS_LABEL, arrayOf(ID_ATTRIBUTE), arrayOf(id.toString()), arrayOf(FORMAT_ATTRIBUTE), arrayOf(format), false)
        if (output == null) {
            findId(id)?.setFormat(formats, format)
            return null
        } else return if (output.isNotEmpty()) output else context.getString(R.string.rss_not_found)
    }

    fun setColor(id: Int, color: String): String? {
        val output = XMLPrefsManager.set(rssIndexFile, RSS_LABEL, arrayOf(ID_ATTRIBUTE), arrayOf(id.toString()), arrayOf(COLOR_ATTRIBUTE), arrayOf(color), false)
        if (output == null) {
            findId(id)?.color = Color.parseColor(color)
            return null
        } else return if (output.isNotEmpty()) output else context.getString(R.string.rss_not_found)
    }

    fun setDateTag(id: Int, tag: String): String? {
        val output = XMLPrefsManager.set(rssIndexFile, RSS_LABEL, arrayOf(ID_ATTRIBUTE), arrayOf(id.toString()), arrayOf(DATE_TAG_ATTRIBUTE), arrayOf(tag), false)
        if (output == null) {
            findId(id)?.dateTag = tag
            return null
        } else return if (output.isNotEmpty()) output else context.getString(R.string.rss_not_found)
    }

    fun setEntryTag(id: Int, tag: String): String? {
        val output = XMLPrefsManager.set(rssIndexFile, RSS_LABEL, arrayOf(ID_ATTRIBUTE), arrayOf(id.toString()), arrayOf(ENTRY_TAG_ATTRIBUTE), arrayOf(tag), false)
        if (output == null) {
            findId(id)?.entryTag = tag
            return null
        } else return if (output.isNotEmpty()) output else context.getString(R.string.rss_not_found)
    }

    fun setIncludeIfMatches(id: Int, regex: String): String? {
        val output = XMLPrefsManager.set(rssIndexFile, RSS_LABEL, arrayOf(ID_ATTRIBUTE), arrayOf(id.toString()), arrayOf(INCLUDE_ATTRIBUTE), arrayOf(regex), false)
        if (output == null) {
            findId(id)?.setIncludeIfMatches(regex)
            return null
        } else return if (output.isNotEmpty()) output else context.getString(R.string.rss_not_found)
    }

    fun setExcludeIfMatches(id: Int, regex: String): String? {
        val output = XMLPrefsManager.set(rssIndexFile, RSS_LABEL, arrayOf(ID_ATTRIBUTE), arrayOf(id.toString()), arrayOf(EXCLUDE_ATTRIBUTE), arrayOf(regex), false)
        if (output == null) {
            findId(id)?.setExcludeIfMatches(regex)
            return null
        } else return if (output.isNotEmpty()) output else context.getString(R.string.rss_not_found)
    }

    fun setWifiOnly(id: Int, wifiOnly: Boolean): String? {
        val output = XMLPrefsManager.set(rssIndexFile, RSS_LABEL, arrayOf(ID_ATTRIBUTE), arrayOf(id.toString()), arrayOf(WIFIONLY_ATTRIBUTE), arrayOf(wifiOnly.toString()), false)
        if (output == null) {
            findId(id)?.wifiOnly = wifiOnly
            return null
        } else return if (output.isNotEmpty()) output else context.getString(R.string.rss_not_found)
    }

    fun addRegexCommand(id: Int, on: String, regex: String, cmd: String): String? {
        val output = XMLPrefsManager.add(rssIndexFile, REGEX_CMD_LABEL, arrayOf(ID_ATTRIBUTE, ON_ATTRIBUTE, XMLPrefsManager.VALUE_ATTRIBUTE, CMD_ATTRIBUTE),
            arrayOf(id.toString(), on, regex, cmd))
        return if (output == null) {
            cmdRegexes.add(CmdableRegex(id, on, regex, cmd))
            null
        } else if (output.isNotEmpty()) output else context.getString(R.string.output_error)
    }

    fun rmRegexCommand(id: Int): String? {
        val output = XMLPrefsManager.removeNode(rssIndexFile, REGEX_CMD_LABEL, arrayOf(ID_ATTRIBUTE), arrayOf(id.toString()))
        return if (output == null) {
            cmdRegexes.removeAll { it.id == id }
            null
        } else if (output.isNotEmpty()) output else context.getString(R.string.id_notfound)
    }

    private val updateRunnable = object : Runnable {
        override fun run() {
            for (feed in feeds) {
                if (feed.needUpdate()) updateRss(feed, false)
            }
            handler.postDelayed(this, RSS_CHECK_DELAY.toLong())
        }
    }

    private fun updateRss(feed: Rss, firstTime: Boolean, force: Boolean = false) {
        if (!force && feed.wifiOnly && connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)?.isConnected != true) {
            feed.lastCheckedClient = System.currentTimeMillis()
            feed.updateFile(rssIndexFile)
            return
        }

        object : StoppableThread() {
            override fun run() {
                super.run()
                if (!Tuils.hasInternetAccess()) {
                    if (force) Tuils.sendOutput(Color.RED, context, R.string.no_internet)
                    return
                }
                try {
                    val request = Request.Builder().url(feed.url).get().build()
                    val response = client.newCall(request).execute()

                    if (response.isSuccessful && (firstTime || response.code != 304)) {
                        val body = response.body
                        val bytes = if (body != null) {
                            Tuils.download(body.byteStream(), File(root, "${RSS_LABEL}${feed.id}.xml"))
                        } else 0L

                        if (showDownloadMessage) {
                            var c: CharSequence = Tuils.span(null, downloadFormat ?: "", downloadMessageColor, Int.MAX_VALUE)
                            val kb = bytes.toDouble() / 1024.0
                            val mb = kb / 1024.0
                            val gb = mb / 1024.0

                            val kbS = Tuils.round(kb, 2).toString()
                            val mbS = Tuils.round(mb, 2).toString()
                            val gbS = Tuils.round(gb, 2).toString()

                            c = urlPattern!!.matcher(c).replaceAll(feed.url)
                            c = idPattern!!.matcher(c).replaceAll(feed.id.toString())
                            c = gbPattern!!.matcher(c).replaceAll(gbS)
                            c = mbPattern!!.matcher(c).replaceAll(mbS)
                            c = kbPattern!!.matcher(c).replaceAll(kbS)
                            c = bPattern!!.matcher(c).replaceAll(bytes.toString())
                            c = TimeManager.instance?.replace(c) ?: c

                            Tuils.sendOutput(downloadMessageColor, context, c)
                        }

                        if (bytes == 0L) {
                            Tuils.sendOutput(Color.RED, context, context.getString(R.string.rss_invalid_empty) + Tuils.SPACE + feed.id)
                        } else {
                            response.close()
                            if (feed.show) parse(feed, true)
                        }
                    }
                    feed.lastCheckedClient = System.currentTimeMillis()
                    feed.updateFile(rssIndexFile)
                } catch (e: Exception) {
                    Tuils.log(e)
                }
            }
        }.start()
    }

    fun updateRss(feedId: Int, firstTime: Boolean, force: Boolean): Boolean {
        val rss = findId(feedId) ?: return false
        updateRss(rss, firstTime, force)
        return true
    }

    private fun parse(feed: Rss, time: Boolean): Boolean {
        val rssFile = File(root, "${RSS_LABEL}${feed.id}.xml")
        if (!rssFile.exists()) return false

        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()
        val doc = try {
            dBuilder.parse(rssFile)
        } catch (e: SAXParseException) {
            Tuils.sendXMLParseError(context, PATH, e)
            return false
        }
        doc.documentElement.normalize()

        var greatestTime = -1L
        var foundOneDateAtLeast = false
        var updated = false

        val entryTag = feed.entryTag ?: ENTRY_CHILD
        val dateTag = feed.dateTag ?: PUBDATE_CHILD
        val list = doc.getElementsByTagName(entryTag)

        if (list.length == 0) {
            Tuils.sendOutput(Color.RED, context, context.getString(R.string.rss_invalid_entry_tag) + Tuils.SPACE + entryTag)
            return false
        }

        for (c in list.length - 1 downTo 0) {
            val element = list.item(c) as? Element ?: continue
            if (time) {
                val l = element.getElementsByTagName(dateTag)
                if (l.length == 0) continue
                foundOneDateAtLeast = true
                val date = l.item(0).textContent
                val d = try {
                    feed.timeFormat?.parse(date) ?: defaultRSSDateFormat.parse(date)
                } catch (e: Exception) {
                    Tuils.sendOutput(Color.RED, context, rssFile.name + ": " + context.getString(R.string.rss_invalid_timeformat))
                    return false
                }
                val timeLong = d?.time ?: -1L
                greatestTime = Math.max(greatestTime, timeLong)
                if (feed.lastShownItem < timeLong) {
                    updated = true
                    showItem(feed, element, false)
                }
            } else {
                updated = true
                showItem(feed, element, true)
            }
        }

        if (time && !foundOneDateAtLeast) {
            Tuils.sendOutput(Color.RED, context, context.getString(R.string.rss_invalid_date) + Tuils.SPACE + dateTag)
        } else if (greatestTime != -1L) {
            feed.lastShownItem = greatestTime
            feed.updateLastShownItem(rssIndexFile)
        }
        return updated
    }

    private val formatPattern = Pattern.compile("%(?:\\[(\\d+)\\])?(?:\\[([^]]+)\\])?([a-zA-Z]+)")
    private val removeTags = Pattern.compile("<[^>]+>")

    private fun showItem(feed: Rss, item: Element, userRequested: Boolean) {
        val cp = (feed.format ?: defaultFormat ?: "").replace(Tuils.patternNewline.pattern().toRegex(), Tuils.NEWLINE)
        var s: CharSequence = Tuils.span(cp, feed.color)
        val dateTag = feed.dateTag ?: PUBDATE_CHILD
        val m = formatPattern.matcher(cp)
        while (m.find()) {
            val length = m.group(1)
            val color = m.group(2)
            val tag = m.group(3) ?: ""
            var value: String
            var cl = feed.color
            val ls = item.getElementsByTagName(tag)
            if (ls.length == 0) value = Tuils.EMPTYSTRING
            else {
                value = ls.item(0).textContent?.trim() ?: Tuils.EMPTYSTRING
                if (tag == dateTag) {
                    val d = try {
                        feed.timeFormat?.parse(value) ?: defaultRSSDateFormat.parse(value)
                    } catch (e: Exception) {
                        Tuils.log(e)
                        null
                    }
                    value = if (d != null) TimeManager.instance!!.replace(timeFormat ?: "", d.time, Int.MAX_VALUE).toString() else value
                } else {
                    value = HtmlEscape.unescapeHtml(value)
                    hideTagPatterns?.forEach { p -> value = p.matcher(value).replaceAll(Tuils.EMPTYSTRING) }
                    value = removeTags.matcher(value).replaceAll(Tuils.EMPTYSTRING)
                    try {
                        val l = length!!.toInt()
                        if (value.length > l) value = value.substring(0, l) + "..."
                    } catch (e: Exception) {}
                }
                try {
                    if (color != null) cl = Color.parseColor(color)
                } catch (e: Exception) {}
            }
            val replace: CharSequence = if (cl != feed.color && value.isNotEmpty()) Tuils.span(null, value, cl, Int.MAX_VALUE) else value
            s = TextUtils.replace(s, arrayOf(m.group(0)), arrayOf(replace))
        }

        if (includeRssDefault) {
            if (feed.excludeIfMatches?.matcher(s.toString())?.find() == true) return
        } else {
            if (feed.includeIfMatches?.matcher(s.toString())?.find() != true && feed.includeIfMatches != null) return
        }

        if (!userRequested) {
            for (r in cmdRegexes) {
                val reg = r.regex ?: continue
                if (!Tuils.arrayContains(r.on, feed.id)) continue
                var cmd = r.cmd
                val rssMatcher = reg.matcher(s.toString())
                if (rssMatcher.matches() || rssMatcher.find()) {
                    for (c in 1..rssMatcher.groupCount()) {
                        cmd = cmd.replace("%$c", rssMatcher.group(c) ?: "")
                    }
                    val intent = Intent(MainManager.ACTION_EXEC).apply {
                        putExtra(MainManager.NEED_WRITE_INPUT, false)
                        putExtra(MainManager.CMD, cmd)
                        putExtra(MainManager.CMD_COUNT, MainManager.commandCount)
                    }
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
                }
            }
        }

        var url: String? = null
        val list = item.getElementsByTagName(LINK_CHILD)
        if (list.length != 0) {
            val n = list.item(0)
            url = n.textContent
            if (n is Element && url.isNullOrEmpty()) {
                url = n.getAttribute(HREF_ATTRIBUTE)
            }
        }
        val action = if (url.isNullOrEmpty()) null else "search -u $url"
        Tuils.sendOutput(context, s, TerminalManager.CATEGORY_NO_COLOR, if (click) action else null)
    }

    class Rss(
        var url: String,
        var updateTimeSeconds: Long,
        var lastCheckedClient: Long,
        var lastShownItem: Long,
        var id: Int,
        var show: Boolean,
        var format: String?,
        var tempInclude: String?,
        var tempExclude: String?,
        var color: Int,
        var wifiOnly: Boolean,
        var timeFormat: SimpleDateFormat?,
        var entryTag: String?,
        var dateTag: String?
    ) {
        var includeIfMatches: Pattern? = null
        var excludeIfMatches: Pattern? = null

        constructor(url: String, updateTimeSeconds: Long, id: Int, show: Boolean) : this(
            url, updateTimeSeconds, -1L, -1L, id, show, null, null, null, Int.MAX_VALUE, false, null, null, null
        )

        fun needUpdate(): Boolean = System.currentTimeMillis() - lastCheckedClient >= (updateTimeSeconds * 1000)

        fun updateLastShownItem(rssFile: File) {
            XMLPrefsManager.set(rssFile, RSS_LABEL, arrayOf(ID_ATTRIBUTE), arrayOf(id.toString()), arrayOf(LAST_SHOWN_ITEM_ATTRIBUTE),
                arrayOf(lastShownItem.toString()), false)
        }

        fun updateIncludeIfMatches() {
            if (tempInclude != null) {
                try {
                    val id = tempInclude!!.toInt()
                    includeIfMatches = RegexManager.instance.get(id).regex
                } catch (exc: Exception) {
                    includeIfMatches = Pattern.compile(tempInclude)
                }
            }
        }

        fun updateExcludeIfMatches() {
            if (tempExclude != null) {
                try {
                    val id = tempExclude!!.toInt()
                    excludeIfMatches = RegexManager.instance.get(id).regex
                } catch (exc: Exception) {
                    excludeIfMatches = Pattern.compile(tempExclude)
                }
            }
        }

        fun updateFile(rssFile: File) {
            XMLPrefsManager.set(rssFile, RSS_LABEL, arrayOf(ID_ATTRIBUTE), arrayOf(id.toString()),
                arrayOf(LASTCHECKED_ATTRIBUTE), arrayOf(lastCheckedClient.toString()), false)
        }

        fun updateFormat(formats: List<XMLPrefsManager.IdValue>) {
            if (format != null) {
                try {
                    val fid = format!!.toInt()
                    formats.find { it.id == fid }?.let { format = it.value }
                } catch (exc: Exception) {}
            }
        }

        fun setFormat(formats: List<XMLPrefsManager.IdValue>, format: String) {
            this.format = format
            updateFormat(formats)
        }

        fun setIncludeIfMatches(regex: String) {
            tempInclude = regex
            updateIncludeIfMatches()
        }

        fun setExcludeIfMatches(regex: String) {
            tempExclude = regex
            updateExcludeIfMatches()
        }

        override fun equals(other: Any?): Boolean {
            return when (other) {
                is Rss -> id == other.id
                is Int -> id == other
                else -> false
            }
        }

        override fun hashCode(): Int = id

        override fun toString(): String {
            val dots = ":"
            return "$ID_LABEL$dots ${Tuils.SPACE}$id${Tuils.SPACE}" +
                    "$URL_LABEL$dots ${Tuils.SPACE}$url${Tuils.SPACE}" +
                    "$UPDATE_TIME_LABEL$dots ${Tuils.SPACE}$updateTimeSeconds${Tuils.SPACE}" +
                    "$SHOW_LABEL$dots ${Tuils.SPACE}$show"
        }

        companion object {
            const val ID_LABEL = "ID"
            const val URL_LABEL = "URL"
            const val UPDATE_TIME_LABEL = "update time"
            const val SHOW_LABEL = "show"

            fun fromElement(t: Element): Rss {
                val id = t.getAttribute(ID_ATTRIBUTE).toIntOrNull() ?: -1
                val url = t.getAttribute(URL_ATTRIBUTE) ?: ""
                val updateTime = t.getAttribute(TIME_ATTRIBUTE).toLongOrNull() ?: (60 * 30L)
                val show = t.getAttribute(SHOW_ATTRIBUTE).toBooleanStrictOrNull() ?: true
                val lastChecked = XMLPrefsManager.getLongAttribute(t, LASTCHECKED_ATTRIBUTE)
                val lastShown = XMLPrefsManager.getLongAttribute(t, LAST_SHOWN_ITEM_ATTRIBUTE)
                val format = XMLPrefsManager.getStringAttribute(t, FORMAT_ATTRIBUTE)
                val include = XMLPrefsManager.getStringAttribute(t, INCLUDE_ATTRIBUTE)
                val exclude = XMLPrefsManager.getStringAttribute(t, EXCLUDE_ATTRIBUTE)
                val color = try { Color.parseColor(t.getAttribute(COLOR_ATTRIBUTE)) } catch (e: Exception) { Int.MAX_VALUE }
                val wifiOnly = t.getAttribute(WIFIONLY_ATTRIBUTE).toBooleanStrictOrNull() ?: false
                val timeF = XMLPrefsManager.getStringAttribute(t, TIME_FORMAT_ATTRIBUTE)
                val rootN = XMLPrefsManager.getStringAttribute(t, ENTRY_TAG_ATTRIBUTE)
                val timeN = XMLPrefsManager.getStringAttribute(t, DATE_TAG_ATTRIBUTE)

                return Rss(url, updateTime, lastChecked, lastShown, id, show, format, include, exclude, color, wifiOnly,
                    timeF?.let { SimpleDateFormat(it, Locale.US) }, rootN, timeN)
            }
        }
    }

    private fun removeId(id: Int) {
        feeds.removeAll { it.id == id }
    }

    fun findId(id: Int): Rss? = feeds.find { it.id == id }

    private fun prepare(): Boolean {
        if (!root.isDirectory) root.mkdir()
        if (!rssIndexFile.exists()) {
            return try {
                rssIndexFile.createNewFile()
                XMLPrefsManager.resetFile(rssIndexFile, NAME)
                false
            } catch (e: Exception) {
                false
            }
        }
        return true
    }

    private inner class CmdableRegex(id: Int, on: String, regex: String, val cmd: String) : RegexManager.Regex() {
        var onArray: IntArray? = null

        init {
            this.id = id
            this.literalPattern = regex
            val separator = Tuils.firstNonDigit(on)
            if (separator == 0.toChar()) {
                try {
                    this.onArray = intArrayOf(on.toInt())
                } catch (e: Exception) {}
            } else {
                var currentOn = on
                if (separator == ' ') {
                    val s2 = Tuils.firstNonDigit(Tuils.removeSpaces(on))
                    if (s2 != 0.toChar()) {
                        currentOn = Tuils.removeSpaces(on)
                    }
                }
                val split = currentOn.split(Pattern.quote(separator.toString()).toRegex()).toTypedArray()
                this.onArray = IntArray(split.size) { i ->
                    split[i].toIntOrNull() ?: Int.MAX_VALUE
                }
            }
        }

        // Helper to check if on contains id, since Regex class might not have it or use it differently
        val on: IntArray get() = onArray ?: intArrayOf()
    }

    companion object {
        const val TIME_ATTRIBUTE = "updateTimeSec"
        const val SHOW_ATTRIBUTE = "show"
        const val URL_ATTRIBUTE = "url"
        const val LASTCHECKED_ATTRIBUTE = "lastChecked"
        const val LAST_SHOWN_ITEM_ATTRIBUTE = "lastShownItem"
        const val ID_ATTRIBUTE = "id"
        const val FORMAT_ATTRIBUTE = "format"
        const val INCLUDE_ATTRIBUTE = "includeIfMatches"
        const val EXCLUDE_ATTRIBUTE = "excludeIfMatches"
        const val COLOR_ATTRIBUTE = "color"
        const val WIFIONLY_ATTRIBUTE = "wifiOnly"
        const val TIME_FORMAT_ATTRIBUTE = "timeFormat"
        const val DATE_TAG_ATTRIBUTE = "pubDateTag"
        const val ENTRY_TAG_ATTRIBUTE = "entryTag"
        const val ON_ATTRIBUTE = "on"
        const val CMD_ATTRIBUTE = "cmd"

        const val RSS_LABEL = "rss"
        const val FORMAT_LABEL = "format"
        const val REGEX_CMD_LABEL = "regex"

        private const val PUBDATE_CHILD = "pubDate"
        private const val ENTRY_CHILD = "item"
        private const val LINK_CHILD = "link"
        private const val HREF_ATTRIBUTE = "href"

        val values = XMLPrefsList()
        const val PATH = "rss.xml"
        const val NAME = "RSS"
        var instance: RssManager? = null
    }
}
