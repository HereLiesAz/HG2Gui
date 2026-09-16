This is the current master draft of “The Hitchhiker’s Guide to Termux.”



Perform a serious editorial pass on the attached file, not rewrite it from scratch.



The goal is to make the writing feel sharper, stranger, more coherent, more technically accurate, even if by sci-fi logic.



IMPORTANT:

\- Treat the attached file as canonical source material.

\- Preserve its structure, chapters, commands, recurring characters, internal mythology, and cross-references unless there is a strong editorial reason to change them.

\- Do not silently replace established ideas with unrelated new ones.

\- Do not turn it into generic “space jokes,” programmer one-liners, or parody catchphrases.

\- Do not imitate or reproduce copyrighted Douglas Adams passages. The goal is to use high-level comedic mechanisms associated with his work: literalizing innocent words, following absurd logic seriously, bureaucratic understatement, delayed reversals, tangents that become more important than the original subject, and ideas that unexpectedly connect across entries.

\- Every technical claim about Unix, Termux, shell behavior, networking, files, processes, Git, permissions, etc. must remain accurate.

\- The Guide may be absurd. The command description may not be false.



CORE CONCEPT OF THIS VERSION



The Guide has always been a book of answers.



In this universe, answers exist before the questions that make them useful.



The new AI/CLI capability changes something fundamental: the Guide can now ask questions and act on its own curiosity.



The AI is not a magical omniscient oracle grafted onto the Guide. It is more like the first researcher who lives inside the book.



The Guide itself should feel alive:

\- entries cite other entries;

\- editorial departments disagree;

\- corrections create new problems;

\- ideas migrate between chapters;

\- old entries acquire amendments;

\- missing material stays missing;

\- duplicate/recovered chapters can contradict the main index;

\- information connects in ways the original editors did not intend.



This is inspired by Douglas Adams’ real-world vision for h2g2 as:

\- collaborative;

\- constantly updated;

\- full of gaps;

\- increasingly contextual;

\- built around connecting information;

\- redesigned through use;

\- evolutionary rather than finished.



COMEDIC METHOD



The strongest jokes in this project usually work like this:



1\. Begin with a technically innocent word or phrase.

2\. Notice that the word has another ordinary meaning.

3\. Treat that second meaning with complete seriousness.

4\. Follow the resulting logic much farther than anyone sensible would.

5\. Allow the tangent to invent a new institution, historical event, legal dispute, religion, scientific controversy, editorial department, etc.

6\. Then unexpectedly reconnect that absurdity to the real command.

7\. Keep the prose calm and matter-of-fact.



Example mechanism, NOT wording to reuse:

“party” in “one party gives another party information” becomes an actual birthday party, which becomes a problem because God exists outside the universe and has no intuitive sense of scale.



Established continuity includes:

\- Zorblep Thwack and the catastrophic birthday party.

\- God exists outside length, width, height, time, etc., loves creation, but has no useful sense of scale.

\- Oolon Coluphid wrote books about God before finally getting around to asking “Who is this God person, anyway?”

\- Chapter 5 is genuinely missing. Formal police investigations are underway.

\- A lower-case `chapter\_05` exists and insists it is not Chapter 5.

\- File Rights, Directory Workers, Package Management, Networking, Artificial Wisdom and other editorial/institutional threads may recur.

\- `whois`, `find`, `which`, `help`, etc. create an interesting prehistory of questions entering a Guide that traditionally only had answers.

\- Commands should cross-reference one another where this makes the world feel richer.



STYLE FAILURE MODES TO REMOVE



Be aggressive about cutting or rewriting:

\- generic “the universe is absurd” jokes;

\- generic “computers hate humans” jokes;

\- generic “AI is confidently wrong” jokes;

\- interchangeable programmer humor;

\- jokes that amount to “this command is scary”;

\- one-line punchlines pasted onto factual explanations;

\- repetitive “The Guide considers…” / “The Guide recommends…” scaffolding;

\- overly tidy three-beat jokes;

\- faux-profound cosmic language that does not grow from the actual command;

\- jokes whose only mechanism is alliteration;

\- references that feel like imitation rather than original reasoning;

\- explanations that announce the joke instead of letting the logic create it.



The funniest passages should feel as though the writer discovered the joke halfway through explaining the command and then had no responsible way to stop.



RUN FOUR SEPARATE EDITORIAL PASSES



PASS 1 — VOICE / COMEDY CRITIC

Read the entire file and identify passages that:

\- feel generic;

\- feel too polished or “AI-written”;

\- have weak or obvious punchlines;

\- rely on surface-level cosmic humor;

\- explain the joke instead of reasoning into it;

\- lack an interesting innocent word to derail;

\- are much weaker than the strongest passages.



Rewrite those passages.



Do not change strong material just for novelty.



PASS 2 — CONTINUITY / LIVING GUIDE CRITIC

Read the entire file again looking only at internal continuity.



Strengthen:

\- recurring institutions;

\- callbacks;

\- editorial disputes;

