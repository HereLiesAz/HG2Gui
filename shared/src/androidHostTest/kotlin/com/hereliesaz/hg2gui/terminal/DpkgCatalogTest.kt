package com.hereliesaz.hg2gui.terminal

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DpkgCatalogTest {
    @Test
    fun dependencyView_resolvesAlternativesProvidersAndReverseImpact() {
        val prefix = Files.createTempDirectory("hg2gui-dpkg-catalog").toFile()
        try {
            val status = File(prefix, "var/lib/dpkg/status").apply {
                parentFile.mkdirs()
                writeText(
                    """
                    Package: base
                    Status: install ok installed
                    Version: 1
                    Provides: virtual-base

                    Package: mid
                    Status: install ok installed
                    Version: 1
                    Depends: missing | virtual-base

                    Package: app
                    Status: install ok installed
                    Version: 1
                    Pre-Depends: mid (>= 1)

                    Package: plugin
                    Status: install ok installed
                    Version: 1
                    Depends: app
                    """.trimIndent()
                )
            }
            assertTrue(status.isFile)

            val app = DpkgCatalog.dependencyView(prefix, "app")
            assertEquals(listOf("mid"), app.directDependencies)
            assertEquals(listOf("base", "mid"), app.dependencyClosure)
            assertEquals(listOf("plugin"), app.directDependents)
            assertEquals(listOf("plugin"), app.dependentClosure)

            val base = DpkgCatalog.dependencyView(prefix, "base")
            assertEquals(listOf("mid"), base.directDependents)
            assertEquals(listOf("app", "mid", "plugin"), base.dependentClosure)
        } finally {
            prefix.deleteRecursively()
        }
    }

    @Test
    fun dependencyView_returnsEmptyViewForUnknownPackage() {
        val prefix = Files.createTempDirectory("hg2gui-dpkg-catalog-empty").toFile()
        try {
            val view = DpkgCatalog.dependencyView(prefix, "nope")
            assertTrue(view.directDependencies.isEmpty())
            assertTrue(view.dependencyClosure.isEmpty())
            assertTrue(view.directDependents.isEmpty())
            assertTrue(view.dependentClosure.isEmpty())
        } finally {
            prefix.deleteRecursively()
        }
    }
}
