/**
 * @file constants.ts
 * @description Stores static configuration, the command definitions tree, and utility functions
 * for traversing the command hierarchy.
 */

import { MenuOption, SystemContext } from './types';

export const INITIAL_WELCOME_MSG = `
HITCHHIKER TERMINAL v4.2
--------------------------------
> KERNEL: LINUX 2.6.x (PROBABLY)
> UI: RETRO-FUTURISTIC
> DON'T PANIC.
`;

/**
 * Color Palette Constants
 * Matches the flat, high-contrast aesthetic of the 2005 movie UI.
 */
const C = {
  GREEN: 'bg-[#A4F644]', // Primary text/success color
  ORANGE: 'bg-[#F6A444]', // Secondary/Warning
  RED: 'bg-[#D92B2B]',    // Error/High Importance
  CYAN: 'bg-[#44F6F6]',   // Info/Structure
  PURPLE: 'bg-[#9370DB]', // Special/Auxiliary
  GRAY: 'bg-[#444444]',   // Neutral/Disabled
  BLUE: 'bg-[#3B82F6]',   // Windows/Corporate
};

// --- Reusable Package Lists ---
/**
 * Helper to generate a consistent list of mock packages for installation commands.
 */
const createPackageList = (prefix: string, packages: string[], color: string): MenuOption[] => {
    return packages.map(pkg => ({
        id: `pkg-${pkg}`,
        label: pkg,
        value: pkg,
        color: color
    }));
};

const PACKAGES_COMMON = createPackageList('common', ['git', 'curl', 'vim', 'htop', 'node', 'python'], C.CYAN);

// --- Modular Menus ---

const GIT_MENU: MenuOption = {
  id: 'root-git',
  label: 'GIT',
  value: 'git',
  color: C.ORANGE,
  children: [
      { id: 'git-status', label: 'STATUS', value: 'status', color: C.GREEN },
      { id: 'git-add', label: 'ADD', value: 'add', color: C.GREEN, children: [
          { id: 'git-add-all', label: 'ALL (.)', value: '.', color: C.CYAN },
      ]},
      { id: 'git-commit', label: 'COMMIT', value: 'commit', color: C.GREEN, children: [
          { id: 'git-commit-m', label: 'MESSAGE (-m)', value: '-m', color: C.CYAN },
      ]},
      { id: 'git-push', label: 'PUSH', value: 'push', color: C.RED },
      { id: 'git-pull', label: 'PULL', value: 'pull', color: C.CYAN },
      { id: 'git-checkout', label: 'CHECKOUT', value: 'checkout', color: C.ORANGE }
  ]
};

const DOCKER_MENU: MenuOption = {
    id: 'root-docker',
    label: 'DOCKER',
    value: 'docker',
    color: C.CYAN,
    children: [
        { id: 'doc-ps', label: 'PS', value: 'ps', color: C.GREEN },
        { id: 'doc-images', label: 'IMAGES', value: 'images', color: C.GREEN },
        { id: 'doc-run', label: 'RUN', value: 'run', color: C.ORANGE },
        { id: 'doc-compose', label: 'COMPOSE', value: 'compose', color: C.PURPLE }
    ]
};

const COMMON_COMMANDS: MenuOption[] = [
    { id: 'root-ls', label: 'LS', value: 'ls', color: C.GRAY },
    { id: 'root-cd', label: 'CD', value: 'cd', color: C.GRAY },
    { id: 'root-clear', label: 'CLEAR', value: 'clear', color: C.GRAY },
    { id: 'root-help', label: 'HELP', value: 'help', color: C.GREEN },
];

// --- OS Specific Menus ---

// Ubuntu / Debian Context
const APT_MENU: MenuOption = {
    id: 'sudo-apt',
    label: 'APT',
    value: 'apt',
    color: C.ORANGE,
    children: [
        { id: 'apt-update', label: 'UPDATE', value: 'update', color: C.GREEN },
        { id: 'apt-install', label: 'INSTALL', value: 'install', color: C.GREEN, children: [
            { id: 'grp-common', label: 'PACKAGES', value: '', color: C.PURPLE, children: PACKAGES_COMMON }
        ]},
        { id: 'apt-remove', label: 'REMOVE', value: 'remove', color: C.RED },
    ]
};

const SNAP_MENU: MenuOption = {
    id: 'sudo-snap',
    label: 'SNAP',
    value: 'snap',
    color: C.ORANGE,
    children: [
        { id: 'snap-install', label: 'INSTALL', value: 'install', color: C.GREEN },
    ]
};

// MacOS Context
const BREW_MENU: MenuOption = {
    id: 'root-brew',
    label: 'BREW',
    value: 'brew',
    color: C.ORANGE,
    children: [
        { id: 'brew-install', label: 'INSTALL', value: 'install', color: C.GREEN, children: [
             { id: 'grp-brew-pkg', label: 'FORMULAE', value: '', color: C.PURPLE, children: PACKAGES_COMMON }
        ]},
        { id: 'brew-cask', label: 'CASK', value: 'cask', color: C.CYAN },
        { id: 'brew-update', label: 'UPDATE', value: 'update', color: C.GREEN },
    ]
};

