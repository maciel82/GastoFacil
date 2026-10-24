package com.example.gastofacil.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.gastofacil.data.local.TransactionEntity
import com.example.gastofacil.ui.viewmodel.MainViewModel

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    viewModel: MainViewModel,
    transactionToEdit: TransactionEntity? = null,
    onDismiss: () -> Unit,
) {
    val isEditing = transactionToEdit != null
    val isInstallmentPurchase = transactionToEdit?.let {
        it.totalInstallments > 1 && (it.isInstallment || !it.installmentGroupId.isNullOrBlank())
    } == true

    var title by remember { mutableStateOf(transactionToEdit?.title ?: "") }
    var amountText by remember {
        mutableStateOf(
            if (transactionToEdit != null && isInstallmentPurchase) {
                val totalAmt = kotlin.math.round(transactionToEdit.amount * transactionToEdit.totalInstallments * 100.0) / 100.0
                if (totalAmt % 1.0 == 0.0) totalAmt.toLong().toString() else totalAmt.toString()
            } else {
                transactionToEdit?.amount?.toString() ?: ""
            }
        )
    }
    var storeLocation by remember { mutableStateOf(transactionToEdit?.storeLocation ?: "") }
    var type by remember { mutableStateOf(transactionToEdit?.type ?: "EXPENSE") }
    var paymentMethod by remember { mutableStateOf(transactionToEdit?.paymentMethod ?: "CREDIT") }
    var selectedBank by remember { mutableStateOf(transactionToEdit?.bank ?: "Nubank") }

    // Coleta as categorias salvas no banco de dados
    val categoriesList by viewModel.categories.collectAsState(initial = emptyList())
    var selectedCategory by remember { mutableStateOf(transactionToEdit?.category ?: "Mercado") }

    var isInstallment by remember { mutableStateOf(isInstallmentPurchase) }
    var totalInstallmentsText by remember { mutableStateOf(transactionToEdit?.totalInstallments?.toString() ?: "2") }

    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }

    val bankOptions = remember { listOf("Nubank", "PicPay", "RecargaPay", "Outro") }

    // Extrai os nomes das categorias do banco para o dropdown
    val categoryOptions = categoriesList.map { it.name }

    // Se a categoria selecionada não estiver na lista (ex: lista vazia no carregamento), usa a primeira ou "Mercado"
    if (categoryOptions.isNotEmpty() && !categoryOptions.contains(selectedCategory)) {
        selectedCategory = categoryOptions.first()
    }


    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Editar Lançamento" else "Novo Lançamento") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título (ex: Supermercado)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Valor (R$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = storeLocation,
                    onValueChange = { storeLocation = it },
                    label = { Text("Local / Loja (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Categoria Dropdown
                Text("Categoria:", style = MaterialTheme.typography.bodyMedium)
                ExposedDropdownMenuBox(
                    expanded = isCategoryDropdownExpanded,
                    onExpandedChange = { isCategoryDropdownExpanded = !isCategoryDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Selecione a Categoria") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = isCategoryDropdownExpanded,
                        onDismissRequest = { isCategoryDropdownExpanded = false }
                    ) {
                        categoryOptions.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category) },
                                onClick = {
                                    selectedCategory = category
                                    isCategoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Tipo: Gasto / Receita
                Text("Tipo de Transação:", style = MaterialTheme.typography.bodyMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = type == "EXPENSE",
                        onClick = { type = "EXPENSE" },
                        label = { Text("Gasto") }
                    )
                    FilterChip(
                        selected = type == "INCOME",
                        onClick = { type = "INCOME" },
                        label = { Text("Receita") }
                    )
                }

                // Banco
                Text("Banco:", style = MaterialTheme.typography.bodyMedium)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    bankOptions.forEach { bank ->
                        FilterChip(
                            selected = selectedBank == bank,
                            onClick = { selectedBank = bank },
                            label = { Text(text = bank, maxLines = 1) }
                        )
                    }
                }

                // Forma de Pagamento
                Text("Forma de Pagamento:", style = MaterialTheme.typography.bodyMedium)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        "CREDIT" to "Crédito",
                        "DEBIT" to "Débito",
                        "PIX" to "PIX",
                        "MONEY" to "Dinheiro"
                    ).forEach { (code, label) ->
                        FilterChip(
                            selected = paymentMethod == code,
                            onClick = { paymentMethod = code },
                            label = { Text(text = label, maxLines = 1) }
                        )
                    }
                }

                // Parcelado (desabilitado ou oculto na edição para evitar recriar faturas complexas acidentalmente, ou liberado se preferir)
                if (!isEditing) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = isInstallment,
                            onCheckedChange = { isInstallment = it }
                        )
                        Text("Compra Parcelada?")
                    }

                    if (isInstallment) {
                        OutlinedTextField(
                            value = totalInstallmentsText,
                            onValueChange = { totalInstallmentsText = it },
                            label = { Text("Número de Parcelas") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountValue = amountText.replace(",", ".").toDoubleOrNull() ?: 0.0
                    val totalInst = if (isInstallment) totalInstallmentsText.toIntOrNull() ?: 1 else 1

                    if (title.isNotBlank() && (amountValue > 0.0)) {
                        if (isEditing && transactionToEdit != null) {
                            // Atualiza o registro existente no banco
                            val updatedTransaction = transactionToEdit.copy(
                                title = title.trim(),
                                storeLocation = storeLocation.ifBlank { null },
                                amount = amountValue,
                                type = type,
                                paymentMethod = paymentMethod,
                                bank = selectedBank,
                                category = selectedCategory
                            )
                            viewModel.updateTransaction(updatedTransaction, newTotalAmount = amountValue)
                        } else {
                            // Cria um novo registro
                            val newTransaction = TransactionEntity(
                                title = title.trim(),
                                storeLocation = storeLocation.ifBlank { null },
                                amount = amountValue,
                                date = System.currentTimeMillis(),
                                type = type,
                                paymentMethod = paymentMethod,
                                bank = selectedBank,
                                category = selectedCategory,
                                categoryId = 1L,
                                isInstallment = isInstallment && (totalInst > 1),
                                currentInstallment = 1,
                                totalInstallments = totalInst
                            )
                            viewModel.addTransaction(newTransaction)
                        }
                        onDismiss()
                    }
                }
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}