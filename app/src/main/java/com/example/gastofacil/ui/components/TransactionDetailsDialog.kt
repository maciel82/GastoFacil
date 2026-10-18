package com.example.gastofacil.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gastofacil.data.local.TransactionEntity
import com.example.gastofacil.ui.theme.ExpenseRedText
import com.example.gastofacil.ui.theme.IncomeGreenText
import com.example.gastofacil.ui.theme.SageCardBackground
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TransactionDetailsDialog(
    transaction: TransactionEntity,
    onDismiss: () -> Unit
) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    val dateFormat = SimpleDateFormat("dd 'de' MMMM 'de' yyyy, HH:mm", Locale("pt", "BR"))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Detalhes do Lançamento",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Título e Valor
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SageCardBackground)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = transaction.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = (if (transaction.type == "INCOME") "+ " else "- ") + currencyFormat.format(transaction.amount),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (transaction.type == "INCOME") IncomeGreenText else ExpenseRedText
                        )
                    }
                }

                // Tabela de Informações
                DetailRow(label = "Tipo", value = if (transaction.type == "INCOME") "Receita" else "Gasto")
                DetailRow(label = "Categoria", value = transaction.category)
                DetailRow(label = "Forma de Pagamento", value = transaction.paymentMethod)

                transaction.bank?.let { bank ->
                    if (bank.isNotBlank()) {
                        DetailRow(label = "Banco / Conta", value = bank)
                    }
                }

                transaction.storeLocation?.let { location ->
                    if (location.isNotBlank()) {
                        DetailRow(label = "Local / Loja", value = location)
                    }
                }

                DetailRow(label = "Data", value = dateFormat.format(Date(transaction.date)))

                if (transaction.isInstallment) {
                    DetailRow(
                        label = "Parcelamento",
                        value = "em ${transaction.totalInstallments}x"
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar")
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 13.sp, color = Color.Gray)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
    }
}