\- consequences of earlier chapters;

\- the answer-before-question cosmology;

\- the Guide slowly learning to ask questions;

\- relationships between commands.



Remove continuity errors.



Avoid over-crosslinking. Connections should feel discovered, not mechanically inserted.



PASS 3 — TECHNICAL ACCURACY CRITIC

Audit every command and technical statement.



Verify that:

\- descriptions are accurate;

\- command names/expansions are not falsely asserted;

\- shell semantics are correct;

\- edge cases are not presented as universal behavior;

\- Termux-specific/custom commands are described only according to what the file establishes;

\- comedy does not imply false technical behavior.



If a joke depends on a technically false premise, rewrite the joke rather than weakening the factual description.



Do not use outside assumptions about custom commands unless the attached file establishes them.



PASS 4 — MURDER PASS

Now edit for ruthlessness.



Delete or compress:

\- redundant paragraphs;

\- second explanations of the same joke;

\- unnecessary punchline tags;

\- generic filler;

\- repeated sentence rhythms;

\- unnecessary uses of “The Guide” as sentence subject;

\- anything merely “pretty good” sitting next to something excellent.



Prefer a shorter strong entry over a longer competent one.



Do not flatten deliberate pacing where repetition is doing real comedic work.



OUTPUT



1\. Produce a revised full master file.

2\. Preserve Markdown structure.

3\. Preserve all commands unless there is an actual duplication/error that should be resolved.

4\. Preserve animation candidates, but improve them if the prose edit changes the joke.

5\. Do not merely give me notes. Actually apply the edits.

6\. Also give me a short editorial report containing:

&#x20;  - strongest sections;

&#x20;  - sections most heavily revised;

&#x20;  - any technical claims you were uncertain about;

&#x20;  - any passages you deliberately left alone because they were already strong.

7\. If you make a major continuity decision, mention it in the report.

8\. Save the revised file under a new filename so the attached original remains untouched.



Most importantly: do not rewrite everything to demonstrate activity.



A good editorial pass should leave fingerprints in the weak places and almost none in the strong ones.

# 

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

`ls` supplies names. It does not say why they are there, whether they are important, who invited them, or why one of them is called `final\_FINAL\_2`.

In this respect a directory listing resembles the guest list after a very successful party: everyone is accounted for and nobody can explain the evening.

`ls` is therefore not nosy.

It merely takes attendance.

### `echo` — echo

Repeats what you tell it.

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

The right to demand that their owner stop naming them things like `final\_final\_REAL2.txt`.

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

\---

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

### `apt-get update` — advanced package tool get

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

\---

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

**Update:** A file labelled `chapter\_05` has since been recovered from the lower-case index. It insists it is not Chapter 5. Chapter 5 has not issued a statement.

\---

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

### `vi` — visual

Opens the Vi text editor.

The name *visual* dates from an age when showing the text you were editing was a feature worth mentioning.

This is worth remembering whenever a modern product advertises something so obvious that you wonder how it could possibly be a feature. Given enough time, today's miracle becomes tomorrow's missing checkbox.

Vi shows you the text.

What happens next is between you and Vi.

It is powerful, compact, and operated through a command language whose designers correctly observed that ordinary typing contains a scandalous number of unnecessary keystrokes.

Beginners often ask how to exit.

Experienced users know.

Very experienced users know several ways and have opinions about which one reveals character.

The Guide recommends leaving before this stage.

## Chapter 7 — Processes

A process is a program while it is happening.

This distinction is important.

A recipe is not a cake.

A musical score is not a symphony.

A program sitting harmlessly on disk is not yet consuming your battery, memory, patience, and one suspiciously large percentage of the CPU.

For that, it must become a process.

### `top` — table of processes

Displays active processes and their use of system resources.

The name *top* suggests a competition.

This was a mistake.

A process near the bottom can improve its position simply by consuming more CPU. Once this is noticed, the ranking becomes an incentive scheme for misbehaviour.

`top` refreshes continuously, so every process has repeated opportunities to become more important by becoming more expensive.

Management consultants later reinvented this.

Unix kept the source code.

**Animation candidate**

A leaderboard appears:

1. sensible\_process — 2%
2. useful\_process — 1%
3. idle\_process — 0%

The process at the bottom notices the word **TOP**.

It starts eating CPU.

Its percentage rises.

The others notice.

Soon every process is shovelling CPU into itself while climbing the board.

One reaches 99%.

Confetti falls.

The terminal freezes.

The winner remains technically on top.

### `kill` — kill

Sends a signal to a process.

The default signal asks the process to terminate.

This is an unusually restrained meaning of the word *kill*.

Unix could have called the command `signal`, but this would have created the misleading impression that signals are harmless.

It could have called it `terminate`, but not every signal terminates.

So it called the whole business `kill`, rather as a postal service might call itself **Murder** because one of the letters it delivers contains a bomb.

Most processes, when sent the ordinary termination signal, are given the opportunity to tidy up and leave peacefully.

