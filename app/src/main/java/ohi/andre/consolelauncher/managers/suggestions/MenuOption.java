package ohi.andre.consolelauncher.managers.suggestions;

import java.util.List;

public class MenuOption {
    public String id;
    public String label;
    public String value;
    public int color;
    public List<MenuOption> children;

    public MenuOption(String id, String label, String value, int color) {
        this.id = id;
        this.label = label;
        this.value = value;
        this.color = color;
        this.children = null;
    }

    public MenuOption(String id, String label, String value, int color, List<MenuOption> children) {
        this.id = id;
        this.label = label;
        this.value = value;
        this.color = color;
        this.children = children;
    }
}
