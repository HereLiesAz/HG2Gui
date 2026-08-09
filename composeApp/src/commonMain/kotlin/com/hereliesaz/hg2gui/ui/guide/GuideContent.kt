package com.hereliesaz.hg2gui.ui.guide

/**
 * One glossary entry: a real command paired with an invented, tone-not-fiction definition -
 * the Guide adds flavor, never a false claim about what the command actually does.
 */
data class GuideEntry(
    val cmd: String,
    val full: String,
    val blurb: String,
    val chapterLabel: String,
    val chapterTitle: String,
    val chapterIntro: String
) {
    /** The index row's teaser - only the blurb's first sentence, the rest is saved for the entry
     *  itself so the index reads as a table of contents, not the whole book. */
    val teaser: String get() = blurb.substringBefore('.').trim() + "."
}

private data class Chapter(val label: String, val title: String, val intro: String, val entries: List<Triple<String, String, String>>)

private val CHAPTERS = listOf(
    Chapter(
        "Chapter 1", "Standard Input",
        "The Hitchhiker's Guide to the Galaxy defines Terminal as \"fatal,\" and encourages anyone using one to emulate that.",
        listOf(
            Triple("ls", "list", "Lists every file in the directory, but completely glosses over every file without a name."),
            Triple("echo", "echo", "Because it wasn't all Narcissus' fault."),
            Triple("cd", "change directory", "Changes your directory, but sadly, not your destiny, duties, death, damnation, or deal.")
        )
    ),
    Chapter(
        "Chapter 2", "File Permissions",
        "To prevent the entire universe from collapsing under the weight of its own poorly written shell scripts, the Unix permissions model was invented to ensure nobody could actually touch anything of value.",
        listOf(
            Triple("chmod", "change mode", "Charades played with the operating system, pretending you have power over the inevitable decay of your data."),
            Triple("chown", "change owner", "Transfers the crushing, inescapable existential burden of ownership from one doomed user to another.")
        )
    ),
    Chapter(
        "Chapter 3", "Navigation",
        "Space is big. You just won't believe how vastly, hugely, mind-bogglingly big it is. But it is nothing compared to the labyrinthine absurdity of the root file system.",
        listOf(
            Triple("pwd", "print working directory", "Proves without a shadow of a doubt that you are exactly where you didn't want to be."),
            Triple("mkdir", "make directory", "Manufactures a meticulously sterile void, perfectly prepared to house your digital disappointments.")
        )
    ),
    Chapter(
        "Chapter 4", "Package Management",
        "The Guide notes that all major galactic civilizations go through three distinct and recognizable phases of package management: Survival (How do I install this?), Inquiry (Why is this dependency broken?), and Sophistication (Where did my free space go?).",
        listOf(
            Triple("pkg install", "package install", "Procures perfectly packaged problems you previously didn't possess."),
            Triple("apt-get update", "advanced package tool get", "Asks the universe if entropy has escalated since you last inquired. Spoiler: It has.")
        )
    ),
    Chapter(
        "Chapter 6", "Text Editors",
        "(Note: Chapter 5 was eaten by a small, heavily armed logic flaw.) There is a long and storied history of holy wars across the galaxy, mostly fought over the proper way to exit a text editor.",
        listOf(
            Triple("nano", "nano's another editor", "Nurtures your neuroses by explicitly explaining how to escape the very cage you just built."),
            Triple("vi", "visual", "Vindicates the Vogon view that true art requires unyielding, inescapable agony.")
        )
    ),
    Chapter(
        "Chapter 7", "Processes",
        "A process is simply a program that has managed to briefly trick the CPU into believing it has a purpose.",
        listOf(
            Triple("top", "table of processes", "Tally of the parasitic processes currently consuming the cold, calculating core of your computer."),
            Triple("kill", "kill", "Kicks the metaphorical chair out from beneath a process that has outlived its microscopic usefulness.")
        )
    ),
    Chapter(
        "Chapter 8", "Storage",
        "The Encyclopedia Galactica defines a file system as a logical method for storing data. The Guide defines it as a digital sock drawer where you will inevitably lose the one script you actually need.",
        listOf(
            Triple("df", "disk free", "Documents the depressing depletion of your disk space, offering absolutely zero psychological support."),
            Triple("rm", "remove", "Renders reality slightly emptier, rapidly resolving the temporary existence of your trivial files.")
        )
    ),
    Chapter(
        "Chapter 9", "Shell Scripting",
        "If you are going to do something utterly pointless, the Guide advises that you should at least automate it so you can be somewhere else when the error logs start flooding in.",
        listOf(
            Triple("bash", "bourne-again shell", "Bashes your fragile, flawed human logic repeatedly against the unforgiving anvil of machine syntax."),
            Triple("source", "source", "Sucks the soul out of a separate script, violently assimilating its variables like a digital vampire.")
        )
    ),
    Chapter(
        "Chapter 10", "Networking",
        "Connecting to the internet from a terminal emulator is much like shouting into a black hole, except occasionally the black hole shouts back to offer you a suspiciously cheap pharmaceutical product.",
        listOf(
            Triple("ping", "packet internet groper", "Pokes the pitch-black abyss of the internet, praying something out there pokes back."),
            Triple("ssh", "secure shell", "Sends your consciousness hurtling through hyperspace, solely so you can shatter systems from the safety of your sofa.")
        )
    ),
    Chapter(
        "Chapter 11", "Self-Knowledge",
        "The Guide notes that HG2Gui also ships four small oracles of its own, bootstrapped straight into the shell rather than fetched by any package manager. Three turn the lens inward. One dares to look outward, but only ever as far as the single address you hand it.",
        listOf(
            Triple("osint-lookup", "open-source intelligence lookup", "Obsessively obtains obscure open-source omens about the one domain you dared to name, then offers them up without a shred of context or comfort."),
            Triple("net-inventory", "network inventory", "Narrates the naked, humiliating truth of your own network - every interface, route, and DNS server you never bothered to name."),
            Triple("harden-check", "harden check", "Halfheartedly hunts for holes in your own home, then hands you a list of horrors you now cannot un-know."),
            Triple("sysinfo", "system information", "Surveys the sprawling, sorry state of everything you've installed, and solemnly informs you exactly how much storage it cost you.")
        )
    ),
    Chapter(
        "chapter_01", "The Absurdity of Redundancy",
        "It is a well-documented phenomenon that any sufficiently advanced filesystem will spontaneously generate duplicate files with slightly altered casing, just to see if the user is paying attention.",
        listOf(Triple("ls -a", "list, all", "Lifts the veil on the lurking, hidden files, highlighting the horrific reality that your system has secrets."))
    ),
    Chapter(
        "chapter_02", "Doppelgänger Permissions",
        "You may attempt to read this file, but it will only tell you that the permissions you thought you understood were merely another language game designed to keep you from realizing the terminal is actually empty.",
        listOf(Triple("sudo", "superuser do", "Summons a superficial sense of supremacy, temporarily tricking the terminal into trusting your terrible ideas."))
    ),
    Chapter(
        "chapter_03", "The Infinite Directory",
        "A wise philosopher once noted that having two maps of the exact same territory does not help you find your keys any faster.",
        listOf(Triple("rmdir", "remove directory", "Removes the remnants of a room that was already agonizingly absent of anything."))
    ),
    Chapter(
        "chapter_04", "Ghost Dependencies",
        "You have successfully downloaded a duplicate chapter about package management. The dependencies for understanding this chapter include a complete abandonment of logical coherence.",
        listOf(Triple("dpkg", "debian package", "Delivers the dependent data with the depressing demeanor of a doomed, depressed dictionary."))
    ),
    Chapter(
        "chapter_05", "The Anomaly",
        "Curiously, while the uppercase Chapter 5 was destroyed by a logic flaw, this file survived. It contains nothing but the creeping dread of realization.",
        listOf(Triple("find", "find", "Frantically flails through the filesystem, finally confirming the absolute absence of whatever you were foolish enough to look for."))
    ),
    Chapter(
        "chapter_06", "The Mirror Editor",
        "An editor editing a file about an editor editing a duplicate file.",
        listOf(Triple("cat", "concatenate", "Carelessly coughs the complete contents of a file directly onto your screen, utterly unconcerned with context or comprehension."))
    ),
    Chapter(
        "chapter_07", "Zombie Processes",
        "This file is the textual equivalent of a zombie process. It has no parent, it serves no function, and it simply refuses to die.",
        listOf(Triple("ps", "process status", "Parades the pathetic, purgatorial processes perpetually trapped in the processor's penal colony."))
    ),
    Chapter(
        "chapter_08", "Backing up the Void",
        "To ensure the utmost safety of your completely irrelevant data, it is highly recommended to keep a lower-case copy of every upper-case file.",
        listOf(Triple("tar", "tape archive", "Traps your trivial texts in a tight, terrible tomb, ensuring they degrade together in the dark."))
    ),
    Chapter(
        "chapter_09", "Looping the Loop",
        "Automating the creation of the very file you are reading. Time is an illusion. Terminal history doubly so.",
        listOf(Triple("grep", "global regular expression print", "Grabs greedily at the galactic garbage heap, guaranteeing you only gather other, slightly sharper garbage."))
    ),
    Chapter(
        "chapter_10", "The Ultimate Handshake",
        "The final duplicate file. By this point, the Guide assumes you have either mastered the command line or have thrown your device into the nearest body of water.",
        listOf(Triple("ifconfig", "interface configuration", "Inappropriately forces your interface to strip and show its subnet mask to complete, calculating strangers."))
    )
)

/** Chapter label + title + entries, for the index. */
data class GuideChapter(val label: String, val title: String, val entries: List<GuideEntry>)

object GuideBook {
    val chapters: List<GuideChapter> = CHAPTERS.map { ch ->
        GuideChapter(ch.label, ch.title, ch.entries.map { (cmd, full, blurb) ->
            GuideEntry(cmd, full, blurb, ch.label, ch.title, ch.intro)
        })
    }

    /** Every entry, flattened in reading order - the order Next/Prev and the entry index walk. */
    val entries: List<GuideEntry> = chapters.flatMap { it.entries }
}
