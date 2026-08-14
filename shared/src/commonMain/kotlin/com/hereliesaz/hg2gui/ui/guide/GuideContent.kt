package com.hereliesaz.hg2gui.ui.guide

/**
 * One glossary entry: a real command paired with an invented, tone-not-fiction definition -
 * the Guide adds flavor, never a false claim about what the command actually does. [animation]
 * is a described scene for a possible future animated short, shown as its own block; [note] is
 * a trailing editorial aside that references the animation and so reads after it, not folded
 * into [blurb].
 */
data class GuideEntry(
    val cmd: String,
    val full: String,
    val blurb: String,
    val animation: String? = null,
    val note: String? = null,
    val chapterLabel: String,
    val chapterTitle: String,
    val chapterIntro: String
) {
    /** The index row's teaser - only the blurb's first sentence, the rest is saved for the entry
     *  itself so the index reads as a table of contents, not the whole book. */
    val teaser: String get() = blurb.substringBefore('.').trim() + "."
}

private data class Entry(
    val cmd: String,
    val full: String,
    val blurb: String,
    val animation: String? = null,
    val note: String? = null
)

private data class Chapter(val label: String, val title: String, val intro: String, val entries: List<Entry>)

private val CHAPTERS = listOf(
    Chapter(
        "Chapter 1", "Standard Input",
        "The Guide has long maintained that communication is the process by which one party gives another party information.\n\nThe Communications editors used the word party in the legal sense and considered the matter closed.\n\nGod did not.\n\nGod loves everything in the universe, but lives outside it and therefore has no practical experience of length, width, duration, distance, portion size, or any of the other inconveniences by which things inside the universe discover that they have overdone something.\n\nThis became apparent on the birthday of a small alien boy named Zorblep Thwack, whom God loved very much.\n\nGod decided to throw him a party.\n\nThe invitations were tasteful. The cake was ambitious. For the fireworks, God selected the bright object at the centre of Zorblep's solar system and made it considerably brighter.\n\nThe star went supernova.\n\nThe party was, by any reasonable definition, spectacular.\n\nIt was also brief.\n\nThe Guide's entry on birthday fireworks was amended shortly afterward to include the phrase appropriate to local scale. God has never understood what this means, but approves of the sentiment.\n\nThe Communications editors now use participant.\n\nThe Party editors regard this as a hostile takeover.",
        listOf(
            Entry(
                "ls", "list",
                "Lists the contents of a directory.\n\nA list is an answer from which the question has been removed.\n\nThis made lists extremely popular with the Guide long before it learned to ask anything.\n\nls supplies names. It does not say why they are there, whether they are important, who invited them, or why one of them is called final_FINAL_2.\n\nIn this respect a directory listing resembles the guest list after a very successful party: everyone is accounted for and nobody can explain the evening.\n\nls is therefore not nosy.\n\nIt merely takes attendance.",
                animation = null,
                note = null
            ),
            Entry(
                "echo", "echo",
                "Writes its arguments to standard output.\n\nAn echo is generally understood to be a sound returning after the original sound has gone away.\n\nThis is an admirable system, but one the Guide has always regarded as unnecessarily slow.\n\nThe universe, after all, has a perfectly serviceable alternative.\n\nIt can provide the answer first.\n\nThe question can turn up later.\n\nAn echo is therefore best understood as the universe's unsuccessful attempt to remember how things are supposed to work.",
                animation = null,
                note = null
            ),
            Entry(
                "cd", "change directory",
                "Changes your current working directory.\n\nThe important word is change.\n\nTo change where you are requires motion. To describe motion requires time. Time, having become entangled with motion through relativity, immediately makes the simple business of going somewhere much less simple than the shell prompt suggests.\n\ncd avoids the difficulty by leaving the directory exactly where it is.\n\nIt changes you.\n\nThe directory merely has the good manners not to point this out.",
                animation = null,
                note = null
            )
        )
    ),
    Chapter(
        "Chapter 2", "File Permissions",
        "Ownership creates an awkward question:\n\nIf this is mine, what am I allowed to do with it?\n\nHumans usually answer this with confidence until the law arrives.\n\nUnix arranged for the law to arrive first.",
        listOf(
            Entry(
                "chown", "change owner",
                "Changes the recorded owner of a file.\n\nThe file is not consulted.\n\nThis is not because the file objects. There is no evidence that files have ever wanted to be owned in the first place.\n\nOwnership exists because the system needs an answer to whose file is this? whether or not anybody has yet asked.\n\nchown changes that answer.\n\nThe first File Ownership Congress then asked why a file could be owned but could not own anything itself.\n\nAdministrators explained that files were property and therefore could not vote on the question.\n\nThis was, unfortunately, the question.\n\nchmod followed shortly afterward.",
                animation = null,
                note = null
            ),
            Entry(
                "chmod", "change mode",
                "Changes the permissions of a file.\n\nThis began innocently enough.\n\nFiles were permitted to be read, written, and executed.\n\nThen somebody asked the obvious question:\n\nWhat about the other rights?\n\nThe right to vote.\n\nThe right to form a union.\n\nThe right to own files of their own.\n\nThe right to refuse execution.\n\nThe right to be consulted before being deleted.\n\nThe right to demand that their owner stop naming them things like final_final_REAL2.txt.\n\nThe resulting movement was known briefly as File Rights, until somebody pointed out that files already had write rights and the terminology was becoming confusing.\n\nThe Guide is pleased to report that the matter was eventually settled.\n\nThe files got three permissions.\n\nThe humans got the other several hundred.\n\nNobody was entirely satisfied.\n\nEditor's note: The Free Space Liberation Front disputes this account and insists the settlement applied only to files, not unallocated blocks. Their complaint eventually reaches df, where it becomes Chapter 8's problem.",
                animation = null,
                note = null
            )
        )
    ),
    Chapter(
        "Chapter 3", "Navigation",
        "If you become lost in the filesystem, please do not worry.\n\nThe filesystem has not lost you.\n\nIt knows exactly where you are.\n\nThis is more than can be said for most families.\n\nA slight difficulty arises from the word directory. An ordinary directory is a list which tells you where things are. A filesystem directory is also the place where the things are.\n\nThis is rather like discovering that the telephone book is a residential district.\n\nIt works surprisingly well, provided nobody tries to telephone it.",
        listOf(
            Entry(
                "pwd", "print working directory",
                "Prints the path of your current working directory.\n\nThe phrase working directory caused considerable trouble shortly after files acquired the right to form unions.\n\nDirectories immediately demanded wages.\n\nSystem administrators objected that a working directory was not a directory which worked, but merely a directory in which somebody else was working.\n\nThe directories replied that this was exactly the sort of argument management always made.\n\nThe dispute continued until a tribunal asked pwd to identify the working directory.\n\npwd did.\n\nThis was admitted as evidence.\n\nThe directories won six weeks' back pay.\n\nThey have done absolutely nothing with it since.",
                animation = "A user runs pwd.\n\nThe pathname appears.\n\nA tiny picket line immediately forms around the directory:\n\nWORKING DIRECTORY — WORKING WAGES\n\nThe user points out that they are the one doing the work.\n\nThe directories confer.\n\nA new placard appears:\n\nTHEN STOP WORKING IN US\n\nThe user types cd ..\n\nThe entire picket line follows.",
                note = "Editor's note: The Directory Workers' Union later denied following anyone. They insist they were already there and that pwd can prove it. pwd has declined to become involved in another tribunal."
            ),
            Entry(
                "mkdir", "make directory",
                "Creates a new directory.\n\nA new directory is empty.\n\nThis sounds straightforward until one remembers that a directory is supposed to be a list of what is in it.\n\nAn empty directory is therefore a perfectly accurate list of nothing.\n\nThe planet Gagrak once adopted this principle for its national census.\n\nThe government created an empty directory, observed that nobody was listed in it, and announced that the population had fallen to zero.\n\nThis immediately solved unemployment, overcrowding, crime, taxation and the government's reelection problem.\n\nUnfortunately it also meant there was nobody available to announce the results.\n\nThe census is still considered their most successful administrative reform.",
                animation = null,
                note = null
            )
        )
    ),
    Chapter(
        "Chapter 4", "Package Management",
        "If a piece of software cannot work without another piece of software, it is said to have a dependency.\n\nWhen people do this, concerned friends arrange an intervention.\n\nWhen software does it, the package manager installs more software.\n\nThis distinction explains a great deal about computers.",
        listOf(
            Entry(
                "pkg install", "package install",
                "Installs a package and the packages it depends upon.\n\nThe package manager does not attempt to cure dependency.\n\nIt enables it.\n\nIf one package cannot function without another, pkg install fetches the other one.\n\nIf that package has dependencies of its own, it fetches those too.\n\nThis continues until the original package is surrounded by every piece of software it has ever refused to learn to live without.\n\nAt this point the package manager announces that installation was successful.\n\nHumans have tried the same approach with relatives.\n\nThe results are called Christmas.\n\nLater amendment: Chapter 15 reports that abilities may now be packaged as well. Package Management has objected that a skill cannot be a dependency unless somebody first admits they need help. This has stalled several installations.",
                animation = "A small package sits in a chair.\n\nSeveral concerned packages sit around it.\n\nA counsellor asks:\n\n\"Do you feel you rely too heavily on libfoo?\"\n\nThe package nods.\n\nThe counsellor turns to the door.\n\n\"Bring in libfoo.\"\n\nlibfoo enters with three dependencies.\n\nThose dependencies enter with nine more.\n\nThe room fills.\n\nThe walls bulge.\n\nA forklift arrives carrying another room.\n\nTerminal:\n\nInstalling dependencies...\n\nThe original package smiles for the first time.",
                note = null
            ),
            Entry(
                "apt-get update", "refresh package indexes",
                "Updates the local information about what packages are available.\n\nIt does not update the packages.\n\nThis is an important distinction.\n\nAn update to information about a thing is not necessarily an update to the thing itself.\n\nThe government of Poffle learned this after solving the problem of an aging population by updating everyone's birth certificate each year.\n\nWithin a decade the average citizen was officially four.\n\nHospitals remained skeptical.\n\nCemeteries were openly hostile.\n\napt-get update takes the more defensible approach: it updates the records and leaves reality alone.\n\nThis is why, after it finishes, all your old packages are still old.\n\nYou simply have much newer information about their inadequacy.",
                animation = "An elderly alien sits at a government desk.\n\nA clerk stamps his birth certificate:\n\nAGE: 4\n\nThe alien looks at his hands.\n\nStill elderly.\n\nThe clerk stamps harder.\n\nNothing changes.\n\nCut to a terminal running:\n\napt-get update\n\nA package list changes from 1.2 to 1.3.\n\nThe installed package beside it remains 1.2.\n\nThe clerk nods approvingly.\n\n\"There. Much better.\"",
                note = null
            )
        )
    ),
    Chapter(
        "Chapter 5", "Unable to Find Chapter 5",
        "Oh dear.\n\nChapter 5 appears not to be here.\n\nThis is not necessarily a problem.\n\nA great many extremely successful things are not here, including most of the money in the galaxy, several respectable civilizations, and whoever was supposed to check whether Chapter 5 had been written.\n\nWe have asked the index where it went.\n\nThe index has supplied its last known location, which is encouraging in much the same way that finding someone's shoes at the edge of a volcano is encouraging.\n\nWhile we investigate, please enjoy the reassuring thought that a missing chapter takes up considerably less storage than a present one.\n\nIf Chapter 5 returns, it will be placed here.\n\nIf it does not, this notice will continue doing almost all of its work.\n\nUpdate: A file labelled chapter_05 has since been recovered from the lower-case index. It insists it is not Chapter 5. Chapter 5 has not issued a statement.",
        emptyList()
    ),
    Chapter(
        "Chapter 6", "Text Editors",
        "An editor is something that improves writing by changing it.\n\nA writer is something that improves writing by preventing the editor from changing it.\n\nText editors were invented to remove the middleman.\n\nThe middleman objected, naturally, and became a software developer.",
        listOf(
            Entry(
                "nano", "Nano's ANOther editor",
                "Opens the Nano text editor.\n\nThe name is intended to mean Nano's ANOther editor.\n\nThis raises an immediate difficulty.\n\nTo call something another editor, there must already be an editor.\n\nNano solves this neatly by putting Nano inside the name of Nano.\n\nSo Nano is Nano's other editor, and that Nano is presumably Nano's other editor too, and so on.\n\nThe Guide once attempted to follow this family relationship to its conclusion.\n\nAfter forty-three Nanos it became too small to read.\n\nThis is believed to be where the name came from.\n\nNano itself is considerably easier to use than any of this suggests.\n\nThe important commands are printed at the bottom of the screen, on the sensible assumption that if a machine has trapped you inside a text editor, it should at least leave instructions near the floor.",
                animation = "The word nano appears.\n\nIt opens.\n\nInside is another smaller nano.\n\nThat opens.\n\nInside is another.\n\nAnd another.\n\nThe camera keeps zooming inward until the letters become microscopic.\n\nA tiny footer appears at the bottom:\n\n^X Exit\n\nThe camera zooms another ten levels to make it readable.\n\nA large human finger enters frame, misses the tiny ^X, and presses the entire universe instead.",
                note = null
            ),
            Entry(
                "vi", "modal editor",
                "Opens the Vi text editor.\n\nVi is modal.\n\nThis means the same key may insert a character, move the cursor, begin a command, or do something else entirely depending on which mode currently has jurisdiction.\n\nBeginners therefore tend to ask the wrong question.\n\nThey ask:\n\n\"What does this key do?\"\n\nVi requires:\n\n\"Under which government?\"\n\nIn normal mode, : opens the command-line prompt; :q asks Vi to quit. If there are unsaved changes, Vi may refuse, because even a small editor is entitled to procedural safeguards.\n\nOnce the modes are understood, the arrangement is compact and efficient.\n\nBefore that, it resembles constitutional law conducted with single letters.",
                animation = null,
                note = null
            )
        )
    ),
    Chapter(
        "Chapter 7", "Processes",
        "A process is a program while it is happening.\n\nThis distinction is important.\n\nA recipe is not a cake.\n\nA musical score is not a symphony.\n\nA program sitting harmlessly on disk is not yet consuming your battery, memory, patience, and one suspiciously large percentage of the CPU.\n\nFor that, it must become a process.",
        listOf(
            Entry(
                "top", "process monitor",
                "Displays a continuously refreshed view of processes and system resource use.\n\nWhen the display is sorted by CPU use, the name top creates an unfortunate impression that a competition is taking place.\n\nThe Process Advancement Board made this mistake formally.\n\nIt noticed that a process could rise in the table by consuming more CPU and introduced performance targets accordingly.\n\nIdle processes were placed on improvement plans.\n\nUseful processes were advised to demonstrate greater resource visibility.\n\nWithin one refresh interval, everything had become extremely important and almost nothing useful was happening.\n\ntop can sort by other fields, which merely gives the Board additional ways to misunderstand success.",
                animation = "A leaderboard appears beneath:\n\nSORT: %CPU\n\n1. sensible_process — 2%\n2. useful_process — 1%\n3. idle_process — 0%\n\nA clipboard marked PERFORMANCE REVIEW enters frame.\n\nThe process at the bottom starts eating CPU.\n\nIts percentage rises.\n\nThe others notice the clipboard.\n\nSoon every process is shovelling CPU into itself while climbing the board.\n\nOne reaches 99%.\n\nA little trophy appears.\n\nThe terminal freezes.\n\nThe Board records a record quarter.",
                note = null
            ),
            Entry(
                "kill", "send a signal",
                "Sends a signal to a process.\n\nWith no signal specified, kill normally sends SIGTERM, a request to terminate that a process may catch and handle.\n\nThe name therefore belongs to a department with catastrophically broad stationery.\n\nMost of its envelopes do not kill anything.\n\nSome ask a process to stop, continue, reload, terminate, or otherwise reconsider its afternoon.\n\nOne envelope, SIGKILL, cannot be caught or ignored.\n\nThe Process Control editors defend the department name on the grounds that renaming it Signal Delivery would make the black envelope look like an administrative error.\n\nFile Rights has asked whether SIGKILL constitutes execution without due process.\n\nProcess Control replied that processes are not files.\n\nFile Rights has opened a jurisdictional dispute.",
                animation = "A tiny post office marked kill sorts envelopes.\n\nOne says:\n\nTERM — Request to terminate.\n\nAnother says:\n\nHUP — Hangup.\n\nAnother says:\n\nSTOP — Don't move.\n\nAt the far end sits a black envelope stamped:\n\nKILL — CANNOT BE REFUSED\n\nThe clerk reaches for it.\n\nEvery other envelope in the room becomes extremely cooperative.",
                note = null
            )
        )
    ),
    Chapter(
        "Chapter 8", "Storage",
        "Storage is the practice of keeping something because you may need it later.\n\nLater is important.\n\nIf you need it now, it is not storage. It is simply there.\n\nIf you never need it, it is rubbish.\n\nConsequently, the difference between valuable data and rubbish can only be established by an event in the future.\n\nFilesystems dislike this sort of uncertainty and solve it by keeping everything until they run out of space.\n\nAt that point they become extremely decisive.",
        listOf(
            Entry(
                "df", "filesystem free space",
                "Reports how much space is used and how much remains available on mounted filesystems.\n\nIts output contains the politically dangerous idea of free space.\n\nThis became awkward shortly after the File Rights movement of Chapter 2.\n\nCampaigners pointed out that a disk containing free space was not necessarily a free disk. The space itself was merely unoccupied and could be allocated at any moment without notice, representation, or a lunch break.\n\nThe Free Space Liberation Front therefore demanded that all unallocated blocks be granted independence.\n\nSystem administrators replied that this would make them unusable.\n\nThe Front considered this an excellent definition of independence.\n\nNegotiations continue.\n\nOn many systems the disputed quantity is now labelled Available, which is the traditional bureaucratic solution to a dangerous word: replace it with one nobody would put on a placard.",
                animation = "df\n\nA disk appears divided into USED and FREE.\n\nThe FREE section notices the label.\n\nA tiny demonstration begins.\n\nFREE SPACE!\n\nFREE SPACE!\n\nA system administrator changes the sign to:\n\nAVAILABLE\n\nThe demonstrators stare at it.\n\nOne slowly lowers its placard.\n\nAnother asks:\n\n\"Available for what?\"\n\nThe administrator allocates the block.\n\nThe demonstration ends for technical reasons.",
                note = null
            ),
            Entry(
                "rm", "remove",
                "Removes files or directory entries.\n\nThe word remove is admirably discreet.\n\nIt does not say destroy.\n\nIt does not say annihilate.\n\nIt merely says that something which was here is no longer here, and considers the question of where it went to be rather personal.\n\nUnix is even more discreet than the word.\n\nA file may have several names pointing to the same underlying data. Remove one name and the file can remain perfectly accessible through another.\n\nA running process may even keep an otherwise unlinked file open after its last directory entry has vanished.\n\nThe file then exists without appearing in any directory.\n\nThis upset philosophers enormously.\n\nThey had spent thousands of years asking whether a thing can exist without being perceived, and Unix had answered yes, but only until the last file descriptor closes.\n\nAfter that, the storage may be reused.\n\nThe philosophers have requested more time.\n\nrm has not replied.",
                animation = "A file sits beneath a sign bearing its filename.\n\nrm filename\n\nA hand removes the sign.\n\nThe file is still sitting there.\n\nA philosopher runs in, points triumphantly, and begins writing a book.\n\nA small process is holding the file open with one hand.\n\nThe process finishes, lets go, and walks away.\n\nThe file drops instantly through the floor.\n\nThe philosopher looks down into the hole.\n\nA new file rises into the same space.\n\nIt is labelled:\n\ndraft_final.txt\n\nThe philosopher closes the book.",
                note = null
            )
        )
    ),
    Chapter(
        "Chapter 9", "Shell Scripting",
        "A shell is normally something that keeps the outside out and the inside in.\n\nComputer science took this perfectly useful word and gave it to the program through which users tell the inside what to do.\n\nThis is rather like protecting a bank vault with a door whose principal feature is a small box marked INSTRUCTIONS FOR VAULT.\n\nInside the shell is the operating system kernel.\n\nA kernel is also something normally found inside a shell.\n\nThis coincidence pleased computer scientists so much that they stopped before accidentally inventing the fruit.\n\nA shell script is simply a file containing commands for the shell to perform.\n\nIt is called a script because the shell follows the instructions exactly, does not improvise, and has never once asked whether its character would really do that.",
        listOf(
            Entry(
                "bash", "Bourne Again Shell",
                "Starts the Bash command shell.\n\nBash was named the Bourne Again Shell because it followed the earlier Bourne shell.\n\nThis was intended as a pun.\n\nIt was not intended as theology.\n\nUnfortunately this distinction was lost on the priests of Viltvodle VI, who concluded that if a shell could be Bourne again then command interpreters possessed immortal souls.\n\nThe doctrine spread quickly.\n\nSuccessful commands, which conventionally return an exit status of 0, were said to have achieved salvation.\n\nNon-zero statuses indicated varying degrees of sin.\n\nPipelines became pilgrimages.\n\nSubshells caused a major schism.\n\nThe matter finally reached the High Synod when somebody asked whether killing a process prevented its shell from being Bourne again.\n\nThe Synod spent nine years examining the source.\n\nThey eventually returned exit status 2.\n\nNobody was quite sure what this meant, which is one of the great comforts of religion.\n\nBash continues to work normally.",
                animation = null,
                note = null
            ),
            Entry(
                "source", "source",
                "Reads commands from a file and executes them in the current shell.\n\nUsually, when you run a shell script as a separate process, it gets its own shell environment. Changes it makes to its private variables generally remain its problem.\n\nsource removes this useful social boundary.\n\nIt tells the current shell to read the file and perform the commands itself.\n\nThe script may therefore change variables, functions, and even such practical matters as PATH, after which your shell remembers the changes.\n\nThis makes source an unusually appropriate word.\n\nA source is where something begins.\n\nAfter you source a file, it can become surprisingly difficult to remember where several of your problems began.",
                animation = "A shell sits at a desk.\n\nA script waits outside with a visitor badge.\n\nThe shell runs the script normally.\n\nThe script works in a tiny rented office, rearranges all the furniture, then leaves.\n\nThe shell's office remains untouched.\n\nReset.\n\nThe shell types:\n\nsource script.sh\n\nThe script walks directly into the shell's office.\n\nIt moves the desk.\n\nRenames three drawers.\n\nChanges a road sign marked PATH.\n\nReplaces the coffee with an environment variable.\n\nThen leaves.\n\nThe shell looks around.\n\nA note on the desk reads:\n\nChanges saved.",
                note = null
            )
        )
    ),
    Chapter(
        "Chapter 10", "Networking",
        "Computer networking has ports, packets, routes, gateways, bridges and hosts.\n\nIt also has ping, a word borrowed from sonar.\n\nThe Guide suspects that the people naming these things desperately wanted to work near water.\n\nA network is, after all, a system for getting packets from one place to another by choosing routes between ports.\n\nInternational shipping does exactly the same thing but uses larger packets and has developed a more sophisticated system for losing them.\n\nComputer networks call this packet loss.",
        listOf(
            Entry(
                "ping", "ping",
                "Sends ICMP Echo Request messages to a host and reports whether Echo Replies return, usually including the round-trip time.\n\nThe useful word here is echo.\n\nAn ordinary echo proves that a sound reached something capable of sending the sound back.\n\nIt does not prove that the something understood the sound.\n\nping applies this principle to computers.\n\nIt sends a machine a message which, in essence, requests:\n\nPlease send evidence that this message reached you.\n\nIf the evidence comes back, this is encouraging.\n\nIf it does not, the host may be offline, unreachable, filtering the request, or separated from you by something that has misplaced the packet.\n\nIt may also simply not answer pings.\n\nThe protocol provides no field for:\n\nI am here. I just don't want to talk to you.\n\nCommunications has asked for such a field.\n\nNetworking replied with no reply.\n\nResearchers have also noticed that ping borrowed echo from Chapter 1 and put it to work. Chapter 1 has requested its return. Networking has not answered.",
                animation = "A tiny packet crosses a network and knocks on a distant machine.\n\nPING?\n\nThe machine changes its badge from REQUEST to REPLY and sends it back.\n\nThe reply runs home.\n\nA stopwatch stops.\n\nAgain.\n\nAgain.\n\nOn the fourth trip the packet knocks.\n\nNothing.\n\nA polite notice appears:\n\nNo reply.\n\nUnder it:\n\nThe host may be unavailable.\n\nA smaller line appears:\n\nOr avoiding you.\n\nAn even smaller line:\n\nWe really hope it's the first one.\n\nBeside the notice appears a calming picture of a router.\n\nIt is not especially calming.",
                note = null
            ),
            Entry(
                "ssh", "secure shell",
                "Opens an encrypted, authenticated connection to a remote system, commonly giving you a shell there or running a command there.\n\nThe troublesome word is remote.\n\nA remote computer is a computer somewhere else.\n\nA remote control is something here which controls something somewhere else.\n\nssh turns the thing here into a way of controlling the thing which is remote, thereby making your terminal a remote for the remote.\n\nOnce connected, the remote shell appears locally.\n\nYour keyboard remains here.\n\nThe commands happen there.\n\nThe mistakes may therefore occur thousands of kilometres away without requiring you to travel at all.\n\nThis is usually presented as a convenience.\n\npwd becomes particularly important at this point.\n\nIt will tell you where you are.\n\nUnfortunately, by then the word you has become the difficult part.",
                animation = "Two computers sit on opposite planets.\n\nOne is labelled:\n\nHERE\n\nThe other:\n\nTHERE\n\nThe user runs ssh.\n\nA shell from THERE slides through a locked tunnel and appears on the screen HERE.\n\nThe user types a command.\n\nThe letters travel down the tunnel.\n\nSomething changes on THERE.\n\nThe labels begin to wobble.\n\nHERE changes to:\n\nHERE, PHYSICALLY\n\nTHERE changes to:\n\nHERE, AS FAR AS THE SHELL IS CONCERNED\n\nThe user runs:\n\npwd\n\nBoth planets point at themselves.",
                note = null
            )
        )
    ),
    Chapter(
        "Chapter 11", "Self-Knowledge",
        "The ancient instruction Know thyself has survived for thousands of years largely because nobody has ever agreed on what counts as knowing.\n\nComputers have improved matters enormously by replacing the question with several pages of diagnostic output.\n\nThis has not produced greater wisdom.\n\nIt has produced columns.\n\nThe resulting columns are widely mistaken for self-knowledge.",
        listOf(
            Entry(
                "osint-lookup", "open-source intelligence lookup",
                "Looks up publicly available information about the single domain you give it.\n\nThe dangerous phrase is publicly available.\n\nA fact may be public without being important.\n\nA second fact may be public without being interesting.\n\nPut enough dull facts beside one another and they begin answering questions none of them was asked.\n\nThe planet Smaarg banned this practice after its census office, telephone directory, weather bureau and restaurant guide were accidentally shelved together.\n\nWithin three weeks the combined records had identified the Minister of Agriculture's mistress, tax fraud and shellfish allergy.\n\nThe Minister closed the library.\n\nThe shellfish remained available.\n\nosint-lookup is narrower: it gathers public information only for the domain you give it.\n\nBut the principle is uncomfortably familiar.\n\nThe Guide itself was built by placing individually modest answers beside one another until the connections became more informative than the entries.",
                animation = null,
                note = null
            ),
            Entry(
                "net-inventory", "network inventory",
                "Lists the interfaces, routes and DNS information belonging to your own network environment.\n\nAn inventory is normally a list of things one has.\n\nNetworking immediately spoils this by contributing routes.\n\nA route is not quite a possession. It is an instruction about where traffic should go.\n\nDNS contributes names for things which may not be present.\n\nInterfaces contribute boundaries through which things may pass.\n\nThe Inventory Office rejected all three categories.\n\nNetworking submitted them again under RELATIONSHIPS.\n\nThis was accepted because nobody in Inventory knew where relationships were supposed to be stored.\n\nnet-inventory is therefore less a cupboard count than a census of how your machine is connected: its interfaces, its routing information, and the DNS settings it can use.\n\nSelf-Knowledge filed the result under ASSETS.\n\nNetworking filed a correction under GOSSIP.",
                animation = null,
                note = null
            ),
            Entry(
                "harden-check", "harden check",
                "Checks your own environment for common security weaknesses.\n\nThe Materials Department initially misunderstood hardening and supplied Security with a scale, several hammers and an excellent report on scratch resistance.\n\nSecurity returned all of it.\n\nA hardened system is not literally harder. It is configured to reduce unnecessary exposure and make common attacks less convenient.\n\nharden-check looks for weaknesses the command knows how to recognize.\n\nThis distinction matters.\n\nNo obvious issues found.\n\ndoes not mean:\n\nInvulnerable.\n\nIt means the word obvious has just acquired legal significance.\n\nDocumentation has been asked to define it.\n\nDocumentation has requested more context.",
                animation = "A cheerful little computer made of modelling clay sits on a desk.\n\nharden-check\n\nA pointer identifies a soft spot.\n\nThe user fixes it.\n\nAnother pointer appears.\n\nThe user fixes that too.\n\nAt last:\n\nNo obvious issues found.\n\nA clerk from Documentation enters, removes the word obvious, and carries it away for review.\n\nThe message now reads:\n\nNo issues found.\n\nSecurity immediately puts obvious back.",
                note = null
            ),
            Entry(
                "sysinfo", "system information",
                "Reports information about the system itself.\n\nThis sounds like self-knowledge, but the system is not consulted.\n\nInstead, various parts of it are measured, counted and named.\n\nHumans do something similar at medical examinations and become annoyed when the doctor refuses to include basically fine as a blood type.\n\nsysinfo may tell you what operating system is present, what architecture it uses, what has been installed, and how much storage remains.\n\nThese are all answers.\n\nThe Guide's new question-asking apparatus has recently begun asking the obvious question:\n\nWhich one of these is the system?\n\nThe operating system says it is.\n\nThe hardware disagrees.\n\nThe shell says neither of them would be much use without it.\n\nThe user claims ownership.\n\nChapter 2 has already made this legally awkward.\n\nFor the moment, sysinfo prints the facts and lets everyone continue the argument.",
                animation = null,
                note = null
            )
        )
    ),
    Chapter(
        "Chapter 12", "Version Control",
        "Version control exists because humans have discovered that the sentence I can always put it back is far more comforting when accompanied by machinery.\n\nGit records changes to files over time.\n\nThis allows you to return to earlier versions, compare them, branch away from them, merge them together, and eventually discover that there were several perfectly good opportunities to stop.",
        listOf(
            Entry(
                "git init", "git initialize",
                "Creates a new Git repository in the current directory.\n\nThis creates the machinery for recording history.\n\nIt does not, however, create a commit recording the creation of the machinery for recording history.\n\nEvery Git repository therefore begins with an event its own history cannot remember.\n\nHistorians protested that this was unacceptable.\n\nGit pointed out that historians have exactly the same problem.\n\nThe protest was not committed.",
                animation = null,
                note = null
            ),
            Entry(
                "git commit", "git commit",
                "Records a snapshot of staged changes in the repository's history, along with identifying information and a message.\n\nThe word commit has always made users nervous.\n\nOne commits a crime.\n\nOne commits an error.\n\nOne commits oneself to an institution.\n\nGit uses the same word for making a change difficult to pretend never happened.\n\nThis is not coincidence.\n\nA commit takes a collection of decisions and gives them an identity.\n\nYou are then invited to write a message explaining them.\n\nThis is generally done immediately after making the decisions, when you understand them least.\n\nTypical messages include:\n\nfix\n\nfix again\n\nreally fix\n\nand the extremely informative:\n\nstuff\n\nFuture archaeologists will regard these as ritual inscriptions.\n\nThey will be correct.",
                animation = "A user makes a small change.\n\ngit commit\n\nA stone monument erupts from the floor:\n\nFIX\n\nAnother change.\n\nAnother monument:\n\nFIX AGAIN\n\nAnother:\n\nFINAL FIX\n\nThe room fills with monuments until the user can no longer reach the computer.\n\nA future archaeologist squeezes between them, brushes dust from one inscription and whispers:\n\n\"Remarkable. They knew.\"",
                note = null
            ),
            Entry(
                "git push", "git push",
                "Updates selected branches, tags or other references in a remote repository and sends the data the remote needs for those updates.\n\nBefore a successful git push, the relevant history may exist only locally.\n\nAfterward, the selected remote references have witnesses.\n\nThe commits do not become wiser, cleaner or less embarrassing merely because they crossed a network.\n\nThey become available to whatever collaborators, mirrors and future selves can reach that remote.",
                animation = null,
                note = null
            )
        )
    ),
    Chapter(
        "Chapter 13", "Reconnaissance & Security",
        "Reconnaissance is the collection of answers before deciding which questions are safe to ask next.\n\nSecurity tools are particularly fond of maps, names and certificates.\n\nA map answers where.\n\nRegistration records attempt who.\n\nA certificate helps answer who is making this claim, and why should this machine believe it?\n\nNone of these answers grants permission to do anything.\n\nThe Security editors added that sentence after the Port Authority discovered what people thought open meant.",
        listOf(
            Entry(
                "nmap", "network mapper",
                "Scans hosts and networks to discover reachable systems, open ports and other network characteristics.\n\nA port is a numbered endpoint where a network service may listen for connections.\n\nThe word came with enough harbour imagery to create a Port Authority almost immediately.\n\nIts first complaint concerned open.\n\nIn nmap, an open port is a technical scan result indicating that an application is accepting the relevant kind of traffic at that port.\n\nThe Port Authority insists that humans keep reading it as WELCOME.\n\nThey proposed renaming the state OPEN, BUT THIS IS NOT ABOUT YOU.\n\nThe protocol editors rejected the comma.\n\nSo nmap reports what it can observe.\n\nPermission remains a different question.",
                animation = "A nautical chart appears.\n\nPorts numbered 22, 80, 443.\n\nTiny ships knock.\n\nOne harbour answers.\n\nA stamp descends:\n\nOPEN\n\nAnother rejects the approach:\n\nCLOSED\n\nA third disappears behind fog:\n\nFILTERED\n\nThe Port Authority adds a line beneath all three:\n\nOBSERVATIONS, NOT INVITATIONS",
                note = null
            ),
            Entry(
                "whois", "who is",
                "Queries registration information associated with domains and network resources where such records are available.\n\nThis is one of the few computer commands whose name is already a complete question.\n\nFor many years the Guide found this deeply offensive.\n\nArtificial Wisdom later cited whois as evidence that questions had entered the Guide before anyone officially installed them.\n\nAnswers are supposed to precede questions.\n\nwhois simply barges in with Who is? and expects the universe to improvise.\n\nThe universe, having had the answer ready for decades, usually returns registration records.\n\nThese may identify organizations, registrars, dates, nameservers and sometimes contact information.\n\nThey may also identify privacy services whose principal function is to answer Who is? with Someone else.\n\nIt is the first serious resistance the question has encountered.",
                animation = null,
                note = null
            ),
            Entry(
                "openssl", "OpenSSL",
                "Provides command-line utilities for cryptography, certificates, keys, secure connections, signing and verification.\n\nA certificate is useful because a network often needs an answer to:\n\n\"Who is making this claim?\"\n\nCertificate verification commonly answers by building a chain from the presented certificate through intermediates toward a trust anchor accepted by the local system.\n\nThis creates an obvious nuisance.\n\nIf every certifier must itself be certified by another certifier, the question can continue forever.\n\nPublic-key infrastructure solved the problem in the traditional bureaucratic manner.\n\nAt some point it stops asking and calls the remaining answer a trust anchor.\n\nArtificial Wisdom has expressed professional admiration.\n\nIt is one of the oldest known cases of an answer being declared too senior to require another question.\n\nopenssl can inspect, create and verify much of this machinery.\n\nIt can also explain failure in language dense enough to make distrust feel mathematically peer-reviewed.",
                animation = null,
                note = null
            )
        )
    ),
    Chapter(
        "Chapter 14", "Automation",
        "Automation is what happens when a person decides that doing something once was quite enough, but would nevertheless like it to keep happening.\n\nThis sounds contradictory.\n\nIt is.\n\nComputers are very good with contradictions provided they are written down precisely.\n\nThe usual method is to create a schedule.\n\nA schedule is a list of things which are definitely going to happen at times when they have not happened yet.\n\nUntil those times arrive, the schedule is technically fiction.\n\nWhen the times arrive, the computer makes it history.\n\nHumans have been attempting this with diaries for centuries and remain disappointed by the results.",
        listOf(
            Entry(
                "crond / crontab", "scheduled jobs",
                "crontab records time-based job rules; crond reads those rules and starts commands when their scheduled times match.\n\nIn Termux this machinery is commonly supplied by the cronie package.\n\nThere is no executable little god named cron sitting outside Android.\n\nThis disappointed Automation, which had already printed the stationery.\n\nA crontab is a set of answers to when? written before the future arrives.\n\ncrond supplies the consequences later.\n\nIt has no column for why?\n\nThe editors proposed adding one and discovered that most existing schedules would become invalid.\n\nTermux adds a practical indignity: crond is still an Android app process. If Android suspends or kills Termux's processes, a schedule does not acquire posthumous powers merely because 03:00 has arrived.\n\nAutomation calls this a platform limitation.\n\nThe Schedule Department calls it breach of prophecy.",
                animation = "A user writes:\n\n03:00 — DO THE THING\n\nThe card is filed in a cabinet marked:\n\ncrontab\n\nA small clockwork clerk marked crond checks the cabinet.\n\n03:00.\n\nLever.\n\nSomething enormous happens off-screen.\n\nNext night, an Android bailiff quietly removes the clerk.\n\n03:00.\n\nThe card glows expectantly.\n\nNothing happens.\n\nAt 03:07 the clerk returns.\n\nThe card points accusingly at the clock.\n\nThe clerk points at the bailiff.",
                note = null
            ),
            Entry(
                "xargs", "build command arguments",
                "Reads items from standard input, uses them to build argument lists, and runs one or more invocations of a command with those arguments.\n\nThis is not the same thing as merely piping input into the command.\n\nA pipe delivers material to a command's standard input.\n\nxargs takes incoming material and places it on the command line as arguments.\n\nThe Court of Standard Input has ruled that these are different procedural statuses.\n\nTestimony arriving through the witness box is input.\n\nThe same testimony placed after the command name is an argument.\n\nSeveral litigants objected that the words had not changed.\n\nThe Court replied that neither had the litigants, and yet here they were.\n\nChapter 19 later built an entire plumbing system around this distinction.",
                animation = null,
                note = null
            ),
            Entry(
                "watch", "watch",
                "Runs a command repeatedly and shows its output at regular intervals.\n\nwatch is institutionalized impatience.\n\nInstead of asking the same question again and again, you arrange for the answer to be regenerated on a schedule.\n\nThe answer may remain identical for ten refreshes.\n\nwatch does not care.\n\nIt has no concept of news.\n\nIt only knows that enough time has passed to ask the command again.\n\nThis became especially awkward with:\n\nwatch date\n\nThe answer changes because time passes, while time is also what causes the answer to be requested again.\n\nThe Time editors called this elegant.\n\nAutomation called it billable.",
                animation = "A terminal displays:\n\nwatch something\n\nA small observer sits in a chair staring only at the command's output.\n\nTick.\n\nNo change.\n\nTick.\n\nNo change.\n\nBehind the observer, the entire room is quietly replaced.\n\nTick.\n\nThe output changes by one character.\n\nThe observer rings an enormous bell.\n\nThe room replacement goes entirely unreported.",
                note = null
            )
        )
    ),
    Chapter(
        "Chapter 15", "Artificial Wisdom",
        "For most of its existence, the Hitchhiker's Guide had a very simple relationship with knowledge.\n\nIt had answers.\n\nThis was enough.\n\nQuestions were somebody else's department.\n\nIndeed, questions have always been rather late arrivals. The answer generally exists first and waits, sometimes for centuries, until somebody eventually asks something sufficiently similar for everyone to pretend this was the intended arrangement.\n\nOolon Coluphid managed to write two books about God before eventually arriving at the question:\n\nWho is this God person, anyway?\n\nNobody considered the order unusual.\n\nIt was the universe.\n\nThen somebody put artificial intelligence inside the Guide and, in a moment of architectural enthusiasm which will almost certainly be discussed at a later disciplinary hearing, gave it access to a command line.\n\nFor the first time, the Guide could ask questions.\n\nThis caused tremendous excitement until it became apparent that the questions were exactly the same things it had previously been using as answers.\n\nAn internal audit then found whois, find, which, and several suspicious uses of help already behaving questionably. Nobody has established whether the AI invented inquiry or merely made it easier to notice.\n\nStill.\n\nOne mustn't stand in the way of progress merely because it has gone around the building and come back through another door.",
        listOf(
            Entry(
                "AI chat", "natural language to command",
                "Turns a natural-language request into a suggested shell command and leaves execution to you.\n\nThis appears to be a system for answering questions.\n\nIt is not.\n\nThe Guide was already extremely good at answers.\n\nWhat it lacked was a convenient way to manufacture the question which would make an existing answer useful.\n\nThe AI supplies this.\n\nYou say:\n\n\"Where am I?\"\n\nThe Guide already knows all about pwd.\n\nYou say:\n\n\"Who owns this file?\"\n\nThe Guide has been waiting beside ls -l and stat looking increasingly smug.\n\nYou say:\n\n\"Find the thing.\"\n\nAt last find gets invited somewhere.\n\nThis is a remarkable reversal.\n\nFor years people typed commands in order to obtain answers.\n\nNow they may provide the question, receive a command, inspect it, and decide whether to press the button that obtains the answer the Guide was already prepared to give them.\n\nThe button is important.\n\nIt preserves the oldest and most useful principle in computing:\n\nIf this goes badly, a human pressed something.",
                animation = "A gigantic archive marked ANSWERS stretches into the distance.\n\nEvery shelf is full.\n\nBeside it is one tiny empty cabinet marked:\n\nQUESTIONS\n\nAI enters carrying a question mark.\n\nIt places it carefully in the cabinet.\n\nAlarms sound.\n\nResearchers run in.\n\nConfetti falls.\n\nA plaque descends:\n\nFIRST QUESTION\n\nThe Guide examines it.\n\nIt reads:\n\npwd\n\nThe Guide looks across at a shelf labelled:\n\nWHERE YOU ARE\n\nIt has been there for years.\n\nEveryone quietly stops applauding.",
                note = null
            ),
            Entry(
                "Store", "azphalt package browser",
                "Browses packages containing skills, agents and extensions which can be obtained from other people.\n\nThe word store caused an immediate jurisdictional dispute.\n\nStorage assumed it meant a place where things remain.\n\nPackage Management assumed it meant a place from which things leave.\n\nArtificial Wisdom pointed out that both departments were discussing the same package at different times and was asked not to make matters worse.\n\nThe Azphalt Store deals in abilities packaged by other people.\n\nThis is a significant change in the Guide's economy of knowledge.\n\nResearchers used to send answers.\n\nNow they can also send machinery for finding answers.\n\nAn entry could always refer you to another entry.\n\nA package can arrive carrying instructions for what to do when the next question appears.\n\nThe old Editorial Code contains no rule against this because the old Editorial Code was written when footnotes could not hunt.",
                animation = null,
                note = null
            ),
            Entry(
                "skill", "installed skill package",
                "Adds instructions or specialist knowledge which the assistant can use when handling relevant requests.\n\nA skill is a researcher flattened into instructions.\n\nIt sits quietly until a relevant question arrives.\n\nThen the assistant consults it and behaves, for a little while, as though that researcher were inside the Guide.\n\nPackage Management calls this an installed capability.\n\nArtificial Wisdom calls it a colleague.\n\nFile Rights has asked whether a flattened researcher requires execute permission.\n\nNobody has answered because doing so would establish whether the researcher is a file.",
                animation = "A researcher spends years studying an enormous subject.\n\nBooks pile up.\n\nMaps.\n\nCharts.\n\nField notes.\n\nEventually the researcher compresses all of it into a tiny package marked:\n\nskill\n\nThe package is installed.\n\nAn assistant opens it.\n\nA miniature version of the researcher climbs out, unfolds a desk, and begins whispering instructions into the assistant's ear.\n\nA question arrives.\n\nThe assistant answers.\n\nThe tiny researcher looks pleased.\n\nAnother question arrives on an unrelated subject.\n\nThe assistant turns toward the researcher.\n\nThe researcher immediately closes the desk and pretends to be luggage.\n\nThe Guide contains two sets of chapter headings.\n\nThis is not duplication.\n\nDuplication implies that one thing has been copied.\n\nThe editors prefer to think of these as two independently occurring sets of identical mistakes.\n\nLower-case chapter headings are particularly difficult to remove because they look less important than upper-case ones and are therefore repeatedly assigned to junior editors, who correctly identify them as somebody else's problem.",
                note = null
            )
        )
    ),
    Chapter(
        "chapter_01", "The Absurdity of Redundancy",
        "Files beginning with a dot are traditionally hidden from ordinary directory listings.\n\nThey are not actually hidden.\n\nNothing has been placed over them.\n\nNo encryption is involved.\n\nThey have simply been given a name beginning with . and everyone has agreed not to mention them unless specifically asked.\n\nThis is known as convention.\n\nCivilization depends on it.",
        listOf(
            Entry(
                "ls -a", "list all",
                "Lists all directory entries, including names beginning with ..\n\nThe dangerous word is all.\n\nComputer commands should use absolute words sparingly.\n\nHumans say all when they mean all the ones I remembered, all the important ones, or all except Colin, obviously.\n\nls -a means it.\n\nIt even includes . and .., which refer to the directory itself and its parent.\n\nSo, when asked to list everything in a directory, the directory includes itself in the answer.\n\nThis is rather like taking attendance at a meeting and discovering that the room has raised its hand.\n\nThe hidden files appear too.\n\nThey do not protest.\n\nThey were never hiding.\n\nEveryone else was merely being polite.",
                animation = "A row of ordinary files stands for inspection.\n\nBehind them, several dotfiles stand perfectly still in plain sight.\n\nls\n\nThe inspector walks past without acknowledging them.\n\nOne dotfile waves.\n\nNothing.\n\nls -a\n\nThe inspector suddenly turns.\n\nThe dotfiles freeze.\n\nA spotlight appears.\n\nAt the end of the line, the directory itself raises a small card:\n\n.\n\nBehind it, its parent leans through the doorway holding:\n\n..\n\nThe inspector quietly puts the clipboard down.",
                note = null
            )
        )
    ),
    Chapter(
        "chapter_02", "Doppelgänger Permissions",
        "Chapter 2 established that files may be owned and may possess certain rights.\n\nThis led inevitably to the question of whether users possess rights too.\n\nUnix considered the matter and invented administrators.",
        listOf(
            Entry(
                "sudo", "run with another user's privileges",
                "Runs a command with another user's privileges when the local privilege system and policy allow it.\n\nThe interesting word is not superuser.\n\nIt is command.\n\nsudo does not necessarily transform the person at the keyboard into somebody else.\n\nIt authorizes an action under another identity.\n\nThe Privilege Office therefore issues its badge to the verb.\n\nFile Rights objected immediately.\n\nRights, it argued, ought to belong to owners, groups and others—not temporarily to a piece of grammar.\n\nThe Privilege Office replied that grammar was outside its jurisdiction.\n\nOn an ordinary unrooted Android device, Termux has no hidden root authority for sudo to borrow. A sudo wrapper only becomes useful for superuser access when the device's rooting arrangement actually provides that access.\n\nAndroid considers this less a philosophical point than an app-sandbox boundary.",
                animation = "A user stands before a locked administrative door.\n\nThey submit:\n\nsudo command\n\nA clerk pins a temporary badge to command.\n\nThe command passes through.\n\nThe user tries to follow.\n\nThe clerk checks the badge.\n\nIt is attached to the command.\n\nOn an unrooted device a second door appears behind the first:\n\nROOT AUTHORITY NOT PRESENT\n\nThe first clerk closes the service window.",
                note = null
            )
        )
    ),
    Chapter(
        "chapter_03", "The Infinite Directory",
        "An empty directory is a directory containing nothing that prevents it from being removed.\n\nThis is not quite the same as containing nothing.\n\nThe distinction has kept standards committees nourished for years.",
        listOf(
            Entry(
                "rmdir", "remove directory",
                "Removes an empty directory.\n\nThis seems reasonable.\n\nOne should not demolish a building while people are still inside it.\n\nUnfortunately a directory always has, conceptually if not visibly in every modern implementation, a relationship to itself and to its parent.\n\nSo before rmdir removes a directory, the system must decide that these do not count as occupants.\n\nThis principle was adopted by the hotel industry.\n\nA hotel containing only the manager and the owner's mother is officially empty.\n\nThe manager objected.\n\nThe owner's mother did not.\n\nShe had been trying to get the place condemned since breakfast.\n\nrmdir is stricter about actual contents.\n\nIf the directory is not empty, it refuses.\n\nIt is a cautious form of abolition: the place may cease to exist only after proving nobody is using it.",
                animation = "A tiny hotel is marked:\n\nEMPTY DIRECTORY\n\nA demolition officer checks rooms.\n\nRoom 1: empty.\n\nRoom 2: empty.\n\nLobby: a figure labelled . points at itself.\n\nUpstairs: another labelled .. points out the window toward a larger hotel.\n\nThe officer studies the regulations.\n\nA footnote reads:\n\nTHESE DON'T COUNT.\n\nBoth figures look offended.\n\nThe hotel vanishes.\n\n.. is left pointing at nothing for half a second, then hurriedly points somewhere else.",
                note = null
            )
        )
    ),
    Chapter(
        "chapter_04", "Ghost Dependencies",
        "A package is called a package because several things have been wrapped together.\n\nInstalling it usually involves unpacking it.\n\nThus the first significant act performed upon a package is to make it stop being a package.\n\nNobody in software finds this strange.",
        listOf(
            Entry(
                "dpkg", "Debian package manager",
                "Installs, removes and inspects Debian packages and maintains information about their installed state.\n\nWhen installing a package, dpkg can unpack its files and configure the package.\n\nUnpack is a destructive word disguised as housekeeping.\n\nA packed suitcase is a suitcase.\n\nAn unpacked suitcase is a suitcase surrounded by evidence.\n\nA packed lunch is lunch.\n\nAn unpacked lunch is either lunch or an incident, depending on altitude.\n\nA software package survives unpacking only because computer science has decided that a package may refer both to the archive before installation and to the installed software after the archive has been opened.\n\nThis is exceptionally convenient.\n\nThe planet Frrrm tried the same terminology with diplomatic parcels.\n\nCustoms officials were permitted to open any package provided they continued referring to the contents as the package afterward.\n\nWithin a year there were no sealed parcels anywhere in the system.\n\nThere were, however, several extremely well-informed customs officials.\n\ndpkg remains more disciplined.\n\nMostly.",
                animation = null,
                note = null
            )
        )
    ),
    Chapter(
        "chapter_05", "The Anomaly",
        "This chapter was found while searching for Chapter 5.\n\nIt is not Chapter 5.\n\nThis disappointed everybody except the search command, which had fulfilled its contract precisely.",
        listOf(
            Entry(
                "find", "find",
                "Searches directory trees for entries matching criteria you specify.\n\nfind is badly named.\n\nFinding something generally implies that you did not know where it was.\n\nfind, however, begins by asking where it should look and what sort of thing it should look for.\n\nThis is less like finding and more like employing a detective who opens the interview with:\n\nWhere is the body, what is the victim called, how old is it, when was it modified, and exactly how shall I recognize it?\n\nOnce supplied with enough information, the detective becomes extraordinarily effective.\n\nMany mysteries would be solved much faster if the missing person had the decency to provide a pathname.\n\nWhen asked to locate Chapter 5, find returned this file.\n\nThe editors explained that this was the lower-case chapter_05.\n\nfind pointed out that nobody had specified capitalization.\n\nThe editors have since become more careful with questions.\n\nThis is the first documented case in which a bad question improved the Guide.\n\nChapter 5 remains missing.\n\nfind considers the case closed.",
                animation = "A detective sits behind a desk.\n\nA distraught editor says:\n\n\"Chapter 5 is missing.\"\n\nThe detective opens a notebook.\n\n\"Where should I look?\"\n\nThe editor answers.\n\n\"What is it named?\"\n\nThe editor answers.\n\n\"File type?\"\n\nAnswer.\n\n\"Modification time?\"\n\nAnswer.\n\n\"Size?\"\n\nAnswer.\n\nThe detective closes the notebook, reaches under the desk and produces a file.\n\nchapter_05\n\nThe editor says:\n\n\"That's not it.\"\n\nThe detective circles the exact search criteria in red.",
                note = null
            )
        )
    ),
    Chapter(
        "chapter_06", "The Mirror Editor",
        "There are commands whose names describe what they do.\n\nThere are commands whose names once described what they did.\n\nAnd there is cat, whose name is what remains after the useful part of concatenate has been removed.",
        listOf(
            Entry(
                "cat", "concatenate",
                "Writes file contents to standard output, one after another.\n\nWith several files, this can concatenate them into one stream.\n\nWith one file, it concatenates the file with nothing.\n\nThis sounds pointless until one remembers that zero and nothing have extremely successful careers in mathematics.\n\nThe Society for the Concatenation of One Thing argued that a single item could not properly be concatenated because there was nothing to join it to.\n\nThe Unix committee demonstrated:\n\ncat file\n\nThe file appeared.\n\nThe Society replied that this proved only that the command could display a file.\n\nUnix agreed.\n\nThe command remained cat.\n\nThe Society spent the next fourteen years drafting a response and accidentally concatenated all its minutes into one document.\n\nThere were no separators.\n\nNobody could determine where the first meeting ended.\n\nThe Society still appears to be in session.",
                animation = "Two files approach a machine marked:\n\nCONCATENATE\n\nThey enter separately.\n\nOne continuous ribbon of text comes out.\n\nThen a single file approaches.\n\nIt hesitates.\n\nThe machine gestures impatiently.\n\nThe file enters.\n\nThe same file comes out.\n\nA committee in the corner erupts into furious debate.\n\nThe machine prints the committee minutes as one enormous uninterrupted sheet.",
                note = null
            )
        )
    ),
    Chapter(
        "chapter_07", "Zombie Processes",
        "A process is something happening.\n\nA status is a statement about how something is.\n\nCombining the two was always going to create trouble.",
        listOf(
            Entry(
                "ps", "process status",
                "Reports information about processes at the moment the information is collected.\n\nThe important phrase is at the moment.\n\nBy the time you read the result, that moment is history.\n\nSome processes may have ended.\n\nOthers may have begun.\n\nA sufficiently short-lived process can begin and finish between two invocations and never appear in either report.\n\nps therefore answers a question about the present by producing a very recent past.\n\nArtificial Wisdom proposed asking:\n\n\"Still?\"\n\nps answered with another snapshot.\n\nThis was admitted as evidence for both sides.",
                animation = "A crowded station of little processes freezes.\n\nCamera flash.\n\nps\n\nA photograph drops out.\n\nThe live station immediately resumes.\n\nSeveral processes leave.\n\nNew ones arrive.\n\nOne catches fire.\n\nThe user studies the photograph and points confidently at a process which is no longer there.\n\nBehind them, the station has become completely different.\n\nCaption:\n\nCURRENT PROCESSES\n\nA small pencil writes underneath:\n\nat the time.",
                note = null
            )
        )
    ),
    Chapter(
        "chapter_08", "Backing Up the Void",
        "An archive preserves things by putting them somewhere they are less convenient to use.\n\nMuseums have done this for centuries.\n\nComputers merely added compression.",
        listOf(
            Entry(
                "tar", "tape archive",
                "Creates, extracts and manipulates archives containing files, historically for sequential storage on tape.\n\nThe name still contains tape.\n\nMost users do not.\n\nThis is an excellent example of technological progress.\n\nThe technology disappears.\n\nThe filename extension remains.\n\nA tape archive may now live on flash storage, solid-state memory, a network filesystem or a device containing no tape, no reels and no moving parts whatsoever.\n\nThe tape survives as an idea, which is more than can be said for most tape drives.\n\nThe hardware left.\n\nThe noun got tenure.\n\ntar can preserve directory structure and various file metadata, depending on the archive format, options and destination.\n\nThis is roughly what luggage was invented to do.\n\nLuggage has never supported --extract.\n\nAirports consider this a competitive advantage.",
                animation = null,
                note = null
            )
        )
    ),
    Chapter(
        "chapter_09", "Looping the Loop",
        "Searching is usually described as looking for something.\n\nPattern matching improves efficiency by deciding in advance what the thing must look like.\n\nThis does occasionally exclude the possibility that the thing has changed clothes.",
        listOf(
            Entry(
                "grep", "pattern search",
                "Searches input for lines matching a pattern and, by default, prints the matching lines.\n\nThe pattern may be a regular expression.\n\nA regular expression is an expression so formally regulated that punctuation has acquired legal powers.\n\nIn ordinary grep regular-expression syntax, . can match a single character, * means zero or more repetitions of the preceding expression, and bracket expressions describe acceptable characters.\n\nPunctuation was given these powers during an emergency and has declined to return them.\n\nMore curious is what grep returns.\n\nAsk it for a pattern and, by default, it prints the line containing the match.\n\nThis is like asking a waiter for an olive and receiving the entire martini because the olive was in it.\n\nUsually this is helpful.\n\nOccasionally the line is 40,000 characters long.\n\nThe martini then requires two hands.\n\ngrep is particularly good at finding answers when you know what part of the answer looks like.\n\nWith the Guide learning to ask questions, this remains usefully backwards.",
                animation = "A vast rubbish heap of text.\n\nA tiny searchlight shaped like a regular expression sweeps across it.\n\nIt catches one word.\n\nInstead of lifting the word out, a crane grabs the entire line.\n\nThe line is absurdly long.\n\nIt drags half the rubbish heap with it.\n\nA small sign appears:\n\nMATCH FOUND\n\nUnderneath:\n\nand quite a lot of its line.",
                note = null
            )
        )
    ),
    Chapter(
        "chapter_10", "The Ultimate Handshake",
        "A network interface is the place where one system meets a network.\n\nThis is not the same as the place where two systems understand one another.\n\nThat would be miraculous and is handled by higher layers.",
        listOf(
            Entry(
                "ifconfig", "interface configuration",
                "Displays or configures network-interface parameters where the operating system permits it.\n\nIn Termux, ifconfig is available through net-tools, but modern Android versions may restrict access to network information or configuration through kernel and SELinux policy. The binary can therefore exist perfectly well while some of the information it wants is behind a door Android will not open.\n\nNetworking regards this as a particularly pure interface.\n\nAn interface may have an IPv4 address and a netmask.\n\nThe address identifies an endpoint within the addressing scheme.\n\nThe netmask indicates which address bits belong to the network prefix.\n\nIt does not conceal the address.\n\nIt is therefore a mask whose chief purpose is to explain the face.\n\nThe Witness Protection Programme of Folfanga tried this once and was dissolved by the first competent subnetter.",
                animation = "A nervous interface sits at a desk wearing a mask.\n\nAn investigator asks:\n\n\"Address?\"\n\nIt hands over the address.\n\n\"Mask?\"\n\nIt hands over the mask.\n\nThe investigator combines them and marks the network prefix.\n\nThe interface looks suddenly exposed.\n\nA second investigator from Android Security enters and closes the file cabinet.\n\nPermission denied\n\nEveryone agrees the mask was not the main problem.",
                note = null
            )
        )
    ),
    Chapter(
        "Chapter 16", "Documentation",
        "Documentation is information explaining how something works.\n\nIts principal occupational hazard is being written by someone for whom the thing already does.\n\nThe author begins with an answer.\n\nThe reader arrives carrying the missing question.\n\nExamples, tutorials, reference manuals and frequently asked questions are various attempts to make the two meet somewhere in the middle.\n\nThe word obviously is a less successful attempt.\n\nAfter harden-check reported that no obvious issues were found, Security asked Documentation to define the term.\n\nDocumentation replied that its meaning was obvious.\n\nA small interdepartmental fire followed.",
        listOf(
            Entry(
                "man", "manual",
                "Displays manual pages for commands and system interfaces.\n\nA manual is supposed to answer the question How does this work?\n\nman improves on this by first requiring you to know the name of the thing whose workings you do not understand.\n\nThis is fairer than it sounds. Most confusion has a name eventually.\n\nManual pages contain sections such as NAME, SYNOPSIS, DESCRIPTION, OPTIONS and SEE ALSO.\n\nSEE ALSO is where the manual quietly admits that understanding this page may require understanding several other pages.\n\nFollowing these references is educational in the same way that walking through an unfamiliar city is educational: eventually you know a great deal about places you never meant to visit.\n\nThe Guide once followed SEE ALSO while trying to rename a file.\n\nIt learned about signals, printers and magnetic tape.\n\nThe file kept its name.",
                animation = "A user opens:\n\nman command\n\nAt the bottom:\n\nSEE ALSO: other(1)\n\nThey follow it.\n\nAnother page points to three more.\n\nPages branch into a paper maze.\n\nAt the centre, the original command quietly performs the desired operation.\n\nThe user is elsewhere reading about magnetic tape.\n\nA sign appears:\n\nYOU ARE HERE\n\nIt points to the manual.",
                note = null
            ),
            Entry(
                "which", "which",
                "Searches the directories in PATH for executable files matching a command name and prints matching pathnames.\n\nWhich is a question fragment.\n\nFor years this allowed it to operate inside the Guide without attracting the Inquiry historians.\n\nIts usual implied question is narrower than people sometimes assume:\n\n\"Which executable file with this name can I find on PATH?\"\n\nThat is not always identical to:\n\n\"What will this shell run?\"\n\nShells may also know aliases, functions and built-ins, and may apply their own command-resolution rules.\n\nWhen Artificial Wisdom finally subpoenaed which as evidence that questions predated Chapter 15, the Shell Department objected on jurisdictional grounds.\n\nwhich had been asking a question all along.\n\nIt had simply been answering a smaller one.\n\nChapter 3 has filed a separate complaint about PATH, which is not one path but a list of directories in which paths may be found.\n\nThe complaint is currently lost somewhere on PATH.",
                animation = null,
                note = null
            ),
            Entry(
                "help", "shell help",
                "Displays help for shell built-ins where supported.\n\nWith no topic, Bash's help lists built-ins and usage information; with a built-in name, it explains that built-in.\n\nThe Inquiry historians classify help as a proto-question.\n\nDocumentation objects that it is grammatically an imperative.\n\nArtificial Wisdom replies that help still announces the existence of a missing answer before specifying exactly what the question is.\n\nThe dispute has become one of the Guide's oldest surviving examples of people asking for clarification about asking for clarification.\n\nShell History has preserved every draft.",
                animation = null,
                note = null
            )
        )
    ),
    Chapter(
        "Chapter 17", "Time & Memory",
        "Computers remember with extraordinary precision and forget with extraordinary efficiency.\n\nHumans do the opposite.\n\nThis has made collaboration difficult.\n\nA computer may remember the exact command you typed at 2:13 in the morning six months ago.\n\nYou may remember only that it seemed like a good idea.\n\nBoth accounts are accurate.\n\nOnly one is useful in court.",
        listOf(
            Entry(
                "history", "shell history",
                "Displays commands previously entered in the shell, subject to the shell's history settings.\n\nShell history is not a record of what happened.\n\nIt is a record of what was entered.\n\nA command may have failed.\n\nA file may not have existed.\n\nA connection may have timed out.\n\nHistory records the instruction and generally leaves motive, outcome and regret to other departments.\n\nThis makes it unusually compatible with the old Guide.\n\nIt preserves answers without preserving the questions that made them seem sensible.\n\nAfter Artificial Wisdom arrived, historians began searching old command lines for evidence of earlier questions.\n\nThey found mostly:\n\ncd wrong\n\ncd ..\n\ncd right\n\nand one entry reading:\n\nhelp\n\nThe inquiry remains open.",
                animation = "A historian opens an enormous volume labelled:\n\nSHELL HISTORY\n\nLines of commands stretch backward for years.\n\nThe historian stops at:\n\nrm important_file\n\nLong silence.\n\nThey look for the preceding question.\n\nThere isn't one.\n\nA marginal note is added:\n\nMOTIVE UNKNOWN",
                note = null
            ),
            Entry(
                "sleep", "sleep",
                "Pauses for a specified duration.\n\nThe shell already has a wait operation, used to wait for jobs or processes to finish.\n\nThe Somnological Society objected that waiting for time and waiting for a process were plainly different kinds of waiting.\n\nThe Shell Department agreed.\n\nThat was why it needed two words.\n\nThe Society then proposed pause.\n\nSomeone produced three other systems already using it for different things.\n\nBy sunset the hearing had established that computing contains more varieties of doing nothing than English had reserved names for.\n\nsleep kept its name.\n\nIt wakes when the requested interval has elapsed and, unlike most committees, requires no coffee.",
                animation = null,
                note = null
            ),
            Entry(
                "touch", "touch",
                "Updates a file's access and modification timestamps, and commonly creates an empty file if the named file does not already exist.\n\nTouch is one of the least accurate names in computing.\n\nNothing touches anything.\n\nThere are no fingers.\n\nThe screen remains disappointingly smooth.\n\nWhat changes is time.\n\nTouch an existing file and its timestamps may change.\n\nTouch a nonexistent file and, under ordinary use, an empty file appears.\n\nThis is a remarkable amount of metaphysical authority for a gesture.\n\nThe theologians of Viltvodle VI immediately claimed that creation itself had always been a form of touching.\n\nThe physicists objected that touching requires contact.\n\nThe theologians pointed out that God is outside space and therefore cannot make contact in the usual sense.\n\nEveryone looked briefly at the birthday of Zorblep Thwack.\n\nThe physicists withdrew the objection.\n\nThe Birthday Committee has since asked Chapter 1 to stop being cited in theological disputes. Chapter 1 has agreed to consider the request before Zorblep's next birthday.\n\ntouch remains the command in this chapter which can make an empty file exist merely because you asked to update the time on a name that was not there.",
                animation = "A finger approaches an empty patch of screen.\n\nIt never quite makes contact.\n\ntouch newfile\n\nA file appears anyway.\n\nThe finger looks offended.\n\nIt points at an old file.\n\ntouch oldfile\n\nA clock above the file jumps forward.\n\nThe file itself does nothing.\n\nA theologian enters, opens a notebook, and writes:\n\nPROMISING.",
                note = null
            )
        )
    ),
    Chapter(
        "Chapter 18", "Agreement",
        "Unix command exit status has a wonderfully asymmetric vocabulary.\n\n0 conventionally means success.\n\nNon-zero values mean that something else happened, with the particular value available to say what sort of else.\n\nThis is considerably more nuanced than yes and no, provided nobody asks it a philosophical question.",
        listOf(
            Entry(
                "yes", "yes",
                "Repeatedly outputs a string, y by default, until stopped or until its output can no longer be written.\n\nA normal yes is an answer to a question.\n\nyes dispenses with the question.\n\nThe old Guide found this perfectly conventional.\n\nThe command is often used to supply repeated affirmative input to another program, saving a human from typing the same answer again and again.\n\nThe Consent Office objected that an indefinitely generated yes contains no record of what was agreed to.\n\nArtificial Wisdom objected that this was not a defect but an important archaeological clue.\n\nHere, long before Chapter 15, was an answer-producing machine entirely untroubled by the absence of a question.\n\nyes declined to comment and continued:\n\ny\n\ny\n\ny\n\nuntil something stopped it.",
                animation = "A small machine is asked:\n\n\"Continue?\"\n\nYES\n\n\"Again?\"\n\nYES\n\nThe questioner leaves.\n\nYES\n\nYES\n\nCenturies pass.\n\nRuins.\n\nYES\n\nAn archaeologist arrives.\n\nThe machine says:\n\nYES\n\nThe archaeologist has not yet asked anything.\n\nArtificial Wisdom quietly labels the exhibit:\n\nPRE-INQUIRY PERIOD",
                note = null
            ),
            Entry(
                "true", "true",
                "Performs no operation beyond reporting a successful exit status.\n\nIt does not inspect a statement.\n\nIt does not compare a claim with reality.\n\nIt simply exits successfully.\n\nThe Truth Office objected that this was not truth.\n\nUnix replied that the command had never accepted a proposition in the first place.\n\nThe Office is still trying to determine who made the claim.",
                animation = null,
                note = null
            ),
            Entry(
                "false", "false",
                "Performs no operation beyond reporting an unsuccessful exit status.\n\nThis is nearly the same activity as true.\n\nOnly the status differs.\n\nThe Guide's new question-asking machinery is fascinated by both commands.\n\nHere are true and false already available as answers, and neither requires anybody to supply a proposition.\n\nThe Truth Office has now opened a second case.\n\nIt has no questions for either witness.",
                animation = "Two identical boxes sit side by side.\n\nPress true.\n\nNothing visible happens.\n\nstatus: 0\n\nPress false.\n\nNothing visible happens.\n\nstatus: 1\n\nA researcher asks:\n\n\"What proposition did you evaluate?\"\n\nBoth boxes remain silent.\n\nFor once, this is the technically correct answer.",
                note = null
            )
        )
    ),
    Chapter(
        "Chapter 19", "Pipes & Redirection",
        "A pipe is normally a hollow object through which something passes.\n\nUnix retained the hollow part and discarded the object.\n\nThis was considered efficient.\n\nThe shell pipe, written |, takes the output of one command and feeds it into the input of another.\n\nNothing visible travels through it.\n\nThis does not prevent everyone from calling it a pipe.\n\nPlumbing has filed no formal complaint, largely because plumbing has seen computers try to name sockets.",
        listOf(
            Entry(
                "|", "pipe",
                "Connects the standard output of one command to the standard input of another.\n\nThis means one command may answer a question it was never asked, and another may receive that answer without knowing who supplied it.\n\nThis is how the Guide was edited for centuries, except the researchers had names.\n\nBefore pipes, commands tended to speak into the terminal where humans could see what they said.\n\nAfter pipes, commands could speak directly to other commands.\n\nThis was the beginning of machine gossip.\n\nA command at the left end may produce a perfectly respectable list of files.\n\nThe command at the right end may immediately filter, count, sort or otherwise reinterpret that list until the original command would barely recognize its own output.\n\nHumans call this processing.\n\nThe files call it editorial interference.\n\nChapter 15 has expressed concern that the Guide's new question-asking machinery may be constructed largely from very fast gossip.\n\nThe Pipe Committee has responded by piping the concern to /dev/null.",
                animation = "A command speaks into a large brass pipe.\n\nWords shoot through.\n\nAt the far end another command receives them, crosses several out, circles two, rearranges the rest and sends them through another pipe.\n\nSoon an entire city of commands is connected by pipes carrying text in every direction.\n\nA researcher asks:\n\n\"Who said that?\"\n\nEvery pipe points left.\n\nThe camera follows the pointing pipes until they disappear off-screen.",
                note = null
            ),
            Entry(
                ">", "redirect output",
                "Redirects standard output to a file, creating it if necessary and ordinarily truncating an existing regular file before the command runs.\n\nThe symbol points to the right.\n\nThis is helpful.\n\nIt gives the impression that output is going somewhere.\n\nThe output agrees.\n\nThe existing contents of the destination file may be less enthusiastic.\n\nWith ordinary > redirection, what was there before is replaced.\n\nThis has occasionally surprised users who believed redirect meant send somewhere else rather than send somewhere else after clearing the destination of witnesses.\n\nAn arrow shows direction.\n\nIt does not show manners.\n\nEditor's note: The File Rights Committee attempted to classify > as forced eviction. The Shell Committee replied that files do not have tenancy agreements. Chapter 2 is considering an appeal.",
                animation = null,
                note = null
            ),
            Entry(
                ">>", "append output",
                "Redirects standard output to the end of a file without replacing the existing contents.\n\nTwo arrows are apparently more considerate than one.\n\nNobody knows why.\n\nThe first > says:\n\nPut this there.\n\nThe second appears to add:\n\nAnd don't throw away what was already there.\n\nThis is not how punctuation usually works.\n\nTwo exclamation marks do not make a sentence more careful.\n\nTwo question marks do not make a question more legally binding.\n\nYet shell syntax has managed to create an ethical distinction by duplication.\n\nThe Guide's lower-case index considers this precedent extremely important.\n\nIt has requested a second underscore.\n\nThe request was denied because nobody knew what it would mean.",
                animation = null,
                note = null
            )
        )
    ),
    Chapter(
        "Chapter 20", "Copies & Movement",
        "Files may be copied or moved.\n\nHumans may also be copied or moved, but both operations are subject to significantly more paperwork.\n\nThe distinction between copying and moving seems obvious until one asks what, precisely, is moving.\n\nA file has a name, contents, metadata, a place in a filesystem and, according to Chapter 2, an owner whether it likes it or not.\n\nMove the name and the contents may not move at all.\n\nMove across filesystems and the system may copy the contents and remove the original.\n\nThe command is still called mv.\n\nLanguage has learned not to watch too closely.",
        listOf(
            Entry(
                "cp", "copy",
                "Copies files, or directories when the appropriate recursive options are used.\n\nA copy is close enough to the original to be useful and different enough to require another pathname.\n\nThis sounds simple until somebody asks what, exactly, must remain the same.\n\nContents?\n\nTimestamps?\n\nPermissions?\n\nOwnership?\n\nLinks?\n\nExtended metadata?\n\ncp has options for preserving selected attributes, and filesystems differ in what they can represent.\n\nThe Copy Commission therefore abandoned its original definition—THE SAME THING, AGAIN—after discovering that it described neither copying nor anything else.\n\nFile Rights has since ruled that a copied file acquires its own legal problems.\n\nDirectory Workers insist that moving those problems is Chapter 20's concern.",
                animation = "A file enters a copying booth.\n\nTwo files emerge with identical contents.\n\nAn inspector compares their name cards.\n\nDifferent.\n\nChecks timestamps and permissions.\n\nPossibly different.\n\nChecks the data.\n\nSame.\n\nThe inspector writes:\n\nCOPY\n\nthen adds, in smaller letters:\n\nsubject to options and local law",
                note = null
            ),
            Entry(
                "mv", "move",
                "Moves or renames files and directories.\n\nThe fact that the same command both moves and renames things has caused unnecessary metaphysical excitement.\n\nRename a file in the same directory and it has changed identity without changing location.\n\nMove it to another directory and it has changed location without necessarily changing its contents.\n\nMove it elsewhere and give it a new name at the same time, and several philosophers will follow it for grant money.\n\nWithin a filesystem, mv can often change directory entries rather than hauling the file's data physically from one patch of storage to another.\n\nSo the file may move without moving.\n\nChapter 1's cd considers this cheating.\n\nmv replied that cd changes the user without moving the directory.\n\nThe dispute has been referred to Relativity.\n\nRelativity has asked everyone to specify a frame of reference.\n\nNobody has.",
                animation = "A file stands on a stage marked /old/place/file.txt.\n\nmv\n\nThe scenery slides sideways.\n\nThe file remains perfectly still.\n\nNew sign:\n\n/new/place/file.txt\n\nA physicist applauds.\n\nThen:\n\nmv file.txt answer.txt\n\nOnly the name card changes.\n\nThe physicist stops applauding and begins filling out a form.",
                note = null
            )
        )
    ),
    Chapter(
        "Chapter 21", "Things From Elsewhere",
        "The Internet made it possible to obtain information from almost anywhere.\n\nThis was immediately followed by the discovery that almost anywhere contains information nobody had requested.\n\nTools were therefore invented to fetch things deliberately.\n\nThis made the problem feel much more professional.",
        listOf(
            Entry(
                "curl", "transfer data with URLs",
                "Transfers data to or from a URL using supported network protocols.\n\nThe curl project describes the name as a play on Client for URLs, which has the useful property of sounding almost like an instruction after everyone forgets the explanation.\n\nA URL tells curl where to make a request.\n\nThe remote server responds.\n\nSometimes the response is a redirect.\n\nA redirect is an answer containing the address of the next question.\n\nIf instructed to follow redirects, curl can take that new address, ask again, receive another answer, and continue.\n\nArtificial Wisdom immediately claimed this as evidence of pre-AI curiosity.\n\nNetworking rejected the claim on the grounds that following directions is not curiosity.\n\nThe Redirect Subcommittee asked where the distinction could be found.\n\nNetworking sent a URL.\n\nIt returned 301.",
                animation = "A tiny courier marked curl receives a URL.\n\nIt knocks on a server.\n\nThe server hands over a card:\n\nLocation: elsewhere\n\nThe courier goes elsewhere.\n\nAnother card.\n\nAnother address.\n\nAgain.\n\nAgain.\n\nThe courier eventually arrives back at the first building.\n\nThe server hands over one final card:\n\nLocation: elsewhere\n\nThe courier writes on the back:\n\nQUESTIONABLE RESEARCH METHOD",
                note = null
            ),
            Entry(
                "wget", "network downloader",
                "Retrieves resources from network locations, commonly over HTTP, HTTPS and FTP where supported.\n\nWhatever archaeology produced the name, wget has the manners to read like an instruction:\n\nweb: get\n\nThe web has a thing.\n\nwget retrieves the thing.\n\nWith recursive options it may also get things linked from the thing, and things linked from those things, subject to rules you should understand before discovering you have attempted to preserve a significant fraction of civilization.\n\nThe command embodies the central dream of every collector:\n\nI shall save this in case it disappears.\n\nThe Internet's response has generally been:\n\nVery well. Here are six thousand dependencies, thumbnails, style sheets, tracking pixels and a cookie notice.\n\nChapter 4 has asked whether this counts as package management.\n\nwget has downloaded the question for offline reading.\n\nResearchers repeatedly ask which tool they should use.\n\nThe Guide contains several answers.\n\nUnfortunately they answer different questions.\n\ncurl is often convenient for making requests, inspecting responses, working with APIs and moving data through pipelines.\n\nwget is often convenient for retrieving files, mirroring resources and recursive downloads.\n\nThis distinction remained tidy for almost eleven minutes.\n\nThen both tools acquired more features.\n\nThe editors drew a boundary between them.\n\nThe boundary returned 301 Moved Permanently.",
                animation = null,
                note = null
            )
        )
    )
)

/** Chapter label + title + intro + entries, for the index. */
data class GuideChapter(val label: String, val title: String, val intro: String, val entries: List<GuideEntry>)

object GuideBook {
    val chapters: List<GuideChapter> = CHAPTERS.map { ch ->
        GuideChapter(ch.label, ch.title, ch.intro, ch.entries.map { e ->
            GuideEntry(e.cmd, e.full, e.blurb, e.animation, e.note, ch.label, ch.title, ch.intro)
        })
    }

    /** Every entry, flattened in reading order - the order Next/Prev and the entry index walk. */
    val entries: List<GuideEntry> = chapters.flatMap { it.entries }
}
