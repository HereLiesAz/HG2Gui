package ohi.andre.consolelauncher.managers.suggestions;

import java.util.List;

public class MenuOption {
    public String id;
    public String label;
    public String value;
    public int colorRes;
    public List<MenuOption> children;

    public MenuOption(String id, String label, String value, int colorRes) {
        this.id = id;
        this.label = label;
        this.value = value;
        this.colorRes = colorRes;
        this.children = null;
    }

    public MenuOption(String id, String label, String value, int colorRes, List<MenuOption> children) {
        this.id = id;
        this.label = label;
        this.value = value;
        this.colorRes = colorRes;
        this.children = children;
    }
}