This makes `kill` one of the very few forms of killing in which the victim may save its work first.

There are less polite signals.

The Guide advises using those only when the process has stopped listening, stopped cooperating, and begun behaving as though the computer belongs to it.

At that point everyone agrees the ownership question has become academic.

**Animation candidate**

A tiny post office marked `kill` sorts envelopes.

One says:

`TERM — Please leave when convenient.`

Another says:

`HUP — Something changed.`

Another says:

`STOP — Don't move.`

At the far end sits a black envelope stamped:

`KILL`

The clerk reaches for it.

Every other envelope in the room becomes extremely cooperative.

\---

## Chapter 8 — Storage

Storage is the practice of keeping something because you may need it later.

*Later* is important.

If you need it now, it is not storage. It is simply there.

If you never need it, it is rubbish.

Consequently, the difference between valuable data and rubbish can only be established by an event in the future.

Filesystems dislike this sort of uncertainty and solve it by keeping everything until they run out of space.

At that point they become extremely decisive.

### `df` — disk free

Reports how much space is used and how much remains available on mounted filesystems.

The name means *disk free*.

This became politically awkward shortly after the File Rights movement of Chapter 2.

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

`draft\_final.txt`

The philosopher closes the book.

\---

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

\---

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

The Guide considers this a serious omission from digital communication and a surprisingly common feature of personal communication.

Researchers have also noticed that `ping` appears to have borrowed `echo` from Chapter 1 and put it to work in Networking. Chapter 1 has requested its return. Networking has not answered.

**Animation candidate**

A tiny packet crosses a network and knocks on a distant machine.

**PING?**

The machine hands the same packet back.

The packet runs home.

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

\---

## Chapter 11 — Self-Knowledge

The ancient instruction **Know thyself** has survived for thousands of years because it contains no specification for completion.

Computers improved this immediately.

They count things.

Memory. Storage. Interfaces. Routes. Packages. Versions.

The resulting columns are widely mistaken for self-knowledge.

This is unfair to the columns. They have never claimed to know anything.

### `osint-lookup` — open-source intelligence lookup

Looks up publicly available information about the single domain you give it.

The phrase *open-source intelligence* contains a small trap.

The information is open.

The intelligence generally happens after you put several pieces of it next to one another.

One public fact is a fact.

Two public facts are two facts.

Thirty-seven public facts, properly arranged, may suddenly know where you work, what mail server you use, when the domain was registered and which administrator is about to have a very educational afternoon.

The planet Smaarg discovered this after its census office, telephone directory, weather bureau and restaurant guide were accidentally indexed together.

Within three weeks the combined records had identified the Minister of Agriculture's mistress, tax fraud and fatal shellfish allergy.

The Minister closed the library.

The shellfish remained public.

`osint-lookup` is considerably better behaved.

It investigates the one domain you give it and returns what it can find.

The domain is not asked whether it regards this as better behaviour.

### `net-inventory` — network inventory

Lists the interfaces, routes and DNS information belonging to your own network environment.

An inventory usually lists things.

A network inventory lists interfaces, addresses, routes and nameservers, several of which are better described as relationships between things.

This is like taking inventory of a house and writing:

**four chairs, one table, the route to the kitchen, and the fact that everyone calls the spare room Kevin.**

Computers find this perfectly sensible.

Interfaces say where the machine meets a network.

Routes say which way traffic should go.

DNS says which names should become which addresses.

Taken together they form a sort of autobiography written entirely as directions.

Humans attempted an equivalent.

It was called **family tree**.

Development stopped when nobody could agree whether marriage was an interface, a route, or a DNS failure.

### `harden-check` — harden check

Checks your own environment for common security weaknesses.

The verb *harden* is worth approaching carefully.

A hard system is more difficult to damage.

It is also more difficult to change.

A perfectly hardened system would therefore be one which could not be altered at all.

This is secure.

It is also a rock.

Computer security spends much of its time trying to approach the rock without quite becoming one.

`harden-check` assists by pointing out the places where your system is still pleasantly soft enough for somebody else to get in.

This can be upsetting.

The Guide recommends remembering that discovering a hole does not create the hole.

It merely ruins the afternoon.

**Animation candidate**

A cheerful little computer made of modelling clay sits on a desk.

`harden-check`

A pointer identifies a soft spot.

The user presses it.

Dent.

Another soft spot.

Dent.

The user begins wrapping the computer in metal.

More metal.

Concrete.

A vault.

A mountain.

At last:

`No obvious issues found.`

The user tries to type.

Nothing happens.

A small caption appears:

**Excellent.**

Then:

**Unfortunately, so is everything else.**

### `sysinfo` — system information

Reports information about the system itself.

The dangerous word is *system*.

Ask five engineers what the system is and you may receive the hardware, the operating system, the shell, the installed software, the network, or a hand gesture indicating all of it plus the desk.

`sysinfo` avoids choosing.

It reports facts about several of these things and leaves the noun unresolved.

This is wise.

The Guide's new question-asking machinery once asked:

**Which one is the system?**

