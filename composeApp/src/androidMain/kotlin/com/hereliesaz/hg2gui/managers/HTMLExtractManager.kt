package com.hereliesaz.hg2gui.managers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.net.Uri
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.hereliesaz.hg2gui.R
import com.hereliesaz.hg2gui.UIManager
import com.hereliesaz.hg2gui.managers.xml.XMLPrefsManager
import com.hereliesaz.hg2gui.managers.xml.options.Behavior
import com.hereliesaz.hg2gui.managers.xml.options.Theme
import com.hereliesaz.hg2gui.tuils.LongClickableSpan
import com.hereliesaz.hg2gui.tuils.CalculationEngine
import com.hereliesaz.hg2gui.tuils.Tuils
import com.jayway.jsonpath.JsonPath
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.htmlcleaner.CleanerProperties
import org.htmlcleaner.HtmlCleaner
import org.htmlcleaner.TagNode
import org.jsoup.Jsoup
import org.w3c.dom.Element
import org.xml.sax.SAXParseException
import java.io.File
import java.util.regex.Pattern

class HTMLExtractManager(context: Context, private val client: OkHttpClient) {

    private val xpaths: MutableList<StoreableValue> = ArrayList()
    private val jsons: MutableList<StoreableValue> = ArrayList()
    private val formats: MutableList<StoreableValue> = ArrayList()

    private val receiver: BroadcastReceiver

    private val defaultFormat: String?
    private val weatherFormat: String?
    private val weatherColor: Int
    private val linkColor: Int
    private val outputColor: Int
    private val optionalValueSeparator: String?

    init {
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.getIntExtra(BROADCAST_COUNT, 0) < broadcastCount) return
                broadcastCount++

