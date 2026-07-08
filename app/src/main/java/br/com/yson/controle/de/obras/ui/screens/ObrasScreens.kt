package br.com.yson.controle.de.obras.ui.screens

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.Manifest
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.data.model.*
import com.example.ui.viewmodel.ObrasViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.DecimalFormat
import java.util.Locale

// --- UTILS ---
class PhoneVisualTransformation : VisualTransformation {
    override fun filter(text: androidx.compose.ui.text.AnnotatedString): TransformedText {
        val raw = text.text.filter { it.isDigit() }
        val trimmed = if (raw.length > 11) raw.substring(0, 11) else raw
        
        val out = StringBuilder()
        val isCell = trimmed.length == 11
        
        for (i in trimmed.indices) {
            if (i == 0) out.append("(")
            out.append(trimmed[i])
            if (i == 1) out.append(") ")
            if (isCell) {
                if (i == 6) out.append("-")
            } else {
                if (i == 5) out.append("-")
            }
        }
        
        val offsetTranslator = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                val transformedOffset = when {
                    offset <= 1 -> offset + 1 // only "(" added
                    offset <= if (isCell) 7 else 6 -> offset + 3 // "(" and ") " added
                    else -> offset + 4 // "(", ") ", and "-" added
                }
                return transformedOffset.coerceAtMost(out.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 0) return 0
                var digitCount = 0
                for (i in 0 until offset.coerceAtMost(out.length)) {
                    if (out[i].isDigit()) {
                        digitCount++
                    }
                }
                return digitCount.coerceAtMost(trimmed.length)
            }
        }
        
        return TransformedText(
            text = androidx.compose.ui.text.AnnotatedString(out.toString()),
            offsetMapping = offsetTranslator
        )
    }
}

fun formatCurrency(value: Double): String {
    val df = DecimalFormat.getCurrencyInstance(Locale("pt", "BR"))
    return df.format(value)
}

fun formatPhoneString(input: String): String {
    val clean = input.filter { it.isDigit() }
    val length = clean.length
    if (length == 0) return ""
    
    val sb = java.lang.StringBuilder()
    val truncated = if (length > 11) clean.substring(0, 11) else clean
    val size = truncated.length
    
    if (size > 0) {
        sb.append("(")
        val dddSize = if (size >= 2) 2 else size
        sb.append(truncated.substring(0, dddSize))
        if (size >= 2) {
            sb.append(") ")
        }
    }
    if (size > 2) {
        val remaining = truncated.substring(2)
        if (remaining.length <= 4) {
            sb.append(remaining)
        } else if (remaining.length == 5) {
            sb.append(remaining)
        } else if (remaining.length <= 8) {
            sb.append(remaining.substring(0, 4))
            sb.append("-")
            sb.append(remaining.substring(4))
        } else {
            sb.append(remaining.substring(0, 5))
            sb.append("-")
            sb.append(remaining.substring(5))
        }
    }
    return sb.toString()
}

fun formatCurrencyString(digits: String): String {
    val clean = digits.filter { it.isDigit() }
    if (clean.isEmpty()) return "0,00"
    val parsed = clean.toLongOrNull() ?: 0L
    val doubleValue = parsed / 100.0
    val formatter = java.text.DecimalFormat("#,##0.00", java.text.DecimalFormatSymbols(java.util.Locale("pt", "BR")))
    return formatter.format(doubleValue)
}

fun parseCurrencyStringToDouble(formatted: String): Double {
    val clean = formatted.filter { it.isDigit() }
    if (clean.isEmpty()) return 0.0
    return (clean.toDoubleOrNull() ?: 0.0) / 100.0
}

fun formatDoubleToCurrencyString(value: Double): String {
    val formatter = java.text.DecimalFormat("#,##0.00", java.text.DecimalFormatSymbols(java.util.Locale("pt", "BR")))
    return formatter.format(value)
}

// --- COLOR THEME HELPERS ---
object ThemeColors {
    val WarningAmber = Color(0xFFFFD700) // High-contrast Gold
    val DarkSlateBackground = Color(0xFF000000) // Pure Black background
    val MutedSlateCard = Color(0xFF151922) // High-contrast Slate dark card
    val ConstructionGold = Color(0xFFFFD700) // High-contrast Gold
    val BorderMuted = Color(0xFF4B5563) // Strong contrast border (slate grey)
    val SoftRedBg = Color(0xFFDC2626) // High contrast red
    val SoftRedText = Color(0xFFFFFFFF) // White text for high contrast on red background
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObrasTopAppBar(
    title: String,
    onBackClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        },
        navigationIcon = {
            if (onBackClick != null) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        ),
        modifier = Modifier.border(width = 1.dp, color = ThemeColors.BorderMuted.copy(alpha = 0.5f))
    )
}

// --- MAIN / HOME SCREEN ---
@Composable
fun MainScreen(
    navController: NavController,
    viewModel: ObrasViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val fornecedores by viewModel.allFornecedores.collectAsStateWithLifecycle()
    val prestadores by viewModel.allPrestadores.collectAsStateWithLifecycle()
    val obras by viewModel.allObras.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { ObrasTopAppBar(title = "Controle de Obras") }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Flat Clean Minimal Header exactly matching the design theme
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Gestão de Obras",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 24.sp,
                        letterSpacing = (-0.5).sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Perfil",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Controle global de fornecedores e prestadores",
                    fontSize = 14.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "Cadastros Globais",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 4.dp)
            )

            // Buttons: FORNECEDORES, PRESTADORES, OBRAS
            ElevatedDashboardButton(
                text = "FORNECEDORES",
                icon = Icons.Default.Person,
                countText = "${fornecedores.size} cadastrados",
                colorAccent = MaterialTheme.colorScheme.primary,
                testTag = "fornecedores_nav_button",
                onClick = { navController.navigate("fornecedores") }
            )

            ElevatedDashboardButton(
                text = "PRESTADORES",
                icon = Icons.Default.Settings,
                countText = "${prestadores.size} cadastrados",
                colorAccent = ThemeColors.WarningAmber,
                testTag = "prestadores_nav_button",
                onClick = { navController.navigate("prestadores") }
            )

            ElevatedDashboardButton(
                text = "OBRAS",
                icon = Icons.Default.Home,
                countText = "${obras.size} cadastradas",
                colorAccent = Color(0xFF26A69A),
                testTag = "obras_nav_button",
                onClick = { navController.navigate("obras") }
            )

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = ThemeColors.BorderMuted, thickness = 1.dp)
            
            // CSV CSV Backup Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                border = BoxBorder(ThemeColors.BorderMuted)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Exportar/Importar",
                            tint = ThemeColors.ConstructionGold
                        )
                        Text(
                            text = "Backup de Cadastro (CSV)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "Exporte ou importe o cadastro global de fornecedores e prestadores para fazer backup ou compartilhar com outro dispositivo.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                navController.navigate("csv_backup")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_csv_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Exportar CSV", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                navController.navigate("csv_backup")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("import_csv_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ThemeColors.WarningAmber
                            )
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Importar CSV", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ElevatedDashboardButton(
    text: String,
    icon: ImageVector,
    countText: String,
    colorAccent: Color,
    testTag: String,
    onClick: () -> Unit
) {
    val isHighlight = text.uppercase().contains("OBRAS") || text.uppercase().contains("RELATÓRIO")
    val containerColor = if (isHighlight) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.secondary
    }
    val contentColor = if (isHighlight) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSecondary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isHighlight) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = text,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = text,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = contentColor
                )
                Text(
                    text = countText,
                    fontSize = 12.sp,
                    color = contentColor.copy(alpha = 0.7f)
                )
            }
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Abrir",
                tint = contentColor.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