The operating system volunteered.

The hardware objected.

The shell said neither of them would get much done without it.

The user claimed ownership.

Chapter 2 requested counsel.

`sysinfo` printed the storage figures and quietly left.

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

Sends local commits to a remote repository.

Before `git push`, your history is local.

After `git push`, it has witnesses.

This is the principal difference.

The commits themselves do not become wiser, cleaner or less embarrassing merely because they have crossed a network. They simply become available to other people, who may now pull them onto their own machines and preserve the evidence.

Git calls this distribution.

Courts use another word.

## Chapter 13 — Reconnaissance \& Security

Reconnaissance is the practice of learning about something before doing anything foolish to it.

This distinguishes it from most other human activity.

Security tools are particularly fond of maps, names and certificates.

A map tells you where the doors are.

A name tells you who owns the building.

A certificate tells you whether the person answering the door is entitled to claim that it is the building.

After this, you may begin worrying properly.

### `nmap` — network mapper

Scans hosts and networks to discover reachable systems, open ports and other network characteristics.

A *port* is a numbered place where network services may listen for connections.

This resembles a harbour closely enough that nobody bothered finding a new word.

`nmap` is therefore a map of ports which may contain no water, ships, customs officials or regrettable bars near the docks.

It tells you which ports appear open.

An open port is not necessarily an invitation.

Front doors are frequently open.

This does not mean the occupants have requested a census.

The Guide recommends remembering this distinction because computers are extraordinarily literal and lawyers have learned to compensate.

**Animation candidate**

A nautical chart appears.

Ports numbered `22`, `80`, `443`.

Tiny ships approach.

At `22`, a customs officer asks for credentials.

At `80`, a waiter hands over a webpage.

At `443`, the same waiter wears a padlock.

A ship approaches a closed port.

A giant sign drops:

**NO SERVICE**

The captain writes:

**Interesting.**

A lawyer rises from the sea.

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

The Guide admires this.

It is the first serious resistance the question has encountered.

### `openssl` — OpenSSL

Provides tools and libraries for cryptography, certificates, keys, secure connections, signing, verification and several other activities involving the mathematical arrangement of distrust.

A *certificate* certifies something.

This is useful because on a network you frequently need to establish that the machine claiming to be someone really is that someone.

Humans ordinarily solve this by recognizing faces.

Computers have no faces and, where they do, they are usually decorative.

So they use certificates.

A certificate is vouched for by a certificate authority.

The authority is trusted because your system contains a list saying it is trusted.

This naturally raises the question of who certifies the certifiers.

There are answers.

They involve chains.

Asking what the chains are ultimately attached to is possible, but tends to turn a certificate inspection into a philosophy seminar.

OpenSSL can inspect, create and verify much of this machinery.

It can also produce error messages of such concentrated authority that users often assume the mathematics itself is disappointed in them.

This is not true.

Mathematics has seen worse.

\---

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

### `cron` — chronos

Runs scheduled commands at specified times or intervals.

The useful thing about `cron` is that it does not need your future self to remember anything.

Your present self writes down an instruction.

Your future self then receives the consequences.

This is called automation.

A similar arrangement involving money is called debt.

The name comes from *chronos*, time, which is appropriate because `cron` concerns itself almost entirely with when something happens and very little with whether it was a good idea.

At 03:00 it will run the command.

At 03:00 tomorrow it will run it again.

It will continue doing this with the calm moral confidence of a machine that was given explicit instructions by somebody who is no longer available for questioning.

This is why old systems occasionally perform mysterious tasks at four in the morning.

Nobody knows why.

The person who knew left the company in 2017.

`cron` knows.

`cron` does not consider *why* part of the schedule.

**Animation candidate**

A user writes on a card:

`03:00 — DO THE THING`

The card is handed to a small clockwork clerk.

Night.

03:00.

The clerk wakes, pulls a lever, and something enormous happens off-screen.

The user bolts upright in bed.

Next night.

03:00.

Lever.

Same enormous noise.

The user rushes in and grabs the clerk.

**"WHY ARE YOU DOING THAT?"**

The clerk points to the card.

The user reads it.

Long pause.

In tiny handwriting at the bottom:

`— you`

The clock strikes 03:00 again.

The clerk reaches for the lever.

### `xargs` — extended arguments

Builds command arguments from standard input and runs a command with them.

An *argument* in computing is information supplied to a command so that the command knows what you want it to do.

An argument in ordinary life is what happens after somebody thinks they already know what you want them to do.

The distinction is important.

`xargs` takes things arriving through standard input and turns them into arguments for another command.

This is extremely efficient.

Rather than having one long argument yourself, you may pipe hundreds of small arguments into a machine and let it continue the disagreement on your behalf.

The machine does not mind.

It has no position.

It merely arranges the arguments and passes them on.

Several diplomatic services attempted to adopt `xargs` for peace negotiations.

The talks became dramatically faster.

Peace did not.

### `watch` — watch

Runs a command repeatedly and shows its output at regular intervals.

