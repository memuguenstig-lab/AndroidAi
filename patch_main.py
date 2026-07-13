import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

# Update applyChanges call
content = content.replace('viewModel.applyChanges(context, s.response.changes)', 'viewModel.applyChanges(context, s.response)')

# Show actions in ProposedChangesScreen
old_ui = '''        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            items(state.response.changes) { change ->'''
            
new_ui = '''        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            if (state.response.actions.isNotEmpty()) {
                item {
                    Text("Actions to execute", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                items(state.response.actions) { action ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "Action: ${action.actionType}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(text = action.parameters.toString(), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
            items(state.response.changes) { change ->'''
            
content = content.replace(old_ui, new_ui)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
