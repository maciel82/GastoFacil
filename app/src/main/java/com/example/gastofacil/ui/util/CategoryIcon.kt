package com.example.gastofacil.ui.util

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.gastofacil.ui.theme.DarkForestGreen
import com.example.gastofacil.ui.theme.SageCardBackground

fun getCategoryIcon(category: String?, type: String = "EXPENSE"): ImageVector {
    if (type == "INCOME") return Icons.AutoMirrored.Filled.TrendingUp
    return when (category) {
        "Lazer" -> Icons.Default.SportsEsports
        "Mercado" -> Icons.Default.ShoppingCart
        "Alimentação" -> Icons.Default.Restaurant
        "Compras Online" -> Icons.Default.ShoppingBag
        "Transporte" -> Icons.Default.DirectionsCar
        "Educação" -> Icons.Default.School
        "Contas de casa" -> Icons.Default.Home
        "Trabalho" -> Icons.Default.Work
        "Saúde Capilar" -> Icons.Default.ContentCut
        "Contas de outros" -> Icons.Default.Receipt
        else -> Icons.Default.Category
    }
}

@Composable
fun CategoryIconBox(
    category: String?,
    modifier: Modifier = Modifier,
    type: String = "EXPENSE",
    backgroundColor: Color = SageCardBackground,
    iconTint: Color = DarkForestGreen,
) {
    Surface(
        shape = CircleShape,
        color = backgroundColor,
        modifier = modifier.size(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = getCategoryIcon(category, type),
                contentDescription = category ?: "Categoria",
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
