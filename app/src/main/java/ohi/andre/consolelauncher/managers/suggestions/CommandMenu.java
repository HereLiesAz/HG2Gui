package ohi.andre.consolelauncher.managers.suggestions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ohi.andre.consolelauncher.R;

public class CommandMenu {

    private static List<MenuOption> TREE;

    static {
        TREE = new ArrayList<>();

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

        TREE.add(new MenuOption("root-git", "GIT", "git", C_ORANGE, gitChildren));

        // DOCKER
        List<MenuOption> dockerChildren = new ArrayList<>();
        dockerChildren.add(new MenuOption("doc-ps", "PS", "ps", C_GREEN));
        dockerChildren.add(new MenuOption("doc-images", "IMAGES", "images", C_GREEN));
        dockerChildren.add(new MenuOption("doc-run", "RUN", "run", C_ORANGE));
        dockerChildren.add(new MenuOption("doc-compose", "COMPOSE", "compose", C_PURPLE));
        TREE.add(new MenuOption("root-docker", "DOCKER", "docker", C_CYAN, dockerChildren));

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

        TREE.add(new MenuOption("root-sudo", "SUDO", "sudo", C_RED, sudoChildren));

        // Common
        TREE.add(new MenuOption("root-ls", "LS", "ls", C_GRAY));
        TREE.add(new MenuOption("root-cd", "CD", "cd", C_GRAY));
        TREE.add(new MenuOption("root-clear", "CLEAR", "clear", C_GRAY));
        TREE.add(new MenuOption("root-help", "HELP", "help", C_GREEN));

        // Switch OS
        List<MenuOption> switchOsChildren = new ArrayList<>();
        switchOsChildren.add(new MenuOption("sw-ubuntu", "UBUNTU", "ubuntu", C_ORANGE, R.drawable.ic_os_ubuntu));
        switchOsChildren.add(new MenuOption("sw-macos", "MACOS", "macos", C_GRAY, R.drawable.ic_os_macos));
        switchOsChildren.add(new MenuOption("sw-win", "WINDOWS", "windows", C_BLUE, R.drawable.ic_os_windows));

        TREE.add(new MenuOption("sys-switch", "SWITCH OS", "switch-os", C_GRAY, switchOsChildren));
    }

    public static List<MenuOption> getSuggestions(String input) {
        if (input == null) input = "";

        String[] parts = input.trim().split("\\s+");
        String cleanInput = input.trim();
        boolean isTypingNewWord = input.endsWith(" ");

        if (cleanInput.isEmpty() || (parts.length == 1 && !isTypingNewWord)) {
            String keyword = parts.length > 0 && !cleanInput.isEmpty() ? parts[0] : "";
            List<MenuOption> result = new ArrayList<>();
            for (MenuOption opt : TREE) {
                if (opt.value.startsWith(keyword)) {
                    result.add(opt);
                }
            }
            return result;
        }

        List<MenuOption> currentNode = TREE;

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
