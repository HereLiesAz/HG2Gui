# HG2Gui Animation Design System

## Guide Tangents

**Canonical editorial and animation rule**

> A Guide tangent begins with an absurd assertion delivered as fact. Its apparent implausibility is remedied by explanation, not retraction. The explanation proceeds through internally consistent science-fiction logic until the assertion appears inevitable — or at least considerably more troublesome to dispute.

---

## 1. Purpose

Tangents are not interruptions to the Guide. They are one of its principal methods of explanation. They let a technical entry widen into language, history, bureaucracy, anthropology, physics, religion, architecture, or any other field unfortunate enough to share a word with Unix.

**The tangent may be wholly fictitious. The technical claim may not be. The Guide is permitted to invent the road; it is not permitted to move the destination.**

---

## 2. Primary Entry Point: Absurd Etymology

The preferred way into a tangent is an etymological explanation: the Guide states where a command name came from, with complete confidence, and then explains the increasingly implausible history required to make that statement true.

> **Default pattern:** absurd fact → necessary explanation → larger absurdity → rigorous explanation → technical reality.

---

## 3. Tangent Logic

1. State the absurd proposition without hesitation, apology, quotation marks, or a wink.
2. Do not announce that the proposition is hard to believe. The existence of the explanation is the acknowledgement.
3. Explain it seriously. Each explanation should create the need for the next explanation.
4. Use coherent science-fiction logic: invented institutions, species, historical accidents, standards bodies, technologies, customs, legal systems, or physical laws.
5. Allow the explanation to become more important than the sentence that caused it.
6. Return to the technical subject without signalling a return. The Guide behaves as though the detour was necessary background.

---

## 4. Truth Boundary

| May be invented | Must remain accurate |
|---|---|
| Etymology and ancient history | What the command actually does |
| Planets, species and institutions | Command syntax shown on screen |
| Bureaucracies, customs and laws | Filesystem, process, network and shell behaviour |
| Causation inside the tangent | Distinctions such as records vs installed packages |
| Editorial mythology and cross-references | Any operational claim a user could rely on |

---

## 5. Accepted Tangent Forms

### Absurd etymology — PRIMARY

A false but internally rigorous origin story for the command name.

### Lexical collision

Another ordinary meaning of the same word is treated as a neighbouring Guide entry.

### Editorial contamination

A cross-reference, nearby definition, or classification quietly hijacks the entry.

### Literalized metaphor

Technical language becomes physical: pipes are pipes, dependencies require interventions, privileges require badges.

### Diagram escape

An explanatory diagram becomes sufficiently elaborate to acquire its own subject matter.

---

## 6. Animation Rules for Tangents

- A tangent is a real animation sequence, not an app-shell title card or storyboard page.
- It obeys the same 1:1 frame, single-background, limited-palette and flat-vector production rules as the technical sequences.
- It may introduce a new cast when the tangent requires one, but recurring characters remain locked once established.
- A tangent may visually leave the technical world entirely. It must still feel like the same Guide: spare geometry, dry staging, strong negative space, minimal acting, and literal visual logic.
- No decorative UI, entry titles, tabs, borders, production labels, or framing may be painted into the animation.
- The tangent should end on an image or idea that makes the following technical entry feel inevitable rather than merely adjacent.

---

## 7. Placement and Frequency

Tangents should be selective. Their value comes from the possibility that any innocent word may contain an entire misplaced civilization, not from proving that every command does.

- Major tangents: approximately one in every four entries at most.
- Short explanatory detours may occur more often when they are tightly attached to the command name.
- An existing technical sequence does not need to be replaced; the tangent may precede it as a neighbouring Guide entry.
- Do not add a tangent merely because a pun exists. It must generate an explanation worth following.

---

## 8. Canonical Precedents

### `cat` — lexical / etymological cluster

The Guide may first present an entirely serious entry concerning cats — including theology, physics, domestic authority, or competing historical explanations — because these are also entries for the word `cat`.

The canonical concatenate sequence then follows without transition or apology.

**Required technical landing:** `cat` concatenates file contents and, with one file, outputs that file. The feline material never alters that fact.

### `mv` — The Elevator Incident

The preferred home for the elevator tangent is immediately before `mv`.

The Guide confidently explains that the computing sense of **move** is historically entangled with elevator classification. The explanation requires an account of an elevator whose idea of movement eventually becomes broader than the building, the atmosphere, and good judgment.

The elevator sequence may expand into a substantial visual tangent, then cut directly to the canonical `mv` animation in which a file may remain visually still while its pathname or name changes.

The contrast is the point.

> The Guide does not go off-topic. It reveals that the topic was much larger and significantly worse organized than previously reported.

---

## 9. Failure Modes

- Beginning with doubt: “Nobody knows why…” / “It is suspicious that…” / “Perhaps…”
- Flagging the joke: “Oddly,” “believe it or not,” “somehow,” or other verbal elbowing.
- Random whimsy with no causal chain.
- A tangent whose only mechanism is a pun.
- Correcting or retracting the absurd assertion instead of explaining it.
- Returning with “back to the command,” “anyway,” or similar scaffolding.
- Letting the tangent introduce a false technical claim.
- Using a tangent as an excuse to abandon established visual continuity.

---

## 10. Acceptance Test

A tangent is ready when all of the following are true:

- The opening assertion is absurd and stated as ordinary fact.
- The reader can infer why an explanation is being supplied without being told that the assertion is implausible.
- Every explanatory beat follows logically from the previous one.
- The tangent becomes stranger by becoming more rigorous, not more random.
- The final connection to the technical entry feels unavoidable in retrospect.
- The command explanation remains exact.
- The animation can be staged using the project’s established visual system without importing app UI or film characters.

---

> **Editorial maxim:** When a command name can support an explanation, explain it. Accuracy becomes mandatory at the point where the explanation reaches the command.
