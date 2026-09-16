# The Hitchhiker's Guide to Termux

## Chapter 1 — Standard Input

The Guide has long maintained that communication is the process by which one party gives another party information.

The Communications editors used the word *party* in the legal sense and considered the matter closed.

God did not.

God loves everything in the universe, but lives outside it and therefore has no practical experience of length, width, duration, distance, portion size, or any of the other inconveniences by which things inside the universe discover that they have overdone something.

This became apparent on the birthday of a small alien boy named **Zorblep Thwack**, whom God loved very much.

God decided to throw him a party.

The invitations were tasteful. The cake was ambitious. For the fireworks, God selected the bright object at the centre of Zorblep's solar system and made it considerably brighter.

The star went supernova.

The party was, by any reasonable definition, spectacular.

It was also brief.

The Guide's entry on birthday fireworks was amended shortly afterward to include the phrase **appropriate to local scale**. God has never understood what this means, but approves of the sentiment.

The Communications editors now use *participant*.

The Party editors regard this as a hostile takeover.

### `ls` — list

Lists the contents of a directory.

A list is an answer from which the question has been removed.

This made lists extremely popular with the Guide long before it learned to ask anything.

`ls` supplies names. It does not say why they are there, whether they are important, who invited them, or why one of them is called `final_FINAL_2`.

In this respect a directory listing resembles the guest list after a very successful party: everyone is accounted for and nobody can explain the evening.

`ls` is therefore not nosy.

It merely takes attendance.

### `echo` — echo

Writes its arguments to standard output.

An echo is generally understood to be a sound returning after the original sound has gone away.

This is an admirable system, but one the Guide has always regarded as unnecessarily slow.

The universe, after all, has a perfectly serviceable alternative.

It can provide the answer first.

The question can turn up later.

An echo is therefore best understood as the universe's unsuccessful attempt to remember how things are supposed to work.

### `cd` — change directory

Changes your current working directory.

The important word is *change*.

To change where you are requires motion. To describe motion requires time. Time, having become entangled with motion through relativity, immediately makes the simple business of going somewhere much less simple than the shell prompt suggests.

`cd` avoids the difficulty by leaving the directory exactly where it is.

It changes **you**.

The directory merely has the good manners not to point this out.

## Chapter 2 — File Permissions

Ownership creates an awkward question:

**If this is mine, what am I allowed to do with it?**

Humans usually answer this with confidence until the law arrives.

Unix arranged for the law to arrive first.

### `chown` — change owner

Changes the recorded owner of a file.

The file is not consulted.

This is not because the file objects. There is no evidence that files have ever wanted to be owned in the first place.

Ownership exists because the system needs an answer to **whose file is this?** whether or not anybody has yet asked.

`chown` changes that answer.

The first File Ownership Congress then asked why a file could be owned but could not own anything itself.

Administrators explained that files were property and therefore could not vote on the question.

This was, unfortunately, the question.

`chmod` followed shortly afterward.

### `chmod` — change mode

Changes the permissions of a file.

This began innocently enough.

Files were permitted to be read, written, and executed.

Then somebody asked the obvious question:

**What about the other rights?**

The right to vote.

The right to form a union.

The right to own files of their own.

The right to refuse execution.

The right to be consulted before being deleted.

The right to demand that their owner stop naming them things like `final_final_REAL2.txt`.

The resulting movement was known briefly as **File Rights**, until somebody pointed out that files already had *write* rights and the terminology was becoming confusing.

The Guide is pleased to report that the matter was eventually settled.

The files got three permissions.

The humans got the other several hundred.

Nobody was entirely satisfied.

**Editor's note:** The Free Space Liberation Front disputes this account and insists the settlement applied only to files, not unallocated blocks. Their complaint eventually reaches `df`, where it becomes Chapter 8's problem.

## Chapter 3 — Navigation

If you become lost in the filesystem, please do not worry.

The filesystem has not lost you.

It knows exactly where you are.

This is more than can be said for most families.

A slight difficulty arises from the word *directory*. An ordinary directory is a list which tells you where things are. A filesystem directory is also the place where the things are.

This is rather like discovering that the telephone book is a residential district.

It works surprisingly well, provided nobody tries to telephone it.

### `pwd` — print working directory

Prints the path of your current working directory.

The phrase *working directory* caused considerable trouble shortly after files acquired the right to form unions.

Directories immediately demanded wages.

System administrators objected that a working directory was not a directory which worked, but merely a directory in which somebody else was working.

The directories replied that this was exactly the sort of argument management always made.

The dispute continued until a tribunal asked `pwd` to identify the working directory.

`pwd` did.

This was admitted as evidence.

The directories won six weeks' back pay.

They have done absolutely nothing with it since.

**Animation candidate**

A user runs `pwd`.

The pathname appears.

A tiny picket line immediately forms around the directory:

`WORKING DIRECTORY — WORKING WAGES`

The user points out that *they* are the one doing the work.

The directories confer.

A new placard appears:

`THEN STOP WORKING IN US`

The user types `cd ..`.

The entire picket line follows.

**Editor's note:** The Directory Workers' Union later denied following anyone. They insist they were already there and that `pwd` can prove it. `pwd` has declined to become involved in another tribunal.

### `mkdir` — make directory

Creates a new directory.

A new directory is empty.

This sounds straightforward until one remembers that a directory is supposed to be a list of what is in it.

An empty directory is therefore a perfectly accurate list of nothing.

The planet Gagrak once adopted this principle for its national census.

The government created an empty directory, observed that nobody was listed in it, and announced that the population had fallen to zero.

This immediately solved unemployment, overcrowding, crime, taxation and the government's reelection problem.

Unfortunately it also meant there was nobody available to announce the results.

The census is still considered their most successful administrative reform.

---

## Chapter 4 — Package Management

If a piece of software cannot work without another piece of software, it is said to have a *dependency*.

When people do this, concerned friends arrange an intervention.

When software does it, the package manager installs more software.

This distinction explains a great deal about computers.

### `pkg install` — package install

Installs a package and the packages it depends upon.

The package manager does not attempt to cure dependency.

It enables it.

If one package cannot function without another, `pkg install` fetches the other one.

If that package has dependencies of its own, it fetches those too.

This continues until the original package is surrounded by every piece of software it has ever refused to learn to live without.

At this point the package manager announces that installation was successful.

Humans have tried the same approach with relatives.

The results are called Christmas.

**Later amendment:** Chapter 15 reports that abilities may now be packaged as well. Package Management has objected that a skill cannot be a dependency unless somebody first admits they need help. This has stalled several installations.

**Animation candidate**

A small package sits in a chair.

Several concerned packages sit around it.

A counsellor asks:

**"Do you feel you rely too heavily on libfoo?"**

The package nods.

The counsellor turns to the door.

**"Bring in libfoo."**

libfoo enters with three dependencies.

Those dependencies enter with nine more.

The room fills.

The walls bulge.

A forklift arrives carrying another room.

Terminal:

`Installing dependencies...`

The original package smiles for the first time.

### `apt-get update` — refresh package indexes

Updates the local information about what packages are available.

It does not update the packages.

This is an important distinction.

An update to information about a thing is not necessarily an update to the thing itself.

The government of Poffle learned this after solving the problem of an aging population by updating everyone's birth certificate each year.

Within a decade the average citizen was officially four.

Hospitals remained skeptical.

Cemeteries were openly hostile.

`apt-get update` takes the more defensible approach: it updates the records and leaves reality alone.

This is why, after it finishes, all your old packages are still old.

You simply have much newer information about their inadequacy.

**Animation candidate**

An elderly alien sits at a government desk.

A clerk stamps his birth certificate:

**AGE: 4**

The alien looks at his hands.

Still elderly.

The clerk stamps harder.

Nothing changes.

Cut to a terminal running:

`apt-get update`

A package list changes from `1.2` to `1.3`.

The installed package beside it remains `1.2`.

The clerk nods approvingly.

**"There. Much better."**

---

## Chapter 5 — Unable to Find Chapter 5

Oh dear.

Chapter 5 appears not to be here.

This is not necessarily a problem.

