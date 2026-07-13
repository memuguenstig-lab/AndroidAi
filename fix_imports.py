import sys

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

# Remove duplicate imports
content = content.replace("import androidx.activity.result.contract.ActivityResultContracts\n", "", 1)

# Remove duplicate context val definition (it's declared twice inside MainScreen)
content = content.replace("val context = LocalContext.current\n    val context = LocalContext.current", "val context = LocalContext.current")

# In MainScreen, the first definition of context is at the top, let's see where it is
lines = content.split('\n')
new_lines = []
in_main_screen = False
context_declared = False
for line in lines:
    if "fun MainScreen" in line:
        in_main_screen = True
    
    if in_main_screen and "val context = LocalContext.current" in line:
        if not context_declared:
            context_declared = True
            new_lines.append(line)
        else:
            continue
    else:
        new_lines.append(line)

content = '\n'.join(new_lines)

# Fix compilation errors about settings dialog context and context() vs context parameter
content = content.replace("context = context,", "context = context,")

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)

