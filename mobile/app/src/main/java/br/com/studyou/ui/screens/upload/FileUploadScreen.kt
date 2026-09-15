package br.com.studyou.ui.screens.upload

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.studyou.ui.components.BackTopBar
import br.com.studyou.ui.components.PrimaryButton
import br.com.studyou.ui.di.LocalAppContainer
import br.com.studyou.ui.theme.StudyouTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun FileUploadRoute(onBack: () -> Unit, onUploaded: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: FileUploadViewModel = viewModel(
        factory = viewModelFactory {
            initializer { FileUploadViewModel(container.fileRepository) }
        }
    )
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {}
    )
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val pickDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch {
                val picked = readPickedFile(context, it)
                if (picked != null) {
                    viewModel.onFileSelected(picked.name, picked.sizeBytes, picked.mimeType, picked.content)
                }
            }
        }
    }

    FileUploadScreen(
        uiState = uiState,
        onBack = onBack,
        onPickFileClick = {
            pickDocumentLauncher.launch(
                arrayOf(
                    "application/pdf",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                    "image/*"
                )
            )
        },
        onRemoveSelectedFile = viewModel::onRemoveSelectedFile,
        onUploadClick = { viewModel.upload(onUploaded) }
    )
}

private data class PickedFile(val name: String, val sizeBytes: Long, val mimeType: String, val content: ByteArray)

private suspend fun readPickedFile(context: Context, uri: Uri): PickedFile? = withContext(Dispatchers.IO) {
    val cursor = context.contentResolver.query(uri, null, null, null, null) ?: return@withContext null
    val (name, sizeBytes) = cursor.use {
        if (!it.moveToFirst()) return@withContext null
        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
        val name = if (nameIndex >= 0) it.getString(nameIndex) else uri.lastPathSegment.orEmpty()
        val size = if (sizeIndex >= 0) it.getLong(sizeIndex) else 0L
        name to size
    }
    val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@withContext null
    PickedFile(name, sizeBytes, mimeType, bytes)
}

@Composable
fun FileUploadScreen(
    uiState: FileUploadUiState,
    onBack: () -> Unit,
    onPickFileClick: () -> Unit,
    onRemoveSelectedFile: () -> Unit,
    onUploadClick: () -> Unit
) {
    Scaffold(topBar = { BackTopBar("Enviar arquivo", onBack) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
            Text(
                text = "Envie um material de estudo e o StudYou cria jogos personalizados a partir do conteúdo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onPickFileClick)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp))
                    .padding(vertical = 32.dp)
            ) {
                Icon(
                    Icons.Filled.UploadFile,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.height(40.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "PDF, DOCX, PPTX ou imagem — até 20 MB",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            uiState.selectedFile?.let { file ->
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Icon(Icons.Filled.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
                            Text(file.name, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            Text(
                                text = formatFileSize(file.sizeBytes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = onRemoveSelectedFile, enabled = !uiState.isUploading) {
                            Icon(Icons.Filled.Close, contentDescription = "Remover")
                        }
                    }
                }
            }

            uiState.errorMessage?.let { message ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.weight(1f))

            if (uiState.isUploading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp).padding(end = 10.dp))
                    Text("Enviando arquivo...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            PrimaryButton(
                text = "Enviar arquivo",
                onClick = onUploadClick,
                enabled = uiState.selectedFile != null && !uiState.isUploading
            )
        }
    }
}

private fun formatFileSize(sizeBytes: Long): String {
    val mb = sizeBytes / (1024.0 * 1024.0)
    return "%.1f MB".format(mb)
}

@Preview(showBackground = true)
@Composable
private fun FileUploadScreenPreview() {
    StudyouTheme {
        FileUploadScreen(
            uiState = FileUploadUiState(
                selectedFile = SelectedFile("Ecossistemas – Cap.4.pdf", 3_566_000, "application/pdf", ByteArray(0))
            ),
            onBack = {},
            onPickFileClick = {},
            onRemoveSelectedFile = {},
            onUploadClick = {}
        )
    }
}
