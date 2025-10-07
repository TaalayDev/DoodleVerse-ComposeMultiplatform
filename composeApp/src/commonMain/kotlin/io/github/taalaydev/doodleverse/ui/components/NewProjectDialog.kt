package io.github.taalaydev.doodleverse.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.composables.icons.lucide.ArrowDownUp
import com.composables.icons.lucide.ArrowRightLeft
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Pen
import doodleverse.composeapp.generated.resources.Res
import doodleverse.composeapp.generated.resources.cancel
import doodleverse.composeapp.generated.resources.create
import doodleverse.composeapp.generated.resources.edit_project
import doodleverse.composeapp.generated.resources.new_project
import doodleverse.composeapp.generated.resources.project_name
import doodleverse.composeapp.generated.resources.save
import doodleverse.composeapp.generated.resources.template
import io.github.taalaydev.doodleverse.data.models.ProjectModel
import io.github.taalaydev.doodleverse.ui.theme.ThemeManager
import io.github.taalaydev.doodleverse.ui.theme.rememberThemeManager
import org.jetbrains.compose.resources.stringResource

data class ProjectTemplate(
    val name: String,
    val aspectRationWidth: Float,
    val aspectRationHeight: Float,
    val aspectRationLabel: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewProjectDialog(
    themeManager: ThemeManager = rememberThemeManager(),
    onDismissRequest: () -> Unit,
    onConfirm: (String, Float, Float) -> Unit,
    properties: DialogProperties = DialogProperties(),
    showCancelButton: Boolean = true
) {
    val theme by themeManager.currentTheme.collectAsState()
    var projectName by remember { mutableStateOf("") }
    var width by remember { mutableStateOf(1080f) }
    var height by remember { mutableStateOf(1080f) }
    var selectedTemplateIndex by remember { mutableStateOf(0) }

    var templatesMenuExpanded by remember { mutableStateOf(false) }

    val templates = listOf(
        ProjectTemplate("Square", 1080f, 1080f, "(1:1)"),
        ProjectTemplate("16:9", 1920f, 1080f, "(16:9)"),
        ProjectTemplate("4:3", 1440f, 1080f, "(4:3)"),
        ProjectTemplate("A4", 1358f, 1920f, "(A4)"),
        ProjectTemplate("A3", 1358f, 1920f, "(A3)"),
        ProjectTemplate("A2", 1358f, 1920f, "(A2)"),
        ProjectTemplate("A1", 1358f, 1920f, "(A1)"),
        ProjectTemplate("Custom", 0f, 0f, ""),
    )

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties,
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f),
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(Res.string.new_project),
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = projectName,
                    onValueChange = { projectName = it },
                    label = { Text(stringResource(Res.string.project_name)) },
                    leadingIcon = { Icon(Icons.Default.Create, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                ExposedDropdownMenuBox(
                    expanded = templatesMenuExpanded,
                    onExpandedChange = {
                        templatesMenuExpanded = it
                    },
                ) {
                    OutlinedTextField(
                        value = templates[selectedTemplateIndex].name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(Res.string.template)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = false) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = templatesMenuExpanded,
                        onDismissRequest = {
                            templatesMenuExpanded = false
                        }
                    ) {
                        templates.forEachIndexed { index, template ->
                            DropdownMenuItem(
                                text = { Text("${template.name} ${template.aspectRationLabel}") },
                                onClick = {
                                    selectedTemplateIndex = index
                                    if (index != templates.lastIndex) {
                                        width = template.aspectRationWidth
                                        height = template.aspectRationHeight
                                    }

                                    templatesMenuExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (selectedTemplateIndex == templates.lastIndex) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = width.toString(),
                            onValueChange = { width = it.toFloatOrNull() ?: width },
                            label = { Text("Aspect Ratio Width") },
                            leadingIcon = { Icon(Lucide.ArrowRightLeft, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = height.toString(),
                            onValueChange = { height = it.toFloatOrNull() ?: height },
                            label = { Text("Aspect Ratio Height") },
                            leadingIcon = { Icon(Lucide.ArrowDownUp, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (showCancelButton) {
                        TextButton(onClick = onDismissRequest) {
                            Text(stringResource(Res.string.cancel))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Button(
                        onClick = {
                            if (projectName.isNotBlank() && width > 0 && height > 0) {
                                onConfirm(projectName, width, height)
                            }
                        },
                        enabled = projectName.isNotBlank() && width > 0 && height > 0
                    ) {
                        Text(stringResource(Res.string.create))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProjectDialog(
    project: ProjectModel,
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit,
    properties: DialogProperties = DialogProperties(),
) {
    var projectName by remember { mutableStateOf(project.name) }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties,
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = stringResource(Res.string.edit_project),
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = projectName,
                    onValueChange = { projectName = it },
                    label = { Text(stringResource(Res.string.project_name)) },
                    leadingIcon = { Icon(Lucide.Pen, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text(stringResource(Res.string.cancel))
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { onConfirm(projectName) },
                        enabled = projectName.isNotBlank() && projectName != project.name
                    ) {
                        Text(stringResource(Res.string.save))
                    }
                }
            }
        }
    }
}