package com.example.gastofacil.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.gastofacil.data.local.SuggestedTransactionEntity
import com.example.gastofacil.ui.theme.DarkForestGreen
import com.example.gastofacil.ui.theme.ExpenseRedText
import com.example.gastofacil.ui.theme.IncomeGreenText
import com.example.gastofacil.ui.theme.SageCardBackground
import com.example.gastofacil.ui.theme.WarmGoldFab
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SuggestedTransactionsSection(
    suggestions: List<SuggestedTransactionEntity>,
    onAccept: (SuggestedTransactionEntity) -> Unit,
    onDismiss: (SuggestedTransactionEntity) -> Unit
) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("pt", "BR")) }
    val timeFormat = remember { SimpleDateFormat("HH:mm - dd/MM", Locale("pt", "BR")) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Cabeçalho da seção
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = DarkForestGreen,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "SUGESTÕES AUTOMÁTICAS",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkForestGreen
                )
                if (suggestions.isNotEmpty()) {
                    Surface(
                        shape = CircleShape,
                        color = WarmGoldFab,
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Text(
                            text = suggestions.size.toString(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkForestGreen,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        if (suggestions.isEmpty()) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SageCardBackground.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Nenhuma transação pendente detectada via SMS ou Notificações de Banco.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(suggestions, key = { it.id }) { suggestion ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SageCardBackground),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.width(280.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            // Top Row: Banco / Origem e Data
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = DarkForestGreen.copy(alpha = 0.1f)
                                ) {
                                    Text(
                                        text = "${suggestion.bank ?: "Banco"} • ${suggestion.source}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DarkForestGreen,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Text(
                                    text = timeFormat.format(Date(suggestion.date)),
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Título e Categoria
                            Text(
                                text = suggestion.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.DarkGray,
                                maxLines = 1
                            )

                            Text(
                                text = "${suggestion.category} • ${suggestion.paymentMethod}",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Valor e Botões
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = currencyFormat.format(suggestion.amount),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (suggestion.type == "INCOME") IncomeGreenText else ExpenseRedText
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    // Descartar
                                    IconButton(
                                        onClick = { onDismiss(suggestion) },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(ExpenseRedText.copy(alpha = 0.1f), CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Descartar",
                                            tint = ExpenseRedText,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Aceitar
                                    IconButton(
                                        onClick = { onAccept(suggestion) },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(IncomeGreenText, CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Aceitar",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
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
