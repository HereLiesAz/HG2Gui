path = 'app/src/main/java/ohi/andre/consolelauncher/managers/suggestions/CommandMenu.java'
with open(path, 'r') as f:
    content = f.read()

# Remove the specific block
block_to_remove = """
        // Switch OS (Hidden/Utility)
        List<MenuOption> switchChildren = new ArrayList<>();
        switchChildren.add(new MenuOption("sw-ubuntu", "UBUNTU", "ubuntu", C_ORANGE));
        switchChildren.add(new MenuOption("sw-macos", "MACOS", "macos", C_GRAY));
        switchChildren.add(new MenuOption("sw-win", "WINDOWS", "windows", C_BLUE));
        tree.add(new MenuOption("sys-switch", "SWITCH OS", "switch-os", C_GRAY, switchChildren));"""

if block_to_remove in content:
    content = content.replace(block_to_remove, "")
else:
    # Try with slightly different whitespace if copy-paste failed
    pass

with open(path, 'w') as f:
    f.write(content)