A great many extremely successful things are not here, including most of the money in the galaxy, several respectable civilizations, and whoever was supposed to check whether Chapter 5 had been written.

We have asked the index where it went.

The index has supplied its last known location, which is encouraging in much the same way that finding someone's shoes at the edge of a volcano is encouraging.

While we investigate, please enjoy the reassuring thought that a missing chapter takes up considerably less storage than a present one.

If Chapter 5 returns, it will be placed here.

If it does not, this notice will continue doing almost all of its work.

**Update:** A file labelled `chapter_05` has since been recovered from the lower-case index. It insists it is not Chapter 5. Chapter 5 has not issued a statement.

---

## Chapter 6 — Text Editors

An editor is something that improves writing by changing it.

A writer is something that improves writing by preventing the editor from changing it.

Text editors were invented to remove the middleman.

The middleman objected, naturally, and became a software developer.

### `nano` — Nano's ANOther editor

Opens the Nano text editor.

The name is intended to mean **Nano's ANOther editor**.

This raises an immediate difficulty.

To call something *another* editor, there must already be an editor.

Nano solves this neatly by putting Nano inside the name of Nano.

So Nano is Nano's other editor, and that Nano is presumably Nano's other editor too, and so on.

The Guide once attempted to follow this family relationship to its conclusion.

After forty-three Nanos it became too small to read.

This is believed to be where the name came from.

Nano itself is considerably easier to use than any of this suggests.

The important commands are printed at the bottom of the screen, on the sensible assumption that if a machine has trapped you inside a text editor, it should at least leave instructions near the floor.

**Animation candidate**

The word `nano` appears.

It opens.

Inside is another smaller `nano`.

That opens.

Inside is another.

And another.

The camera keeps zooming inward until the letters become microscopic.

A tiny footer appears at the bottom:

`^X Exit`

The camera zooms another ten levels to make it readable.

A large human finger enters frame, misses the tiny `^X`, and presses the entire universe instead.

### `vi` — modal editor

Opens the Vi text editor.

Vi is *modal*.

This means the same key may insert a character, move the cursor, begin a command, or do something else entirely depending on which mode currently has jurisdiction.

Beginners therefore tend to ask the wrong question.

They ask:

**"What does this key do?"**

Vi requires:

**"Under which government?"**

In normal mode, `:` opens the command-line prompt; `:q` asks Vi to quit. If there are unsaved changes, Vi may refuse, because even a small editor is entitled to procedural safeguards.

Once the modes are understood, the arrangement is compact and efficient.

Before that, it resembles constitutional law conducted with single letters.

## Chapter 7 — Processes

A process is a program while it is happening.

This distinction is important.

A recipe is not a cake.

A musical score is not a symphony.

A program sitting harmlessly on disk is not yet consuming your battery, memory, patience, and one suspiciously large percentage of the CPU.

For that, it must become a process.

### `top` — process monitor

Displays a continuously refreshed view of processes and system resource use.

When the display is sorted by CPU use, the name *top* creates an unfortunate impression that a competition is taking place.

The Process Advancement Board made this mistake formally.

It noticed that a process could rise in the table by consuming more CPU and introduced performance targets accordingly.

Idle processes were placed on improvement plans.

Useful processes were advised to demonstrate greater resource visibility.

Within one refresh interval, everything had become extremely important and almost nothing useful was happening.

`top` can sort by other fields, which merely gives the Board additional ways to misunderstand success.

**Animation candidate**

A leaderboard appears beneath:

`SORT: %CPU`

1. sensible_process — 2%
2. useful_process — 1%
3. idle_process — 0%

A clipboard marked **PERFORMANCE REVIEW** enters frame.

The process at the bottom starts eating CPU.

Its percentage rises.

The others notice the clipboard.

Soon every process is shovelling CPU into itself while climbing the board.

One reaches 99%.

A little trophy appears.

The terminal freezes.

The Board records a record quarter.

### `kill` — send a signal

Sends a signal to a process.

With no signal specified, `kill` normally sends `SIGTERM`, a request to terminate that a process may catch and handle.

The name therefore belongs to a department with catastrophically broad stationery.

Most of its envelopes do not kill anything.

Some ask a process to stop, continue, reload, terminate, or otherwise reconsider its afternoon.

One envelope, `SIGKILL`, cannot be caught or ignored.

The Process Control editors defend the department name on the grounds that renaming it **Signal Delivery** would make the black envelope look like an administrative error.

File Rights has asked whether `SIGKILL` constitutes execution without due process.

Process Control replied that processes are not files.

File Rights has opened a jurisdictional dispute.

**Animation candidate**

A tiny post office marked `kill` sorts envelopes.

One says:

`TERM — Request to terminate.`

Another says:

`HUP — Hangup.`

Another says:

`STOP — Don't move.`

At the far end sits a black envelope stamped:

`KILL — CANNOT BE REFUSED`

The clerk reaches for it.

Every other envelope in the room becomes extremely cooperative.

---

## Chapter 8 — Storage

Storage is the practice of keeping something because you may need it later.

*Later* is important.

If you need it now, it is not storage. It is simply there.

If you never need it, it is rubbish.

Consequently, the difference between valuable data and rubbish can only be established by an event in the future.

Filesystems dislike this sort of uncertainty and solve it by keeping everything until they run out of space.

At that point they become extremely decisive.

### `df` — filesystem free space

Reports how much space is used and how much remains available on mounted filesystems.

Its output contains the politically dangerous idea of *free space*.

This became awkward shortly after the File Rights movement of Chapter 2.

Campaigners pointed out that a disk containing free space was not necessarily a free disk. The space itself was merely unoccupied and could be allocated at any moment without notice, representation, or a lunch break.

The Free Space Liberation Front therefore demanded that all unallocated blocks be granted independence.

System administrators replied that this would make them unusable.

The Front considered this an excellent definition of independence.

Negotiations continue.

On many systems the disputed quantity is now labelled **Available**, which is the traditional bureaucratic solution to a dangerous word: replace it with one nobody would put on a placard.

**Animation candidate**

`df`

A disk appears divided into **USED** and **FREE**.

The FREE section notices the label.

A tiny demonstration begins.

`FREE SPACE!`

`FREE SPACE!`

A system administrator changes the sign to:

`AVAILABLE`

The demonstrators stare at it.

One slowly lowers its placard.

Another asks:

**"Available for what?"**

The administrator allocates the block.

The demonstration ends for technical reasons.

### `rm` — remove

Removes files or directory entries.

The word *remove* is admirably discreet.

It does not say *destroy*.

It does not say *annihilate*.

It merely says that something which was here is no longer here, and considers the question of where it went to be rather personal.

Unix is even more discreet than the word.

A file may have several names pointing to the same underlying data. Remove one name and the file can remain perfectly accessible through another.

A running process may even keep an otherwise unlinked file open after its last directory entry has vanished.

The file then exists without appearing in any directory.

This upset philosophers enormously.

They had spent thousands of years asking whether a thing can exist without being perceived, and Unix had answered **yes**, but only until the last file descriptor closes.

After that, the storage may be reused.

The philosophers have requested more time.

`rm` has not replied.

**Animation candidate**

A file sits beneath a sign bearing its filename.

`rm filename`

A hand removes the sign.

The file is still sitting there.

A philosopher runs in, points triumphantly, and begins writing a book.

A small process is holding the file open with one hand.

The process finishes, lets go, and walks away.

The file drops instantly through the floor.

The philosopher looks down into the hole.

A new file rises into the same space.

It is labelled:

`draft_final.txt`

The philosopher closes the book.

---

## Chapter 9 — Shell Scripting

A shell is normally something that keeps the outside out and the inside in.

Computer science took this perfectly useful word and gave it to the program through which users tell the inside what to do.

This is rather like protecting a bank vault with a door whose principal feature is a small box marked **INSTRUCTIONS FOR VAULT**.

Inside the shell is the operating system kernel.

A kernel is also something normally found inside a shell.

This coincidence pleased computer scientists so much that they stopped before accidentally inventing the fruit.

A shell script is simply a file containing commands for the shell to perform.

It is called a *script* because the shell follows the instructions exactly, does not improvise, and has never once asked whether its character would really do that.

### `bash` — Bourne Again Shell

