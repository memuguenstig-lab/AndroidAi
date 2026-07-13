import re

with open('app/src/main/java/com/example/viewmodel/GenerativeAiViewModel.kt', 'r') as f:
    content = f.read()

# I will find callGemini function and replace the buildJsonObject part correctly.
import re

def fix():
    with open('app/src/main/java/com/example/viewmodel/GenerativeAiViewModel.kt', 'r') as f:
        text = f.read()
    
    start_str = "schema = buildJsonObject {"
    end_str = "        val response = RetrofitClient.service.generateContent(settings.geminiApiKey, request)"
    
    if start_str in text and end_str in text:
        start_idx = text.find(start_str)
        end_idx = text.find(end_str)
        
        replacement = """schema = buildJsonObject {
                            put("type", "OBJECT")
                            putJsonObject("properties") {
                                putJsonObject("reasoning") {
                                    put("type", "STRING")
                                    put("description", "Explanation of the changes")
                                }
                                putJsonObject("changes") {
                                    put("type", "ARRAY")
                                    putJsonObject("items") {
                                        put("type", "OBJECT")
                                        putJsonObject("properties") {
                                            putJsonObject("filePath") {
                                                put("type", "STRING")
                                            }
                                            putJsonObject("newContent") {
                                                put("type", "STRING")
                                            }
                                        }
                                        putJsonArray("required") { add(JsonPrimitive("filePath")); add(JsonPrimitive("newContent")) }
                                    }
                                }
                                putJsonObject("actions") {
                                    put("type", "ARRAY")
                                    putJsonObject("items") {
                                        put("type", "OBJECT")
                                        putJsonObject("properties") {
                                            putJsonObject("actionType") {
                                                put("type", "STRING")
                                                put("description", "Action: 'create_file', 'delete_file', 'shell_command', 'web_search'")
                                            }
                                            putJsonObject("parameters") {
                                                put("type", "OBJECT")
                                            }
                                        }
                                        putJsonArray("required") { add(JsonPrimitive("actionType")); add(JsonPrimitive("parameters")) }
                                    }
                                }
                            }
                            putJsonArray("required") { add(JsonPrimitive("reasoning")); add(JsonPrimitive("changes")); add(JsonPrimitive("actions")) }
                        }
                    )
                )
            )
        )
"""
        
        new_text = text[:start_idx] + replacement + text[end_idx:]
        with open('app/src/main/java/com/example/viewmodel/GenerativeAiViewModel.kt', 'w') as f:
            f.write(new_text)

fix()
