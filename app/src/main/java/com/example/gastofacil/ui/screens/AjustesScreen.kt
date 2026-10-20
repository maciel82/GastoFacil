package com.example.gastofacil.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gastofacil.data.local.CategoryEntity
import com.example.gastofacil.data.local.util.BackupManager
import com.example.gastofacil.data.remote.DriveApiService
import com.example.gastofacil.ui.theme.*
import com.example.gastofacil.ui.viewmodel.MainViewModel
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AjustesScreen(
    viewModel: MainViewModel,
    onNavigateToHome: () -> Unit,
    onNavigateToLancamentos: () -> Unit,
    onNavigateToExtrato: () -> Unit,
    onNavigateToMenu: () -> Unit
) {
    var darkModeEnabled by remember { mutableStateOf(false) }
    var notificationsEnabled by remember { mutableStateOf(true) }
    var showCategoryDialog by remember { mutableStateOf(false) }

    var showBudgetDialog by remember { mutableStateOf(false) }
    var budgetInputText by remember { mutableStateOf(if (viewModel.monthlyBudgetLimit > 0) viewModel.monthlyBudgetLimit.toString() else "") }
    var applyAllMonthsSetting by remember { mutableStateOf(viewModel.applyLimitToAllMonths) }

    var showBackupModal by remember { mutableStateOf(false) }
    var isProcessingBackup by remember { mutableStateOf(false) }
    var backupStatusMessage by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Configuração do Google Sign-In solicitando permissão do Google Drive
    val webClientId = "336149224462-apgli772p3vq5p52p62mssk69bv3jtkl.apps.googleusercontent.com"

    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .requestScopes(Scope("https://www.googleapis.com/auth/drive.file"))
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    var pendingDriveAction by remember { mutableStateOf<String?>(null) } // "UPLOAD" ou "DOWNLOAD"

    val performDriveAction: (GoogleSignInAccount, String) -> Unit = { account, action ->
        coroutineScope.launch {
            isProcessingBackup = true
            backupStatusMessage = "A obter credenciais do Google Drive..."
            try {
                val token = withContext(Dispatchers.IO) {
                    GoogleAuthUtil.getToken(
                        context,
                        account.account!!,
                        "oauth2:https://www.googleapis.com/auth/drive.file"
                    )
                }
                val driveApi = DriveApiService(token)
                if (action == "UPLOAD") {
                    backupStatusMessage = "A enviar backup para o Google Drive..."
                    val json = viewModel.exportAllDataJson()
                    val success = driveApi.uploadBackup(json)
                    isProcessingBackup = false
                    backupStatusMessage = if (success) {
                        "Backup guardado no Google Drive com sucesso!"
                    } else {
                        "Falha ao enviar ficheiro para o Google Drive."
                    }
                } else if (action == "DOWNLOAD") {
                    backupStatusMessage = "A procurar backup no Google Drive..."
                    val json = driveApi.downloadBackup()
                    if (!json.isNullOrBlank()) {
                        viewModel.importBackupData(json) { success ->
                            isProcessingBackup = false
                            backupStatusMessage = if (success) {
                                "Restauro do Google Drive concluído com sucesso!"
                            } else {
                                "Erro ao processar o conteúdo do backup descarregado."
                            }
                        }
                    } else {
                        isProcessingBackup = false
                        backupStatusMessage = "Nenhum ficheiro de backup encontrado no Drive."
                    }
                }
            } catch (e: UserRecoverableAuthException) {
                e.printStackTrace()
                isProcessingBackup = false
                pendingDriveAction = action
            } catch (e: Exception) {
                e.printStackTrace()
                isProcessingBackup = false
                backupStatusMessage = "Erro de autenticação com o Google Drive: ${e.message}"
            }
        }
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val action = pendingDriveAction
            if (account != null && action != null) {
                performDriveAction(account, action)
            }
        } catch (e: ApiException) {
            e.printStackTrace()
            isProcessingBackup = false
            backupStatusMessage = "Falha no login com o Google (Código: ${e.statusCode})."
        }
    }

    fun handleGoogleDriveUpload() {
        coroutineScope.launch {
            val account = GoogleSignIn.getLastSignedInAccount(context)
            if (account != null && account.account != null) {
                performDriveAction(account, "UPLOAD")
            } else {
                pendingDriveAction = "UPLOAD"
                googleSignInLauncher.launch(googleSignInClient.signInIntent)
            }
        }
    }

    fun handleGoogleDriveDownload() {
        coroutineScope.launch {
            val account = GoogleSignIn.getLastSignedInAccount(context)
            if (account != null && account.account != null) {
                performDriveAction(account, "DOWNLOAD")
            } else {
                pendingDriveAction = "DOWNLOAD"
                googleSignInLauncher.launch(googleSignInClient.signInIntent)
            }
        }
    }

    // Launcher para criar arquivo local (Exportar)
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                isProcessingBackup = true
                val json = viewModel.exportAllDataJson()
                val success = BackupManager.writeToUri(context, uri, json)
                isProcessingBackup = false
                backupStatusMessage = if (success) "Backup local salvo com sucesso!" else "Erro ao salvar backup local."
            }
        }
    }

    // Launcher para abrir arquivo local (Importar)
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val json = BackupManager.readFromUri(context, uri)
            if (json != null) {
                isProcessingBackup = true
                viewModel.importBackupData(json) { success ->
                    isProcessingBackup = false
                    backupStatusMessage = if (success) "Dados importados com sucesso!" else "Erro ao ler formato do arquivo de backup."
                }
            }
        }
    }

    Scaffold(
        containerColor = SageBackground,
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White
                ),
                title = { Text("Ajustes & Preferências", fontWeight = FontWeight.Bold) }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = SageCardBackground) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Início") },
                    label = { Text("Início") },
                    selected = false,
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = DarkForestGreen,
                        unselectedTextColor = DarkForestGreen
                    ),
                    onClick = onNavigateToHome
                )
                NavigationBarItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Histórico") },
                    label = { Text("Histórico") },
                    selected = false,
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = DarkForestGreen,
                        unselectedTextColor = DarkForestGreen
                    ),
                    onClick = onNavigateToLancamentos
                )
                NavigationBarItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = "Extrato") },
                    label = { Text("Extrato") },
                    selected = false,
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = DarkForestGreen,
                        unselectedTextColor = DarkForestGreen
                    ),
                    onClick = onNavigateToExtrato
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Menu, contentDescription = "Menu") },
                    label = { Text("Menu") },
                    selected = false,
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = DarkForestGreen,
                        unselectedTextColor = DarkForestGreen
                    ),
                    onClick = onNavigateToMenu
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Seção: Aparência e Preferências
            Text(
                text = "PREFERÊNCIAS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray
            )

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    AjusteSwitchItem(
                        icon = Icons.Default.DarkMode,
                        title = "Modo Escuro",
                        subtitle = "Alternar para o tema escuro",
                        checked = darkModeEnabled,
                        onCheckedChange = { darkModeEnabled = it }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = SageBackground)
                    AjusteSwitchItem(
                        icon = Icons.Default.Notifications,
                        title = "Notificações",
                        subtitle = "Lembretes de faturas e vencimentos",
                        checked = notificationsEnabled,
                        onCheckedChange = { notificationsEnabled = it }
                    )
                }
            }

            // Seção: Dados e Segurança
            Text(
                text = "DADOS E SEGURANÇA",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray
            )

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    AjusteActionItem(
                        icon = Icons.Default.Category,
                        title = "Gerenciar Categorias",
                        subtitle = "Visualizar e gerenciar categorias",
                        onClick = { showCategoryDialog = true }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = SageBackground)
                    AjusteActionItem(
                        icon = Icons.Default.Warning,
                        title = "Limite de Gastos Mensais",
                        subtitle = if (viewModel.monthlyBudgetLimit > 0) "Limite: R$ ${viewModel.monthlyBudgetLimit}" else "Nenhum limite configurado",
                        onClick = {
                            budgetInputText = if (viewModel.monthlyBudgetLimit > 0) viewModel.monthlyBudgetLimit.toString() else ""
                            applyAllMonthsSetting = viewModel.applyLimitToAllMonths
                            showBudgetDialog = true
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = SageBackground)
                    AjusteActionItem(
                        icon = Icons.Default.Backup,
                        title = "Backup & Restauração",
                        subtitle = "Exportar ou importar cópia de segurança (Local / Nuvem)",
                        onClick = { showBackupModal = true }
                    )
                }
            }

            // Seção: Sobre
            Text(
                text = "SOBRE O APLICATIVO",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray
            )

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "GastoFácil v1.0.0",
                        fontWeight = FontWeight.Bold,
                        color = DarkForestGreen
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Gestão financeira pessoal com interface Verde Jade",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        if (showCategoryDialog) {
            val categoriesList by viewModel.categories.collectAsState(initial = emptyList())
            var newCategoryName by remember { mutableStateOf("") }

            var categoryToDelete by remember { mutableStateOf<CategoryEntity?>(null) }
            var showConfirmDeleteCategoryDialog by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { showCategoryDialog = false },
                title = { Text("Gerenciar Categorias", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 350.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = newCategoryName,
                                onValueChange = { newCategoryName = it },
                                label = { Text("Nova Categoria") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    if (newCategoryName.isNotBlank()) {
                                        viewModel.addCategory(newCategoryName)
                                        newCategoryName = ""
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddCircle,
                                    contentDescription = "Adicionar",
                                    tint = DarkForestGreen,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = SageBackground)

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                        ) {
                            items(categoriesList, key = { it.id }) { cat ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp, horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = cat.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )

                                    IconButton(
                                        onClick = {
                                            categoryToDelete = cat
                                            showConfirmDeleteCategoryDialog = true
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Excluir Categoria",
                                            tint = ExpenseRedText
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showCategoryDialog = false }) {
                        Text("Concluir")
                    }
                }
            )

            if (showConfirmDeleteCategoryDialog && categoryToDelete != null) {
                AlertDialog(
                    onDismissRequest = {
                        showConfirmDeleteCategoryDialog = false
                        categoryToDelete = null
                    },
                    title = { Text("Excluir Categoria") },
                    text = {
                        Text("Deseja realmente excluir a categoria \"${categoryToDelete?.name}\"?")
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                categoryToDelete?.let { viewModel.deleteCategory(it) }
                                showConfirmDeleteCategoryDialog = false
                                categoryToDelete = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Excluir")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showConfirmDeleteCategoryDialog = false
                                categoryToDelete = null
                            }
                        ) {
                            Text("Cancelar")
                        }
                    }
                )
            }
        }

        // Diálogo de configuração de Limite de Gastos
        if (showBudgetDialog) {
            AlertDialog(
                onDismissRequest = { showBudgetDialog = false },
                title = { Text("Configurar Limite de Gastos", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Defina o valor máximo que deseja gastar no mês:", fontSize = 13.sp, color = Color.Gray)

                        OutlinedTextField(
                            value = budgetInputText,
                            onValueChange = { budgetInputText = it },
                            label = { Text("Valor Limite (R$)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Aplicar a todos os meses?", fontSize = 14.sp)
                            Switch(
                                checked = applyAllMonthsSetting,
                                onCheckedChange = { applyAllMonthsSetting = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = WarmGoldFab)
                            )
                        }
                        Text(
                            text = if (applyAllMonthsSetting) "O limite valerá para qualquer mês selecionado." else "O limite valerá apenas para o mês atual do calendário.",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val limit = budgetInputText.replace(",", ".").toDoubleOrNull() ?: 0.0
                            viewModel.updateBudgetSettings(limit, applyAllMonthsSetting)
                            showBudgetDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkForestGreen)
                    ) {
                        Text("Salvar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBudgetDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        // Diálogo principal de opções de Backup
        if (showBackupModal) {
            AlertDialog(
                onDismissRequest = { if (!isProcessingBackup) showBackupModal = false },
                title = { Text("Backup & Restauração", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Escolha a opção de exportação ou restauração dos seus lançamentos e categorias:", fontSize = 13.sp, color = Color.Gray)

                        if (isProcessingBackup) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(color = DarkForestGreen, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("A processar comunicação...", fontSize = 13.sp, color = DarkForestGreen, fontWeight = FontWeight.Medium)
                            }
                        }

                        HorizontalDivider(color = SageBackground)

                        Text("LOCAL (DISPOSITIVO)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkForestGreen)

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                enabled = !isProcessingBackup,
                                onClick = {
                                    val filename = "GastoFacil_Backup_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}.json"
                                    createDocumentLauncher.launch(filename)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Exportar", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                enabled = !isProcessingBackup,
                                onClick = {
                                    openDocumentLauncher.launch(arrayOf("application/json", "*/*"))
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Importar", fontSize = 12.sp)
                            }
                        }

                        HorizontalDivider(color = SageBackground)

                        Text("NUVEM (GOOGLE DRIVE)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkForestGreen)

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                enabled = !isProcessingBackup,
                                onClick = {
                                    handleGoogleDriveUpload()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DarkForestGreen),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Salvar Drive", fontSize = 12.sp)
                            }

                            Button(
                                enabled = !isProcessingBackup,
                                onClick = {
                                    handleGoogleDriveDownload()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DarkForestGreen),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Baixar Drive", fontSize = 12.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        enabled = !isProcessingBackup,
                        onClick = { showBackupModal = false }
                    ) {
                        Text("Fechar")
                    }
                }
            )
        }

        // Diálogo de mensagem de status (Sucesso / Erro)
        if (backupStatusMessage != null) {
            AlertDialog(
                onDismissRequest = { backupStatusMessage = null },
                title = { Text("Aviso de Backup") },
                text = { Text(backupStatusMessage!!) },
                confirmButton = {
                    Button(onClick = { backupStatusMessage = null }) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

@Composable
fun AjusteSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = DarkForestGreen)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(text = subtitle, fontSize = 12.sp, color = Color.Gray)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = WarmGoldFab)
        )
    }
}

@Composable
fun AjusteActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = DarkForestGreen)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(text = subtitle, fontSize = 12.sp, color = Color.Gray)
            }
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
        }
    }
}
