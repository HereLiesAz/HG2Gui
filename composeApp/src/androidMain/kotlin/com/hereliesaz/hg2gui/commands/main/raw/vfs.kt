package com.hereliesaz.hg2gui.commands.main.raw

import com.hereliesaz.hg2gui.commands.CommandAbstraction
import com.hereliesaz.hg2gui.commands.ExecutePack
import com.hereliesaz.hg2gui.commands.main.MainPack
import com.hereliesaz.hg2gui.managers.VfsManager
import com.hereliesaz.hg2gui.tuils.libsuperuser.Shell

/**
 * Command-line access to the sandboxed [VfsManager] filesystem. Deliberately namespaced under
 * `vfs` rather than shadowing `ls`/`cd`/`cat`/etc. directly: those verbs are already offered as
 * real-shell pills (see CommandTree's SHELL list) and users typing them expect real device
 * files, not a sandbox. `vfs mount` is the one operation that reaches outside the sandbox, and
 * only when root is available.
 */
class vfs : CommandAbstraction {

    override fun exec(pack: ExecutePack): String? {
        val info = pack as MainPack
        val args = pack.args
        val sub = (args?.getOrNull(0) as? String)?.lowercase() ?: return USAGE
        val rest = args.drop(1).map { it as? String ?: "" }
        val context = info.androidContext

        return when (sub) {
            "ls" -> {
                val entries = VfsManager.list(context)
                if (entries.isEmpty()) "(empty)"
                else entries.joinToString("\n") { if (it.isDirectory) it.name + "/" else it.name }
            }
            "pwd" -> VfsManager.currentPath()
            "cd" -> {
                val name = rest.getOrNull(0) ?: return "Usage: vfs cd <dir>"
                if (VfsManager.cd(context, name)) VfsManager.currentPath() else "No such directory: $name"
            }
            "mkdir" -> {
                val name = rest.getOrNull(0) ?: return "Usage: vfs mkdir <name>"
                if (VfsManager.mkdir(context, name)) "Created $name" else "Could not create $name"
            }
            "touch" -> {
                val name = rest.getOrNull(0) ?: return "Usage: vfs touch <name>"
                if (VfsManager.touch(context, name)) "Created $name" else "Could not create $name"
            }
            "cat" -> {
                val name = rest.getOrNull(0) ?: return "Usage: vfs cat <name>"
                VfsManager.readText(context, name) ?: "No such file: $name"
            }
            "rm" -> {
                val name = rest.getOrNull(0) ?: return "Usage: vfs rm <name>"
                if (VfsManager.delete(context, name)) "Removed $name" else "Could not remove $name"
            }
            "mv" -> {
                if (rest.size < 2) return "Usage: vfs mv <from> <to>"
                if (VfsManager.move(context, rest[0], rest[1])) "Moved ${rest[0]} -> ${rest[1]}" else "Could not move"
            }
            "cp" -> {
                if (rest.size < 2) return "Usage: vfs cp <from> <to>"
                if (VfsManager.copy(context, rest[0], rest[1])) "Copied ${rest[0]} -> ${rest[1]}" else "Could not copy"
            }
            "mount" -> mount(context, rest.getOrNull(0))
            else -> USAGE
        }
    }

    private fun mount(context: android.content.Context, target: String?): String {
        if (!Shell.SU.available()) {
            return "Root not available — vfs mount requires a rooted device (su)."
        }
        if (target == null) return "Usage: vfs mount <real-path>"
        val root = VfsManager.init(context)
        val result = Shell.SU.run("mkdir -p '$target' && mount --bind '${root.absolutePath}' '$target'")
        return if (result != null) "Mounted ${root.absolutePath} at $target"
        else "Mount failed — check that su granted access and the target path is valid."
    }

    override fun argType(): IntArray = intArrayOf(CommandAbstraction.PLAIN_TEXT)

    override fun priority(): Int = 3

    override fun helpRes(): Int = 0

    override fun onArgNotFound(pack: ExecutePack, indexNotFound: Int): String? = null

    override fun onNotArgEnough(pack: ExecutePack, nArgs: Int): String? = USAGE

    companion object {
        private const val USAGE = "Usage: vfs <ls|cd|pwd|mkdir|touch|cat|rm|mv|cp|mount> [args]"
    }
}
