package ohi.andre.consolelauncher.managers.suggestions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.managers.SystemContext;
import ohi.andre.consolelauncher.tuils.Tuils;

/**
 * Defines the static command tree for the point-and-click interface.
 * Generates a dynamic tree based on the current {@link SystemContext}.
 */
public class CommandMenu {

    /**
     * Builds the command tree based on the current OS context.
     * @return A list of root-level MenuOptions.
     */
    private static List<MenuOption> getTree() {
        List<MenuOption> tree = new ArrayList<>();
        SystemContext context = SystemContext.getInstance();

        // Colors
        int C_GREEN = R.color.react_green;
        int C_ORANGE = R.color.react_orange;
        int C_RED = R.color.react_red;
        int C_CYAN = R.color.react_cyan;
        int C_PURPLE = R.color.react_purple;
        int C_GRAY = R.color.react_gray;
        int C_BLUE = R.color.react_blue;

        // Common Packages
        List<MenuOption> packagesCommon = new ArrayList<>();
        String[] pkgs = {"git", "curl", "vim", "htop", "node", "python"};
        for (String pkg : pkgs) {
            packagesCommon.add(new MenuOption("pkg-" + pkg, pkg, pkg, C_CYAN));
        }

        // OS Specific
        if (context.getOs() == SystemContext.OSType.UBUNTU) {
            // SUDO (APT)
            List<MenuOption> aptChildren = new ArrayList<>();
            aptChildren.add(new MenuOption("apt-update", "UPDATE", "update", C_GREEN));

            List<MenuOption> aptInstallChildren = new ArrayList<>();
            aptInstallChildren.addAll(packagesCommon);

            aptChildren.add(new MenuOption("apt-install", "INSTALL", "install", C_GREEN, aptInstallChildren));
            aptChildren.add(new MenuOption("apt-remove", "REMOVE", "remove", C_RED));

            List<MenuOption> sudoChildren = new ArrayList<>();
            sudoChildren.add(new MenuOption("sudo-apt", "APT", "apt", C_ORANGE, aptChildren));
            sudoChildren.add(new MenuOption("sudo-snap", "SNAP", "snap", C_ORANGE));

            tree.add(new MenuOption("root-sudo", "SUDO", "sudo", C_RED, sudoChildren));
        } else if (context.getOs() == SystemContext.OSType.MACOS) {
            // BREW
            List<MenuOption> brewChildren = new ArrayList<>();

            List<MenuOption> brewInstallChildren = new ArrayList<>();
            brewInstallChildren.addAll(packagesCommon);
            brewChildren.add(new MenuOption("brew-install", "INSTALL", "install", C_GREEN, brewInstallChildren));

            brewChildren.add(new MenuOption("brew-cask", "CASK", "cask", C_CYAN));
            brewChildren.add(new MenuOption("brew-update", "UPDATE", "update", C_GREEN));

            tree.add(new MenuOption("root-brew", "BREW", "brew", C_ORANGE, brewChildren));
            tree.add(new MenuOption("root-sudo", "SUDO", "sudo", C_RED));
        } else if (context.getOs() == SystemContext.OSType.WINDOWS) {
            // WINGET
            List<MenuOption> wingetChildren = new ArrayList<>();
            wingetChildren.add(new MenuOption("win-install", "INSTALL", "install", C_GREEN));
            wingetChildren.add(new MenuOption("win-search", "SEARCH", "search", C_CYAN));

            tree.add(new MenuOption("root-winget", "WINGET", "winget", C_BLUE, wingetChildren));
            tree.add(new MenuOption("root-ps", "POWERSHELL", "powershell", C_BLUE));
        }

        // GIT
        List<MenuOption> gitChildren = new ArrayList<>();
        gitChildren.add(new MenuOption("git-status", "STATUS", "status", C_GREEN));

        List<MenuOption> gitAddChildren = new ArrayList<>();
        gitAddChildren.add(new MenuOption("git-add-all", "ALL (.)", ".", C_CYAN));
        gitChildren.add(new MenuOption("git-add", "ADD", "add", C_GREEN, gitAddChildren));

        List<MenuOption> gitCommitChildren = new ArrayList<>();
        gitCommitChildren.add(new MenuOption("git-commit-m", "MESSAGE (-m)", "-m", C_CYAN));
        gitChildren.add(new MenuOption("git-commit", "COMMIT", "commit", C_GREEN, gitCommitChildren));

        gitChildren.add(new MenuOption("git-push", "PUSH", "push", C_RED));
        gitChildren.add(new MenuOption("git-pull", "PULL", "pull", C_CYAN));
        gitChildren.add(new MenuOption("git-checkout", "CHECKOUT", "checkout", C_ORANGE));

        tree.add(new MenuOption("root-git", "GIT", "git", C_ORANGE, gitChildren));

        // DOCKER
        List<MenuOption> dockerChildren = new ArrayList<>();
        dockerChildren.add(new MenuOption("doc-ps", "PS", "ps", C_GREEN));
        dockerChildren.add(new MenuOption("doc-images", "IMAGES", "images", C_GREEN));
        dockerChildren.add(new MenuOption("doc-run", "RUN", "run", C_ORANGE));
        dockerChildren.add(new MenuOption("doc-compose", "COMPOSE", "compose", C_PURPLE));
        tree.add(new MenuOption("root-docker", "DOCKER", "docker", C_CYAN, dockerChildren));

        // Common
        tree.add(new MenuOption("root-ls", "LS", "ls", C_GRAY));
        tree.add(new MenuOption("root-cd", "CD", "cd", C_GRAY));
        tree.add(new MenuOption("root-clear", "CLEAR", "clear", C_GRAY));
        tree.add(new MenuOption("root-help", "HELP", "help", C_GREEN));

        // Switch OS (Hidden/Utility)
        List<MenuOption> switchChildren = new ArrayList<>();
        switchChildren.add(new MenuOption("sw-ubuntu", "UBUNTU", "ubuntu", C_ORANGE));
        switchChildren.add(new MenuOption("sw-macos", "MACOS", "macos", C_GRAY));
        switchChildren.add(new MenuOption("sw-win", "WINDOWS", "windows", C_BLUE));
        tree.add(new MenuOption("sys-switch", "SWITCH OS", "switch-os", C_GRAY, switchChildren));

        return tree;
    }