fun BoxBorder(color: Color) = androidx.compose.foundation.BorderStroke(1.dp, color)


// --- FORNECEDORES (GLOBAL) SCREEN ---
@Composable
fun FornecedoresScreen(
    navController: NavController,
    viewModel: ObrasViewModel
) {
    val fornecedores by viewModel.allFornecedores.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            ObrasTopAppBar(
                title = "Fornecedores",
                onBackClick = { navController.popBackStack() }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = ThemeColors.WarningAmber,
                contentColor = Color.Black,
                modifier = Modifier.testTag("add_fornecedor_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar Fornecedor")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (fornecedores.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Person,
                    title = "Nenhum fornecedor cadastrado",
                    description = "Toque no botão + para cadastrar seu primeiro fornecedor global."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(fornecedores) { supplier ->
                        SupplierCard(
                            supplier = supplier,
                            onDelete = { viewModel.deleteFornecedor(it) }
                        )
                    }
                }
            }

            if (showAddDialog) {
                AddSupplierDialog(
                    onDismiss = { showAddDialog = false },
                    onConfirm = { name, phone, notes ->
                        viewModel.insertFornecedor(name, phone, notes)
                        showAddDialog = false
                    }
                )
            }
        }
    }
}

@Composable
fun SupplierCard(supplier: Fornecedor, onDelete: (Fornecedor) -> Unit) {
    var confirmDelete by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = ThemeColors.MutedSlateCard
        ),
        border = BoxBorder(ThemeColors.BorderMuted)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Number identifier
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "#${supplier.id}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = supplier.nome,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Excluir Fornecedor",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.Phone, contentDescription = "Telefone", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                Text(text = supplier.telefone, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            if (supplier.observacoes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Info, contentDescription = "Obs", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                        Text(
                            text = supplier.observacoes,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Excluir Fornecedor?") },
            text = { Text("Isso removerá o fornecedor ${supplier.nome} globalmente. Esta ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(supplier)
                        confirmDelete = false
                    }
                ) { Text("Excluir", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
fun AddSupplierDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    var showPermissionDialog by remember { mutableStateOf(false) }

    val contactPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val projection = arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                )
                context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                        val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        val contactName = cursor.getString(nameIndex) ?: ""
                        val contactNumber = cursor.getString(numberIndex) ?: ""
                        
                        val cleanNumber = contactNumber.filter { it.isDigit() }
                        
                        name = contactName
                        phone = cleanNumber
                        
                        focusRequester.requestFocus()
                    }
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
            contactPickerLauncher.launch(intent)
        } else {
            showPermissionDialog = true
        }
    }

    fun onImportContactsClick() {
        val permissionStatus = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
        if (permissionStatus == PackageManager.PERMISSION_GRANTED) {
            val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
            contactPickerLauncher.launch(intent)
        } else {
            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Permissão Necessária") },
            text = { Text("O aplicativo precisa de permissão de acesso aos contatos para importar os dados. Vá até as configurações do aplicativo para permitir o acesso.") },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionDialog = false
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }) {
                    Text("Abrir Configurações")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ThemeColors.MutedSlateCard),
            border = BoxBorder(ThemeColors.BorderMuted)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Cadastrar Fornecedor",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Button(
                    onClick = { onImportContactsClick() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ThemeColors.ConstructionGold,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Importar dos Contatos", fontWeight = FontWeight.Bold)
                }
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome do Fornecedor") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("supplier_input_name")
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { newValue ->
                        val clean = newValue.filter { it.isDigit() }
                        if (clean.length <= 11) {
                            phone = clean
                        }
                    },
                    label = { Text("Contato Telefônico (Ex: (62) 99834-3435)") },
                    visualTransformation = PhoneVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("supplier_input_phone")
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Observações (texto longo)") },
                    minLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .testTag("supplier_input_notes")
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onConfirm(name, formatPhoneString(phone), notes)
                            }
                        },
                        enabled = name.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("supplier_save_button")
                    ) {
                        Text("Salvar")
                    }
                }
            }
        }
    }
}


// --- PRESTADORES (GLOBAL) SCREEN ---
@Composable
fun PrestadoresScreen(
    navController: NavController,
    viewModel: ObrasViewModel
) {
    val prestadores by viewModel.allPrestadores.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            ObrasTopAppBar(
                title = "Prestadores de Serviço",
                onBackClick = { navController.popBackStack() }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = ThemeColors.WarningAmber,
                contentColor = Color.Black,
                modifier = Modifier.testTag("add_prestador_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar Prestador")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (prestadores.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Person,
                    title = "Nenhum prestador cadastrado",
                    description = "Toque no botão + para cadastrar seu primeiro prestador de serviço global."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(prestadores) { provider ->
                        ProviderCard(
                            provider = provider,
                            onDelete = { viewModel.deletePrestador(it) }
                        )
                    }
                }
            }

            if (showAddDialog) {
                AddProviderDialog(
                    onDismiss = { showAddDialog = false },
                    onConfirm = { name, phone, notes ->
                        viewModel.insertPrestador(name, phone, notes)
                        showAddDialog = false
                    }
                )
            }
        }
    }
}

@Composable
fun ProviderCard(provider: Prestador, onDelete: (Prestador) -> Unit) {
    var confirmDelete by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = ThemeColors.MutedSlateCard
        ),
        border = BoxBorder(ThemeColors.BorderMuted)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(ThemeColors.WarningAmber.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "#${provider.id}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = ThemeColors.WarningAmber
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = provider.nome,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Excluir Prestador",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.Phone, contentDescription = "Telefone", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                Text(text = provider.telefone, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            if (provider.observacoes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Info, contentDescription = "Obs", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                        Text(
                            text = provider.observacoes,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Excluir Prestador?") },
            text = { Text("Isso removerá o prestador ${provider.nome} globalmente. Esta ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(provider)
                        confirmDelete = false
                    }
                ) { Text("Excluir", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
fun AddProviderDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    var showPermissionDialog by remember { mutableStateOf(false) }

    val contactPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val projection = arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                )
                context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                        val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        val contactName = cursor.getString(nameIndex) ?: ""
                        val contactNumber = cursor.getString(numberIndex) ?: ""
                        
                        val cleanNumber = contactNumber.filter { it.isDigit() }
                        
                        name = contactName
                        phone = cleanNumber
                        
                        focusRequester.requestFocus()
                    }
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
            contactPickerLauncher.launch(intent)
        } else {
            showPermissionDialog = true
        }
    }

    fun onImportContactsClick() {
        val permissionStatus = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
        if (permissionStatus == PackageManager.PERMISSION_GRANTED) {
            val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
            contactPickerLauncher.launch(intent)
        } else {
            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Permissão Necessária") },
            text = { Text("O aplicativo precisa de permissão de acesso aos contatos para importar os dados. Vá até as configurações do aplicativo para permitir o acesso.") },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionDialog = false
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }) {
                    Text("Abrir Configurações")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ThemeColors.MutedSlateCard),
            border = BoxBorder(ThemeColors.BorderMuted)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Cadastrar Prestador",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Button(
                    onClick = { onImportContactsClick() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ThemeColors.ConstructionGold,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Importar dos Contatos", fontWeight = FontWeight.Bold)
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome do Prestador") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("provider_input_name")
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { newValue ->
                        val clean = newValue.filter { it.isDigit() }
                        if (clean.length <= 11) {
                            phone = clean
                        }
                    },
                    label = { Text("Contato Telefônico (Ex: (62) 99834-3435)") },
                    visualTransformation = PhoneVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("provider_input_phone")
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Observações (texto longo)") },
                    minLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .testTag("provider_input_notes")
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onConfirm(name, formatPhoneString(phone), notes)
                            }
                        },
                        enabled = name.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("provider_save_button")
                    ) {
                        Text("Salvar")
                    }
                }
            }
        }
    }
}


