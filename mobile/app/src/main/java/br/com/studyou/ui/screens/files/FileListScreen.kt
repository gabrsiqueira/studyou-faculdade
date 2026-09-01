package br.com.studyou.ui.screens.files

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.studyou.domain.model.FileStatus
import br.com.studyou.domain.model.StudyFile
import br.com.studyou.ui.components.LabelChip
import br.com.studyou.ui.di.LocalAppContainer
import br.com.studyou.ui.navigation.Screen
import br.com.studyou.ui.navigation.StudYouBottomNavBar
import br.com.studyou.ui.theme.StudyouTheme

@Composable
fun FileListRoute(
    currentRoute: String?,
    onNavigateBottomNav: (Screen) -> Unit,
    onUploadClick: () -> Unit,
    onFileClick: (StudyFile) -> Unit
) {
    val container = LocalAppContainer.current
    val viewModel: FileListViewModel = viewModel(
        factory = viewModelFactory {
            initializer { FileListViewModel(container.fileRepository, container.userRepository) }
        }
    )
    val uiState by viewModel.uiState.collectAsState()

    FileListScreen(
        uiState = uiState,
        currentRoute = currentRoute,
        onNavigateBottomNav = onNavigateBottomNav,
        onUploadClick = onUploadClick,
        onFileClick = onFileClick
    )
}

@Composable
fun FileListScreen(
    uiState: FileListUiState,
    currentRoute: String?,
    onNavigateBottomNav: (Screen) -> Unit,
    onUploadClick: () -> Unit,
    onFileClick: (StudyFile) -> Unit
) {
    Scaffold(
        bottomBar = { StudYouBottomNavBar(currentRoute, onNavigateBottomNav) },
        floatingActionButton = {
            FloatingActionButton(onClick = onUploadClick) {
                Icon(Icons.Filled.Add, contentDescription = "Enviar arquivo")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Olá, ${uiState.userName}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Vamos continuar aprendendo?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                    Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = uiState.userName.firstOrNull()?.uppercase() ?: "?",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Text(
                    text = "Seus arquivos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${uiState.files.size} enviados",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (uiState.files.isEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize().padding(32.dp)
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "Nenhum arquivo enviado ainda",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Toque no botão + para enviar seu primeiro material de estudo.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }
            } else {
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 20.dp,
                        vertical = 4.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.files, key = { it.id }) { file ->
                        FileRow(file = file, onClick = { if (file.status == FileStatus.COMPLETED) onFileClick(file) })
                    }
                }
            }
        }
    }
}

@Composable
private fun FileRow(file: StudyFile, onClick: () -> Unit) {
    val style = fileStatusStyle(file.status)
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Surface(shape = RoundedCornerShape(12.dp), color = style.containerColor) {
                Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                    if (file.status == FileStatus.ANALYZING || file.status == FileStatus.GENERATING_GAMES) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = style.contentColor,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Filled.Description, contentDescription = null, tint = style.contentColor)
                    }
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(text = file.name, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(
                    text = file.statusDetail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            LabelChip(text = style.label, containerColor = style.containerColor, contentColor = style.contentColor)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FileListScreenPreview() {
    StudyouTheme {
        FileListScreen(
            uiState = FileListUiState(
                userName = "Fred",
                files = listOf(
                    StudyFile("1", "Sistema Solar – Resumo.pdf", FileStatus.COMPLETED, "Enviado hoje, 09:12"),
                    StudyFile("2", "Fotossíntese – Slides.pptx", FileStatus.GENERATING_GAMES, "Gerando jogos..."),
                    StudyFile("3", "Revolução Francesa.docx", FileStatus.ANALYZING, "Analisando conteúdo..."),
                    StudyFile("4", "Guerra Fria – Anotações.pdf", FileStatus.COMPLETED, "Enviado há 3 dias")
                )
            ),
            currentRoute = Screen.Files.route,
            onNavigateBottomNav = {},
            onUploadClick = {},
            onFileClick = {}
        )
    }
}
