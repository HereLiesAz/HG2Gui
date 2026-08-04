package com.hereliesaz.hg2gui.tuils

import java.io.*
import java.net.*
import java.util.*

/**
 * Created by francescoandreuzzi on 28/09/2017.
 */
object NetworkUtils {

    /**
     * Convert byte array to hex string
     * @param bytes
     * @return
     */
    fun bytesToHex(bytes: ByteArray): String {
        val sbuf = StringBuilder()
        for (b in bytes) {
            val intVal = b.toInt() and 0xff
            if (intVal < 0x10) sbuf.append("0")
            sbuf.append(Integer.toHexString(intVal).uppercase(Locale.getDefault()))
        }
        return sbuf.toString()
    }

    /**
     * Get utf8 byte array.
     * @param str
     * @return array of NULL if error was found
     */
    fun getUTF8Bytes(str: String): ByteArray? {
        return try {
            str.toByteArray(Charsets.UTF_8)
        } catch (ex: Exception) {
            null
        }
    }

    /**
     * Load UTF8withBOM or any ansi text file.
     * @param filename
     * @return
     * @throws IOException
     */
    @Throws(IOException::class)
    fun loadFileAsString(filename: String): String {
        val BUFLEN = 1024
        val inputStream = BufferedInputStream(FileInputStream(filename), BUFLEN)
        return try {
            val baos = ByteArrayOutputStream(BUFLEN)
            val bytes = ByteArray(BUFLEN)
            var isUTF8 = false
            var read: Int
            var count = 0
            while (inputStream.read(bytes).also { read = it } != -1) {
                if (count == 0 && read >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
                    isUTF8 = true
                    baos.write(bytes, 3, read - 3) // drop UTF8 bom marker
                } else {
                    baos.write(bytes, 0, read)
                }
                count += read
            }
            if (isUTF8) baos.toString("UTF-8") else baos.toString()
        } finally {
            try {
                inputStream.close()
            } catch (ex: Exception) {
                // ignore
            }
        }
    }

    /**
     * Returns MAC address of the given interface name.
     * @param interfaceName eth0, wlan0 or NULL=use first interface
     * @return mac address or empty string
     */
    fun getMACAddress(interfaceName: String?): String {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                if (interfaceName != null) {
                    if (!intf.name.equals(interfaceName, ignoreCase = true)) continue
                }
                val mac = intf.hardwareAddress ?: return ""
                val buf = StringBuilder()
                for (b in mac) {
                    buf.append(String.format("%02X:", b))
                }
                if (buf.isNotEmpty()) buf.deleteCharAt(buf.length - 1)
                return buf.toString()
            }
        } catch (ex: Exception) {
            // for now eat exceptions
        }
        return ""
    }

    /**
     * Get IP address from first non-localhost interface
     * @param useIPv4 true=return ipv4, false=return ipv6
     * @return address or empty string
     */
    fun getIPAddress(useIPv4: Boolean): String {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress) {
                        val sAddr = addr.hostAddress ?: continue
                        val isIPv4 = sAddr.indexOf(':') < 0

                        if (useIPv4) {
                            if (isIPv4) return sAddr
                        } else {
                            if (!isIPv4) {
                                val delim = sAddr.indexOf('%') // drop ip6 zone suffix
                                return if (delim < 0) {
                                    sAddr.uppercase(Locale.getDefault())
                                } else {
                                    sAddr.substring(0, delim).uppercase(Locale.getDefault())
                                }
                            }
                        }
                    }
                }
            }
        } catch (ex: Exception) {
            // for now eat exceptions
        }
        return ""
    }
}