    /**
     * Helper to calculate the text that should appear before the suggestion.
     * Centralizes tokenization logic to avoid inconsistency.
     * @param input Full user input
     * @return The prefix string (e.g., "git " for input "git st")
     */
    public static String calculateTextBefore(String input) {
        if (input == null) return Tuils.EMPTYSTRING;
        String fullInput = input;
        String[] parts = fullInput.trim().split(Tuils.SPACE);
        if (fullInput.endsWith(Tuils.SPACE)) {
            return fullInput.trim();
        } else if (parts.length > 0) {
            // remove last part
            StringBuilder sb = new StringBuilder();
            for(int i=0; i<parts.length-1; i++) sb.append(parts[i]).append(Tuils.SPACE);
            return sb.toString().trim();
        }
        return Tuils.EMPTYSTRING;
    }

    /**
     * Traverses the command tree to find suggestions for the given input.
     * @param input The current user input.
     * @return A list of MenuOptions that match the next step in the command chain.
     */
    public static List<MenuOption> getSuggestions(String input) {
        if (input == null) input = "";

        List<MenuOption> tree = getTree();

        String[] parts = input.trim().split("\\s+");
        String cleanInput = input.trim();
        boolean isTypingNewWord = input.endsWith(" ");

        if (cleanInput.isEmpty() || (parts.length == 1 && !isTypingNewWord)) {
            String keyword = parts.length > 0 && !cleanInput.isEmpty() ? parts[0] : "";
            List<MenuOption> result = new ArrayList<>();
            for (MenuOption opt : tree) {
                if (opt.value.startsWith(keyword)) {
                    result.add(opt);
                }
            }
            return result;
        }

        List<MenuOption> currentNode = tree;

        String[] traversableParts = isTypingNewWord ? parts : Arrays.copyOf(parts, parts.length - 1);

        for (String token : traversableParts) {
            MenuOption match = null;
            for (MenuOption opt : currentNode) {
                if (opt.value.equals(token)) {
                    match = opt;
                    break;
                }
            }

            if (match != null && match.children != null) {
                currentNode = match.children;
            } else if (match != null) {
                return new ArrayList<>(); // Leaf
            } else {
                return new ArrayList<>(); // No match
            }
        }

        List<MenuOption> result = new ArrayList<>();
        if (!isTypingNewWord) {
            String keyword = parts[parts.length - 1];
            for (MenuOption opt : currentNode) {
                if (opt.value.startsWith(keyword)) {
                    result.add(opt);
                }
            }
        } else {
            result.addAll(currentNode);
        }

        return result;
    }
}