// --- OBRAS CARD & LIST SCREEN ---
@Composable
fun ObrasScreen(
    navController: NavController,
    viewModel: ObrasViewModel
) {
    val obras by viewModel.allObras.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            ObrasTopAppBar(
                title = "Obras Cadastradas",
                onBackClick = { navController.popBackStack() }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = ThemeColors.WarningAmber,
                contentColor = Color.Black,
                modifier = Modifier.testTag("add_obra_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar Obra")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (obras.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Home,
                    title = "Nenhuma obra cadastrada",
                    description = "Gerencie o financeiro de cada canteiro de obras. Cadastre a primeira tocando no botão +."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(obras) { project ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    navController.navigate("lancamento_obras/${project.id}")
                                }
                                .testTag("obra_item_${project.id}"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = ThemeColors.MutedSlateCard
                            ),
                            border = BoxBorder(ThemeColors.BorderMuted)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .background(Color(0xFF26A69A).copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Home,
                                        contentDescription = null,
                                        tint = Color(0xFF26A69A),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = project.nome,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.Place, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                                        Text(
                                            text = project.endereco,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { viewModel.deleteObra(project) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Excluir obra",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Abrir lançamentos",
                                    tint = Color.LightGray.copy(alpha = 0.6f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (showAddDialog) {
                AddObraDialog(
                    onDismiss = { showAddDialog = false },
                    onConfirm = { name, address ->
                        viewModel.insertObra(name, address)
                        showAddDialog = false
                    }
                )
            }
        }
    }
}

@Composable
fun AddObraDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ThemeColors.MutedSlateCard),
            border = BoxBorder(ThemeColors.BorderMuted)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Cadastrar Nova Obra",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome da Obra") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("obra_input_name")
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Endereço da Obra") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("obra_input_address")
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank() && address.isNotBlank()) {
                                onConfirm(name, address)
                            }
                        },
                        enabled = name.isNotBlank() && address.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("obra_save_button")
                    ) {
                        Text("Salvar")
                    }
                }
            }
        }
    }
}


