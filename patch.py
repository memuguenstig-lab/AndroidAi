import re

with open('app/src/main/java/com/example/viewmodel/GenerativeAiViewModel.kt', 'r') as f:
    content = f.read()

# Add isMediaFile
media_func = '''    private fun isMediaFile(name: String): Boolean {
        val ext = name.substringAfterLast('.', "").lowercase()
        val mediaExtensions = setOf(
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "heic", "svg", "ico",
            "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "3gp",
            "mp3", "wav", "ogg", "m4a", "flac", "aac", "opus", "amr",
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "zip", "rar", "tar", "gz", "7z",
            "crypt14", "crypt15" // WhatsApp backups
        )
        return mediaExtensions.contains(ext)
    }

    private fun isTextual(name: String): Boolean {'''
content = content.replace('    private fun isTextual(name: String): Boolean {', media_func)

# skip media in traverseFolderDirect
old_direct = '''            } else if (file.isFile) {
                try {
                    var content = ""'''
new_direct = '''            } else if (file.isFile) {
                if (isMediaFile(name)) continue
                try {
                    var content = ""'''
content = content.replace(old_direct, new_direct)

# skip media in traverseFolder
old_traverse = '''            } else if (file.isFile) {
                try {
                    var content = ""'''
new_traverse = '''            } else if (file.isFile) {
                if (isMediaFile(name)) return@forEach
                try {
                    var content = ""'''
content = content.replace(old_traverse, new_traverse)

with open('app/src/main/java/com/example/viewmodel/GenerativeAiViewModel.kt', 'w') as f:
    f.write(content)
