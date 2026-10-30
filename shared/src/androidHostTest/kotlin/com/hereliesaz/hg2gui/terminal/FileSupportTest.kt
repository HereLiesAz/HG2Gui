package com.hereliesaz.hg2gui.terminal

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FileSupportTest {
    private val old = "/data/data/com.termux/files/usr"
    private val new = "/data/user/0/com.hereliesaz.hg2gui/files/usr"

    @Test
    fun plainBase64_matchesJavaEncoderForEveryRemainder() {
        for (length in 0..40) {
            val bytes = ByteArray(length) { (it * 37 + length).toByte() }
            val expected = java.util.Base64.getEncoder().encodeToString(bytes)
            assertEquals(expected, PlainBase64.encode(bytes))
            assertArrayEquals(bytes, PlainBase64.decode(expected))
        }
    }

    @Test
    fun plainBase64_decodesUnpaddedAndWrappedInput() {
        val text = "héllo wörld/+"
        val wrapped = PlainBase64.encodeString(text).chunked(4).joinToString("\n")
        assertEquals(text, PlainBase64.decodeString(wrapped))
        assertEquals(text, PlainBase64.decodeString(PlainBase64.encodeString(text).trimEnd('=')))
    }

    @Test(expected = IllegalArgumentException::class)
    fun plainBase64_rejectsIllegalCharacters() {
        PlainBase64.decode("ab!d")
    }

    @Test
    fun atomicFiles_replacesContentAndLeavesNoTempFile() {
        withHg2TempDir { dir ->
            val file = File(dir, "nested/state.tsv")
            AtomicFiles.write(file, "one".toByteArray())
            AtomicFiles.write(file, "two".toByteArray())
            assertEquals("two", file.readText())
            assertEquals(listOf("state.tsv"), file.parentFile.list()!!.toList())
        }
    }

    @Test
    fun prefixRewriter_keepsNonUtf8BytesIntact() {
        // 0xE9 is Latin-1 "e acute" and is not valid UTF-8 on its own; a decode/encode round trip
        // would turn it into U+FFFD (EF BF BD).
        val content = byteArrayOf('#'.code.toByte(), '!'.code.toByte()) +
            "$old/bin/sh\n# caf".toByteArray() + byteArrayOf(0xE9.toByte()) + "\n".toByteArray()
        val rewritten = PrefixRewriter.replaceInBytes(content, old, new)!!
        val expected = byteArrayOf('#'.code.toByte(), '!'.code.toByte()) +
            "$new/bin/sh\n# caf".toByteArray() + byteArrayOf(0xE9.toByte()) + "\n".toByteArray()
        assertArrayEquals(expected, rewritten)
    }

    @Test
    fun prefixRewriter_returnsNullWhenPrefixAbsent() {
        assertNull(PrefixRewriter.replaceInBytes("nothing here".toByteArray(), old, new))
        assertNull(PrefixRewriter.replaceInBytes(ByteArray(0), old, new))
    }

    @Test
    fun prefixRewriter_replacesEveryOccurrence() {
        val text = "a $old/x b $old/y"
        assertEquals("a $new/x b $new/y", String(PrefixRewriter.replaceInBytes(text.toByteArray(), old, new)!!))
    }

    @Test
    fun prefixRewriter_rewriteFile_skipsBinaryAndKeepsLatin1Text() {
        withHg2TempDir { dir ->
            val binary = dir.writeBytesAt("elf", byteArrayOf(0x7f, 'E'.code.toByte(), 0) + old.toByteArray())
            assertFalse(PrefixRewriter.rewriteFile(binary, old, new))
            assertTrue(old.toByteArray().all { b -> binary.readBytes().contains(b) })

            val latin1 = dir.writeBytesAt("script", "path=$old\nname=".toByteArray() + byteArrayOf(0xFC.toByte()))
            assertTrue(PrefixRewriter.rewriteFile(latin1, old, new))
            assertArrayEquals("path=$new\nname=".toByteArray() + byteArrayOf(0xFC.toByte()), latin1.readBytes())
        }
    }

    @Test
    fun prefixRewriter_multiReplacementAppliesInOrder() {
        withHg2TempDir { dir ->
            val md5 = dir.write("md5sums", "abc  data/data/com.termux/files/usr/bin/x\n")
            assertTrue(PrefixRewriter.rewriteFile(md5, listOf("data/data/com.termux/files/usr/" to "", old to "")))
            assertEquals("abc  bin/x\n", md5.readText())
        }
    }

    @Test
    fun fileTrees_deleteDoesNotFollowSymlinksIntoOtherDirectories() {
        withHg2TempDir { dir ->
            val outside = File(dir, "outside").apply { mkdirs() }
            val precious = outside.write("keep.txt", "keep")
            val tree = File(dir, "tree").apply { mkdirs() }
            tree.write("a/b.txt", "b")
            Files.createSymbolicLink(File(tree, "link").toPath(), outside.toPath())

            assertTrue(FileTrees.deleteRecursivelyNoFollow(tree))
            assertFalse(tree.exists())
            assertTrue("symlink target must survive", precious.isFile)
        }
    }

    @Test
    fun fileTrees_sizeCountsRegularFilesOnly() {
        withHg2TempDir { dir ->
            val tree = File(dir, "t").apply { mkdirs() }
            tree.write("a", "12345")
            tree.write("sub/b", "123")
            Files.createSymbolicLink(File(tree, "l").toPath(), File(tree, "a").toPath())
            assertEquals(8L, FileTrees.sizeNoFollow(tree))
            assertEquals(0L, FileTrees.sizeNoFollow(File(dir, "missing")))
        }
    }
}
