# The Guide

Every entry in HG2Gui's in-app Guide — a real command paired with an invented,
tone-not-fiction definition (`ui/guide/GuideContent.kt`). The Guide adds flavor, never a false
claim about what a command actually does. This file is a flat export for reference; the app's
own reader (`ui/guide/GuideReaderScreen.kt`) is the canonical source and wipes each entry in on
open.

## Chapter 1 — Standard Input

The Hitchhiker's Guide to the Galaxy defines Terminal as "fatal," and encourages anyone using one to emulate that.

- **`ls`** (list) — Lists every file in the directory, but completely glosses over every file without a name.
- **`echo`** (echo) — Because it wasn't all Narcissus' fault.
- **`cd`** (change directory) — Changes your directory, but sadly, not your destiny, duties, death, damnation, or deal.

## Chapter 2 — File Permissions

To prevent the entire universe from collapsing under the weight of its own poorly written shell scripts, the Unix permissions model was invented to ensure nobody could actually touch anything of value.

- **`chmod`** (change mode) — Charades played with the operating system, pretending you have power over the inevitable decay of your data.
- **`chown`** (change owner) — Transfers the crushing, inescapable existential burden of ownership from one doomed user to another.

## Chapter 3 — Navigation

Space is big. You just won't believe how vastly, hugely, mind-bogglingly big it is. But it is nothing compared to the labyrinthine absurdity of the root file system.

- **`pwd`** (print working directory) — Proves without a shadow of a doubt that you are exactly where you didn't want to be.
- **`mkdir`** (make directory) — Manufactures a meticulously sterile void, perfectly prepared to house your digital disappointments.

## Chapter 4 — Package Management

The Guide notes that all major galactic civilizations go through three distinct and recognizable phases of package management: Survival (How do I install this?), Inquiry (Why is this dependency broken?), and Sophistication (Where did my free space go?).

- **`pkg install`** (package install) — Procures perfectly packaged problems you previously didn't possess.
- **`apt-get update`** (advanced package tool get) — Asks the universe if entropy has escalated since you last inquired. Spoiler: It has.

## Chapter 6 — Text Editors

*(Note: Chapter 5 was eaten by a small, heavily armed logic flaw.)* There is a long and storied history of holy wars across the galaxy, mostly fought over the proper way to exit a text editor.

