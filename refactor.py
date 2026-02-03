import os
import shutil

OLD_PACKAGE = "ohi.andre.consolelauncher"
NEW_PACKAGE = "com.hereliesaz.hg2gui"
OLD_PATH = OLD_PACKAGE.replace(".", "/")
NEW_PATH = NEW_PACKAGE.replace(".", "/")

SRC_DIRS = [
    "app/src/main/java",
    "app/src/fdroid/java",
    "app/src/test/java",
    "app/src/androidTest/java"
]

def replace_in_file(file_path, old_str, new_str):
    if not os.path.exists(file_path):
        return
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        if old_str in content:
            new_content = content.replace(old_str, new_str)
            with open(file_path, 'w', encoding='utf-8') as f:
                f.write(new_content)
            print(f"Updated content in {file_path}")
    except Exception as e:
        print(f"Error updating {file_path}: {e}")

def move_package_dir(base_dir):
    old_full_path = os.path.join(base_dir, OLD_PATH)
    new_full_path = os.path.join(base_dir, NEW_PATH)
    
    if os.path.exists(old_full_path):
        print(f"Moving {old_full_path} to {new_full_path}")
        os.makedirs(new_full_path, exist_ok=True)
        
        for item in os.listdir(old_full_path):
            shutil.move(os.path.join(old_full_path, item), new_full_path)
        
        # Clean up empty old dirs
        current = old_full_path
        while current != base_dir:
            try:
                if not os.listdir(current):
                    os.rmdir(current)
                    print(f"Removed empty dir {current}")
                    current = os.path.dirname(current)
                else:
                    break
            except OSError:
                break

def process_file_content(root_dir):
    for root, dirs, files in os.walk(root_dir):
        for file in files:
            if file.endswith(('.java', '.xml', '.gradle', '.kt')):
                file_path = os.path.join(root, file)
                replace_in_file(file_path, OLD_PACKAGE, NEW_PACKAGE)

def update_strings():
    strings_path = "app/src/main/res/values/strings.xml"
    if os.path.exists(strings_path):
        with open(strings_path, 'r', encoding='utf-8') as f:
            content = f.read()
        if '<string name="app_name">T-UI</string>' in content:
            content = content.replace('<string name="app_name">T-UI</string>', '<string name="app_name">HG2Gui</string>')
            with open(strings_path, 'w', encoding='utf-8') as f:
                f.write(content)
            print("Updated strings.xml app_name")

def update_settings_gradle():
    settings_path = "settings.gradle"
    if os.path.exists(settings_path):
        with open(settings_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # Check if rootProject.name is defined
        if "rootProject.name" in content:
             lines = content.splitlines()
             with open(settings_path, 'w', encoding='utf-8') as f:
                 for line in lines:
                     if "rootProject.name" in line:
                         f.write("rootProject.name = 'HG2Gui'\n")
                     else:
                         f.write(line + "\n")
        else:
            with open(settings_path, 'a', encoding='utf-8') as f:
                f.write("\nrootProject.name = 'HG2Gui'\n")
        print("Updated settings.gradle")

def fix_build_gradle():
    build_gradle = "app/build.gradle"
    if os.path.exists(build_gradle):
        with open(build_gradle, 'r', encoding='utf-8') as f:
            lines = f.readlines()
        
        with open(build_gradle, 'w', encoding='utf-8') as f:
            for line in lines:
                if "outputFileName =" in line:
                    f.write("// " + line) 
                else:
                    f.write(line)
        print("Updated app/build.gradle to fix output path")

def main():
    # 1. Move directories
    for src_dir in SRC_DIRS:
        if os.path.exists(src_dir):
            move_package_dir(src_dir)
    
    # 2. Update file content
    process_file_content("app/src")
    if os.path.exists("app/build.gradle"):
        replace_in_file("app/build.gradle", OLD_PACKAGE, NEW_PACKAGE)
    if os.path.exists("settings.gradle"):
        replace_in_file("settings.gradle", OLD_PACKAGE, NEW_PACKAGE)

    # 3. Specific updates
    update_strings()
    update_settings_gradle()
    fix_build_gradle()

if __name__ == "__main__":
    main()