                when (intent.action) {
                    ACTION_ADD -> {
                        val id = intent.getIntExtra(ID, Int.MAX_VALUE)
                        val tag = intent.getStringExtra(TAG_NAME)
                        val path = intent.getStringExtra(XMLPrefsManager.VALUE_ATTRIBUTE) ?: ""

                        if (tag == StoreableValue.Type.format.name) {
                            if (formats.any { it.id == id }) {
                                Tuils.sendOutput(context, R.string.id_already)
                                return
                            }
                        } else {
                            if (xpaths.any { it.id == id } || jsons.any { it.id == id }) {
                                Tuils.sendOutput(context, R.string.id_already)
                                return
                            }
                        }

                        val values: MutableList<StoreableValue> = try {
                            val p = StoreableValue.Type.valueOf(tag!!)
                            getListFromType(p)
                        } catch (e: Exception) {
                            return
                        }

                        val v = StoreableValue.create(values, context, tag!!, path, id)
                        if (v != null) values.add(v)
                    }
                    ACTION_RM -> {
                        val id = intent.getIntExtra(ID, Int.MAX_VALUE)
                        var check = false

                        val iterators = listOf(xpaths.iterator(), jsons.iterator(), formats.iterator())
                        for (it in iterators) {
                            while (it.hasNext()) {
                                val v = it.next()
                                if (v.id == id) {
                                    v.remove(context)
                                    it.remove()
                                    check = true
                                    break
                                }
                            }
                            if (check) break
                        }

                        if (!check) Tuils.sendOutput(context, R.string.id_notfound)
                    }
                    ACTION_EDIT -> {
                        val id = intent.getIntExtra(ID, Int.MAX_VALUE)
                        val newExpression = intent.getStringExtra(XMLPrefsManager.VALUE_ATTRIBUTE)
                        if (newExpression.isNullOrEmpty()) return

                        val all = xpaths + jsons + formats
                        val target = all.find { it.id == id }
                        if (target != null) {
                            target.edit(context, newExpression)
                        } else {
                            Tuils.sendOutput(context, R.string.id_notfound)
                        }
                    }
                    ACTION_LS -> {
                        val tag = intent.getStringExtra(TAG_NAME)
                        val builder = StringBuilder()
                        try {
                            val p = StoreableValue.Type.valueOf(tag!!)
                            val values = getListFromType(p)
                            for (v in values) {
                                builder.append("- ID: ").append(v.id).append(" -> ").append(v.value).append(Tuils.NEWLINE)
                            }
                        } catch (e: Exception) {
                            builder.append("XPaths:").append(Tuils.NEWLINE)
                            if (xpaths.isEmpty()) builder.append("[]").append(Tuils.NEWLINE)
                            else xpaths.forEach { builder.append(Tuils.DOUBLE_SPACE).append("- ID: ").append(it.id).append(" -> ").append(it.value).append(Tuils.NEWLINE) }

                            builder.append("JsonPaths:").append(Tuils.NEWLINE)
                            if (jsons.isEmpty()) builder.append("[]").append(Tuils.NEWLINE)
                            else jsons.forEach { builder.append(Tuils.DOUBLE_SPACE).append("- ID: ").append(it.id).append(" -> ").append(it.value).append(Tuils.NEWLINE) }

                            builder.append("Formats:").append(Tuils.NEWLINE)
                            if (formats.isEmpty()) builder.append("[]").append(Tuils.NEWLINE)
                            else formats.forEach { builder.append(Tuils.DOUBLE_SPACE).append("- ID: ").append(it.id).append(" -> ").append(it.value).append(Tuils.NEWLINE) }
                        }

                        var text = builder.toString().trim()
                        if (text.isEmpty()) text = "[]"
                        Tuils.sendOutput(context, text)
                    }
                    ACTION_QUERY -> {
                        val website = intent.getStringExtra(XMLPrefsManager.VALUE_ATTRIBUTE) ?: ""
                        val weatherArea = intent.getBooleanExtra(WEATHER_AREA, false)
                        var path = intent.getStringExtra(ID)
                        var format = intent.getStringExtra(FORMAT_ID)

                        if (format == null) {
                            val formatId = intent.getIntExtra(FORMAT_ID, Int.MAX_VALUE)
                            if (formatId != Int.MAX_VALUE) {
                                format = formats.find { it.id == formatId }?.value
                                if (format == null) {
                                    Tuils.sendOutput(context, context.getString(R.string.id_notfound) + ": " + formatId + "(" + StoreableValue.Type.format.name + ")")
                                }
                            }
                        }

                        var pathType = StoreableValue.Type.json
                        if (path == null) {
                            val pathId = intent.getIntExtra(ID, Int.MAX_VALUE)
                            val p = (xpaths + jsons).find { it.id == pathId }
                            if (p != null) {
                                path = p.value
                                pathType = p.type
                            } else {
                                if (pathId != Int.MAX_VALUE) {
                                    Tuils.sendOutput(context, context.getString(R.string.id_notfound) + ": " + pathId)
                                }
                                return
                            }
                        }

                        query(context, path, pathType, format, website, weatherArea)
                    }
                    ACTION_WEATHER -> {
                        val url = intent.getStringExtra(XMLPrefsManager.VALUE_ATTRIBUTE) ?: ""
                        query(context, weatherFormat ?: "", url)
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(ACTION_ADD)
            addAction(ACTION_RM)
            addAction(ACTION_EDIT)
            addAction(ACTION_LS)
            addAction(ACTION_QUERY)
            addAction(ACTION_WEATHER)
        }

        linkColor = XMLPrefsManager.getColor(Theme.link_color)
        outputColor = XMLPrefsManager.getColor(Theme.output_color)
        weatherColor = XMLPrefsManager.getColor(Theme.weather_color)
        defaultFormat = XMLPrefsManager.get(Behavior.htmlextractor_default_format)
        optionalValueSeparator = XMLPrefsManager.get(Behavior.optional_values_separator)
        weatherFormat = XMLPrefsManager.get(Behavior.weather_format)

        LocalBroadcastManager.getInstance(context.applicationContext).registerReceiver(receiver, filter)
        broadcastCount = 0

        val file = File(Tuils.getFolder(), PATH)
        if (!file.exists()) {
            XMLPrefsManager.resetFile(file, NAME)
        }

        try {
            val o = XMLPrefsManager.buildDocument(file, NAME)
            if (o != null) {
                val root = o[1] as Element
                val nodes = root.getElementsByTagName("*")
                for (count in 0 until nodes.length) {
                    val n = nodes.item(count)
                    try {
                        val v = StoreableValue.fromNode(n as Element)
                        if (v != null) {
                            getListFromType(v.type).add(v)
                        }
                    } catch (e: Exception) {}
                }
            } else {
                Tuils.sendXMLParseError(context, PATH)
            }
        } catch (e: SAXParseException) {
            Tuils.sendXMLParseError(context, PATH, e)
        } catch (e: Exception) {
            Tuils.log(e)
        }
    }