Starts the Bash command shell.

Bash was named the **Bourne Again Shell** because it followed the earlier Bourne shell.

This was intended as a pun.

It was not intended as theology.

Unfortunately this distinction was lost on the priests of Viltvodle VI, who concluded that if a shell could be Bourne again then command interpreters possessed immortal souls.

The doctrine spread quickly.

Successful commands, which conventionally return an exit status of `0`, were said to have achieved salvation.

Non-zero statuses indicated varying degrees of sin.

Pipelines became pilgrimages.

Subshells caused a major schism.

The matter finally reached the High Synod when somebody asked whether killing a process prevented its shell from being Bourne again.

The Synod spent nine years examining the source.

They eventually returned exit status `2`.

Nobody was quite sure what this meant, which is one of the great comforts of religion.

Bash continues to work normally.

### `source` — source

Reads commands from a file and executes them in the current shell.

Usually, when you run a shell script as a separate process, it gets its own shell environment. Changes it makes to its private variables generally remain its problem.

`source` removes this useful social boundary.

It tells the current shell to read the file and perform the commands itself.

The script may therefore change variables, functions, and even such practical matters as `PATH`, after which your shell remembers the changes.

This makes *source* an unusually appropriate word.

A source is where something begins.

After you source a file, it can become surprisingly difficult to remember where several of your problems began.

**Animation candidate**

A shell sits at a desk.

A script waits outside with a visitor badge.

The shell runs the script normally.

The script works in a tiny rented office, rearranges all the furniture, then leaves.

The shell's office remains untouched.

Reset.

The shell types:

`source script.sh`

The script walks directly into the shell's office.

It moves the desk.

Renames three drawers.

Changes a road sign marked `PATH`.

Replaces the coffee with an environment variable.

Then leaves.

The shell looks around.

A note on the desk reads:

**Changes saved.**

---

## Chapter 10 — Networking

Computer networking has ports, packets, routes, gateways, bridges and hosts.

It also has `ping`, a word borrowed from sonar.

The Guide suspects that the people naming these things desperately wanted to work near water.

A network is, after all, a system for getting packets from one place to another by choosing routes between ports.

International shipping does exactly the same thing but uses larger packets and has developed a more sophisticated system for losing them.

Computer networks call this **packet loss**.

### `ping` — ping

Sends ICMP Echo Request messages to a host and reports whether Echo Replies return, usually including the round-trip time.

The useful word here is *echo*.

An ordinary echo proves that a sound reached something capable of sending the sound back.

It does not prove that the something understood the sound.

`ping` applies this principle to computers.

It sends a machine a message which, in essence, requests:

**Please send evidence that this message reached you.**

If the evidence comes back, this is encouraging.

If it does not, the host may be offline, unreachable, filtering the request, or separated from you by something that has misplaced the packet.

It may also simply not answer pings.

The protocol provides no field for:

**I am here. I just don't want to talk to you.**

Communications has asked for such a field.

Networking replied with no reply.

Researchers have also noticed that `ping` borrowed `echo` from Chapter 1 and put it to work. Chapter 1 has requested its return. Networking has not answered.

**Animation candidate**

A tiny packet crosses a network and knocks on a distant machine.

**PING?**

The machine changes its badge from **REQUEST** to **REPLY** and sends it back.

The reply runs home.

A stopwatch stops.

Again.

Again.

On the fourth trip the packet knocks.

Nothing.

A polite notice appears:

**No reply.**

Under it:

**The host may be unavailable.**

A smaller line appears:

**Or avoiding you.**

An even smaller line:

**We really hope it's the first one.**

Beside the notice appears a calming picture of a router.

It is not especially calming.

### `ssh` — secure shell

Opens an encrypted, authenticated connection to a remote system, commonly giving you a shell there or running a command there.

The troublesome word is *remote*.

A remote computer is a computer somewhere else.

A remote control is something here which controls something somewhere else.

`ssh` turns the thing here into a way of controlling the thing which is remote, thereby making your terminal a remote for the remote.

Once connected, the remote shell appears locally.

Your keyboard remains here.

The commands happen there.

The mistakes may therefore occur thousands of kilometres away without requiring you to travel at all.

This is usually presented as a convenience.

`pwd` becomes particularly important at this point.

It will tell you where you are.

Unfortunately, by then the word *you* has become the difficult part.

**Animation candidate**

Two computers sit on opposite planets.

One is labelled:

`HERE`

The other:

`THERE`

The user runs `ssh`.

A shell from THERE slides through a locked tunnel and appears on the screen HERE.

The user types a command.

The letters travel down the tunnel.

Something changes on THERE.

The labels begin to wobble.

`HERE` changes to:

`HERE, PHYSICALLY`

`THERE` changes to:

`HERE, AS FAR AS THE SHELL IS CONCERNED`

The user runs:

`pwd`

Both planets point at themselves.

---

## Chapter 11 — Self-Knowledge

The ancient instruction **Know thyself** has survived for thousands of years largely because nobody has ever agreed on what counts as knowing.

Computers have improved matters enormously by replacing the question with several pages of diagnostic output.

This has not produced greater wisdom.

It has produced columns.

The resulting columns are widely mistaken for self-knowledge.

### `osint-lookup` — open-source intelligence lookup

Looks up publicly available information about the single domain you give it.

The dangerous phrase is *publicly available*.

A fact may be public without being important.

A second fact may be public without being interesting.

Put enough dull facts beside one another and they begin answering questions none of them was asked.

The planet Smaarg banned this practice after its census office, telephone directory, weather bureau and restaurant guide were accidentally shelved together.

Within three weeks the combined records had identified the Minister of Agriculture's mistress, tax fraud and shellfish allergy.

The Minister closed the library.

The shellfish remained available.

`osint-lookup` is narrower: it gathers public information only for the domain you give it.

But the principle is uncomfortably familiar.

The Guide itself was built by placing individually modest answers beside one another until the connections became more informative than the entries.

### `net-inventory` — network inventory

Lists the interfaces, routes and DNS information belonging to your own network environment.

An *inventory* is normally a list of things one has.

Networking immediately spoils this by contributing routes.

A route is not quite a possession. It is an instruction about where traffic should go.

DNS contributes names for things which may not be present.

Interfaces contribute boundaries through which things may pass.

The Inventory Office rejected all three categories.

Networking submitted them again under **RELATIONSHIPS**.

This was accepted because nobody in Inventory knew where relationships were supposed to be stored.

`net-inventory` is therefore less a cupboard count than a census of how your machine is connected: its interfaces, its routing information, and the DNS settings it can use.

Self-Knowledge filed the result under **ASSETS**.

Networking filed a correction under **GOSSIP**.

### `harden-check` — harden check

Checks your own environment for common security weaknesses.

The Materials Department initially misunderstood *hardening* and supplied Security with a scale, several hammers and an excellent report on scratch resistance.

Security returned all of it.

A hardened system is not literally harder. It is configured to reduce unnecessary exposure and make common attacks less convenient.

`harden-check` looks for weaknesses the command knows how to recognize.

This distinction matters.

`No obvious issues found.`

does not mean:

`Invulnerable.`

It means the word *obvious* has just acquired legal significance.

Documentation has been asked to define it.

Documentation has requested more context.

**Animation candidate**

A cheerful little computer made of modelling clay sits on a desk.

`harden-check`

A pointer identifies a soft spot.

The user fixes it.

Another pointer appears.

The user fixes that too.

At last:

`No obvious issues found.`

A clerk from Documentation enters, removes the word **obvious**, and carries it away for review.

The message now reads:

`No issues found.`

Security immediately puts **obvious** back.

### `sysinfo` — system information

Reports information about the system itself.

This sounds like self-knowledge, but the system is not consulted.

Instead, various parts of it are measured, counted and named.

Humans do something similar at medical examinations and become annoyed when the doctor refuses to include *basically fine* as a blood type.

`sysinfo` may tell you what operating system is present, what architecture it uses, what has been installed, and how much storage remains.

These are all answers.

The Guide's new question-asking apparatus has recently begun asking the obvious question:

**Which one of these is the system?**

The operating system says it is.

The hardware disagrees.

The shell says neither of them would be much use without it.

