import sys

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

content = content.replace("var localUrl by remember { mutableStateOf(settingsManager.localBaseUrl) }", "var localPath by remember { mutableStateOf(settingsManager.localModelPath) }")
content = content.replace("var localModel by remember { mutableStateOf(settingsManager.localModelName) }", "")

old_fields = """                OutlinedTextField(
                    value = localUrl,
                    onValueChange = { localUrl = it },
                    label = { Text("Local Base URL") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = localModel,
                    onValueChange = { localModel = it },
                    label = { Text("Local Model Name") },
                    singleLine = true
                )"""

new_fields = """                OutlinedTextField(
                    value = localPath,
                    onValueChange = { localPath = it },
                    label = { Text("Local Model Path (.task)") },
                    singleLine = false
                )"""
content = content.replace(old_fields, new_fields)

old_save = """                    settingsManager.localBaseUrl = localUrl
                    settingsManager.localModelName = localModel"""

new_save = """                    settingsManager.localModelPath = localPath"""
content = content.replace(old_save, new_save)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
