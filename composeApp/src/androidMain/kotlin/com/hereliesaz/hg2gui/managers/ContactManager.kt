package com.hereliesaz.hg2gui.managers

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.hereliesaz.hg2gui.PermissionCodes
import com.hereliesaz.hg2gui.util.StoppableThread
import com.hereliesaz.hg2gui.util.Utils
import it.andreuzzi.comparestring2.StringableObject
import java.util.Collections

/**
 * Manages the retrieval and caching of device contacts.
 */
class ContactManager(private val context: Context) {

    private var contacts: MutableList<Contact>? = null // Cached list of contacts
    private val receiver: BroadcastReceiver

    init {
        // Initial load if permission is already granted
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            refreshContacts(context)
        }

        // Register receiver for refresh requests (e.g. after permission grant)
        val filter = IntentFilter(ACTION_REFRESH)

        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == ACTION_REFRESH) {
                    refreshContacts(context)
                }
            }
        }

        LocalBroadcastManager.getInstance(context.applicationContext).registerReceiver(receiver, filter)
    }

    fun destroy(context: Context) {
        LocalBroadcastManager.getInstance(context.applicationContext).unregisterReceiver(receiver)
    }

    /**
     * Re-queries the contacts provider.
     * Runs asynchronously.
     */
    fun refreshContacts(context: Context) {
        // Request permission if missing
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(context as Activity, arrayOf(Manifest.permission.READ_CONTACTS), PermissionCodes.COMMAND_SUGGESTION_REQUEST_PERMISSION)
            return
        }

        object : StoppableThread() {
            override fun run() {
                super.run()

                if (contacts == null) {
                    contacts = ArrayList()
                } else {
                    contacts!!.clear()
                }

                val currentContacts = contacts!!

                // Query for Name, ID, Number, and Primary flag
                val phones = context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(
                        ContactsContract.Contacts.DISPLAY_NAME,
                        ContactsContract.Data.CONTACT_ID,
                        ContactsContract.CommonDataKinds.Phone.NUMBER,
                        ContactsContract.Data.IS_SUPER_PRIMARY
                    ),
                    null,
                    null,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
                )

                if (phones != null) {
                    var lastId = -1
                    var lastNumbers = ArrayList<String>()
                    var nrml = ArrayList<String>() // Normalized numbers to check for duplicates
                    var defaultNumber = 0
                    var name: String? = null
                    var number: String?
                    var id: Int
                    var prim: Int

                    while (phones.moveToNext()) {
                        val contactIdIndex = phones.getColumnIndex(ContactsContract.Data.CONTACT_ID)
                        val numberIndex = phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        val superPrimaryIndex = phones.getColumnIndex(ContactsContract.Data.IS_SUPER_PRIMARY)
                        val displayNameIndex = phones.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)

                        if (contactIdIndex == -1 || numberIndex == -1 || superPrimaryIndex == -1 || displayNameIndex == -1) continue

                        id = phones.getInt(contactIdIndex)
                        number = phones.getString(numberIndex)

                        // Check if this is the primary number
                        prim = phones.getInt(superPrimaryIndex)
                        if (prim > 0) {
                            defaultNumber = lastNumbers.size
                        }

                        if (number == null || number.isEmpty()) continue

                        if (phones.isFirst) {
                            lastId = id
                            name = phones.getString(displayNameIndex)
                        } else if (id != lastId || phones.isLast) {
                            // New contact ID encountered, verify and add previous one
                            lastId = id

                            if (name != null) {
                                currentContacts.add(Contact(name, lastNumbers, defaultNumber))
                            }

                            // Reset buffers for new contact
                            lastNumbers = ArrayList()
                            nrml = ArrayList()
                            name = null
                            defaultNumber = 0

                            name = phones.getString(displayNameIndex)
                        }

                        // Normalize number to avoid duplicates (e.g. spacing differences)
                        val normalized = number.replace(Utils.SPACE.toRegex(), Utils.EMPTYSTRING)
                        if (!nrml.contains(normalized)) {
                            nrml.add(normalized)
                            lastNumbers.add(number)
                        }

                        // Handle the very last contact in the cursor
                        if (name != null && phones.isLast) {
                            currentContacts.add(Contact(name, lastNumbers, defaultNumber))
                        }
                    }
                    phones.close()
                }

                // Cleanup empty contacts
                val iterator = currentContacts.iterator()
                while (iterator.hasNext()) {
                    val c = iterator.next()
                    if (c.numbers.isEmpty()) iterator.remove()
                }

                Collections.sort(currentContacts)
            }
        }.start()
    }

    fun listNames(): List<String> {
        if (contacts == null || contacts!!.isEmpty()) refreshContacts(context)

        val names = ArrayList<String>()
        contacts?.let {
            for (c in it) names.add(c.name)
        }
        return names
    }

    fun getContacts(): List<Contact> {
        if (contacts == null || contacts!!.isEmpty()) refreshContacts(context)

        return ArrayList(contacts ?: emptyList())
    }

    fun listNamesAndNumbers(): List<String> {
        if (contacts == null || contacts!!.isEmpty()) refreshContacts(context)

        val c = ArrayList<String>()
        contacts?.let {
            for (cnt in it) {
                val b = StringBuilder()
                b.append(cnt.name)

                for (n in cnt.numbers) {
                    b.append(Utils.NEWLINE)
                    b.append("\t")
                    b.append(n)
                }

                c.add(b.toString())
            }
        }

        return c
    }

    /**
     * Retrieves detailed info about a contact by phone number.
     * @return Array of strings containing details.
     */
    fun about(phone: String): Array<String?>? {
        var mCursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.CONTACT_ID),
            ContactsContract.CommonDataKinds.Phone.NUMBER + " = ?",
            arrayOf(phone),
            null
        )

        if (mCursor == null || mCursor.count == 0) {
            mCursor?.close()
            return null
        }
        val about = arrayOfNulls<String>(SIZE)

        mCursor.moveToNext()
        val contactIdCol = mCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
        if (contactIdCol == -1) {
            mCursor.close()
            return null
        }
        val id = mCursor.getString(contactIdCol)
        about[CONTACT_ID] = id
        mCursor.close()

        // Query details
        mCursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.TIMES_CONTACTED,
                ContactsContract.CommonDataKinds.Phone.LAST_TIME_CONTACTED,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
            arrayOf(id),
            null
        )

        if (mCursor == null || mCursor.count == 0) {
            mCursor?.close()
            return null
        }
        mCursor.moveToNext()

        val displayNameCol = mCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
        val timesContactedCol = mCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TIMES_CONTACTED)
        val lastTimeContactedCol = mCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LAST_TIME_CONTACTED)
        val numberCol = mCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

        if (displayNameCol == -1 || timesContactedCol == -1 || lastTimeContactedCol == -1 || numberCol == -1) {
            mCursor.close()
            return null
        }

        about[NAME] = mCursor.getString(displayNameCol)
        about[NUMBERS] = Utils.EMPTYSTRING

        var timesContacted = -1
        var lastContacted = Long.MAX_VALUE
        do {
            val tempT = mCursor.getInt(timesContactedCol)
            val tempL = mCursor.getLong(lastTimeContactedCol)

            timesContacted = if (tempT > timesContacted) tempT else timesContacted
            if (tempL > 0) lastContacted = if (tempL < lastContacted) tempL else lastContacted

            val n = mCursor.getString(numberCol)
            about[NUMBERS] = (if (about[NUMBERS]!!.isNotEmpty()) about[NUMBERS] + Utils.NEWLINE else Utils.EMPTYSTRING) + n
        } while (mCursor.moveToNext())
        mCursor.close()

        about[TIME_CONTACTED] = timesContacted.toString()

        // Format last contacted time
        if (lastContacted != Long.MAX_VALUE) {
            val difference = System.currentTimeMillis() - lastContacted
            var sc = difference / 1000
            if (sc < 60) {
                about[LAST_CONTACTED] = "sec: $lastContacted"
            } else {
                var ms = (sc / 60).toInt()
                sc = (ms % 60).toLong()
                if (ms < 60) {
                    about[LAST_CONTACTED] = "min: $ms, sec: $sc"
                } else {
                    var h = ms / 60
                    ms %= 60
                    if (h < 24) {
                        about[LAST_CONTACTED] = "h: $h, min: $ms, sec: $sc"
                    } else {
                        val days = h / 24
                        h %= 24
                        about[LAST_CONTACTED] = "d: $days, h: $h, min: $ms, sec: $sc"
                    }
                }
            }
        }

        return about
    }

    fun findNumber(name: String): String? {
        if (contacts == null) refreshContacts(context)

        contacts?.let {
            for (c in it) {
                if (c.name.equals(name, ignoreCase = true)) {
                    if (c.numbers.isNotEmpty()) return c.numbers[0]
                }
            }
        }

        return null
    }

    fun delete(phone: String): Boolean {
        val uri = fromPhone(phone) ?: return false
        return context.contentResolver.delete(uri, null, null) > 0
    }

    /**
     * Resolves a lookup URI from a phone number.
     */
    fun fromPhone(phone: String): Uri? {
        var mCursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME),
            ContactsContract.CommonDataKinds.Phone.NUMBER + " = ?",
            arrayOf(phone),
            null
        )

        if (mCursor == null || mCursor.count == 0) {
            mCursor?.close()
            return null
        }
        mCursor.moveToNext()
        val displayNameIndex = mCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
        if (displayNameIndex == -1) {
            mCursor.close()
            return null
        }
        val name = mCursor.getString(displayNameIndex)
        mCursor.close()

        mCursor = context.contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(ContactsContract.Contacts.LOOKUP_KEY, ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME),
            ContactsContract.Contacts.DISPLAY_NAME + " = ?",
            arrayOf(name),
            null
        )

        if (mCursor == null || mCursor.count == 0) {
            mCursor?.close()
            return null
        }
        mCursor.moveToNext()
        val lookupKeyIndex = mCursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY)
        val idIndex = mCursor.getColumnIndex(ContactsContract.Contacts._ID)
        if (lookupKeyIndex == -1 || idIndex == -1) {
            mCursor.close()
            return null
        }
        val mCurrentLookupKey = mCursor.getString(lookupKeyIndex)
        val mCurrentId = mCursor.getLong(idIndex)
        mCursor.close()

        return ContactsContract.Contacts.getLookupUri(mCurrentId, mCurrentLookupKey)
    }

    /**
     * DTO for a single contact.
     */
    class Contact(val name: String, val numbers: List<String>, defNumber: Int) : Comparable<Contact>, StringableObject {
        val lowercaseName: String = name.lowercase()
        var selectedNumber: Int = 0
            set(value) {
                field = if (value >= numbers.size) 0 else value
            }

        init {
            selectedNumber = defNumber
        }

        override fun getLowercaseString(): String = lowercaseName

        override fun getString(): String = name

        override fun compareTo(other: Contact): Int {
            // Sort by first letter
            val tf = name.uppercase()[0]
            val of = other.name.uppercase()[0]
            return tf - of
        }
    }

    companion object {
        // Action to trigger a refresh of the contacts list
        const val ACTION_REFRESH = "com.hereliesaz.hg2gui.refresh_contacts"

        // Indexes for the 'about' array
        const val NAME = 0
        const val NUMBERS = 1
        const val TIME_CONTACTED = 2
        const val LAST_CONTACTED = 3
        const val CONTACT_ID = 4
        const val SIZE = CONTACT_ID + 1
    }
}