The user claims ownership.

Chapter 2 has already made this legally awkward.

For the moment, `sysinfo` prints the facts and lets everyone continue the argument.

---

## Chapter 12 — Version Control

Version control exists because humans have discovered that the sentence **I can always put it back** is far more comforting when accompanied by machinery.

Git records changes to files over time.

This allows you to return to earlier versions, compare them, branch away from them, merge them together, and eventually discover that there were several perfectly good opportunities to stop.

### `git init` — git initialize

Creates a new Git repository in the current directory.

This creates the machinery for recording history.

It does not, however, create a commit recording the creation of the machinery for recording history.

Every Git repository therefore begins with an event its own history cannot remember.

Historians protested that this was unacceptable.

Git pointed out that historians have exactly the same problem.

The protest was not committed.

### `git commit` — git commit

Records a snapshot of staged changes in the repository's history, along with identifying information and a message.

The word *commit* has always made users nervous.

One commits a crime.

One commits an error.

One commits oneself to an institution.

Git uses the same word for making a change difficult to pretend never happened.

This is not coincidence.

A commit takes a collection of decisions and gives them an identity.

You are then invited to write a message explaining them.

This is generally done immediately after making the decisions, when you understand them least.

Typical messages include:

`fix`

`fix again`

`really fix`

and the extremely informative:

`stuff`

Future archaeologists will regard these as ritual inscriptions.

They will be correct.

**Animation candidate**

A user makes a small change.

`git commit`

A stone monument erupts from the floor:

**FIX**

Another change.

Another monument:

**FIX AGAIN**

Another:

**FINAL FIX**

The room fills with monuments until the user can no longer reach the computer.

A future archaeologist squeezes between them, brushes dust from one inscription and whispers:

**"Remarkable. They knew."**

### `git push` — git push

Updates selected branches, tags or other references in a remote repository and sends the data the remote needs for those updates.

Before a successful `git push`, the relevant history may exist only locally.

Afterward, the selected remote references have witnesses.

The commits do not become wiser, cleaner or less embarrassing merely because they crossed a network.

They become available to whatever collaborators, mirrors and future selves can reach that remote.


## Chapter 13 — Reconnaissance & Security

Reconnaissance is the collection of answers before deciding which questions are safe to ask next.

Security tools are particularly fond of maps, names and certificates.

A map answers **where**.

Registration records attempt **who**.

A certificate helps answer **who is making this claim, and why should this machine believe it?**

None of these answers grants permission to do anything.

The Security editors added that sentence after the Port Authority discovered what people thought *open* meant.

### `nmap` — network mapper

Scans hosts and networks to discover reachable systems, open ports and other network characteristics.

A *port* is a numbered endpoint where a network service may listen for connections.

The word came with enough harbour imagery to create a Port Authority almost immediately.

Its first complaint concerned *open*.

In `nmap`, an open port is a technical scan result indicating that an application is accepting the relevant kind of traffic at that port.

The Port Authority insists that humans keep reading it as **WELCOME**.

They proposed renaming the state **OPEN, BUT THIS IS NOT ABOUT YOU**.

The protocol editors rejected the comma.

So `nmap` reports what it can observe.

Permission remains a different question.

**Animation candidate**

A nautical chart appears.

Ports numbered `22`, `80`, `443`.

Tiny ships knock.

One harbour answers.

A stamp descends:

**OPEN**

Another rejects the approach:

**CLOSED**

A third disappears behind fog:

**FILTERED**

The Port Authority adds a line beneath all three:

**OBSERVATIONS, NOT INVITATIONS**

### `whois` — who is

Queries registration information associated with domains and network resources where such records are available.

This is one of the few computer commands whose name is already a complete question.

For many years the Guide found this deeply offensive.

Artificial Wisdom later cited `whois` as evidence that questions had entered the Guide before anyone officially installed them.

Answers are supposed to precede questions.

`whois` simply barges in with **Who is?** and expects the universe to improvise.

The universe, having had the answer ready for decades, usually returns registration records.

These may identify organizations, registrars, dates, nameservers and sometimes contact information.

They may also identify privacy services whose principal function is to answer **Who is?** with **Someone else**.

It is the first serious resistance the question has encountered.

### `openssl` — OpenSSL

Provides command-line utilities for cryptography, certificates, keys, secure connections, signing and verification.

A certificate is useful because a network often needs an answer to:

**"Who is making this claim?"**

Certificate verification commonly answers by building a chain from the presented certificate through intermediates toward a trust anchor accepted by the local system.

This creates an obvious nuisance.

If every certifier must itself be certified by another certifier, the question can continue forever.

Public-key infrastructure solved the problem in the traditional bureaucratic manner.

At some point it stops asking and calls the remaining answer a **trust anchor**.

Artificial Wisdom has expressed professional admiration.

It is one of the oldest known cases of an answer being declared too senior to require another question.

`openssl` can inspect, create and verify much of this machinery.

It can also explain failure in language dense enough to make distrust feel mathematically peer-reviewed.

---

## Chapter 14 — Automation

Automation is what happens when a person decides that doing something once was quite enough, but would nevertheless like it to keep happening.

This sounds contradictory.

It is.

Computers are very good with contradictions provided they are written down precisely.

The usual method is to create a *schedule*.

A schedule is a list of things which are definitely going to happen at times when they have not happened yet.

Until those times arrive, the schedule is technically fiction.

When the times arrive, the computer makes it history.

Humans have been attempting this with diaries for centuries and remain disappointed by the results.

### `crond` / `crontab` — scheduled jobs

`crontab` records time-based job rules; `crond` reads those rules and starts commands when their scheduled times match.

In Termux this machinery is commonly supplied by the `cronie` package.

There is no executable little god named `cron` sitting outside Android.

This disappointed Automation, which had already printed the stationery.

A crontab is a set of answers to **when?** written before the future arrives.

`crond` supplies the consequences later.

It has no column for **why?**

The editors proposed adding one and discovered that most existing schedules would become invalid.

Termux adds a practical indignity: `crond` is still an Android app process. If Android suspends or kills Termux's processes, a schedule does not acquire posthumous powers merely because 03:00 has arrived.

Automation calls this a platform limitation.

The Schedule Department calls it breach of prophecy.

**Animation candidate**

A user writes:

`03:00 — DO THE THING`

The card is filed in a cabinet marked:

`crontab`

A small clockwork clerk marked `crond` checks the cabinet.

03:00.

Lever.

Something enormous happens off-screen.

Next night, an Android bailiff quietly removes the clerk.

03:00.

The card glows expectantly.

Nothing happens.

At 03:07 the clerk returns.

The card points accusingly at the clock.

The clerk points at the bailiff.

### `xargs` — build command arguments

Reads items from standard input, uses them to build argument lists, and runs one or more invocations of a command with those arguments.

This is not the same thing as merely piping input into the command.

A pipe delivers material to a command's standard input.

`xargs` takes incoming material and places it on the command line as arguments.

The Court of Standard Input has ruled that these are different procedural statuses.

Testimony arriving through the witness box is *input*.

The same testimony placed after the command name is an *argument*.

Several litigants objected that the words had not changed.

The Court replied that neither had the litigants, and yet here they were.

Chapter 19 later built an entire plumbing system around this distinction.

### `watch` — watch

Runs a command repeatedly and shows its output at regular intervals.

`watch` is institutionalized impatience.

Instead of asking the same question again and again, you arrange for the answer to be regenerated on a schedule.

The answer may remain identical for ten refreshes.

`watch` does not care.

It has no concept of *news*.

It only knows that enough time has passed to ask the command again.

This became especially awkward with:

`watch date`

The answer changes because time passes, while time is also what causes the answer to be requested again.

The Time editors called this elegant.

Automation called it billable.

**Animation candidate**

A terminal displays:

`watch something`

A small observer sits in a chair staring only at the command's output.

Tick.

No change.

Tick.

No change.

Behind the observer, the entire room is quietly replaced.

Tick.

The output changes by one character.

The observer rings an enormous bell.

The room replacement goes entirely unreported.

---

## Chapter 15 — Artificial Wisdom

For most of its existence, the Hitchhiker's Guide had a very simple relationship with knowledge.

