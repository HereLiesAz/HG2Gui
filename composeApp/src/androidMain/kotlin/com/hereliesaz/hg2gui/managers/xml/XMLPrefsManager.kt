package com.hereliesaz.hg2gui.managers.xml

import android.content.Context
import android.graphics.Color
import com.hereliesaz.hg2gui.R
import com.hereliesaz.hg2gui.managers.xml.classes.XMLPrefsElement
import com.hereliesaz.hg2gui.managers.xml.classes.XMLPrefsList
import com.hereliesaz.hg2gui.managers.xml.classes.XMLPrefsSave
import com.hereliesaz.hg2gui.managers.xml.options.Behavior
import com.hereliesaz.hg2gui.managers.xml.options.Cmd
import com.hereliesaz.hg2gui.managers.xml.options.Suggestions
import com.hereliesaz.hg2gui.managers.xml.options.Theme
import com.hereliesaz.hg2gui.managers.xml.options.Toolbar
import com.hereliesaz.hg2gui.managers.xml.options.Ui
import com.hereliesaz.hg2gui.tuils.Tuils
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.SAXParseException
import java.io.File
import java.io.FileOutputStream
import java.io.StringWriter
import java.util.regex.Pattern
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

class XMLPrefsManager private constructor() {

    enum class XMLPrefsRoot(en: Array<out XMLPrefsSave>) : XMLPrefsElement {
        THEME(Theme.values()) {
            override fun delete(): Array<String?> = emptyArray()
        },
        CMD(Cmd.values()) {
            override fun delete(): Array<String?> = emptyArray()
        },
        TOOLBAR(Toolbar.values()) {
            override fun delete(): Array<String?> = emptyArray()
        },
        UI(Ui.values()) {
            override fun delete(): Array<String?> = emptyArray()
        },
        BEHAVIOR(Behavior.values()) {
            override fun delete(): Array<String?> = emptyArray()
        },
        SUGGESTIONS(Suggestions.values()) {
            override fun delete(): Array<String?> = arrayOf(
                "app_suggestions_minrate",
                "contact_suggestions_minrate",
                "song_suggestions_minrate",
                "file_suggestions_minrate"
            )
        };

        var path: String
        var pValues: XMLPrefsList
        var enums: MutableList<XMLPrefsSave>

        init {
            this.pValues = XMLPrefsList()
            this.enums = en.toMutableList()
            this.path = "${this.name.lowercase()}.xml"
        }

        override fun write(save: XMLPrefsSave, value: String) {
            set(File(Tuils.getFolder(), path), save.label(), arrayOf(VALUE_ATTRIBUTE), arrayOf(value))
        }

        override fun getValues(): XMLPrefsList = pValues

        override fun path(): String = path

        abstract override fun delete(): Array<String?>
    }

    class IdValue(var value: String, var id: Int)