    private fun getListFromType(t: StoreableValue.Type): MutableList<StoreableValue> {
        return when (t) {
            StoreableValue.Type.xpath -> xpaths
            StoreableValue.Type.json -> jsons
            StoreableValue.Type.format -> formats
        }
    }

    fun dispose(context: Context) {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver)
    }

    private val weatherFormatPattern = Pattern.compile("%([a-z_]+)(\\d)*(?:\\$\\(([\\.\\+\\-\\*\\/\\^\\d]+)\\))?")

    private fun query(context: Context, path: String, pathType: StoreableValue.Type, format: String?, url: String, weatherArea: Boolean) {
        Thread {
            if (!Tuils.hasInternetAccess()) {
                output(R.string.no_internet, context, weatherArea)
                return@Thread
            }

            try {
                val request = Request.Builder()
                    .url(url)
                    .cacheControl(CacheControl.FORCE_NETWORK)
                    .addHeader("User-Agent", "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:15.0) Gecko/20100101 Firefox/15.0.1")
                    .get()
                    .build()

                val response = client.newCall(request).execute()

                if (response.code == 429 && weatherArea) {
                    val i = Intent(UIManager.ACTION_WEATHER_DELAY)
                    LocalBroadcastManager.getInstance(context.applicationContext).sendBroadcast(i)
                    return@Thread
                } else if (!response.isSuccessful) {
                    val message = context.getString(R.string.internet_error) + Tuils.SPACE + response.code
                    if (weatherArea) {
                        val i = Intent(UIManager.ACTION_WEATHER).apply {
                            putExtra(XMLPrefsManager.VALUE_ATTRIBUTE, message)
                        }
                        LocalBroadcastManager.getInstance(context.applicationContext).sendBroadcast(i)
                    } else {
                        output(message, context, false)
                    }
                    return@Thread
                }

                val inputStream = response.body?.byteStream() ?: return@Thread
                var finalOutput: CharSequence = Tuils.span(Tuils.EMPTYSTRING, outputColor)

                if (weatherArea) {
                    val json = Tuils.inputStreamToString(inputStream)
                    var o: CharSequence = Tuils.span(weatherFormat ?: "", weatherColor)

                    val m = weatherFormatPattern.matcher(weatherFormat ?: "")
                    while (m.find()) {
                        val name = m.group(1)
                        var delay = m.group(2)
                        if (delay.isNullOrEmpty()) delay = "1"
                        val converter = m.group(3)

                        val stopAt = delay.toInt()
                        val p = Pattern.compile("\"$name\":(?:\"([^\"]+)\"|(-?\\d+\\.?\\d*))")
                        val m1 = p.matcher(json)
                        var c = 1
                        while (m1.find()) {
                            if (c == stopAt) {
                                var value = m1.group(1)
                                if (value.isNullOrEmpty()) value = m1.group(2)

                                if (!converter.isNullOrEmpty()) {
                                    try {
                                        var d = value.toDouble()
                                        d = CalculationEngine.textCalculus(d, converter)
                                        value = String.format("%.2f", d)
                                    } catch (e: Exception) {
                                        Tuils.log(e)
                                    }
                                }
                                o = TextUtils.replace(o, arrayOf(m.group(0)), arrayOf(delimiterStart + value + delimiterEnd))
                                break
                            } else c++
                        }
                    }

                    o = replaceLinkColorReplace(context, o, url, linkColor)
                    o = removeDelimiter(o)

                    val i = Intent(UIManager.ACTION_WEATHER).apply {
                        putExtra(XMLPrefsManager.VALUE_ATTRIBUTE, o)
                    }
                    LocalBroadcastManager.getInstance(context.applicationContext).sendBroadcast(i)
                } else if (pathType == StoreableValue.Type.xpath) {
                    val cleaner = HtmlCleaner()
                    val props = cleaner.properties
                    // omitComments is private, but setOmitComments is available in Java. 
                    // In Kotlin it should be reachable if public or via property.
                    // If it is indeed private, we may need to use setOmitComments if available.
                    props.isOmitComments = true

                    val rootNode = cleaner.clean(inputStream)
                    val nodes = rootNode.evaluateXPath(path)
                    if (nodes.isEmpty()) {
                        Tuils.sendOutput(context, R.string.no_result)
                        return@Thread
                    }

                    for (c in nodes.indices) {
                        val node = nodes[c] as TagNode
                        val f = format ?: defaultFormat ?: ""
                        var copy: CharSequence = Tuils.span(f, outputColor)

                        copy = replaceAllAttributesString(copy, node.attributes.entries)
                        copy = replaceTagNameString(copy, node.name, node.attributes)
                        copy = replaceNodeValue(copy, node.text.toString())
                        copy = replaceNewline(copy)
                        copy = replaceLinkColorReplace(context, copy, url, linkColor)
                        copy = removeDelimiter(copy)

                        if (copy.toString().trim().isNotEmpty()) {
                            finalOutput = TextUtils.concat(finalOutput, if (c != 0) Tuils.NEWLINE + Tuils.NEWLINE else Tuils.EMPTYSTRING, copy)
                        }
                    }
                    output(finalOutput, context, weatherArea, TerminalManager.CATEGORY_NO_COLOR)
                } else {
                    val jsonObj = JsonPath.read<Any>(inputStream, path)
                    if (jsonObj is Map<*, *>) {
                        val f = format ?: defaultFormat ?: ""
                        var copy: CharSequence = Tuils.span(f, outputColor)
                        @Suppress("UNCHECKED_CAST")
                        copy = replaceAllAttributesObject(copy, (jsonObj as Map<String, Any?>).entries)
                        @Suppress("UNCHECKED_CAST")
                        copy = replaceTagNameObject(copy, null, jsonObj as Map<String, Any?>)
                        copy = replaceNewline(copy)
                        copy = replaceLinkColorReplace(context, copy, url, linkColor)
                        copy = removeDelimiter(copy)
                        finalOutput = copy
                        output(finalOutput, context, weatherArea, TerminalManager.CATEGORY_NO_COLOR)
                    } else if (jsonObj is List<*>) {
                        for (c in jsonObj.indices) {
                            val f = format ?: defaultFormat ?: ""
                            var copy: CharSequence = Tuils.span(f, outputColor)
                            @Suppress("UNCHECKED_CAST")
                            val m = jsonObj[c] as Map<String, Any?>
                            copy = replaceAllAttributesObject(copy, m.entries)
                            copy = replaceTagNameObject(copy, null, m)
                            copy = replaceNewline(copy)
                            copy = replaceLinkColorReplace(context, copy, url, linkColor)
                            copy = removeDelimiter(copy)

                            if (copy.toString().trim().isNotEmpty()) {
                                finalOutput = TextUtils.concat(finalOutput, if (c != 0) Tuils.NEWLINE + Tuils.NEWLINE else Tuils.EMPTYSTRING, copy)
                            }
                        }
                        output(finalOutput, context, weatherArea, TerminalManager.CATEGORY_NO_COLOR)
                    } else if (jsonObj is String) {
                        finalOutput = Tuils.span(jsonObj.toString(), outputColor)
                        output(finalOutput, context, weatherArea, TerminalManager.CATEGORY_NO_COLOR)
                    } else {
                        Tuils.sendOutput(outputColor, context, jsonObj.toString())
                    }
                }
            } catch (e: Exception) {
                output(e.toString(), context, weatherArea)
                Tuils.toFile(e)
                Tuils.log(e)
            }
        }.start()
    }

    private fun query(context: Context, format: String, url: String) {
        query(context, "", StoreableValue.Type.json, format, url, true)
    }

    companion object {
        const val ACTION_ADD = "com.hereliesaz.hg2gui.htmlextract_add"
        const val ACTION_RM = "com.hereliesaz.hg2gui.htmlextract_rm"
        const val ACTION_EDIT = "com.hereliesaz.hg2gui.htmlextract_edit"
        const val ACTION_LS = "com.hereliesaz.hg2gui.htmlextract_ls"
        const val ACTION_QUERY = "com.hereliesaz.hg2gui.htmlextract_query"
        const val ACTION_WEATHER = "com.hereliesaz.hg2gui.htmlextract_weather"

        const val ID = "id"
        const val FORMAT_ID = "formatId"
        const val TAG_NAME = "tag"
        const val WEATHER_AREA = "wArea"
        const val BROADCAST_COUNT = "broadcastCount"
        const val PATH = "htmlextract.xml"
        const val NAME = "HTMLEXTRACT"

        @JvmField
        var broadcastCount: Int = 0

        private fun output(s: CharSequence, context: Context, weatherArea: Boolean, category: Int = Int.MAX_VALUE) {
            if (weatherArea) {
                val i = Intent(UIManager.ACTION_WEATHER).apply {
                    putExtra(XMLPrefsManager.VALUE_ATTRIBUTE, s)
                }
                LocalBroadcastManager.getInstance(context.applicationContext).sendBroadcast(i)
            } else {
                if (category != Int.MAX_VALUE) Tuils.sendOutput(context, s, category)
                else Tuils.sendOutput(context, s)
            }
        }

        private fun output(stringRes: Int, context: Context, weatherArea: Boolean, category: Int = Int.MAX_VALUE) {
            output(context.getString(stringRes), context, weatherArea, category)
        }

        private val tagName = Pattern.compile("%t(?:\\(([^)]*)\\))?", Pattern.CASE_INSENSITIVE)
        private const val nodeValuePattern = "%v"
        private val allAttributes = Pattern.compile("%a\\(([^\\)]*)\\)\\(([^\\)]*)\\)", Pattern.CASE_INSENSITIVE)
        private val attributeName = Pattern.compile("%an", Pattern.CASE_INSENSITIVE)
        private val attributeValue = Pattern.compile("%av", Pattern.CASE_INSENSITIVE)

        fun removeDelimiter(original: CharSequence): CharSequence {
            var current = original
            var prev: CharSequence
            do {
                prev = current
                current = TextUtils.replace(prev, delimiterArray, delimiterReplacementArray)
            } while (current.length < prev.length)
            return current
        }

        fun replaceAllAttributesObject(original: CharSequence, set: Set<Map.Entry<String, Any?>>): CharSequence {
            val m = allAttributes.matcher(original)
            if (m.find()) {
                val list = set.toList()
                val first = m.group(1) ?: ""
                val separator = m.group(2) ?: ""
                val b = StringBuilder().append(delimiterStart)
                for (i in list.indices) {
                    val e = list[i]
                    var temp = first
                    temp = attributeName.matcher(temp).replaceAll(e.key)
                    temp = attributeValue.matcher(temp).replaceAll(Tuils.removeUnncesarySpaces(e.value?.toString()?.trim() ?: ""))
                    b.append(temp)
                    if (i != list.size - 1) b.append(separator)
                }
                b.append(delimiterEnd)
                return TextUtils.replace(original, arrayOf(m.group()), arrayOf(b.toString().trim()))
            }
            return original
        }

        fun replaceTagNameObject(original: CharSequence, tag: String?, attributes: Map<String, Any?>?): CharSequence {
            val m = tagName.matcher(original)
            if (m.find()) {
                val attribute = m.group(1)
                val currentTag = tag ?: "null"
                var replace = "null"
                if (attribute.isNullOrEmpty()) {
                    replace = currentTag
                } else if (attributes != null) {
                    replace = attributes[attribute]?.toString() ?: "null"
                    if (replace.isEmpty()) replace = "null"
                }
                return TextUtils.replace(original, arrayOf(m.group()), arrayOf(delimiterStart + replace + delimiterEnd))
            }
            return original
        }

        fun replaceAllAttributesString(original: CharSequence, set: Set<Map.Entry<String, String>>): CharSequence {
            val m = allAttributes.matcher(original)
            if (m.find()) {
                val list = set.toList()
                val first = m.group(1) ?: ""
                val separator = m.group(2) ?: ""
                val b = StringBuilder().append(delimiterStart)
                for (i in list.indices) {
                    val e = list[i]
                    var temp = first
                    temp = attributeName.matcher(temp).replaceAll(e.key)
                    temp = attributeValue.matcher(temp).replaceAll(Tuils.removeUnncesarySpaces(e.value.trim()))
                    b.append(temp)
                    if (i != list.size - 1) b.append(separator)
                }
                b.append(delimiterEnd)
                return TextUtils.replace(original, arrayOf(m.group()), arrayOf(b.toString().trim()))
            }
            return original
        }

        fun replaceTagNameString(original: CharSequence, tag: String?, attributes: Map<String, String>?): CharSequence {
            val m = tagName.matcher(original)
            if (m.find()) {
                val attribute = m.group(1)
                val currentTag = tag ?: "null"
                var replace = "null"
                if (attribute.isNullOrEmpty()) {
                    replace = currentTag
                } else if (attributes != null) {
                    replace = attributes[attribute] ?: "null"
                    if (replace.isEmpty()) replace = "null"
                }
                return TextUtils.replace(original, arrayOf(m.group()), arrayOf(delimiterStart + replace + delimiterEnd))
            }
            return original
        }

        fun replaceNodeValue(original: CharSequence, nodeValue: String): CharSequence {
            val parsed = Jsoup.parse(nodeValue).text()
            return TextUtils.replace(original, arrayOf(nodeValuePattern), arrayOf(delimiterStart + Tuils.removeUnncesarySpaces(parsed).trim() + delimiterEnd))
        }

        fun replaceNewline(original: CharSequence): CharSequence {
            var current = original
            var prevLen: Int
            do {
                prevLen = current.length
                current = TextUtils.replace(current, arrayOf(Tuils.patternNewline.pattern()), arrayOf(Tuils.NEWLINE))
            } while (current.length < prevLen)
            return current
        }

        private val colorPattern = Pattern.compile("(#[a-fA-F0-9]{6})\\[([^\\]]+)\\]")
        private val linkPattern = Pattern.compile("#\\[((?:(?:http(?:s)?)|(?:www\\.))[^\\]]+)\\]")
        private val replacePattern = Pattern.compile("#(\\[.+?\\])@#&(.+?)&#@")
        private val extractUrl = Pattern.compile("(.*\\.[^\\/]{2,})\\/", Pattern.CASE_INSENSITIVE)

        const val delimiterStart = "@#&"
        val delimiterEnd = delimiterStart.reversed()
        val delimiterArray = arrayOf(delimiterStart, delimiterEnd)
        val delimiterReplacementArray = arrayOf<CharSequence>(Tuils.EMPTYSTRING, Tuils.EMPTYSTRING)

        fun replaceLinkColorReplace(context: Context, original: CharSequence, url: String, linkColor: Int): CharSequence {
            var current = original
            var m = colorPattern.matcher(current)
            while (m.find()) {
                try {
                    val cl = Color.parseColor(m.group(1))
                    current = TextUtils.replace(current, arrayOf(m.group()), arrayOf(Tuils.span(m.group(2), cl)))
                    m = colorPattern.matcher(current)
                } catch (e: Exception) {
                    Tuils.sendOutput(context, context.getString(R.string.output_invalidcolor) + ": " + m.group(1))
                }
            }

            m = linkPattern.matcher(current)
            while (m.find()) {
                var text = m.group(1) ?: ""
                if (text.startsWith("/")) {
                    val m1 = extractUrl.matcher(url)
                    if (m1.find()) {
                        text = (m1.group(1) ?: "") + text
                    }
                }
                val sp = SpannableString(text)
                sp.setSpan(LongClickableSpan(Uri.parse(text)), 0, sp.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                sp.setSpan(ForegroundColorSpan(linkColor), 0, sp.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                current = TextUtils.replace(current, arrayOf(m.group()), arrayOf(sp))
                m = linkPattern.matcher(current)
            }

            m = replacePattern.matcher(current)
            val optionalValueSeparator = XMLPrefsManager.get(Behavior.optional_values_separator) ?: ""
            while (m.find()) {
                val replaceGroups = m.group(1) ?: ""
                var text = m.group(2) ?: ""
                val groups = replaceGroups.split("]")
                for (group in groups) {
                    val cleanGroup = group.replace("[\\[\\]]".toRegex(), Tuils.EMPTYSTRING)
                    val split = cleanGroup.split(optionalValueSeparator.toRegex())
                    if (split.isEmpty()) continue
                    text = text.replace(split[0], if (split.size > 1) split[1] else "")
                }
                current = TextUtils.replace(current, arrayOf(m.group()), arrayOf(text))
                m = replacePattern.matcher(current)
            }
            return current
        }
    }

    class StoreableValue(val id: Int, var value: String, val type: Type) {
        enum class Type { xpath, json, format }

        fun remove(context: Context) {
            val file = File(Tuils.getFolder(), PATH)
            if (!file.exists()) XMLPrefsManager.resetFile(file, NAME)
            val output = XMLPrefsManager.removeNode(file, type.name, arrayOf(ID), arrayOf(id.toString()))
            if (output != null) {
                if (output.isNotEmpty()) Tuils.sendOutput(Color.RED, context, output)
                else Tuils.sendOutput(Color.RED, context, R.string.id_notfound)
            }
        }

        fun edit(context: Context, newExpression: String) {
            val file = File(Tuils.getFolder(), PATH)
            if (!file.exists()) XMLPrefsManager.resetFile(file, NAME)
            val output = XMLPrefsManager.set(file, type.name, arrayOf(ID), arrayOf(id.toString()), arrayOf(XMLPrefsManager.VALUE_ATTRIBUTE), arrayOf(newExpression), false)
            if (output != null) {
                if (output.isNotEmpty()) Tuils.sendOutput(Color.RED, context, output)
                else Tuils.sendOutput(Color.RED, context, R.string.id_notfound)
            } else {
                this.value = newExpression
            }
        }

        companion object {
            fun fromNode(e: Element): StoreableValue? {
                val nn = e.nodeName
                val id = XMLPrefsManager.getIntAttribute(e, ID)
                val value = XMLPrefsManager.getStringAttribute(e, XMLPrefsManager.VALUE_ATTRIBUTE) ?: ""
                return try {
                    StoreableValue(id, value, Type.valueOf(nn!!))
                } catch (ex: Exception) {
                    null
                }
            }

            fun create(values: List<StoreableValue>, context: Context, tag: String, path: String, id: Int): StoreableValue? {
                if (values.any { it.id == id }) {
                    Tuils.sendOutput(context, R.string.id_already)
                    return null
                }
                val file = File(Tuils.getFolder(), PATH)
                if (!file.exists()) XMLPrefsManager.resetFile(file, NAME)
                val output = XMLPrefsManager.add(file, tag, arrayOf(ID, XMLPrefsManager.VALUE_ATTRIBUTE), arrayOf(id.toString(), path))
                if (output != null) {
                    if (output.isNotEmpty()) Tuils.sendOutput(Color.RED, context, output)
                    else Tuils.sendOutput(Color.RED, context, R.string.output_error)
                    return null
                }
                return StoreableValue(id, path, Type.valueOf(tag))
            }
        }
    }
}