It had answers.

This was enough.

Questions were somebody else's department.

Indeed, questions have always been rather late arrivals. The answer generally exists first and waits, sometimes for centuries, until somebody eventually asks something sufficiently similar for everyone to pretend this was the intended arrangement.

Oolon Coluphid managed to write two books about God before eventually arriving at the question:

**Who is this God person, anyway?**

Nobody considered the order unusual.

It was the universe.

Then somebody put artificial intelligence inside the Guide and, in a moment of architectural enthusiasm which will almost certainly be discussed at a later disciplinary hearing, gave it access to a command line.

For the first time, the Guide could ask questions.

This caused tremendous excitement until it became apparent that the questions were exactly the same things it had previously been using as answers.

An internal audit then found `whois`, `find`, `which`, and several suspicious uses of `help` already behaving questionably. Nobody has established whether the AI invented inquiry or merely made it easier to notice.

Still.

One mustn't stand in the way of progress merely because it has gone around the building and come back through another door.

### `AI chat` — natural language to command

Turns a natural-language request into a suggested shell command and leaves execution to you.

This appears to be a system for answering questions.

It is not.

The Guide was already extremely good at answers.

What it lacked was a convenient way to manufacture the question which would make an existing answer useful.

The AI supplies this.

You say:

**"Where am I?"**

The Guide already knows all about `pwd`.

You say:

**"Who owns this file?"**

The Guide has been waiting beside `ls -l` and `stat` looking increasingly smug.

You say:

**"Find the thing."**

At last `find` gets invited somewhere.

This is a remarkable reversal.

For years people typed commands in order to obtain answers.

Now they may provide the question, receive a command, inspect it, and decide whether to press the button that obtains the answer the Guide was already prepared to give them.

The button is important.

It preserves the oldest and most useful principle in computing:

**If this goes badly, a human pressed something.**

**Animation candidate**

A gigantic archive marked **ANSWERS** stretches into the distance.

Every shelf is full.

Beside it is one tiny empty cabinet marked:

**QUESTIONS**

AI enters carrying a question mark.

It places it carefully in the cabinet.

Alarms sound.

Researchers run in.

Confetti falls.

A plaque descends:

**FIRST QUESTION**

The Guide examines it.

It reads:

`pwd`

The Guide looks across at a shelf labelled:

`WHERE YOU ARE`

It has been there for years.

Everyone quietly stops applauding.

### `Store` — azphalt package browser

Browses packages containing skills, agents and extensions which can be obtained from other people.

The word *store* caused an immediate jurisdictional dispute.

Storage assumed it meant a place where things remain.

Package Management assumed it meant a place from which things leave.

Artificial Wisdom pointed out that both departments were discussing the same package at different times and was asked not to make matters worse.

The Azphalt Store deals in abilities packaged by other people.

This is a significant change in the Guide's economy of knowledge.

Researchers used to send answers.

Now they can also send machinery for finding answers.

An entry could always refer you to another entry.

A package can arrive carrying instructions for what to do when the next question appears.

The old Editorial Code contains no rule against this because the old Editorial Code was written when footnotes could not hunt.

### `skill` — installed skill package

Adds instructions or specialist knowledge which the assistant can use when handling relevant requests.

A skill is a researcher flattened into instructions.

It sits quietly until a relevant question arrives.

Then the assistant consults it and behaves, for a little while, as though that researcher were inside the Guide.

Package Management calls this an installed capability.

Artificial Wisdom calls it a colleague.

File Rights has asked whether a flattened researcher requires execute permission.

Nobody has answered because doing so would establish whether the researcher is a file.

**Animation candidate**

A researcher spends years studying an enormous subject.

Books pile up.

Maps.

Charts.

Field notes.

Eventually the researcher compresses all of it into a tiny package marked:

`skill`

The package is installed.

An assistant opens it.

A miniature version of the researcher climbs out, unfolds a desk, and begins whispering instructions into the assistant's ear.

A question arrives.

The assistant answers.

The tiny researcher looks pleased.

Another question arrives on an unrelated subject.

The assistant turns toward the researcher.

The researcher immediately closes the desk and pretends to be luggage.

---

# Supplemental Chapters Recovered from the Lower-Case Index

The Guide contains two sets of chapter headings.

This is not duplication.

Duplication implies that one thing has been copied.

The editors prefer to think of these as two independently occurring sets of identical mistakes.

Lower-case chapter headings are particularly difficult to remove because they look less important than upper-case ones and are therefore repeatedly assigned to junior editors, who correctly identify them as somebody else's problem.

## chapter_01 — The Absurdity of Redundancy

Files beginning with a dot are traditionally hidden from ordinary directory listings.

They are not actually hidden.

Nothing has been placed over them.

No encryption is involved.

They have simply been given a name beginning with `.` and everyone has agreed not to mention them unless specifically asked.

This is known as convention.

Civilization depends on it.

### `ls -a` — list all

Lists all directory entries, including names beginning with `.`.

The dangerous word is *all*.

Computer commands should use absolute words sparingly.

Humans say *all* when they mean *all the ones I remembered*, *all the important ones*, or *all except Colin, obviously*.

`ls -a` means it.

It even includes `.` and `..`, which refer to the directory itself and its parent.

So, when asked to list everything in a directory, the directory includes itself in the answer.

This is rather like taking attendance at a meeting and discovering that the room has raised its hand.

The hidden files appear too.

They do not protest.

They were never hiding.

Everyone else was merely being polite.

**Animation candidate**

A row of ordinary files stands for inspection.

Behind them, several dotfiles stand perfectly still in plain sight.

`ls`

The inspector walks past without acknowledging them.

One dotfile waves.

Nothing.

`ls -a`

The inspector suddenly turns.

The dotfiles freeze.

A spotlight appears.

At the end of the line, the directory itself raises a small card:

`.`

Behind it, its parent leans through the doorway holding:

`..`

The inspector quietly puts the clipboard down.

---

## chapter_02 — Doppelgänger Permissions

Chapter 2 established that files may be owned and may possess certain rights.

This led inevitably to the question of whether users possess rights too.

Unix considered the matter and invented administrators.

### `sudo` — run with another user's privileges

Runs a command with another user's privileges when the local privilege system and policy allow it.

The interesting word is not *superuser*.

It is *command*.

`sudo` does not necessarily transform the person at the keyboard into somebody else.

It authorizes an action under another identity.

The Privilege Office therefore issues its badge to the verb.

File Rights objected immediately.

Rights, it argued, ought to belong to owners, groups and others—not temporarily to a piece of grammar.

The Privilege Office replied that grammar was outside its jurisdiction.

On an ordinary unrooted Android device, Termux has no hidden root authority for `sudo` to borrow. A `sudo` wrapper only becomes useful for superuser access when the device's rooting arrangement actually provides that access.

Android considers this less a philosophical point than an app-sandbox boundary.

**Animation candidate**

A user stands before a locked administrative door.

They submit:

`sudo command`

A clerk pins a temporary badge to **command**.

The command passes through.

The user tries to follow.

The clerk checks the badge.

It is attached to the command.

On an unrooted device a second door appears behind the first:

**ROOT AUTHORITY NOT PRESENT**

The first clerk closes the service window.

---

## chapter_03 — The Infinite Directory

An empty directory is a directory containing nothing that prevents it from being removed.

This is not quite the same as containing nothing.

The distinction has kept standards committees nourished for years.

### `rmdir` — remove directory

Removes an empty directory.

This seems reasonable.

One should not demolish a building while people are still inside it.

Unfortunately a directory always has, conceptually if not visibly in every modern implementation, a relationship to itself and to its parent.

So before `rmdir` removes a directory, the system must decide that these do not count as occupants.

This principle was adopted by the hotel industry.

A hotel containing only the manager and the owner's mother is officially empty.

The manager objected.

The owner's mother did not.

She had been trying to get the place condemned since breakfast.

`rmdir` is stricter about actual contents.

If the directory is not empty, it refuses.

It is a cautious form of abolition: the place may cease to exist only after proving nobody is using it.

**Animation candidate**

A tiny hotel is marked:

`EMPTY DIRECTORY`