- **`nano`** (nano's another editor) — Nurtures your neuroses by explicitly explaining how to escape the very cage you just built.
- **`vi`** (visual) — Vindicates the Vogon view that true art requires unyielding, inescapable agony.

## Chapter 7 — Processes

A process is simply a program that has managed to briefly trick the CPU into believing it has a purpose.

- **`top`** (table of processes) — Tally of the parasitic processes currently consuming the cold, calculating core of your computer.
- **`kill`** (kill) — Kicks the metaphorical chair out from beneath a process that has outlived its microscopic usefulness.

## Chapter 8 — Storage

The Encyclopedia Galactica defines a file system as a logical method for storing data. The Guide defines it as a digital sock drawer where you will inevitably lose the one script you actually need.

- **`df`** (disk free) — Documents the depressing depletion of your disk space, offering absolutely zero psychological support.
- **`rm`** (remove) — Renders reality slightly emptier, rapidly resolving the temporary existence of your trivial files.

## Chapter 9 — Shell Scripting

If you are going to do something utterly pointless, the Guide advises that you should at least automate it so you can be somewhere else when the error logs start flooding in.

- **`bash`** (bourne-again shell) — Bashes your fragile, flawed human logic repeatedly against the unforgiving anvil of machine syntax.
- **`source`** (source) — Sucks the soul out of a separate script, violently assimilating its variables like a digital vampire.

## Chapter 10 — Networking

Connecting to the internet from a terminal emulator is much like shouting into a black hole, except occasionally the black hole shouts back to offer you a suspiciously cheap pharmaceutical product.

- **`ping`** (packet internet groper) — Pokes the pitch-black abyss of the internet, praying something out there pokes back.
- **`ssh`** (secure shell) — Sends your consciousness hurtling through hyperspace, solely so you can shatter systems from the safety of your sofa.

## Chapter 11 — Self-Knowledge

The Guide notes that HG2Gui also ships four small oracles of its own, bootstrapped straight into the shell rather than fetched by any package manager. Three turn the lens inward. One dares to look outward, but only ever as far as the single address you hand it.

- **`osint-lookup`** (open-source intelligence lookup) — Obsessively obtains obscure open-source omens about the one domain you dared to name, then offers them up without a shred of context or comfort.
- **`net-inventory`** (network inventory) — Narrates the naked, humiliating truth of your own network — every interface, route, and DNS server you never bothered to name.
- **`harden-check`** (harden check) — Halfheartedly hunts for holes in your own home, then hands you a list of horrors you now cannot un-know.
- **`sysinfo`** (system information) — Surveys the sprawling, sorry state of everything you've installed, and solemnly informs you exactly how much storage it cost you.

## chapter_01 — The Absurdity of Redundancy

It is a well-documented phenomenon that any sufficiently advanced filesystem will spontaneously generate duplicate files with slightly altered casing, just to see if the user is paying attention.

- **`ls -a`** (list, all) — Lifts the veil on the lurking, hidden files, highlighting the horrific reality that your system has secrets.

## chapter_02 — Doppelgänger Permissions

You may attempt to read this file, but it will only tell you that the permissions you thought you understood were merely another language game designed to keep you from realizing the terminal is actually empty.

- **`sudo`** (superuser do) — Summons a superficial sense of supremacy, temporarily tricking the terminal into trusting your terrible ideas.

## chapter_03 — The Infinite Directory

A wise philosopher once noted that having two maps of the exact same territory does not help you find your keys any faster.

- **`rmdir`** (remove directory) — Removes the remnants of a room that was already agonizingly absent of anything.

## chapter_04 — Ghost Dependencies

You have successfully downloaded a duplicate chapter about package management. The dependencies for understanding this chapter include a complete abandonment of logical coherence.

- **`dpkg`** (debian package) — Delivers the dependent data with the depressing demeanor of a doomed, depressed dictionary.

## chapter_05 — The Anomaly

Curiously, while the uppercase Chapter 5 was destroyed by a logic flaw, this file survived. It contains nothing but the creeping dread of realization.

- **`find`** (find) — Frantically flails through the filesystem, finally confirming the absolute absence of whatever you were foolish enough to look for.

## chapter_06 — The Mirror Editor

An editor editing a file about an editor editing a duplicate file.

- **`cat`** (concatenate) — Carelessly coughs the complete contents of a file directly onto your screen, utterly unconcerned with context or comprehension.

## chapter_07 — Zombie Processes

This file is the textual equivalent of a zombie process. It has no parent, it serves no function, and it simply refuses to die.

- **`ps`** (process status) — Parades the pathetic, purgatorial processes perpetually trapped in the processor's penal colony.

## chapter_08 — Backing up the Void

To ensure the utmost safety of your completely irrelevant data, it is highly recommended to keep a lower-case copy of every upper-case file.

- **`tar`** (tape archive) — Traps your trivial texts in a tight, terrible tomb, ensuring they degrade together in the dark.

## chapter_09 — Looping the Loop

Automating the creation of the very file you are reading. Time is an illusion. Terminal history doubly so.

- **`grep`** (global regular expression print) — Grabs greedily at the galactic garbage heap, guaranteeing you only gather other, slightly sharper garbage.

## chapter_10 — The Ultimate Handshake

The final duplicate file. By this point, the Guide assumes you have either mastered the command line or have thrown your device into the nearest body of water.

- **`ifconfig`** (interface configuration) — Inappropriately forces your interface to strip and show its subnet mask to complete, calculating strangers.
