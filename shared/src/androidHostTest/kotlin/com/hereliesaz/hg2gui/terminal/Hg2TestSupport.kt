package com.hereliesaz.hg2gui.terminal

import java.io.File
import java.nio.file.Files

/** Runs [block] with a fresh temp directory that is deleted (symlink-safely) afterwards. */
internal inline fun <T> withHg2TempDir(prefix: String = "hg2gui-test", block: (File) -> T): T {
    val dir = Files.createTempDirectory(prefix).toFile()
    try {
        return block(dir)
    } finally {
        FileTrees.deleteRecursivelyNoFollow(dir)
    }
}

/** Creates [relative] under this directory with [text], making parents as needed. */
internal fun File.write(relative: String, text: String): File =
    File(this, relative).also {
        it.parentFile?.mkdirs()
        it.writeText(text)
    }

/** Creates [relative] under this directory with raw [bytes], making parents as needed. */
internal fun File.writeBytesAt(relative: String, bytes: ByteArray): File =
    File(this, relative).also {
        it.parentFile?.mkdirs()
        it.writeBytes(bytes)
    }