A watch tells you what time it is.

`watch` tells you what something is doing while time passes.

These are opposite approaches to the same problem.

The first observes time by watching change.

The second observes change by watching time.

Physicists were delighted by this for several minutes, after which somebody ran:

`watch date`

The machine began repeatedly showing the time while using time to decide when to show the time.

The resulting philosophical loop was harmless, although three graduate students had to be rebooted.

`watch` is most useful when you expect something to change and do not wish to spend the intervening period repeatedly asking whether it has changed yet.

Parents attempted to install it in children.

Children already contain a competing implementation.

**Animation candidate**

A terminal displays:

`watch something`

A small observer sits in a chair staring at `something`.

Clock tick.

No change.

Tick.

No change.

Tick.

The observer leans closer.

Nothing.

Behind the observer, the entire room changes.

Tick.

The command changes one character.

The observer rings an enormous bell.

\---

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

A store is a place where things are stored until somebody wants them, at which point the object of the store is to stop storing them.

Commerce has survived this contradiction for centuries.

The Azphalt Store applies it to abilities.

The original Guide was assembled by researchers who travelled about, learned things and sent the answers back.

A packaged skill is what happens when the researcher sends back not merely an answer but a small, reusable portion of the researcher's method.

The Guide's editors were delighted.

Researchers were less certain.

For years they had been told nobody could replace them.

Now they were being asked to provide installation instructions.

The Store calls this distribution.

The researchers call it **having a meeting**.

### `skill` — installed skill package

Adds instructions or specialist knowledge which the assistant can use when handling relevant requests.

A skill is ordinarily something acquired through learning and practice.

Installing one is therefore rather rude to learning and practice.

It also creates awkward questions.

If a skill can be installed, can talent be copied?

If it can be removed, is forgetting now an administrative action?

If it can be updated, was expertise merely an old version?

The Guide prefers not to become involved.

For practical purposes, a skill is a researcher flattened into instructions.

It sits quietly until a relevant question arrives.

Then the assistant consults it and behaves, briefly, as though it had spent several years somewhere interesting taking notes.

This is not wholly unlike education.

Education usually includes lunch.

**Animation candidate**

A researcher spends years studying an enormous subject.

Books, maps and field notes accumulate.

At last the researcher is compressed into a tiny package marked:

`skill`

The assistant installs it.

A miniature researcher unfolds from the package, sets up a desk and begins whispering instructions.

A relevant question arrives.

The assistant answers.

Another question arrives on a completely unrelated subject.

The assistant turns hopefully toward the tiny researcher.

The researcher folds up the desk and pretends to be luggage.

## chapter\_01 — The Absurdity of Redundancy

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

\---

## chapter\_02 — Doppelgänger Permissions

Chapter 2 established that files may be owned and may possess certain rights.

This led inevitably to the question of whether users possess rights too.

Unix considered the matter and invented administrators.

### `sudo` — superuser do

Runs a command as another user, commonly the superuser, when policy permits it.

The word *superuser* has done enormous damage to expectations.

A superuser cannot fly.

A superuser cannot see through walls.

A superuser cannot reverse time.

A superuser can remove the wall, alter the clock, and delete the file containing the objection.

For administrative purposes this was judged close enough.

But `sudo` does not generally transform you into a superuser.

It lets a command act with another user's privileges.

The superpower belongs to the verb.

This led briefly to the Galactic League of Superverbs.

**Read** joined immediately.

**Write** demanded representation.

**Execute** arrived with security.

**Reboot** ended the first meeting.

`sudo` survives as the computer's way of saying:

**Very well. But we're noting who asked.**

**Animation candidate**

A user faces a locked door.

`sudo open`

A cape drops onto the word **OPEN**.

OPEN flies through the door.

The user remains outside.

They type:

`sudo fly`

A cape lands on **FLY**.

FLY disappears into the sky.

The user watches it go.

The terminal asks:

`password:`

## chapter\_03 — The Infinite Directory

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

`rmdir` is stricter about actual files.

If the directory contains anything else, it refuses.

It is remarkably cautious for a command whose entire purpose is making somewhere cease to be.

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

\---

## chapter\_04 — Ghost Dependencies

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

\---

## chapter\_05 — The Anomaly

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

The Guide does not criticize this method.

Many mysteries would be solved much faster if the missing person had the decency to provide a pathname.

When asked to locate Chapter 5, `find` returned this file.

The editors explained that this was the lower-case `chapter\_05`.

`find` pointed out that nobody had specified capitalization.

The editors have since become more careful with questions.

The editors consider this a promising development.

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

`chapter\_05`

The editor says:

**"That's not it."**

The detective circles the exact search criteria in red.

\---

## chapter\_06 — The Mirror Editor

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

\---

## chapter\_07 — Zombie Processes

A process is something happening.

A status is a statement about how something is.

Combining the two was always going to create trouble.

### `ps` — process status

Reports information about processes at the moment the information is collected.

By the time you read it, that moment has passed.

Some processes may have ended.