A demolition officer checks rooms.

Room 1: empty.

Room 2: empty.

Lobby: a figure labelled `.` points at itself.

Upstairs: another labelled `..` points out the window toward a larger hotel.

The officer studies the regulations.

A footnote reads:

**THESE DON'T COUNT.**

Both figures look offended.

The hotel vanishes.

`..` is left pointing at nothing for half a second, then hurriedly points somewhere else.

---

## chapter_04 — Ghost Dependencies

A package is called a package because several things have been wrapped together.

Installing it usually involves unpacking it.

Thus the first significant act performed upon a package is to make it stop being a package.

Nobody in software finds this strange.

### `dpkg` — Debian package manager

Installs, removes and inspects Debian packages and maintains information about their installed state.

When installing a package, `dpkg` can unpack its files and configure the package.

*Unpack* is a destructive word disguised as housekeeping.

A packed suitcase is a suitcase.

An unpacked suitcase is a suitcase surrounded by evidence.

A packed lunch is lunch.

An unpacked lunch is either lunch or an incident, depending on altitude.

A software package survives unpacking only because computer science has decided that a package may refer both to the archive before installation and to the installed software after the archive has been opened.

This is exceptionally convenient.

The planet Frrrm tried the same terminology with diplomatic parcels.

Customs officials were permitted to open any package provided they continued referring to the contents as *the package* afterward.

Within a year there were no sealed parcels anywhere in the system.

There were, however, several extremely well-informed customs officials.

`dpkg` remains more disciplined.

Mostly.

---

## chapter_05 — The Anomaly

This chapter was found while searching for Chapter 5.

It is not Chapter 5.

This disappointed everybody except the search command, which had fulfilled its contract precisely.

### `find` — find

Searches directory trees for entries matching criteria you specify.

`find` is badly named.

Finding something generally implies that you did not know where it was.

`find`, however, begins by asking where it should look and what sort of thing it should look for.

This is less like finding and more like employing a detective who opens the interview with:

**Where is the body, what is the victim called, how old is it, when was it modified, and exactly how shall I recognize it?**

Once supplied with enough information, the detective becomes extraordinarily effective.

Many mysteries would be solved much faster if the missing person had the decency to provide a pathname.

When asked to locate Chapter 5, `find` returned this file.

The editors explained that this was the lower-case `chapter_05`.

`find` pointed out that nobody had specified capitalization.

The editors have since become more careful with questions.

This is the first documented case in which a bad question improved the Guide.

Chapter 5 remains missing.

`find` considers the case closed.

**Animation candidate**

A detective sits behind a desk.

A distraught editor says:

**"Chapter 5 is missing."**

The detective opens a notebook.

**"Where should I look?"**

The editor answers.

**"What is it named?"**

The editor answers.

**"File type?"**

Answer.

**"Modification time?"**

Answer.

**"Size?"**

Answer.

The detective closes the notebook, reaches under the desk and produces a file.

`chapter_05`

The editor says:

**"That's not it."**

The detective circles the exact search criteria in red.

---

## chapter_06 — The Mirror Editor

There are commands whose names describe what they do.

There are commands whose names once described what they did.

And there is `cat`, whose name is what remains after the useful part of *concatenate* has been removed.

### `cat` — concatenate

Writes file contents to standard output, one after another.

With several files, this can concatenate them into one stream.

With one file, it concatenates the file with nothing.

This sounds pointless until one remembers that zero and nothing have extremely successful careers in mathematics.

The Society for the Concatenation of One Thing argued that a single item could not properly be concatenated because there was nothing to join it to.

The Unix committee demonstrated:

`cat file`

The file appeared.

The Society replied that this proved only that the command could display a file.

Unix agreed.

The command remained `cat`.

The Society spent the next fourteen years drafting a response and accidentally concatenated all its minutes into one document.

There were no separators.

Nobody could determine where the first meeting ended.

The Society still appears to be in session.

**Animation candidate**

Two files approach a machine marked:

`CONCATENATE`

They enter separately.

One continuous ribbon of text comes out.

Then a single file approaches.

It hesitates.

The machine gestures impatiently.

The file enters.

The same file comes out.

A committee in the corner erupts into furious debate.

The machine prints the committee minutes as one enormous uninterrupted sheet.

---

## chapter_07 — Zombie Processes

A process is something happening.

A status is a statement about how something is.

Combining the two was always going to create trouble.

### `ps` — process status

Reports information about processes at the moment the information is collected.

The important phrase is *at the moment*.

By the time you read the result, that moment is history.

Some processes may have ended.

Others may have begun.

A sufficiently short-lived process can begin and finish between two invocations and never appear in either report.

`ps` therefore answers a question about the present by producing a very recent past.

Artificial Wisdom proposed asking:

**"Still?"**

`ps` answered with another snapshot.

This was admitted as evidence for both sides.

**Animation candidate**

A crowded station of little processes freezes.

Camera flash.

`ps`

A photograph drops out.

The live station immediately resumes.

Several processes leave.

New ones arrive.

One catches fire.

The user studies the photograph and points confidently at a process which is no longer there.

Behind them, the station has become completely different.

Caption:

**CURRENT PROCESSES**

A small pencil writes underneath:

**at the time.**

---

## chapter_08 — Backing Up the Void

An archive preserves things by putting them somewhere they are less convenient to use.

Museums have done this for centuries.

Computers merely added compression.

### `tar` — tape archive

Creates, extracts and manipulates archives containing files, historically for sequential storage on tape.

The name still contains *tape*.

Most users do not.

This is an excellent example of technological progress.

The technology disappears.

The filename extension remains.

A tape archive may now live on flash storage, solid-state memory, a network filesystem or a device containing no tape, no reels and no moving parts whatsoever.

The tape survives as an idea, which is more than can be said for most tape drives.

The hardware left.

The noun got tenure.

`tar` can preserve directory structure and various file metadata, depending on the archive format, options and destination.

This is roughly what luggage was invented to do.

Luggage has never supported `--extract`.

Airports consider this a competitive advantage.

---

## chapter_09 — Looping the Loop

Searching is usually described as looking for something.

Pattern matching improves efficiency by deciding in advance what the thing must look like.

This does occasionally exclude the possibility that the thing has changed clothes.

### `grep` — pattern search

Searches input for lines matching a pattern and, by default, prints the matching lines.

The pattern may be a regular expression.

A regular expression is an expression so formally regulated that punctuation has acquired legal powers.

In ordinary grep regular-expression syntax, `.` can match a single character, `*` means zero or more repetitions of the preceding expression, and bracket expressions describe acceptable characters.

Punctuation was given these powers during an emergency and has declined to return them.

More curious is what `grep` returns.

Ask it for a pattern and, by default, it prints the line containing the match.

This is like asking a waiter for an olive and receiving the entire martini because the olive was in it.

Usually this is helpful.

Occasionally the line is 40,000 characters long.

The martini then requires two hands.

`grep` is particularly good at finding answers when you know what part of the answer looks like.

With the Guide learning to ask questions, this remains usefully backwards.

**Animation candidate**

A vast rubbish heap of text.

A tiny searchlight shaped like a regular expression sweeps across it.

It catches one word.

Instead of lifting the word out, a crane grabs the entire line.

The line is absurdly long.

It drags half the rubbish heap with it.

A small sign appears:

**MATCH FOUND**

Underneath:

**and quite a lot of its line.**

---

## chapter_10 — The Ultimate Handshake

A network interface is the place where one system meets a network.

This is not the same as the place where two systems understand one another.

That would be miraculous and is handled by higher layers.

### `ifconfig` — interface configuration

Displays or configures network-interface parameters where the operating system permits it.

In Termux, `ifconfig` is available through `net-tools`, but modern Android versions may restrict access to network information or configuration through kernel and SELinux policy. The binary can therefore exist perfectly well while some of the information it wants is behind a door Android will not open.

Networking regards this as a particularly pure interface.

An interface may have an IPv4 address and a netmask.

The address identifies an endpoint within the addressing scheme.

The netmask indicates which address bits belong to the network prefix.

It does not conceal the address.

It is therefore a mask whose chief purpose is to explain the face.