// Windows Context
const WINGET_MENU: MenuOption = {
    id: 'root-winget',
    label: 'WINGET',
    value: 'winget',
    color: C.BLUE,
    children: [
        { id: 'win-install', label: 'INSTALL', value: 'install', color: C.GREEN },
        { id: 'win-search', label: 'SEARCH', value: 'search', color: C.CYAN },
    ]
};

const POWERSHELL_MENU: MenuOption = {
    id: 'root-ps',
    label: 'POWERSHELL',
    value: 'powershell',
    color: C.BLUE
};

/**
 * Generates the command tree dynamically based on the provided SystemContext.
 * This allows the menu to show 'apt' for Ubuntu but 'brew' for MacOS.
 * 
 * @param context The current system state (OS, user)
 * @returns An array of top-level MenuOptions valid for that OS.
 */
export const getDynamicTree = (context: SystemContext): MenuOption[] => {
    const osSpecific: MenuOption[] = [];

    if (context.os === 'ubuntu') {
        osSpecific.push({
            id: 'root-sudo',
            label: 'SUDO',
            value: 'sudo',
            color: C.RED,
            children: [APT_MENU, SNAP_MENU] // Ubuntu gets Apt and Snap
        });
    } else if (context.os === 'macos') {
        osSpecific.push(BREW_MENU); // Mac gets Brew
        osSpecific.push({ id: 'root-sudo', label: 'SUDO', value: 'sudo', color: C.RED }); // Generic sudo
    } else if (context.os === 'windows') {
        osSpecific.push(WINGET_MENU);
        osSpecific.push(POWERSHELL_MENU);
    }

    return [
        ...osSpecific,
        GIT_MENU,
        DOCKER_MENU,
        ...COMMON_COMMANDS,
        // Hidden command to switch OS for demo purposes
        { id: 'sys-switch', label: 'SWITCH OS', value: 'switch-os', color: C.GRAY, children: [
            { id: 'sw-ubuntu', label: 'UBUNTU', value: 'ubuntu', color: C.ORANGE },
            { id: 'sw-macos', label: 'MACOS', value: 'macos', color: C.GRAY },
            { id: 'sw-win', label: 'WINDOWS', value: 'windows', color: C.BLUE },
        ]}
    ];
};

/**
 * Helper to flatten grouping nodes (those with value='').
 * Grouping nodes are used for visual organization but don't represent a command themselves.
 * 
 * @param nodes The list of nodes to check.
 * @returns A flattened list where grouping containers are replaced by their children.
 */
const expandGroups = (nodes: MenuOption[]): MenuOption[] => {
    let expanded: MenuOption[] = [];
    for (const node of nodes) {
        if (node.value === '' && node.children) {
            expanded.push(...expandGroups(node.children));
        } else {
            expanded.push(node);
        }
    }
    return expanded;
};

/**
 * The core suggestion engine.
 * Traverses the dynamic command tree based on the user's current input string.
 * 
 * @param input The current text in the terminal input.
 * @param context The system context (OS) to determine which tree to traverse.
 * @returns A list of MenuOptions that are valid next steps.
 */
export const getCommandSuggestions = (input: string, context: SystemContext): MenuOption[] => {
    const tree = getDynamicTree(context);
    
    const parts = input.trim().split(/\s+/);
    const cleanInput = input.trim();
    
    // 1. Root Level Check (Empty input or typing first word)
    if (cleanInput === '' || (parts.length === 1 && !input.endsWith(' '))) {
        const expandedRoot = expandGroups(tree);
        const keyword = parts[0] || '';
        return expandedRoot.filter(opt => opt.value.startsWith(keyword));
    }

    // 2. Tree Traversal
    // Walk down the tree matching each word in the input parts
    let currentNode: MenuOption[] = tree;
    
    const isTypingNewWord = input.endsWith(' ');
    // If we have a trailing space, we are looking for children of the last match.
    // If no trailing space, we are still completing the last word.
    const traversableParts = isTypingNewWord ? parts : parts.slice(0, -1);
    
    for (const token of traversableParts) {
        const expandedCurrent = expandGroups(currentNode);
        const match = expandedCurrent.find(opt => opt.value === token);
        
        if (match && match.children) {
            currentNode = match.children;
        } else if (match && !match.children) {
            return []; // Reached a leaf node (end of command chain)
        } else if (!match) {
            return []; // Input deviates from valid tree
        }
    }

    // 3. Final Filtering
    // Return the children of the current node, filtered by what the user has started typing
    const expandedContext = expandGroups(currentNode);
    if (!isTypingNewWord) {
        const keyword = parts[parts.length - 1];
        return expandedContext.filter(opt => opt.value.startsWith(keyword));
    }

    return expandedContext;
};