Others may have begun.

A process which was using no CPU may now be using all of it.

One which appeared healthy may have become a smoking crater with a PID.

`ps` therefore presents the present in the only form computers can reliably provide it:

as very recent history.

Journalism reached the same solution much earlier.

The difference is that `ps` does not write **BREAKING** above it.

This is considered a missed opportunity.

A sufficiently short-lived process may begin and end between two invocations of `ps` and never appear at all.

The Guide calls these *shy processes*.

Operating-system engineers do not.

They were not consulted.

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

\---

## chapter\_08 — Backing Up the Void

An archive preserves things by putting them somewhere they are less convenient to use.

Museums have done this for centuries.

Computers merely added compression.

### `tar` — tape archive

Creates, extracts and manipulates archives containing files, historically for sequential storage on tape.

The name still contains *tape*.

The tape usually does not.

This is one of technology's quieter victories: the machine disappears and the vocabulary refuses to leave.

A tape archive may now live on flash storage, solid-state memory, a network filesystem or hardware containing no tape, reel, spool or satisfactory explanation for the name.

The tape survives as an idea, which is more than can be said for most tape drives.

`tar` also preserves collections of files, directory structure, permissions and other metadata so they can travel together and later be unpacked.

Luggage does roughly the same thing.

The principal difference is that luggage has never implemented `--extract`, although airports continue to approximate it.

## chapter\_09 — Looping the Loop

Searching is usually described as looking for something.

Pattern matching improves efficiency by deciding in advance what the thing must look like.

This does occasionally exclude the possibility that the thing has changed clothes.

### `grep` — global regular expression print

Searches input for lines matching a pattern and prints the matching lines.

The pattern may be a regular expression.

A regular expression is an expression so formally regulated that punctuation has acquired legal powers.

A dot may mean any character.

An asterisk may mean repetition.

Square brackets may organize a small electoral district of acceptable characters.

Giving punctuation this much authority outside a regular expression would be reckless.

Editors already have enough.

More curious is what `grep` returns.

Ask it for a pattern and it prints the line containing the pattern.

This is like asking a waiter for an olive and receiving the entire martini because the olive was in it.

Usually this is helpful.

Occasionally the line is 40,000 characters long.

The martini then requires two hands.

`grep` is particularly good at finding answers when you know what part of the answer looks like.

With the Guide now learning to ask questions, this has become slightly backwards.

Fortunately, backwards is where the Guide keeps most of its experience.

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

**and quite a lot of its friends.**

\---

## chapter\_10 — The Ultimate Handshake

A network interface is the place where one system meets a network.

This is not the same as the place where two systems understand one another.

That would be miraculous and is handled by higher layers.

### `ifconfig` — interface configuration

Displays or configures network-interface parameters on systems where the tool is available.

An interface may have an address and a netmask.

An address usually tells you where something is. A network address tells machines how to reach it. The two notions overlap just enough to cause trouble.

Then there is the mask.

A subnet mask does not conceal the address. It reveals which portion describes the network and which portion describes the host.

It is therefore a mask whose chief purpose is to explain the face.

The Witness Protection Programme of Folfanga tried this once. Every protected witness was issued a mask printed with the part of the address shared by everyone in the neighbourhood.

The programme was discontinued after an unusually efficient census.

Modern systems often provide newer tools for this work, but `ifconfig` remains available in many places, patiently wearing the least secretive mask ever devised.

**Animation candidate**

A nervous interface sits at a desk wearing a mask.

An investigator asks:

**"Address?"**

It hands over the address.

**"Mask?"**

It removes the mask and hands that over too.

The investigator compares them.

The interface looks suddenly exposed.

A protection officer rushes in, puts the mask back on, notices the network portion printed across the front, and turns it inside out.

It is printed there too.

## Chapter 16 — Documentation

Documentation is information explaining how something works.

This is an excellent idea.

Its principal weakness is that it is usually written by someone who already knows how the thing works.

The author therefore begins at the point where the reader hopes to finish.

Computer documentation has developed several ingenious ways around this, including examples, tutorials, reference manuals, frequently asked questions, and the phrase **obviously**.

The Guide recommends immediate suspicion of the last one.

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

Searches the user's command-search path for an executable matching a command name and reports the path it finds.

*Which* is not a complete question.

It walks into the shell, points vaguely, and says:

**Which?**

The shell has learned to infer the missing portion:

**Which executable would you use if I typed this command name?**

It searches `PATH`.

`PATH`, confusingly, is not a path but a list of directories containing possible paths to commands.

Chapter 3 objected to the terminology.

The shell asked:

**Which path?**

Chapter 3 has not replied, partly from principle and partly because nobody knows which one it meant.

### `help` — shell help

Displays help for shell built-ins where supported.

`help` is one of the few commands whose name is also what the user is doing by typing it.

This is efficient, but socially difficult.

People will spend twenty minutes searching error messages, rereading old forum posts and restarting things rather than type `help` while somebody is watching.

The shell has no opinion about this.