    companion object {
        const val XML_DEFAULT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
        const val VALUE_ATTRIBUTE = "value"

        private val factory: DocumentBuilderFactory = DocumentBuilderFactory.newInstance()
        private var builder: DocumentBuilder? = null

        init {
            try {
                builder = factory.newDocumentBuilder()
            } catch (e: ParserConfigurationException) {
            }
        }

        @JvmStatic
        fun dispose() {
            commonsLoaded = false
            for (element in XMLPrefsRoot.entries) {
                element.getValues().list.clear()
            }
        }

        var commonsLoaded = false

        @JvmStatic
        fun loadCommons(context: Context) {
            if (commonsLoaded) return

            val folder = Tuils.getFolder()
            if (folder == null) {
                Tuils.sendOutput(Color.RED, context, R.string.tuinotfound_xmlprefs)
                return
            }

            commonsLoaded = true

            for (element in XMLPrefsRoot.values()) {
                val file = File(folder, element.path)
                if (!file.exists()) {
                    resetFile(file, element.name)
                }

                val o: Array<Any?>?
                try {
                    o = buildDocument(file, element.name)
                    if (o == null) {
                        Tuils.sendXMLParseError(context, element.path)
                        return
                    }
                } catch (e: SAXParseException) {
                    Tuils.sendXMLParseError(context, element.path, e)
                    continue
                } catch (e: Exception) {
                    Tuils.log(e)
                    return
                }

                var d = o[0] as Document
                var root = o[1] as Element?

                val enums = element.enums.toMutableList()
                val deleted = element.delete()
                var needToWrite = false

                if (root == null) {
                    resetFile(file, element.name)
                    try {
                        d = builder!!.parse(file)
                    } catch (e: Exception) {
                    }
                    root = d.getElementsByTagName(element.name).item(0) as Element?
                }
                
                if (root == null) continue

                val nodes = root.getElementsByTagName("*")

                for (count in 0 until nodes.length) {
                    val node = nodes.item(count)
                    val nn = node.nodeName

                    var value: String
                    try {
                        value = node.attributes.getNamedItem(VALUE_ATTRIBUTE).nodeValue
                    } catch (e: Exception) {
                        continue
                    }

                    var check = false
                    var en = 0
                    while (en < enums.size) {
                        val opt = enums[en]

                        if (opt.label() == nn) {
                            val s = enums.removeAt(en)

                            val iv = s.invalidValues()
                            if (iv != null) {
                                for (temp in iv) {
                                    if (temp == value) {
                                        value = opt.defaultValue()
                                        val em = node as Element
                                        em.setAttribute(VALUE_ATTRIBUTE, value)
                                        needToWrite = true
                                        break
                                    }
                                }
                            }

                            element.getValues().add(nn, value)
                            check = true
                            break
                        }
                        en++
                    }

                    if (!check) {
                        val index = Tuils.find(nn, deleted.toList())
                        if (index != -1) {
                            deleted[index] = null
                            val e = node as Element
                            root.removeChild(e)
                            needToWrite = true
                        }
                    }
                }

                if (enums.isEmpty()) {
                    if (needToWrite) writeTo(d, file)
                    continue
                }

                for (s in enums) {
                    val value = s.defaultValue()
                    val em = d.createElement(s.label())
                    em.setAttribute(VALUE_ATTRIBUTE, value)
                    root.appendChild(em)
                    element.getValues().add(s.label(), value)
                }

                writeTo(d, file)
            }
        }

        @Throws(Exception::class)
        @JvmStatic
        fun transform(s: String?, c: Class<*>): Any? {
            if (s == null) throw UnsupportedOperationException()

            if (c == Int::class.javaPrimitiveType || c == Int::class.java) return s.toInt()
            if (c == Color::class.java) return Color.parseColor(s)
            if (c == Boolean::class.javaPrimitiveType || c == Boolean::class.java) return s.toBoolean()
            if (c == String::class.java) return s
            if (c == Float::class.javaPrimitiveType || c == Float::class.java) return s.toFloat()
            if (c == Double::class.javaPrimitiveType || c == Double::class.java) return s.toDouble()
            if (c == File::class.java) {
                if (s.isEmpty()) return null
                val file = File(s)
                if (!file.exists()) throw UnsupportedOperationException()
                return file
            }

            return Tuils.getDefaultValue(c)
        }

        @JvmStatic
        fun getFloat(prefsSave: XMLPrefsSave): Float = get(Float::class.java, prefsSave) ?: 0f

        @JvmStatic
        fun getDouble(prefsSave: XMLPrefsSave): Double = get(Double::class.java, prefsSave) ?: 0.0

        @JvmStatic
        fun getInt(prefsSave: XMLPrefsSave): Int = get(Int::class.java, prefsSave) ?: 0

        @JvmStatic
        fun getBoolean(prefsSave: XMLPrefsSave): Boolean = get(Boolean::class.java, prefsSave) ?: false

        @JvmStatic
        fun getColor(prefsSave: XMLPrefsSave): Int {
            if (prefsSave.parent() == null) return Int.MAX_VALUE

            return try {
                transform(prefsSave.parent().getValues().get(prefsSave).value, Color::class.java) as Int
            } catch (e: Exception) {
                val def = prefsSave.defaultValue()
                if (def == null || def.isEmpty()) {
                    Int.MAX_VALUE
                } else {
                    try {
                        transform(def, Color::class.java) as Int
                    } catch (e1: Exception) {
                        Int.MAX_VALUE
                    }
                }
            }
        }

        @JvmStatic
        fun getString(prefsSave: XMLPrefsSave): String? = get(prefsSave)

        @JvmStatic
        fun <T> get(c: Class<T>, root: XMLPrefsRoot, s: String): T? {
            return try {
                transform(root.getValues().get(s).value, c) as T?
            } catch (e: Exception) {
                null
            }
        }

        @JvmStatic
        fun <T> get(c: Class<T>, prefsSave: XMLPrefsSave): T? {
            return try {
                transform(prefsSave.parent().getValues().get(prefsSave).value, c) as T?
            } catch (e: Exception) {
                Tuils.log(e)
                try {
                    transform(prefsSave.defaultValue(), c) as T?
                } catch (e1: Exception) {
                    Tuils.log(e1)
                    Tuils.getDefaultValue(c) as T?
                }
            }
        }

        @JvmStatic
        fun get(prefsSave: XMLPrefsSave): String? = get(String::class.java, prefsSave)

        @JvmStatic
        fun get(root: XMLPrefsRoot, s: String): String? = get(String::class.java, root, s)

        @JvmStatic
        fun wasChanged(save: XMLPrefsSave, allowLengthZero: Boolean): Boolean {
            val value = get(save) ?: return false
            return (allowLengthZero || value.isNotEmpty()) && value != save.defaultValue()
        }

        private val p1 = Pattern.compile(">")
        private val p3 = Pattern.compile("\n\n")
        private val p1s = ">${Tuils.NEWLINE}"
        private val p3s = Tuils.NEWLINE

        @JvmStatic
        fun fixNewlines(s: String): String {
            var res = p1.matcher(s).replaceAll(p1s)
            res = p3.matcher(res).replaceAll(p3s)
            return res
        }

        @JvmStatic
        @Throws(Exception::class)
        fun buildDocument(file: File, rootName: String?): Array<Any?>? {
            if (!file.exists()) {
                resetFile(file, rootName)
            }

            var d: Document
            try {
                d = builder!!.parse(file)
            } catch (e: Exception) {
                Tuils.log(e)
                val nOfBytes = Tuils.nOfBytes(file)
                if (nOfBytes == 0 && rootName != null) {
                    resetFile(file, rootName)
                    d = builder!!.parse(file)
                } else return null
            }

            val r = d.documentElement
            return arrayOf(d, r)
        }

        @JvmStatic
        fun writeTo(d: Document, f: File) {
            try {
                val transformerFactory = TransformerFactory.newInstance()
                val transformer = transformerFactory.newTransformer()
                val source = DOMSource(d)
                val writer = StringWriter()
                val result = StreamResult(writer)
                transformer.transform(source, result)

                val s = fixNewlines(writer.toString())
                val stream = FileOutputStream(f)
                stream.write(s.toByteArray())
                stream.flush()
                stream.close()
            } catch (e: Exception) {
                Tuils.log(e)
            }
        }

        @JvmStatic
        fun add(file: File, elementName: String, attributeNames: Array<String>, attributeValues: Array<String?>): String? {
            return try {
                val o = try {
                    buildDocument(file, null)
                } catch (e: Exception) {
                    Tuils.log(e)
                    return e.toString()
                } ?: return Tuils.EMPTYSTRING

                val d = o[0] as Document
                val root = o[1] as Element
                val element = d.createElement(elementName)
                for (c in attributeNames.indices) {
                    if (attributeValues[c] == null) continue
                    element.setAttribute(attributeNames[c], attributeValues[c])
                }
                root.appendChild(element)
                writeTo(d, file)
                null
            } catch (e: Exception) {
                Tuils.log(e)
                e.toString()
            }
        }

        @JvmStatic
        fun set(file: File, elementName: String, attributeNames: Array<String>, attributeValues: Array<String?>): String? {
            return set(file, elementName, null, null, attributeNames, attributeValues, true)
        }

        @JvmStatic
        fun set(
            file: File,
            elementName: String,
            thatHasThose: Array<String>?,
            forValues: Array<String>?,
            attributeNames: Array<String>,
            attributeValues: Array<String?>,
            addIfNotFound: Boolean
        ): String? {
            val values = Array(1) { attributeValues }
            return setMany(file, arrayOf(elementName), thatHasThose, forValues, attributeNames, values, addIfNotFound)
        }

        @JvmStatic
        fun setMany(file: File, elementNames: Array<String?>, attributeNames: Array<String>, attributeValues: Array<Array<String?>>): String? {
            return setMany(file, elementNames, null, null, attributeNames, attributeValues, true)
        }

        @JvmStatic
        fun setMany(
            file: File,
            elementNames: Array<String?>,
            thatHasThose: Array<String>?,
            forValues: Array<String>?,
            attributeNames: Array<String>,
            attributeValues: Array<Array<String?>>,
            addIfNotFound: Boolean
        ): String? {
            return try {
                val o = try {
                    buildDocument(file, null)
                } catch (e: Exception) {
                    Tuils.log(e)
                    return e.toString()
                } ?: return Tuils.EMPTYSTRING

                val d = o[0] as Document
                val root = o[1] as Element? ?: return Tuils.EMPTYSTRING

                var nFound = 0

                for (c in elementNames.indices) {
                    val currentName = elementNames[c] ?: continue
                    val nodes = root.getElementsByTagName(currentName)

                    var found = false
                    for (j in 0 until nodes.length) {
                        val n = nodes.item(j)
                        if (n.nodeType == Node.ELEMENT_NODE) {
                            val e = n as Element
                            if (!checkAttributes(e, thatHasThose, forValues, false)) {
                                continue
                            }

                            nFound++
                            for (a in attributeNames.indices) {
                                e.setAttribute(attributeNames[a], attributeValues[c][a])
                            }
                            elementNames[c] = null
                            found = true
                            break
                        }
                    }
                }

                if (nFound < elementNames.size) {
                    for (count in elementNames.indices) {
                        val currentName = elementNames[count]
                        if (currentName == null || currentName.isEmpty()) continue
                        if (!addIfNotFound) continue

                        val element = d.createElement(currentName)
                        for (c in attributeNames.indices) {
                            if (attributeValues[count][c] == null) continue
                            element.setAttribute(attributeNames[c], attributeValues[count][c])
                        }
                        root.appendChild(element)
                    }
                }

                writeTo(d, file)
                if (nFound == 0) Tuils.EMPTYSTRING else null
            } catch (e: Exception) {
                Tuils.log(e)
                Tuils.toFile(e)
                e.toString()
            }
        }

        @JvmStatic
        fun removeNode(file: File, nodeName: String): String? = removeNode(file, nodeName, null, null)

        @JvmStatic
        fun removeNode(file: File, nodeName: String, thatHasThose: Array<String>?, forValues: Array<String>?): String? {
            return try {
                val o = try {
                    buildDocument(file, null)
                } catch (e: Exception) {
                    return e.toString()
                } ?: return Tuils.EMPTYSTRING

                val d = o[0] as Document
                val root = o[1] as Element
                val n = findNode(root, nodeName, thatHasThose, forValues) ?: return Tuils.EMPTYSTRING
                root.removeChild(n)
                writeTo(d, file)
                null
            } catch (e: Exception) {
                e.toString()
            }
        }

        @JvmStatic
        fun removeNode(file: File, thatHasThose: Array<String>?, forValues: Array<String>?): String? {
            return removeNode(file, thatHasThose, forValues, alsoNotFound = false, all = false)
        }

        @JvmStatic
        fun removeNode(file: File, thatHasThose: Array<String>?, forValues: Array<String>?, alsoNotFound: Boolean, all: Boolean): String? {
            return try {
                val o = try {
                    buildDocument(file, null)
                } catch (e: Exception) {
                    return e.toString()
                } ?: return Tuils.EMPTYSTRING

                val d = o[0] as Document
                val root = o[1] as Element
                val list = root.getElementsByTagName("*")
                var check = false

                for (c in 0 until list.length) {
                    val n = list.item(c)
                    if (n !is Element) continue
                    if (checkAttributes(n, thatHasThose, forValues, alsoNotFound)) {
                        check = true
                        root.removeChild(n)
                        if (!all) break
                    }
                }

                writeTo(d, file)
                if (check) null else Tuils.EMPTYSTRING
            } catch (e: Exception) {
                e.toString()
            }
        }

        @JvmStatic
        fun findNode(file: File, nodeName: String): Node? = findNode(file, nodeName, null, null)

        @JvmStatic
        fun findNode(file: File, nodeName: String, thatHasThose: Array<String>?, forValues: Array<String>?): Node? {
            return try {
                val o = try {
                    buildDocument(file, null)
                } catch (e: Exception) {
                    return null
                } ?: return null

                val root = o[1] as Element
                findNode(root, nodeName, thatHasThose, forValues)
            } catch (e: Exception) {
                null
            }
        }

        @JvmStatic
        fun findNode(root: Element, nodeName: String, thatHasThose: Array<String>?, forValues: Array<String>?): Node? {
            val nodes = root.getElementsByTagName(nodeName)
            for (j in 0 until nodes.length) {
                if (checkAttributes(nodes.item(j) as Element, thatHasThose, forValues, false)) return nodes.item(j)
            }
            return null
        }

        @JvmStatic
        fun findNode(root: Element, nodeName: String): Node? = findNode(root, nodeName, null, null)

        @JvmStatic
        fun findNodes(root: Element, nodeName: String?, thatHasThose: Array<String>?, forValue: Array<String>?): List<Node> {
            val nodes = root.getElementsByTagName(nodeName ?: "*")
            val nodeList = mutableListOf<Node>()
            for (c in 0 until nodes.length) {
                val n = nodes.item(c)
                if (n !is Element) continue
                if (checkAttributes(n, thatHasThose, forValue, false)) {
                    nodeList.add(n)
                }
            }
            return nodeList
        }

        @JvmStatic
        fun findNodes(root: Element, thatHasThose: Array<String>?, forValue: Array<String>?): List<Node> = findNodes(root, null, thatHasThose, forValue)

        @JvmStatic
        fun attrValue(file: File, nodeName: String, attrName: String): String? = attrValue(file, nodeName, null, null, attrName)

        @JvmStatic
        fun attrValue(file: File, nodeName: String, thatHasThose: Array<String>?, forValues: Array<String>?, attrName: String): String? {
            val vs = attrValues(file, nodeName, thatHasThose, forValues, arrayOf(attrName))
            return if (vs != null && vs.isNotEmpty()) vs[0] else null
        }

        @JvmStatic
        fun attrValues(file: File, nodeName: String, attrNames: Array<String>): Array<String?>? = attrValues(file, nodeName, null, null, attrNames)

        @JvmStatic
        fun attrValues(file: File, nodeName: String, thatHasThose: Array<String>?, forValues: Array<String>?, attrNames: Array<String>): Array<String?>? {
            try {
                val o = try {
                    buildDocument(file, null)
                } catch (e: Exception) {
                    return null
                } ?: return null

                val root = o[1] as Element
                val nodes = root.getElementsByTagName(nodeName)

                for (count in 0 until nodes.length) {
                    val node = nodes.item(count)
                    val e = node as Element
                    if (!checkAttributes(e, thatHasThose, forValues, false)) continue

                    val values = arrayOfNulls<String>(attrNames.size)
                    for (c in attrNames.indices) values[c] = e.getAttribute(attrNames[c])
                    return values
                }
            } catch (e: Exception) {
            }
            return null
        }

        private fun checkAttributes(e: Element, thatHasThose: Array<String>?, forValues: Array<String>?, alsoIfAttributeNotFound: Boolean): Boolean {
            if (thatHasThose != null && forValues != null && thatHasThose.size == forValues.size) {
                for (a in thatHasThose.indices) {
                    if (!e.hasAttribute(thatHasThose[a])) return alsoIfAttributeNotFound
                    if (forValues[a] != e.getAttribute(thatHasThose[a])) return false
                }
            }
            return true
        }

        @JvmStatic
        fun resetFile(f: File, name: String?): Boolean {
            return try {
                if (f.exists()) f.delete()
                val stream = FileOutputStream(f)
                stream.write(XML_DEFAULT.toByteArray())
                stream.write(("<$name>\n").toByteArray())
                stream.write(("</$name>\n").toByteArray())
                stream.flush()
                stream.close()
                true
            } catch (e: Exception) {
                false
            }
        }

        @JvmStatic
        fun getStringAttribute(e: Element, attribute: String): String? = if (e.hasAttribute(attribute)) e.getAttribute(attribute) else null

        @JvmStatic
        fun getLongAttribute(e: Element, attribute: String): Long {
            val value = getStringAttribute(e, attribute)
            return try {
                value?.toLong() ?: -1L
            } catch (ex: Exception) {
                -1L
            }
        }

        @JvmStatic
        fun getBooleanAttribute(e: Element, attribute: String): Boolean {
            val s = getStringAttribute(e, attribute)
            return s != null && s.toBoolean()
        }

        @JvmStatic
        fun getIntAttribute(e: Element, attribute: String): Int {
            return try {
                getStringAttribute(e, attribute)?.toInt() ?: -1
            } catch (ex: Exception) {
                -1
            }
        }

        @JvmStatic
        fun getFloatAttribute(e: Element, attribute: String): Float {
            return try {
                getStringAttribute(e, attribute)?.toFloat() ?: -1f
            } catch (ex: Exception) {
                -1f
            }
        }
    }
}
