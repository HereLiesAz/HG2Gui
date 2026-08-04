package com.hereliesaz.hg2gui.managers

import android.content.Context
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import androidx.core.content.edit
import androidx.core.net.toUri
import com.hereliesaz.hg2gui.R
import com.hereliesaz.hg2gui.managers.xml.XMLPrefsManager
import com.hereliesaz.hg2gui.managers.xml.options.Theme
import com.hereliesaz.hg2gui.tuils.Tuils
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.regex.Pattern
import kotlin.concurrent.thread

/**
 * Created by francescoandreuzzi on 26/03/2018.
 */
object ChangelogManager {
    private const val PREFS_NAME = "changelogPrefs"

    @JvmStatic
    @JvmOverloads
    fun printLog(context: Context, client: OkHttpClient, force: Boolean = false) {
        val originalUrl = "https://pastebin.com/n5AYHd26"
        val url = if (!originalUrl.contains("raw")) {
            originalUrl.replace(".com/", ".com/raw/")
        } else {
            originalUrl
        }

        val preferences = context.getSharedPreferences(PREFS_NAME, 0)

        // if true, it's the first time the user opens the app. no need to show changelog
        if (!force && MessagesManager.isShowingFirstTimeTutorial(context)) {
            preferences.edit {
                putBoolean(url, true)
            }
            return
        }

        val maxLength = 200

        if (force || !preferences.getBoolean(url, false)) {
            thread {
                if (!Tuils.hasInternetAccess()) {
                    if (force) Tuils.sendOutput(context, R.string.no_internet)
                    return@thread
                }

                val newlinePattern = Pattern.compile("^-", Pattern.MULTILINE)

                try {
                    val builder = Request.Builder()
                        .url(url)
                        .get()

                    val response = client.newCall(builder.build()).execute()

                    if ((!response.isSuccessful) || (response.code == 304)) {
                        if (force) {
                            Tuils.sendOutput(
                                context,
                                context.getString(R.string.internet_error) + Tuils.SPACE + response.code,
                            )
                        }
                        return@thread
                    }

                    val header = "Changelog v6.15"

                    var log = response.body.string()
                    log = newlinePattern.matcher(log).replaceAll(Tuils.DOUBLE_SPACE + "-")

                    val cut = !force && log.length >= maxLength
                    if (cut) {
                        log = log.substring(0, maxLength) + "..."
                    }

                    Tuils.sendOutput(context, header + Tuils.NEWLINE + log)

                    if (cut) {
                        val sp = SpannableString("Click here to see the full changelog")
                        sp.setSpan(
                            ForegroundColorSpan(XMLPrefsManager.getColor(Theme.output_color)),
                            0,
                            sp.length,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                        )
                        sp.setSpan(
                            ForegroundColorSpan(XMLPrefsManager.getColor(Theme.link_color)),
                            6,
                            10,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                        )
                        Tuils.sendOutput(
                            context,
                            sp,
                            TerminalManager.CATEGORY_NO_COLOR,
                            url.toUri()
                        )
                    }

                    preferences.edit {
                        putBoolean(url, true)
                    }
                } catch (e: Exception) {
                    Tuils.sendOutput(context, e.toString())
                    Tuils.log(e)
                }
            }
        }
    }
}