It simply prints the help.

It does, however, keep history.

This may explain the hesitation.

## Chapter 17 — Time \& Memory

Computers remember with extraordinary precision and forget with extraordinary efficiency.

Humans do the opposite.

This has made collaboration difficult.

A computer may remember the exact command you typed at 2:13 in the morning six months ago.

You may remember only that it seemed like a good idea.

Both accounts are accurate.

Only one is useful in court.

### `history` — shell history

Displays commands previously entered in the shell, subject to the shell's history configuration.

History is normally written by the victors.

Shell history is written by whoever had access to the keyboard.

This produces a very different kind of civilization.

It contains no kings.

No treaties.

No heroic battles.

Mostly it contains:

`cd`

`ls`

`cd ..`

`ls`

`cd ..`

and, occasionally,

`sudo`

followed by a long period of silence.

The important thing about history is that it makes the past searchable.

This sounds wonderful until you discover the past contains exact quotations.

The Guide's new question-asking apparatus has become fascinated by shell history.

For the first time it can examine an answer and ask:

**What question made you type that?**

The shell cannot say.

It records commands, not motives.

This is probably for the best.

**Animation candidate**

A grand historical archive opens.

Shelves are labelled by year.

The user expects wars, empires and important speeches.

Instead every volume contains command lines.

One page reads:

`cd wrong`

`cd ..`

`cd right`

Another:

`rm ...`

The next several pages have been torn out.

A historian gasps.

The terminal prints:

`history`

The missing line is still there.

The historian faints.

### `sleep` — sleep

Pauses execution for a specified amount of time.

This may be the most humane command in Unix.

It instructs a process to do nothing for a while and regards this as correct behaviour.

Human institutions have struggled with the concept.

The process is not actually asleep.

It does not dream, snore or wake with a vague recollection of having agreed to something.

It waits.

The Somnological Society of Betelgeuse Minor demanded that the command be renamed `wait`.

Computer scientists explained that `wait` already meant something else.

The Society proposed `rest`.

Taken.

`pause`.

Taken.

`delay`.

Taken.

Eventually they discovered that computing had reserved so many words for doing nothing that there were none left for sleep.

They went to bed.

The process woke first.

### `touch` — touch

Updates file timestamps and, in common use, creates an empty file if the named file does not already exist.

Nothing is touched.

This is the first difficulty.

The second is that touching a nonexistent file can cause a file to exist.

The third is that touching an existing file changes its recorded time without making any corresponding change to its contents.

A gesture has therefore acquired authority over both existence and history.

The theologians immediately became unbearable.

They argued that creation had always been a kind of touch.

The physicists objected that God, being outside space, cannot make contact in the ordinary sense.

Everyone remembered Zorblep Thwack's birthday.

The objection was withdrawn.

`touch` continues performing minor acts of creation without claiming responsibility.

**Animation candidate**

A finger approaches empty space but never quite reaches it.

`touch newfile`

A file appears.

The finger looks surprised.

`touch oldfile`

A clock above an existing file jumps forward while the file itself remains unchanged.

A theologian enters, writes **PROMISING** in a notebook, and is removed by the Birthday Committee.

## Chapter 18 — Agreement

Computers are often accused of being incapable of nuance.

This is unfair.

They possess at least two kinds.

One is `0`.

The other is everything else.

### `yes` — yes

Repeatedly outputs a string, `y` by default, until stopped or until its output can no longer be written.

A normal **yes** is an answer to a question.

`yes` dispenses with the question.

The Guide found this perfectly natural.

For most of its existence it had done much the same thing on a larger budget.

The command is often used to provide repeated affirmative input to another command, saving a human from pressing `y` over and over again.

This is convenient until one notices that agreement has become a process which continues independently of anyone agreeing.

Lawyers noticed.

They have not stopped noticing.

`yes`, meanwhile, continues:

`y`

`y`

`y`

until something kills it.

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

The Guide takes notes.

### `true` — true

Does nothing and reports success.

Unix thereby reduced one of philosophy's oldest problems to an exit status.

`true` does not inspect a statement.

It does not compare a claim with reality.

It performs no useful work at all.

Then it says everything went well.

Administrators immediately understood the concept.

### `false` — false

Does nothing and reports failure.

This is the same amount of activity as `true`.

Only the report differs.

Thus two programs may perform exactly the same operation—nothing—and disagree completely about the result.

The Guide's new question-asking machinery is fascinated by this.

Here are **true** and **false** already available as answers, and neither command requires anybody to supply a proposition.

The universe has rarely been this considerate.

**Animation candidate**

Two identical boxes sit side by side.

Press `true`.

Nothing happens.

A green tick appears.

Press `false`.

Nothing happens.

A red cross appears.

A researcher asks:

**"What happened?"**

The Guide answers:

**Nothing.**

The researcher asks:

**"Was that good?"**

The boxes finally have something to contribute.

## Chapter 19 — Pipes \& Redirection

A pipe is normally a hollow object through which something passes.