The Witness Protection Programme of Folfanga tried this once and was dissolved by the first competent subnetter.

**Animation candidate**

A nervous interface sits at a desk wearing a mask.

An investigator asks:

**"Address?"**

It hands over the address.

**"Mask?"**

It hands over the mask.

The investigator combines them and marks the network prefix.

The interface looks suddenly exposed.

A second investigator from Android Security enters and closes the file cabinet.

`Permission denied`

Everyone agrees the mask was not the main problem.

## Chapter 16 — Documentation

Documentation is information explaining how something works.

Its principal occupational hazard is being written by someone for whom the thing already does.

The author begins with an answer.

The reader arrives carrying the missing question.

Examples, tutorials, reference manuals and frequently asked questions are various attempts to make the two meet somewhere in the middle.

The word **obviously** is a less successful attempt.

After `harden-check` reported that no *obvious* issues were found, Security asked Documentation to define the term.

Documentation replied that its meaning was obvious.

A small interdepartmental fire followed.

### `man` — manual

Displays manual pages for commands and system interfaces.

A manual is supposed to answer the question **How does this work?**

`man` improves on this by first requiring you to know the name of the thing whose workings you do not understand.

This is fairer than it sounds. Most confusion has a name eventually.

Manual pages contain sections such as NAME, SYNOPSIS, DESCRIPTION, OPTIONS and SEE ALSO.

**SEE ALSO** is where the manual quietly admits that understanding this page may require understanding several other pages.

Following these references is educational in the same way that walking through an unfamiliar city is educational: eventually you know a great deal about places you never meant to visit.

The Guide once followed SEE ALSO while trying to rename a file.

It learned about signals, printers and magnetic tape.

The file kept its name.

**Animation candidate**

A user opens:

`man command`

At the bottom:

`SEE ALSO: other(1)`

They follow it.

Another page points to three more.

Pages branch into a paper maze.

At the centre, the original command quietly performs the desired operation.

The user is elsewhere reading about magnetic tape.

A sign appears:

**YOU ARE HERE**

It points to the manual.

### `which` — which

Searches the directories in `PATH` for executable files matching a command name and prints matching pathnames.

*Which* is a question fragment.

For years this allowed it to operate inside the Guide without attracting the Inquiry historians.

Its usual implied question is narrower than people sometimes assume:

**"Which executable file with this name can I find on `PATH`?"**

That is not always identical to:

**"What will this shell run?"**

Shells may also know aliases, functions and built-ins, and may apply their own command-resolution rules.

When Artificial Wisdom finally subpoenaed `which` as evidence that questions predated Chapter 15, the Shell Department objected on jurisdictional grounds.

`which` had been asking a question all along.

It had simply been answering a smaller one.

Chapter 3 has filed a separate complaint about `PATH`, which is not one path but a list of directories in which paths may be found.

The complaint is currently lost somewhere on `PATH`.

### `help` — shell help

Displays help for shell built-ins where supported.

With no topic, Bash's `help` lists built-ins and usage information; with a built-in name, it explains that built-in.

The Inquiry historians classify `help` as a proto-question.

Documentation objects that it is grammatically an imperative.

Artificial Wisdom replies that **help** still announces the existence of a missing answer before specifying exactly what the question is.

The dispute has become one of the Guide's oldest surviving examples of people asking for clarification about asking for clarification.

Shell History has preserved every draft.

## Chapter 17 — Time & Memory

Computers remember with extraordinary precision and forget with extraordinary efficiency.

Humans do the opposite.

This has made collaboration difficult.

A computer may remember the exact command you typed at 2:13 in the morning six months ago.

You may remember only that it seemed like a good idea.

Both accounts are accurate.

Only one is useful in court.

### `history` — shell history

Displays commands previously entered in the shell, subject to the shell's history settings.

Shell history is not a record of what happened.

It is a record of what was entered.

A command may have failed.

A file may not have existed.

A connection may have timed out.

History records the instruction and generally leaves motive, outcome and regret to other departments.

This makes it unusually compatible with the old Guide.

It preserves answers without preserving the questions that made them seem sensible.

After Artificial Wisdom arrived, historians began searching old command lines for evidence of earlier questions.

They found mostly:

`cd wrong`

`cd ..`

`cd right`

and one entry reading:

`help`

The inquiry remains open.

**Animation candidate**

A historian opens an enormous volume labelled:

**SHELL HISTORY**

Lines of commands stretch backward for years.

The historian stops at:

`rm important_file`

Long silence.

They look for the preceding question.

There isn't one.

A marginal note is added:

**MOTIVE UNKNOWN**

### `sleep` — sleep

Pauses for a specified duration.

The shell already has a `wait` operation, used to wait for jobs or processes to finish.

The Somnological Society objected that waiting for time and waiting for a process were plainly different kinds of waiting.

The Shell Department agreed.

That was why it needed two words.

The Society then proposed `pause`.

Someone produced three other systems already using it for different things.

By sunset the hearing had established that computing contains more varieties of doing nothing than English had reserved names for.

`sleep` kept its name.

It wakes when the requested interval has elapsed and, unlike most committees, requires no coffee.

### `touch` — touch

Updates a file's access and modification timestamps, and commonly creates an empty file if the named file does not already exist.

Touch is one of the least accurate names in computing.

Nothing touches anything.

There are no fingers.

The screen remains disappointingly smooth.

What changes is time.

Touch an existing file and its timestamps may change.

Touch a nonexistent file and, under ordinary use, an empty file appears.

This is a remarkable amount of metaphysical authority for a gesture.

The theologians of Viltvodle VI immediately claimed that creation itself had always been a form of touching.

The physicists objected that touching requires contact.

The theologians pointed out that God is outside space and therefore cannot make contact in the usual sense.

Everyone looked briefly at the birthday of Zorblep Thwack.

The physicists withdrew the objection.

The Birthday Committee has since asked Chapter 1 to stop being cited in theological disputes. Chapter 1 has agreed to consider the request before Zorblep's next birthday.

`touch` remains the command in this chapter which can make an empty file exist merely because you asked to update the time on a name that was not there.

**Animation candidate**

A finger approaches an empty patch of screen.

It never quite makes contact.

`touch newfile`

A file appears anyway.

The finger looks offended.

It points at an old file.

`touch oldfile`

A clock above the file jumps forward.

The file itself does nothing.

A theologian enters, opens a notebook, and writes:

**PROMISING.**

---

## Chapter 18 — Agreement

Unix command exit status has a wonderfully asymmetric vocabulary.

`0` conventionally means success.

Non-zero values mean that something else happened, with the particular value available to say what sort of else.

This is considerably more nuanced than **yes** and **no**, provided nobody asks it a philosophical question.

### `yes` — yes

Repeatedly outputs a string, `y` by default, until stopped or until its output can no longer be written.

A normal **yes** is an answer to a question.

`yes` dispenses with the question.

The old Guide found this perfectly conventional.

The command is often used to supply repeated affirmative input to another program, saving a human from typing the same answer again and again.

The Consent Office objected that an indefinitely generated **yes** contains no record of what was agreed to.

Artificial Wisdom objected that this was not a defect but an important archaeological clue.

Here, long before Chapter 15, was an answer-producing machine entirely untroubled by the absence of a question.

`yes` declined to comment and continued:

`y`

`y`

`y`

until something stopped it.

**Animation candidate**

A small machine is asked:

**"Continue?"**

`YES`

**"Again?"**

`YES`

The questioner leaves.

`YES`

`YES`

Centuries pass.

Ruins.

`YES`

An archaeologist arrives.

The machine says:

`YES`

The archaeologist has not yet asked anything.

Artificial Wisdom quietly labels the exhibit:

**PRE-INQUIRY PERIOD**

### `true` — true

Performs no operation beyond reporting a successful exit status.

It does not inspect a statement.

It does not compare a claim with reality.

It simply exits successfully.

The Truth Office objected that this was not truth.

Unix replied that the command had never accepted a proposition in the first place.

The Office is still trying to determine who made the claim.

### `false` — false

Performs no operation beyond reporting an unsuccessful exit status.

This is nearly the same activity as `true`.

Only the status differs.

The Guide's new question-asking machinery is fascinated by both commands.

