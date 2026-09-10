package com.hereliesaz.hg2gui.ui.guide

/** Stable HG2Gui concepts that deserve Guide treatment independent of the historical command book. */
object GuideCanonicalConcepts {
    private const val LABEL = "Appendix HG2"
    private const val TITLE = "Authority, Packages, and Other Ways to Regret Nothing"
    private const val INTRO =
        "A terminal traditionally gives a program whatever authority the shell already has, which is efficient in the same way that leaving every door unlocked is efficient. HG2Gui prefers to know which door, whose key, and whether anyone actually meant to open it."

    val chapter = GuideChapter(
        label = LABEL,
        title = TITLE,
        intro = INTRO,
        entries = listOf(
            GuideEntry(
                cmd = "hg2package",
                full = "HG2Gui package lifecycle",
                blurb = "Manages HG2Gui's runtime policy for installed packages without pretending to be their package manager. hg2package can disable or enable a package, preview and reset managed runtime state, create restore points, and place execution behind isolation. Installation still belongs to pkg, pip, pipx, npm, or gem. Bureaucracy is most useful when each department knows what it is not allowed to claim.",
                chapterLabel = LABEL,
                chapterTitle = TITLE,
                chapterIntro = INTRO
            ),
            GuideEntry(
                cmd = "hg2package isolate",
                full = "isolate an installed package",
                blurb = "Marks a package for private-root execution. Its next run receives a private PRoot filesystem and private HOME/XDG state, and common adb/root tools are stripped or masked. The isolation audit records observed sandbox changes and sampled runtime activity afterward. It is a boundary, not a magical force field: the sampler is not kernel audit, and the Guide refuses to promote adjectives into guarantees merely because they look good on a brochure.",
                chapterLabel = LABEL,
                chapterTitle = TITLE,
                chapterIntro = INTRO
            ),
            GuideEntry(
                cmd = "hg2auth",
                full = "HG2Gui execution authority",
                blurb = "Requests authority explicitly. Ordinary commands remain at app authority; adb and root operations require their own path and foreground approval. A package does not inherit elevated authority merely because HG2Gui can obtain it. This disappoints software that believes proximity to power is a constitutional right, which is most software once it has met a shell.",
                chapterLabel = LABEL,
                chapterTitle = TITLE,
                chapterIntro = INTRO
            )
        )
    )
}

object GuideCatalog {
    val chapters: List<GuideChapter> = GuideBook.chapters + GuideCanonicalConcepts.chapter
    val entries: List<GuideEntry> = chapters.flatMap { it.entries }
    val commands: Set<String> = entries.map { it.cmd }.toSet()
}