// --- SCREEN: LANÇAMENTO OBRAS ---
@Composable
fun LancamentoObrasScreen(
    obraId: Int,
    navController: NavController,
    viewModel: ObrasViewModel
) {
    val coroutineScope = rememberCoroutineScope()
    var currentObra by remember { mutableStateOf<Obra?>(null) }
    
    LaunchedEffect(obraId) {
        currentObra = viewModel.allObras.value.find { it.id == obraId }
    }

    Scaffold(
        topBar = {
            ObrasTopAppBar(
                title = "Lançamentos da Obra",
                onBackClick = { navController.popBackStack() }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Construction detailed header card
            currentObra?.let { obra ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = ThemeColors.MutedSlateCard
                    ),
                    border = BoxBorder(ThemeColors.BorderMuted)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = null,
                                tint = Color(0xFF26A69A),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = obra.nome,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "📍 ${obra.endereco}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Action Selection: LOTE DA OBRA, BUROCRACIA DA OBRA, FORNECEDORES DA OBRA, PRESTADORES DA OBRA, RELATÓRIO
            ElevatedDashboardButton(
                text = "LOTE",
                icon = Icons.Default.Place,
                countText = "Lançamento de lote",
                colorAccent = Color(0xFF9C27B0), // Purple accent
                testTag = "lote_obra_nav_button",
                onClick = { navController.navigate("lote_obra/$obraId") }
            )

            ElevatedDashboardButton(
                text = "BUROCRACIA",
                icon = Icons.Default.Info,
                countText = "Lançamento de burocracia (Taxas, Alvarás, etc)",
                colorAccent = Color.Gray,
                testTag = "burocracia_obra_nav_button",
                onClick = { navController.navigate("burocracia_obra/$obraId") }
            )

            ElevatedDashboardButton(
                text = "FORNECEDORES DA OBRA",
                icon = Icons.Default.Person,
                countText = "Lançamentos e pagamentos de fornecedores",
                colorAccent = MaterialTheme.colorScheme.primary,
                testTag = "fornecedores_obra_nav_button",
                onClick = { navController.navigate("fornecedores_obra/$obraId") }
            )

            ElevatedDashboardButton(
                text = "PRESTADORES DA OBRA",
                icon = Icons.Default.Settings,
                countText = "Lançamentos e pagamentos de prestadores",
                colorAccent = ThemeColors.WarningAmber,
                testTag = "prestadores_obra_nav_button",
                onClick = { navController.navigate("prestadores_obra/$obraId") }
            )

            ElevatedDashboardButton(
                text = "RELATÓRIO",
                icon = Icons.Default.List,
                countText = "Resumo financeiro e fechamento da obra",
                colorAccent = Color(0xFF4CAF50),
                testTag = "relatorio_obra_nav_button",
                onClick = { navController.navigate("relatorio_obra/$obraId") }
            )
        }
    }
}


// --- SCREEN: LOTE DA OBRA ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoteObraScreen(
    obraId: Int,
    navController: NavController,
    viewModel: ObrasViewModel
) {
    val context = LocalContext.current
    var currentObra by remember { mutableStateOf<Obra?>(null) }
    val launchedLotes by viewModel.getLotesForObra(obraId).collectAsStateWithLifecycle(emptyList())

    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(obraId) {
        val db = com.example.data.database.AppDatabase.getDatabase(context)
        currentObra = db.appDao().getObraById(obraId)
    }

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Lote da Obra", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text(currentObra?.nome ?: "Carregando...", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFF9C27B0),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar Lote")
            }
        }
    ) { paddingValues ->
        if (launchedLotes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text(
                    "Nenhum lote registrado para esta obra.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(launchedLotes, key = { it.id }) { lote ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFF9C27B0))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = lote.nome.uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { viewModel.deleteLoteObra(lote) }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "Excluir Lote", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Tamanho", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                                    val formattedSize = java.text.NumberFormat.getNumberInstance(java.util.Locale("pt", "BR")).format(lote.tamanhoM2)
                                    Text("$formattedSize m²", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Valor do Lote", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                                    Text(
                                        formatCurrency(lote.valor),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = ThemeColors.WarningAmber
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddLoteObraDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { nome, tamanhoM2, valor ->
                viewModel.insertLoteObra(obraId, nome, tamanhoM2, valor)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun AddLoteObraDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Int, Double) -> Unit
) {
    var nomeStr by remember { mutableStateOf("") }
    var tamanhoM2Str by remember { mutableStateOf("") }
    var valorStr by remember { mutableStateOf("0,00") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Lançar Lote",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = nomeStr,
                    onValueChange = { nomeStr = it },
                    label = { Text("Nome do Lote") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = tamanhoM2Str,
                    onValueChange = { newValue ->
                        val clean = newValue.filter { it.isDigit() }
                        // format to brazil number like 1.000.000
                        val formatted = if (clean.isEmpty()) "" else {
                            val parsed = clean.toLongOrNull() ?: 0L
                            java.text.NumberFormat.getNumberInstance(java.util.Locale("pt", "BR")).format(parsed)
                        }
                        tamanhoM2Str = formatted
                    },
                    label = { Text("Tamanho (m²)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = valorStr,
                    onValueChange = { valorStr = formatCurrencyString(it) },
                    label = { Text("Valor (R$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("CANCELAR", color = MaterialTheme.colorScheme.error)
                    }
                    Button(
                        onClick = {
                            val cleanTamanho = tamanhoM2Str.filter { it.isDigit() }
                            val tamanhoM2Val = cleanTamanho.toIntOrNull() ?: 0
                            val valorVal = parseCurrencyStringToDouble(valorStr)
                            if (nomeStr.isNotBlank() && tamanhoM2Val >= 1 && valorVal > 0.0) {
                                onConfirm(nomeStr, tamanhoM2Val, valorVal)
                            }
                        },
                        enabled = nomeStr.isNotBlank() && (tamanhoM2Str.filter { it.isDigit() }.toIntOrNull() ?: 0) >= 1 && parseCurrencyStringToDouble(valorStr) > 0.0,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("SALVAR", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// --- SCREEN: BUROCRACIA DA OBRA ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BurocraciaObraScreen(
    obraId: Int,
    navController: NavController,
    viewModel: ObrasViewModel
) {
    val context = LocalContext.current
    var currentObra by remember { mutableStateOf<Obra?>(null) }
    val launchedBurocracias by viewModel.getBurocraciasForObra(obraId).collectAsStateWithLifecycle(emptyList())

    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(obraId) {
        val db = com.example.data.database.AppDatabase.getDatabase(context)
        currentObra = db.appDao().getObraById(obraId)
    }

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Burocracia da Obra", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text(currentObra?.nome ?: "Carregando...", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = ThemeColors.WarningAmber,
                contentColor = Color.Black
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar Burocracia")
            }
        }
    ) { paddingValues ->
        if (launchedBurocracias.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text(
                    "Nenhuma burocracia registrada para esta obra.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(launchedBurocracias, key = { it.id }) { burocracia ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = Color.Gray)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = burocracia.nome.uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { viewModel.deleteBurocraciaObra(burocracia) }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "Excluir Burocracia", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Data Controle", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                                    Text(burocracia.dataControle, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Valor", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                                    Text(
                                        formatCurrency(burocracia.valorPago),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = ThemeColors.WarningAmber
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddBurocraciaObraDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { nome, valorPago, dataControle ->
                viewModel.insertBurocraciaObra(obraId, nome, valorPago, dataControle)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun AddBurocraciaObraDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String) -> Unit
) {
    var nomeStr by remember { mutableStateOf("") }
    var valorStr by remember { mutableStateOf("0,00") }
    val formatter = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
    var dataStr by remember { mutableStateOf(formatter.format(java.util.Date())) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Lançar Burocracia",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = nomeStr,
                    onValueChange = { nomeStr = it },
                    label = { Text("Nome da Burocracia") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = valorStr,
                    onValueChange = { valorStr = formatCurrencyString(it) },
                    label = { Text("Valor Pago (R$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = dataStr,
                    onValueChange = { dataStr = it },
                    label = { Text("Data Controle") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("CANCELAR", color = MaterialTheme.colorScheme.error)
                    }
                    Button(
                        onClick = {
                            val valorVal = parseCurrencyStringToDouble(valorStr)
                            if (nomeStr.isNotBlank() && valorVal > 0.0) {
                                onConfirm(nomeStr, valorVal, dataStr.ifBlank { formatter.format(java.util.Date()) })
                            }
                        },
                        enabled = nomeStr.isNotBlank() && parseCurrencyStringToDouble(valorStr) > 0.0,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("SALVAR", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// --- SCREEN: FORNECEDORES DA OBRA ---
@Composable
fun FornecedoresObraScreen(
    obraId: Int,
    navController: NavController,
    viewModel: ObrasViewModel
) {
    val context = LocalContext.current
    var currentObra by remember { mutableStateOf<Obra?>(null) }
    val globalFornecedores by viewModel.allFornecedores.collectAsStateWithLifecycle()
    val launchedFornecedores by viewModel.getFornecedoresForObra(obraId).collectAsStateWithLifecycle(emptyList())
    
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedItemForPaymentsDetails by remember { mutableStateOf<FornecedorObraWithDetails?>(null) }

    LaunchedEffect(obraId) {
        currentObra = viewModel.allObras.value.find { it.id == obraId }
    }

    Scaffold(
        topBar = {
            ObrasTopAppBar(
                title = "Fornecedores da Obra",
                onBackClick = { navController.popBackStack() }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (globalFornecedores.isEmpty()) {
                        Toast.makeText(context, "Cadastre fornecedores no cadastro global antes!", Toast.LENGTH_LONG).show()
                    } else {
                        showAddDialog = true
                    }
                },
                containerColor = ThemeColors.WarningAmber,
                contentColor = Color.Black,
                modifier = Modifier.testTag("add_fornecedor_obra_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Lançar Fornecedor")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header of Obras
                currentObra?.let { obra ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Text(text = obra.nome, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp)
                            Text(text = "Lançamentos de materiais e serviços contratados", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                if (launchedFornecedores.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Default.Person,
                        title = "Nenhum fornecedor lançado",
                        description = "Aloque fornecedores globais a esta obra tocando no botão +."
                    )
                } else {
                    val groupedFornecedores = launchedFornecedores
                        .groupBy { it.fornecedorNome }
                        .toSortedMap()

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        groupedFornecedores.forEach { (nome, itemsForSupplier) ->
                            item(key = "header_$nome") {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = nome,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }
                            items(itemsForSupplier, key = { it.id }) { item ->
                                LaunchedFornecedorCard(
                                    item = item,
                                    onClick = { selectedItemForPaymentsDetails = item },
                                    onDelete = { viewModel.deleteFornecedorObra(item) }
                                )
                            }
                        }
                    }
                }
            }

            if (showAddDialog) {
                AddFornecedorObraDialog(
                    globalList = globalFornecedores,
                    onDismiss = { showAddDialog = false },
                    getHistoricalTotalPaid = { supplierId ->
                        viewModel.getHistoricalTotalPaidForFornecedor(supplierId)
                    },
                    onConfirm = { providerId, contractedVal, paidVal, date ->
                        viewModel.launchFornecedorObra(obraId, providerId, contractedVal, paidVal, date)
                        showAddDialog = false
                    }
                )
            }

            if (selectedItemForPaymentsDetails != null) {
                PaymentsHistoryDialog(
                    titleName = selectedItemForPaymentsDetails!!.fornecedorNome,
                    contractedValueText = "Valor Total: ${formatCurrency(selectedItemForPaymentsDetails!!.valorServico)}",
                    fornecedorObraId = selectedItemForPaymentsDetails!!.id,
                    viewModel = viewModel,
                    onDismiss = { selectedItemForPaymentsDetails = null },
                    onAddNewPayment = { valAmt, dateTxt ->
                        viewModel.launchPagamentoFornecedor(selectedItemForPaymentsDetails!!, valAmt, dateTxt)
                        // Trigger a temporary update to prevent waiting for Flow emissions
                        selectedItemForPaymentsDetails = selectedItemForPaymentsDetails!!.copy(
                            valorPago = selectedItemForPaymentsDetails!!.valorPago + valAmt,
                            dataPagamento = dateTxt
                        )
                    },
                    onDeletePayment = { pagamento ->
                        viewModel.deletePagamentoFornecedor(pagamento, selectedItemForPaymentsDetails!!)
                    }
                )
            }
        }
    }
}

@Composable
fun LaunchedFornecedorCard(
    item: FornecedorObraWithDetails,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("launched_supplier_card_${item.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = ThemeColors.MutedSlateCard
        ),
        border = BoxBorder(ThemeColors.BorderMuted)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.fornecedorNome,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(text = "📞 ${item.fornecedorTelefone}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Excluir lançamento",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Expandir pagamentos",
                    tint = ThemeColors.WarningAmber,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = ThemeColors.BorderMuted, thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(text = "Valor Total", fontSize = 13.sp, color = Color.LightGray)
                    Text(text = formatCurrency(item.valorServico), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Total Pago", fontSize = 13.sp, color = Color.LightGray)
                    Text(
                        text = formatCurrency(item.valorPago),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (item.valorPago >= item.valorServico) Color(0xFF4CAF50) else ThemeColors.WarningAmber
                    )
                }
            }

            if (item.valorPago > 0.0) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .padding(vertical = 6.dp, horizontal = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Último Pagamento:", fontSize = 13.sp, color = Color.LightGray)
                        Text(text = "Pago em: ${item.dataPagamento}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun AddFornecedorObraDialog(
    globalList: List<Fornecedor>,
    onDismiss: () -> Unit,
    getHistoricalTotalPaid: suspend (Int) -> Double,
    onConfirm: (Int, Double, Double, String) -> Unit
) {
    var selectedFornecedor by remember { mutableStateOf<Fornecedor?>(null) }
    var expanded by remember { mutableStateOf(false) }

    var valorServicoStr by remember { mutableStateOf("0,00") }
    var valorPagoStr by remember { mutableStateOf("0,00") }
    var dateString by remember { mutableStateOf("") }

    LaunchedEffect(selectedFornecedor) {
        selectedFornecedor?.let { supplier ->
            val totalPaid = getHistoricalTotalPaid(supplier.id)
            valorServicoStr = if (totalPaid > 0.0) formatDoubleToCurrencyString(totalPaid) else "0,00"
        }
    }

    // Prefill date with contemporary structure
    LaunchedEffect(Unit) {
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        dateString = sdf.format(java.util.Date())
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ThemeColors.MutedSlateCard),
            border = BoxBorder(ThemeColors.BorderMuted)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Lançar Fornecedor na Obra",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Beautiful Custom Selector (Droplist)
                Text(
                    text = "Selecione o Fornecedor (Cadastro Global)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expanded = !expanded },
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        border = BoxBorder(ThemeColors.BorderMuted)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = selectedFornecedor?.nome ?: "Selecione",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                if (selectedFornecedor != null && selectedFornecedor!!.telefone.isNotBlank()) {
                                    Text(
                                        text = "📞 ${selectedFornecedor!!.telefone}",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            Icon(
                                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.ArrowDropDown,
                                contentDescription = if (expanded) "Fechar menu" else "Abrir menu",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, ThemeColors.BorderMuted, RoundedCornerShape(8.dp))
                    ) {
                        globalList.forEach { supplier ->
                            DropdownMenuItem(
                                text = {
                                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                        Text(
                                            text = supplier.nome,
                                            fontWeight = if (supplier.id == selectedFornecedor?.id) FontWeight.Bold else FontWeight.Medium,
                                            color = if (supplier.id == selectedFornecedor?.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                            fontSize = 15.sp
                                        )
                                        if (supplier.telefone.isNotBlank()) {
                                            Text(
                                                text = "📞 ${supplier.telefone}",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 12.sp
                                            )
                                        }
                                        if (supplier.observacoes.isNotBlank()) {
                                            Text(
                                                text = supplier.observacoes,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    selectedFornecedor = supplier
                                    expanded = false
                                },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = valorServicoStr,
                    onValueChange = {},
                    label = { Text("Valor Total (Histórico)") },
                    readOnly = true,
                    enabled = false,
                    supportingText = {
                        Text("Calculado automaticamente a partir do histórico de pagamentos")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("forn_laun_valor_servico")
                )

                OutlinedTextField(
                    value = valorPagoStr,
                    onValueChange = { valorPagoStr = formatCurrencyString(it) },
                    label = { Text("Valor Pago") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("forn_laun_valor_pago")
                )

                OutlinedTextField(
                    value = dateString,
                    onValueChange = { dateString = it },
                    label = { Text("Data de Pagamento (dd/mm/aaaa)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("forn_laun_data_pagamento")
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val fId = selectedFornecedor?.id
                            val serviceVal = parseCurrencyStringToDouble(valorServicoStr)
                            val paidVal = parseCurrencyStringToDouble(valorPagoStr)
                            if (fId != null && paidVal > 0.0) {
                                onConfirm(fId, serviceVal, paidVal, dateString.ifBlank { "-" })
                            }
                        },
                        enabled = selectedFornecedor != null && 
                                parseCurrencyStringToDouble(valorPagoStr) > 0.0,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("forn_laun_save_button")
                    ) {
                        Text("Salvar Lançamento")
                    }
                }
            }
        }
    }
}


// --- SCREEN: PRESTADORES DA OBRA ---
@Composable
fun PrestadoresObraScreen(
    obraId: Int,
    navController: NavController,
    viewModel: ObrasViewModel
) {
    val context = LocalContext.current
    var currentObra by remember { mutableStateOf<Obra?>(null) }
    val globalPrestadores by viewModel.allPrestadores.collectAsStateWithLifecycle()
    val launchedPrestadores by viewModel.getPrestadoresForObra(obraId).collectAsStateWithLifecycle(emptyList())
    
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedItemForPaymentsDetails by remember { mutableStateOf<PrestadorObraWithDetails?>(null) }

    LaunchedEffect(obraId) {
        currentObra = viewModel.allObras.value.find { it.id == obraId }
    }

    Scaffold(
        topBar = {
            ObrasTopAppBar(
                title = "Prestadores da Obra",
                onBackClick = { navController.popBackStack() }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (globalPrestadores.isEmpty()) {
                        Toast.makeText(context, "Cadastre prestadores no cadastro global antes!", Toast.LENGTH_LONG).show()
                    } else {
                        showAddDialog = true
                    }
                },
                containerColor = ThemeColors.WarningAmber,
                contentColor = Color.Black,
                modifier = Modifier.testTag("add_prestador_obra_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Lançar Prestador/Serviço")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                currentObra?.let { obra ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Text(text = obra.nome, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                            Text(text = "Relação de prestadores e consumo de materiais", fontSize = 12.sp, color = Color.LightGray)
                        }
                    }
                }

                if (launchedPrestadores.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Default.Person,
                        title = "Nenhum prestador lançado",
                        description = "Aloque prestadores globais a esta obra tocando no botão +."
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(launchedPrestadores) { item ->
                            LaunchedPrestadorCard(
                                item = item,
                                onClick = { selectedItemForPaymentsDetails = item },
                                onDelete = { viewModel.deletePrestadorObra(item) }
                            )
                        }
                    }
                }
            }

            if (showAddDialog) {
                AddPrestadorObraDialog(
                    globalList = globalPrestadores,
                    launchedList = launchedPrestadores,
                    onDismiss = { showAddDialog = false },
                    onConfirm = { providerId, contractedVal, paidVal, date, existingId ->
                        viewModel.launchPrestadorObra(obraId, providerId, contractedVal, paidVal, date, existingId)
                        showAddDialog = false
                    }
                )
            }

            if (selectedItemForPaymentsDetails != null) {
                PaymentsHistoryPrestadorDialog(
                    titleName = selectedItemForPaymentsDetails!!.prestadorNome,
                    initialContractedValue = selectedItemForPaymentsDetails!!.valorMaterial,
                    prestadorObraId = selectedItemForPaymentsDetails!!.id,
                    viewModel = viewModel,
                    onDismiss = { selectedItemForPaymentsDetails = null },
                    onAddNewPayment = { valAmt, dateTxt ->
                        viewModel.launchPagamentoPrestador(selectedItemForPaymentsDetails!!, valAmt, dateTxt)
                        // Trigger a temporary update to prevent waiting for Flow emissions
                        selectedItemForPaymentsDetails = selectedItemForPaymentsDetails!!.copy(
                            valorPago = selectedItemForPaymentsDetails!!.valorPago + valAmt,
                            dataPagamento = dateTxt
                        )
                    },
                    onUpdateContractValue = { newContractedVal ->
                        viewModel.updateValorContratadoPrestador(
                            prestadorObraId = selectedItemForPaymentsDetails!!.id,
                            obraId = selectedItemForPaymentsDetails!!.obraId,
                            prestadorId = selectedItemForPaymentsDetails!!.prestadorId,
                            nuevoValorContratado = newContractedVal,
                            dataPagamento = selectedItemForPaymentsDetails!!.dataPagamento
                        )
                        // Trigger a temporary update to prevent waiting for Flow emissions
                        selectedItemForPaymentsDetails = selectedItemForPaymentsDetails!!.copy(
                            valorMaterial = newContractedVal
                        )
                    },
                    onDeletePayment = { pagamento ->
                        viewModel.deletePagamentoPrestador(pagamento, selectedItemForPaymentsDetails!!)
                    }
                )
            }
        }
    }
}

@Composable
fun LaunchedPrestadorCard(
    item: PrestadorObraWithDetails,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("launched_provider_card_${item.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = ThemeColors.MutedSlateCard
        ),
        border = BoxBorder(ThemeColors.BorderMuted)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(ThemeColors.WarningAmber.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = ThemeColors.WarningAmber, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.prestadorNome,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                    Text(text = "📞 ${item.prestadorTelefone}", fontSize = 14.sp, color = Color.LightGray)
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Excluir lançamento",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Expandir pagamentos",
                    tint = ThemeColors.WarningAmber,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = ThemeColors.BorderMuted, thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(text = "Valor Contratado", fontSize = 13.sp, color = Color.LightGray)
                    Text(text = formatCurrency(item.valorMaterial), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Total Pago", fontSize = 13.sp, color = Color.LightGray)
                    Text(
                        text = formatCurrency(item.valorPago),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (item.valorPago >= item.valorMaterial) Color(0xFF4CAF50) else ThemeColors.WarningAmber
                    )
                }
            }

            if (item.valorPago > 0.0) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .padding(vertical = 6.dp, horizontal = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Último Pagamento:", fontSize = 13.sp, color = Color.LightGray)
                        Text(text = "Pago em: ${item.dataPagamento}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun AddPrestadorObraDialog(
    globalList: List<Prestador>,
    launchedList: List<PrestadorObraWithDetails>,
    onDismiss: () -> Unit,
    onConfirm: (Int, Double, Double, String, Int?) -> Unit
) {
    val availablePrestadores = remember(globalList, launchedList) {
        globalList.filter { p -> launchedList.none { it.prestadorId == p.id } }
    }
    
    var selectedPrestador by remember(availablePrestadores) { mutableStateOf<Prestador?>(null) }
    var expanded by remember { mutableStateOf(false) }

    var valorMaterialStr by remember { mutableStateOf("0,00") }
    var valorPagoStr by remember { mutableStateOf("0,00") }
    var dateString by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        dateString = sdf.format(java.util.Date())
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ThemeColors.MutedSlateCard),
            border = BoxBorder(ThemeColors.BorderMuted)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Lançar Prestador",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (availablePrestadores.isEmpty()) {
                    Text(
                        text = "Todos os prestadores cadastrados já estão lançados nesta obra.",
                        color = ThemeColors.WarningAmber,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                    )
                } else {
                    // Beautiful Custom Selector (Droplist)
                    Text(
                        text = "Selecione o Prestador (Cadastro Global)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expanded = !expanded },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            border = BoxBorder(ThemeColors.BorderMuted)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = selectedPrestador?.nome ?: "Selecione",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    if (selectedPrestador != null && selectedPrestador!!.telefone.isNotBlank()) {
                                        Text(
                                            text = "📞 ${selectedPrestador!!.telefone}",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.ArrowDropDown,
                                    contentDescription = if (expanded) "Fechar menu" else "Abrir menu",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, ThemeColors.BorderMuted, RoundedCornerShape(8.dp))
                        ) {
                            availablePrestadores.forEach { prestador ->
                                DropdownMenuItem(
                                    text = {
                                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                            Text(
                                                text = prestador.nome,
                                                fontWeight = if (prestador.id == selectedPrestador?.id) FontWeight.Bold else FontWeight.Medium,
                                                color = if (prestador.id == selectedPrestador?.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                fontSize = 15.sp
                                            )
                                            if (prestador.telefone.isNotBlank()) {
                                                Text(
                                                    text = "📞 ${prestador.telefone}",
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 12.sp
                                                )
                                            }
                                            if (prestador.observacoes.isNotBlank()) {
                                                Text(
                                                    text = prestador.observacoes,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                    fontSize = 11.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        selectedPrestador = prestador
                                        expanded = false
                                    },
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }

                    if (selectedPrestador != null) {
                        Surface(
                            color = Color(0xFF0F291E),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.border(1.dp, Color(0xFF10B981), RoundedCornerShape(4.dp))
                        ) {
                            Text(
                                text = "Novo prestador na obra",
                                color = Color(0xFF6EE7B7),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = valorMaterialStr,
                        onValueChange = { valorMaterialStr = formatCurrencyString(it) },
                        label = { Text("Valor Contratado") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("prest_laun_valor_material")
                    )

                    OutlinedTextField(
                        value = valorPagoStr,
                        onValueChange = { valorPagoStr = formatCurrencyString(it) },
                        label = { Text("Valor Pago Inicial (Opcional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("prest_laun_valor_pago")
                    )

                    OutlinedTextField(
                        value = dateString,
                        onValueChange = { dateString = it },
                        label = { Text("Data do Lançamento / Pagamento") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("prest_laun_data_pagamento")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val pId = selectedPrestador?.id
                            val materialVal = parseCurrencyStringToDouble(valorMaterialStr)
                            val paidVal = parseCurrencyStringToDouble(valorPagoStr)
                            if (pId != null && materialVal > 0.0) {
                                onConfirm(pId, materialVal, paidVal, dateString.ifBlank { "-" }, null)
                            }
                        },
                        enabled = selectedPrestador != null && parseCurrencyStringToDouble(valorMaterialStr) > 0.0,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("prest_laun_save_button")
                    ) {
                        Text("Salvar Lançamento")
                    }
                }
            }
        }
    }
}


// --- GENERIC HISTORY PAYMENTS FLOWS DIALOG — FORNECEDORES ---
@Composable
fun PaymentsHistoryDialog(
    titleName: String,
    contractedValueText: String,
    fornecedorObraId: Int,
    viewModel: ObrasViewModel,
    onDismiss: () -> Unit,
    onAddNewPayment: (Double, String) -> Unit,
    onDeletePayment: (PagamentoFornecedor) -> Unit
) {
    val paymentList by viewModel.getPagamentosForFornecedorObra(fornecedorObraId).collectAsStateWithLifecycle(emptyList())
    var showAddPaymentForm by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ThemeColors.MutedSlateCard),
            border = BoxBorder(ThemeColors.BorderMuted)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Name Tag
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = titleName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color.White
                        )
                        Text(text = contractedValueText, fontSize = 14.sp, color = ThemeColors.WarningAmber)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Fechar", tint = Color.LightGray)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Accumulative sum card
                val totalPaidSum = paymentList.sumOf { it.valor }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(8.dp),
                    border = BoxBorder(ThemeColors.BorderMuted)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("SOMA TOTAL PAGA:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.LightGray)
                        Text(formatCurrency(totalPaidSum), fontWeight = FontWeight.ExtraBold, fontSize = 19.sp, color = Color(0xFF4CAF50))
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Histórico de Pagamentos", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    
                    // Button to add payment
                    Button(
                        onClick = { showAddPaymentForm = !showAddPaymentForm },
                        colors = ButtonDefaults.buttonColors(containerColor = ThemeColors.WarningAmber, contentColor = Color.Black),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("add_payment_history_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (showAddPaymentForm) "Fechar" else "Lançar Pago", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))

                AnimatedVisibility(visible = showAddPaymentForm) {
                    AddPaymentInlineForm(
                        onSubmit = { amt, date ->
                            onAddNewPayment(amt, date)
                            showAddPaymentForm = false
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (paymentList.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Nenhum pagamento registrado neste histórico.", color = Color.Gray, fontSize = 14.sp, textAlign = TextAlign.Center)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(paymentList) { payment ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(Icons.Default.DateRange, contentDescription = null, tint = ThemeColors.ConstructionGold, modifier = Modifier.size(16.dp))
                                        Text(text = "Pago em ${payment.data}:", fontSize = 15.sp, color = Color.White)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = formatCurrency(payment.valor),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        IconButton(onClick = { onDeletePayment(payment) }, modifier = Modifier.size(36.dp)) {
                                            Icon(Icons.Default.Delete, contentDescription = "Excluir Pagamento", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- GENERIC HISTORY PAYMENTS FLOWS DIALOG — PRESTADORES ---
@Composable
fun PaymentsHistoryPrestadorDialog(
    titleName: String,
    initialContractedValue: Double,
    prestadorObraId: Int,
    viewModel: ObrasViewModel,
    onDismiss: () -> Unit,
    onAddNewPayment: (Double, String) -> Unit,
    onUpdateContractValue: (Double) -> Unit,
    onDeletePayment: (PagamentoPrestador) -> Unit
) {
    val paymentList by viewModel.getPagamentosForPrestadorObra(prestadorObraId).collectAsStateWithLifecycle(emptyList())
    var showAddPaymentForm by remember { mutableStateOf(false) }

    var isEditingContract by remember { mutableStateOf(false) }
    var newContractValStr by remember { mutableStateOf(formatDoubleToCurrencyString(initialContractedValue)) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ThemeColors.MutedSlateCard),
            border = BoxBorder(ThemeColors.BorderMuted)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = titleName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color.White
                        )
                        
                        if (isEditingContract) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = newContractValStr,
                                    onValueChange = { newContractValStr = formatCurrencyString(it) },
                                    label = { Text("Novo Valor Contratado", fontSize = 11.sp, color = Color.White) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedLabelColor = ThemeColors.WarningAmber,
                                        unfocusedLabelColor = Color.LightGray,
                                        focusedBorderColor = ThemeColors.WarningAmber,
                                        unfocusedBorderColor = Color.LightGray
                                    ),
                                    modifier = Modifier.weight(1f).height(56.dp).testTag("edit_contract_val_field")
                                )
                                IconButton(
                                    onClick = {
                                        val newVal = parseCurrencyStringToDouble(newContractValStr)
                                        if (newVal > 0.0) {
                                            onUpdateContractValue(newVal)
                                            isEditingContract = false
                                        }
                                    },
                                    modifier = Modifier.background(Color(0xFF0F291E), CircleShape).size(36.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = "Salvar novo contrato", tint = Color(0xFF6EE7B7), modifier = Modifier.size(18.dp))
                                }
                                IconButton(
                                    onClick = {
                                        isEditingContract = false
                                        newContractValStr = formatDoubleToCurrencyString(initialContractedValue)
                                    },
                                    modifier = Modifier.background(Color(0xFF2E1A1A), CircleShape).size(36.dp)
                                ) {
                                    Icon(Icons.Default.Clear, contentDescription = "Cancelar", tint = Color(0xFFFCA5A5), modifier = Modifier.size(18.dp))
                                }
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.clickable {
                                    newContractValStr = formatDoubleToCurrencyString(initialContractedValue)
                                    isEditingContract = true
                                }
                            ) {
                                Text(
                                    text = "Valor Contratado: ${formatCurrency(initialContractedValue)}",
                                    fontSize = 14.sp,
                                    color = ThemeColors.WarningAmber
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Editar valor contratado",
                                    tint = ThemeColors.WarningAmber.copy(alpha = 0.8f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Fechar", tint = Color.LightGray)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                val totalPaidSum = paymentList.sumOf { it.valor }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(8.dp),
                    border = BoxBorder(ThemeColors.BorderMuted)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("SOMA TOTAL PAGA:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.LightGray)
                        Text(formatCurrency(totalPaidSum), fontWeight = FontWeight.ExtraBold, fontSize = 19.sp, color = Color(0xFF4CAF50))
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Histórico de Pagamentos", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    
                    Button(
                        onClick = { showAddPaymentForm = !showAddPaymentForm },
                        colors = ButtonDefaults.buttonColors(containerColor = ThemeColors.WarningAmber, contentColor = Color.Black),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("add_payment_history_button_prest")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (showAddPaymentForm) "Fechar" else "Lançar Pago", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))

                AnimatedVisibility(visible = showAddPaymentForm) {
                    AddPaymentInlineForm(
                        onSubmit = { amt, date ->
                            onAddNewPayment(amt, date)
                            showAddPaymentForm = false
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (paymentList.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Nenhum pagamento registrado neste histórico.", color = Color.Gray, fontSize = 14.sp, textAlign = TextAlign.Center)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(paymentList) { payment ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(Icons.Default.DateRange, contentDescription = null, tint = ThemeColors.ConstructionGold, modifier = Modifier.size(16.dp))
                                        Text(text = "Pago em ${payment.data}:", fontSize = 15.sp, color = Color.White)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = formatCurrency(payment.valor),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        IconButton(onClick = { onDeletePayment(payment) }, modifier = Modifier.size(36.dp)) {
                                            Icon(Icons.Default.Delete, contentDescription = "Excluir Pagamento", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddPaymentInlineForm(
    onSubmit: (Double, String) -> Unit
) {
    var amountText by remember { mutableStateOf("0,00") }
    var dateString by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        dateString = sdf.format(java.util.Date())
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.15f)),
        border = BoxBorder(ThemeColors.BorderMuted)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Registrar Novo Pagamento", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = formatCurrencyString(it) },
                    label = { Text("Valor Pago", fontSize = 11.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("inline_payment_amount")
                )

                OutlinedTextField(
                    value = dateString,
                    onValueChange = { dateString = it },
                    label = { Text("Data", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("inline_payment_date")
                )
            }
            Button(
                onClick = {
                    val amt = parseCurrencyStringToDouble(amountText)
                    if (amt > 0.0) {
                        onSubmit(amt, dateString.ifBlank { "-" })
                        amountText = "0,00"
                    }
                },
                enabled = parseCurrencyStringToDouble(amountText) > 0.0,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50), contentColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("inline_payment_save_button")
            ) {
                Text("Confirmar Pagamento", fontSize = 12.sp)
            }
        }
    }
}


// --- SCREEN: RELATÓRIO DA OBRA ---
@Composable
fun RelatorioObraScreen(
    obraId: Int,
    navController: NavController,
    viewModel: ObrasViewModel
) {
    var currentObra by remember { mutableStateOf<Obra?>(null) }
    
    val launchedFornecedores by viewModel.getFornecedoresForObra(obraId).collectAsStateWithLifecycle(emptyList())
    val launchedPrestadores by viewModel.getPrestadoresForObra(obraId).collectAsStateWithLifecycle(emptyList())
    val launchedLotes by viewModel.getLotesForObra(obraId).collectAsStateWithLifecycle(emptyList())
    val launchedBurocracias by viewModel.getBurocraciasForObra(obraId).collectAsStateWithLifecycle(emptyList())

    LaunchedEffect(obraId) {
        currentObra = viewModel.allObras.value.find { it.id == obraId }
    }

    val totalContractedSuppliers = launchedFornecedores.sumOf { it.valorServico }
    val totalPaidSuppliers = launchedFornecedores.sumOf { it.valorPago }

    val totalContractedProviders = launchedPrestadores.sumOf { it.valorMaterial }
    val totalPaidProviders = launchedPrestadores.sumOf { it.valorPago }
    
    val totalLotes = launchedLotes.sumOf { it.valor }
    val totalBurocracia = launchedBurocracias.sumOf { it.valorPago }

    val grandTotalContracted = totalContractedSuppliers + totalContractedProviders + totalLotes + totalBurocracia
    val grandTotalPaid = totalPaidSuppliers + totalPaidProviders + totalLotes + totalBurocracia

    Scaffold(
        topBar = {
            ObrasTopAppBar(
                title = "Relatório da Obra",
                onBackClick = { navController.popBackStack() }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Document header card
            currentObra?.let { obra ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = ThemeColors.MutedSlateCard),
                    border = BoxBorder(ThemeColors.BorderMuted)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "RELATÓRIO FINANCEIRO DE OBRA",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            color = ThemeColors.WarningAmber
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = obra.nome,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Icon(Icons.Default.Place, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                            Text(text = obra.endereco, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Text(
                text = "Métricas de Serviços",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            // Service providers metric card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BoxBorder(ThemeColors.BorderMuted),
                colors = CardDefaults.cardColors(containerColor = ThemeColors.MutedSlateCard)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Prestadores Alocados", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${launchedPrestadores.size}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Divider(color = ThemeColors.BorderMuted, thickness = 1.dp)

                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Total Contratado (Serviços)", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatCurrency(totalContractedProviders), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }

                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Valor Total Pago", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatCurrency(totalPaidProviders), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF00E676))
                    }
                    
                    val pct = if (totalContractedProviders > 0) (totalPaidProviders / totalContractedProviders) else 0.0
                    LinearProgressIndicator(
                        progress = pct.toFloat().coerceIn(0f, 1f),
                        color = Color(0xFF00E676),
                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                    Text(
                        text = "Progresso de Quitação: ${(pct * 100).toInt()}%",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End
                    )
                }
            }

            Text(
                text = "Métricas de Materiais",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            // Suppliers metric card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BoxBorder(ThemeColors.BorderMuted),
                colors = CardDefaults.cardColors(containerColor = ThemeColors.MutedSlateCard)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Fornecedores Alocados", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${launchedFornecedores.size}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Divider(color = ThemeColors.BorderMuted, thickness = 1.dp)

                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Total Contratado (Materiais)", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatCurrency(totalContractedSuppliers), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }

                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Valor Total Pago", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatCurrency(totalPaidSuppliers), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF00E5FF))
                    }

                    val pct = if (totalContractedSuppliers > 0) (totalPaidSuppliers / totalContractedSuppliers) else 0.0
                    LinearProgressIndicator(
                        progress = pct.toFloat().coerceIn(0f, 1f),
                        color = Color(0xFF00E5FF),
                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                    Text(
                        text = "Progresso de Quitação: ${(pct * 100).toInt()}%",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End
                    )
                }
            }

            // Lotes metric card
            Text(
                text = "Métricas de Lotes",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BoxBorder(ThemeColors.BorderMuted),
                colors = CardDefaults.cardColors(containerColor = ThemeColors.MutedSlateCard)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Lotes Lançados", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${launchedLotes.size}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Divider(color = ThemeColors.BorderMuted, thickness = 1.dp)

                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Valor Total dos Lotes", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatCurrency(totalLotes), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF9C27B0))
                    }
                }
            }

            // Burocracia metric card
            Text(
                text = "Métricas de Burocracia",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BoxBorder(ThemeColors.BorderMuted),
                colors = CardDefaults.cardColors(containerColor = ThemeColors.MutedSlateCard)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Burocracias Lançadas", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${launchedBurocracias.size}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Divider(color = ThemeColors.BorderMuted, thickness = 1.dp)

                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Valor Total Burocracia", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatCurrency(totalBurocracia), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.Gray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Divider(color = ThemeColors.BorderMuted, thickness = 1.dp)

            // Balance Consolidation Brief
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                border = BoxBorder(ThemeColors.BorderMuted)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "CONSOLIDAÇÃO TOTAL DA OBRA",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Custo Geral Contratado:", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatCurrency(grandTotalContracted), fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Desembolso Geral Realizado:", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatCurrency(grandTotalPaid), fontSize = 20.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}


// --- EMPTY STATE HELPER VIEW ---
@Composable
fun EmptyStateView(
    icon: ImageVector,
    title: String,
    description: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(MaterialTheme.colorScheme.secondary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = description,
            textAlign = TextAlign.Center,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CsvBackupScreen(
    navController: NavController,
    viewModel: ObrasViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val fornecedores by viewModel.allFornecedores.collectAsStateWithLifecycle()
    val prestadores by viewModel.allPrestadores.collectAsStateWithLifecycle()

    // Upon entry check: "Caso não tenha arquivos cadastrados nem em fornecedores e prestadores mostrar a mensagem de que não tem dados cadastrados e já voltar a tela inicial."
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(600) // Wait briefly for DB query flow initialization
        if (viewModel.allFornecedores.value.isEmpty() && viewModel.allPrestadores.value.isEmpty()) {
            Toast.makeText(context, "Não há dados cadastrados em fornecedores ou prestadores!", Toast.LENGTH_LONG).show()
            navController.popBackStack("main", inclusive = false)
        }
    }

    // SAF Import Launcher
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        BufferedReader(InputStreamReader(inputStream)).use { reader ->
                            val csvContent = reader.readText()
                            val importedCount = viewModel.importCsvContent(csvContent)
                            Toast.makeText(
                                context,
                                "$importedCount registros importados com sucesso!",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Erro ao importar CSV: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // SAF Export Launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val csvContent = viewModel.getCsvContentForExport()
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        OutputStreamWriter(outputStream).use { writer ->
                            writer.write(csvContent)
                            Toast.makeText(context, "Cadastro global exportado com sucesso!", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Erro ao exportar CSV: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            ObrasTopAppBar(
                title = "Backup de Dados (CSV)",
                onBackClick = { navController.popBackStack() }
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                color = Color.Transparent
            ) {
                Button(
                    onClick = { navController.popBackStack("main", inclusive = false) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onBackground,
                        contentColor = MaterialTheme.colorScheme.background
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("back_to_home_button")
                ) {
                    Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "VOLTAR PARA A TELA DE INÍCIO",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // High-visibility design matching other screens
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = null,
                tint = ThemeColors.ConstructionGold,
                modifier = Modifier.size(64.dp)
            )

            Text(
                text = "Exportação e Importação de Cadastros",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Aqui você pode salvar uma cópia de segurança (CSV) dos fornecedores e prestadores cadastrados globalmente, ou recuperar uma cópia existente de outro dispositivo.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Actions box
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BoxBorder(ThemeColors.BorderMuted)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = { exportLauncher.launch("cadastro_global_obras.csv") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("screen_export_csv_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Exportar CSV", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = { importLauncher.launch(arrayOf("text/comma-separated-values", "text/csv", "*/*")) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("screen_import_csv_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ThemeColors.WarningAmber,
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Importar CSV", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