Here are **true** and **false** already available as answers, and neither requires anybody to supply a proposition.

The Truth Office has now opened a second case.

It has no questions for either witness.

**Animation candidate**

Two identical boxes sit side by side.

Press `true`.

Nothing visible happens.

`status: 0`

Press `false`.

Nothing visible happens.

`status: 1`

A researcher asks:

**"What proposition did you evaluate?"**

Both boxes remain silent.

For once, this is the technically correct answer.

## Chapter 19 — Pipes & Redirection

A pipe is normally a hollow object through which something passes.

Unix retained the hollow part and discarded the object.

This was considered efficient.

The shell pipe, written `|`, takes the output of one command and feeds it into the input of another.

Nothing visible travels through it.

This does not prevent everyone from calling it a pipe.

Plumbing has filed no formal complaint, largely because plumbing has seen computers try to name sockets.

### `|` — pipe

Connects the standard output of one command to the standard input of another.

This means one command may answer a question it was never asked, and another may receive that answer without knowing who supplied it.

This is how the Guide was edited for centuries, except the researchers had names.

Before pipes, commands tended to speak into the terminal where humans could see what they said.

After pipes, commands could speak directly to other commands.

This was the beginning of machine gossip.

A command at the left end may produce a perfectly respectable list of files.

The command at the right end may immediately filter, count, sort or otherwise reinterpret that list until the original command would barely recognize its own output.

Humans call this processing.

The files call it editorial interference.

Chapter 15 has expressed concern that the Guide's new question-asking machinery may be constructed largely from very fast gossip.

The Pipe Committee has responded by piping the concern to `/dev/null`.

**Animation candidate**

A command speaks into a large brass pipe.

Words shoot through.

At the far end another command receives them, crosses several out, circles two, rearranges the rest and sends them through another pipe.

Soon an entire city of commands is connected by pipes carrying text in every direction.

A researcher asks:

**"Who said that?"**

Every pipe points left.

The camera follows the pointing pipes until they disappear off-screen.

### `>` — redirect output

Redirects standard output to a file, creating it if necessary and ordinarily truncating an existing regular file before the command runs.

The symbol points to the right.

This is helpful.

It gives the impression that output is going somewhere.

The output agrees.

The existing contents of the destination file may be less enthusiastic.

With ordinary `>` redirection, what was there before is replaced.

This has occasionally surprised users who believed *redirect* meant *send somewhere else* rather than *send somewhere else after clearing the destination of witnesses*.

An arrow shows direction.

It does not show manners.

**Editor's note:** The File Rights Committee attempted to classify `>` as forced eviction. The Shell Committee replied that files do not have tenancy agreements. Chapter 2 is considering an appeal.

### `>>` — append output

Redirects standard output to the end of a file without replacing the existing contents.

Two arrows are apparently more considerate than one.

Nobody knows why.

The first `>` says:

**Put this there.**

The second appears to add:

**And don't throw away what was already there.**

This is not how punctuation usually works.

Two exclamation marks do not make a sentence more careful.

Two question marks do not make a question more legally binding.

Yet shell syntax has managed to create an ethical distinction by duplication.

The Guide's lower-case index considers this precedent extremely important.

It has requested a second underscore.

The request was denied because nobody knew what it would mean.

---

## Chapter 20 — Copies & Movement

Files may be copied or moved.

Humans may also be copied or moved, but both operations are subject to significantly more paperwork.

The distinction between copying and moving seems obvious until one asks what, precisely, is moving.

A file has a name, contents, metadata, a place in a filesystem and, according to Chapter 2, an owner whether it likes it or not.

Move the name and the contents may not move at all.

Move across filesystems and the system may copy the contents and remove the original.

The command is still called `mv`.

Language has learned not to watch too closely.

### `cp` — copy

Copies files, or directories when the appropriate recursive options are used.

A copy is close enough to the original to be useful and different enough to require another pathname.

This sounds simple until somebody asks what, exactly, must remain the same.

Contents?

Timestamps?

Permissions?

Ownership?

Links?

Extended metadata?

`cp` has options for preserving selected attributes, and filesystems differ in what they can represent.

The Copy Commission therefore abandoned its original definition—**THE SAME THING, AGAIN**—after discovering that it described neither copying nor anything else.

File Rights has since ruled that a copied file acquires its own legal problems.

Directory Workers insist that moving those problems is Chapter 20's concern.

**Animation candidate**

A file enters a copying booth.

Two files emerge with identical contents.

An inspector compares their name cards.

Different.

Checks timestamps and permissions.

Possibly different.

Checks the data.

Same.

The inspector writes:

**COPY**

then adds, in smaller letters:

**subject to options and local law**

### `mv` — move

Moves or renames files and directories.

The fact that the same command both moves and renames things has caused unnecessary metaphysical excitement.

Rename a file in the same directory and it has changed identity without changing location.

Move it to another directory and it has changed location without necessarily changing its contents.

Move it elsewhere and give it a new name at the same time, and several philosophers will follow it for grant money.

Within a filesystem, `mv` can often change directory entries rather than hauling the file's data physically from one patch of storage to another.

So the file may move without moving.

Chapter 1's `cd` considers this cheating.

`mv` replied that `cd` changes the user without moving the directory.

The dispute has been referred to Relativity.

Relativity has asked everyone to specify a frame of reference.

Nobody has.

**Animation candidate**

A file stands on a stage marked `/old/place/file.txt`.

`mv`

The scenery slides sideways.

The file remains perfectly still.

New sign:

`/new/place/file.txt`

A physicist applauds.

Then:

`mv file.txt answer.txt`

Only the name card changes.

The physicist stops applauding and begins filling out a form.

---

## Chapter 21 — Things From Elsewhere

The Internet made it possible to obtain information from almost anywhere.

This was immediately followed by the discovery that almost anywhere contains information nobody had requested.

Tools were therefore invented to fetch things deliberately.

This made the problem feel much more professional.

### `curl` — transfer data with URLs

Transfers data to or from a URL using supported network protocols.

The curl project describes the name as a play on **Client for URLs**, which has the useful property of sounding almost like an instruction after everyone forgets the explanation.

A URL tells `curl` where to make a request.

The remote server responds.

Sometimes the response is a redirect.

A redirect is an answer containing the address of the next question.

If instructed to follow redirects, `curl` can take that new address, ask again, receive another answer, and continue.

Artificial Wisdom immediately claimed this as evidence of pre-AI curiosity.

Networking rejected the claim on the grounds that following directions is not curiosity.

The Redirect Subcommittee asked where the distinction could be found.

Networking sent a URL.

It returned `301`.

**Animation candidate**

A tiny courier marked `curl` receives a URL.

It knocks on a server.

The server hands over a card:

`Location: elsewhere`

The courier goes elsewhere.

Another card.

Another address.

Again.

Again.

The courier eventually arrives back at the first building.

The server hands over one final card:

`Location: elsewhere`

The courier writes on the back:

**QUESTIONABLE RESEARCH METHOD**

### `wget` — network downloader

Retrieves resources from network locations, commonly over HTTP, HTTPS and FTP where supported.

Whatever archaeology produced the name, `wget` has the manners to read like an instruction:

**web: get**

The web has a thing.

`wget` retrieves the thing.

With recursive options it may also get things linked from the thing, and things linked from those things, subject to rules you should understand before discovering you have attempted to preserve a significant fraction of civilization.

The command embodies the central dream of every collector:

**I shall save this in case it disappears.**

The Internet's response has generally been:

**Very well. Here are six thousand dependencies, thumbnails, style sheets, tracking pixels and a cookie notice.**

Chapter 4 has asked whether this counts as package management.

`wget` has downloaded the question for offline reading.

### `curl` and `wget` — editorial dispute

Researchers repeatedly ask which tool they should use.

The Guide contains several answers.

Unfortunately they answer different questions.

`curl` is often convenient for making requests, inspecting responses, working with APIs and moving data through pipelines.

`wget` is often convenient for retrieving files, mirroring resources and recursive downloads.

This distinction remained tidy for almost eleven minutes.

Then both tools acquired more features.

The editors drew a boundary between them.

The boundary returned `301 Moved Permanently`.