Unix retained the hollow part and discarded the object.

This was considered efficient.

The shell pipe, written `|`, takes the output of one command and feeds it into the input of another.

Nothing visible travels through it.

This does not prevent everyone from calling it a pipe.

Plumbing has filed no formal complaint, largely because plumbing has seen computers try to name sockets.

### `|` — pipe

Connects the standard output of one command to the standard input of another.

One command speaks.

Another hears it.

Neither needs to know much about the other.

This is the Unix pipe, and it is responsible for an alarming amount of cooperation between programs that have never been introduced.

Before pipes, output commonly arrived at the terminal where a human could interfere.

After pipes, commands could pass answers directly to other commands, which could filter, count, sort or reinterpret them before any person had a chance to misunderstand the original.

This was the beginning of machine gossip.

Chapter 15 suspects that the Guide's new question-asking machinery is largely a sophisticated arrangement for passing answers between things until one of them looks like a question.

The Pipe Committee received this accusation.

It forwarded it.

**Animation candidate**

A command speaks into a brass pipe.

Words shoot through to another command, which crosses some out, circles others and sends the result into another pipe.

Soon an entire city is connected by pipes carrying text.

A researcher asks:

**"Who said that?"**

Every pipe points left.

The camera follows them until they disappear over the horizon.

### `>` — redirect output

Redirects standard output to a file, creating it if necessary and ordinarily replacing its previous contents.

The symbol points to the right.

This is helpful.

It gives the impression that output is going somewhere.

The output agrees.

The existing contents of the destination file may be less enthusiastic.

With ordinary `>` redirection, what was there before is replaced.

This has occasionally surprised users who believed *redirect* meant *send somewhere else* rather than *send somewhere else after clearing the destination of witnesses*.

The Guide recommends reading arrows carefully.

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

\---

## Chapter 20 — Copies \& Movement

Files may be copied or moved.

Humans may also be copied or moved, but both operations are subject to significantly more paperwork.

The distinction between copying and moving seems obvious until one asks what, precisely, is moving.

A file has a name, contents, metadata, a place in a filesystem and, according to Chapter 2, an owner whether it likes it or not.

Move the name and the contents may not move at all.

Move across filesystems and the system may copy the contents and remove the original.

The command is still called `mv`.

Language has learned not to watch too closely.

### `cp` — copy

Copies files or directories, subject to the options you give it.

A copy is another instance of something that was supposed to be the same.

The trouble begins immediately afterward.

Give the copy a different pathname and it is the same thing somewhere else.

Edit one copy and it becomes a different thing with the same past.

Edit both and they become relatives.

The Directory Workers' Union says copying creates employment.

The File Rights Committee says it creates a person.

The filesystem says it creates another file and continues working while everyone else argues.

The argument currently occupies 4.7 GB.

**Animation candidate**

A file enters a copying machine.

An identical file emerges.

Both are labelled `report.txt`.

One moves to `/backup`.

The other is edited.

A family tree appears between them:

**original**

and

**also original, according to itself**

The copier produces a third before the committee can adjourn.

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

\---

## Chapter 21 — Things From Elsewhere

The Internet made it possible to obtain information from almost anywhere.

This was immediately followed by the discovery that almost anywhere contains information nobody had requested.

Tools were therefore invented to fetch things deliberately.

This made the problem feel much more professional.

### `curl` — transfer data with URLs

Transfers data to or from a URL using supported network protocols.

The name originally referred to *client URL*.

This has not stopped people imagining the command curling things.

The Internet contains enough straight lines already.

A URL tells `curl` where to make a request.

The remote server responds.

The response may be a webpage, JSON, a file, headers, an error, a redirect, or a detailed explanation of why you are not authorized to see the thing you have just demonstrated considerable interest in seeing.

`curl` can follow redirects if instructed.

A redirect is the network equivalent of being told:

**Not here. Try over there.**

If the next server redirects again, the conversation becomes directions given by strangers.

Humans usually become lost after three.

`curl` can continue until policy, configuration or good sense intervenes.

Good sense is not enabled by default.

**Animation candidate**

A tiny courier marked `curl` receives a URL.

It knocks on a server.

The server points to another building.

The courier runs there.

That building points elsewhere.

Again.

Again.

Soon the courier is sprinting around a planet following arrows.

Finally it reaches the original building from the other side.

The server hands over:

`301`

The courier looks at the camera.

### `wget` — network downloader

Retrieves resources from network locations, commonly over HTTP, HTTPS and FTP where supported.

`wget` is refreshingly literal.

The web has a thing.

`wget` gets it.

With recursive options it may also get things linked from the thing, then things linked from those things, until the modest desire to save one page begins resembling an archaeological programme.

This is the dream of every collector:

**I shall keep this in case it disappears.**

The Internet is happy to assist.

It may include stylesheets, images, scripts, thumbnails and several things whose only visible purpose is asking whether you accept cookies.

Chapter 4 asked whether recursive downloading counts as dependency management.

`wget` saved the question for offline reading.

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

