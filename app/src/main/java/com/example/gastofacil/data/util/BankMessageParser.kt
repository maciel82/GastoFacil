package com.example.gastofacil.data.util

import android.util.Log
import com.example.gastofacil.data.local.SuggestedTransactionEntity
import java.util.Locale

object BankMessageParser {

    fun parseMessage(senderOrApp: String, messageBody: String): SuggestedTransactionEntity? {
        return try {
            if (messageBody.isBlank()) return null
            val bodyLower = messageBody.lowercase(Locale("pt", "BR"))

            // 1. Identificar Banco com base no remetente ou conteúdo
            val bank = when {
                senderOrApp.contains("nubank", true) || bodyLower.contains("nubank") -> "Nubank"
                senderOrApp.contains("picpay", true) || bodyLower.contains("picpay") -> "PicPay"
                senderOrApp.contains("recargapay", true) || bodyLower.contains("recarga") -> "RecargaPay"
                senderOrApp.contains("bradesco", true) || bodyLower.contains("bradesco") -> "Bradesco"
                senderOrApp.contains("itau", true) || bodyLower.contains("itaú") || bodyLower.contains("itau") -> "Itaú"
                senderOrApp.contains("santander", true) || bodyLower.contains("santander") -> "Santander"
                senderOrApp.contains("caixa", true) || bodyLower.contains("caixa") -> "Caixa"
                senderOrApp.contains("inter", true) || bodyLower.contains("banco inter") -> "Inter"
                senderOrApp.contains("c6", true) || bodyLower.contains("c6 bank") -> "C6 Bank"
                else -> "Outro"
            }

            // 2. Identificar se é Entrada (INCOME) ou Saída (EXPENSE)
            val isIncome = bodyLower.contains("recebeu") ||
                    bodyLower.contains("pix recebido") ||
                    bodyLower.contains("depósito") ||
                    bodyLower.contains("deposito") ||
                    bodyLower.contains("salário") ||
                    bodyLower.contains("salario") ||
                    bodyLower.contains("transferência recebida") ||
                    bodyLower.contains("transferencia recebida") ||
                    bodyLower.contains("estorno") ||
                    bodyLower.contains("reembolso")

            val isExpense = bodyLower.contains("compra") ||
                    bodyLower.contains("pix enviado") ||
                    bodyLower.contains("cartão") ||
                    bodyLower.contains("cartao") ||
                    bodyLower.contains("aprovada") ||
                    bodyLower.contains("gasto") ||
                    bodyLower.contains("débito") ||
                    bodyLower.contains("debito") ||
                    bodyLower.contains("pagamento") ||
                    bodyLower.contains("pago") ||
                    bodyLower.contains("transferência enviada") ||
                    bodyLower.contains("saque")

            if (!isIncome && !isExpense) return null

            val type = if (isIncome) "INCOME" else "EXPENSE"

            // 3. Extrair Valor numérico (ex: R$ 150,00 ou 150.00)
            val regex = Regex("""R\$\s*([\d.]+,\d{2})|([\d]+\.\d{2})|([\d]+,\d{2})""")
            val matchResult = regex.find(messageBody)

            val rawAmountStr = if (matchResult != null) {
                (matchResult.groups[1]?.value ?: matchResult.groups[2]?.value ?: matchResult.groups[3]?.value ?: matchResult.value)
                    .replace(".", "")
                    .replace(",", ".")
            } else {
                val fallbackRegex = Regex("""[\d]+[.,][\d]{2}""")
                val fallbackMatch = fallbackRegex.find(messageBody) ?: return null
                fallbackMatch.value.replace(".", "").replace(",", ".")
            }

            val amount = rawAmountStr.toDoubleOrNull() ?: return null

            // 4. Identificar Forma de Pagamento com segurança baseada no tipo
            val paymentMethod = if (type == "INCOME") {
                if (bodyLower.contains("dinheiro")) "MONEY" else "PIX"
            } else {
                when {
                    bodyLower.contains("pix") -> "PIX"
                    bodyLower.contains("débito") || bodyLower.contains("debito") -> "DEBIT"
                    bodyLower.contains("dinheiro") || bodyLower.contains("saque") -> "MONEY"
                    bodyLower.contains("crédito") || bodyLower.contains("credito") || bodyLower.contains("cartão") || bodyLower.contains("cartao") -> "CREDIT"
                    else -> "DEBIT"
                }
            }

            // 5. Título / Estabelecimento aproximado
            val title = if (isIncome) {
                val senderMatch = Regex("""(de|por)\s+([A-Za-z0-9\s*]+)(?=\s+R\$|\s+valor|\s+às|\.|$)""").find(messageBody)
                val extractedSender = senderMatch?.groups?.get(2)?.value?.trim()
                if (!extractedSender.isNullOrBlank()) "Pix Recebido - $extractedSender" else "Entrada / Pix Recebido"
            } else {
                val storeMatch = Regex("""(em|no|na|pela|pelo)\s+([A-Za-z0-9\s*]+)(?=\s+R\$|\s+valor|\s+às|\.|$)""").find(messageBody)
                val storeName = storeMatch?.groups?.get(2)?.value?.trim()
                if (!storeName.isNullOrBlank()) storeName.replaceFirstChar { it.uppercase() } else "Compra no Cartão / Pix"
            }

            val category = if (isIncome) "Trabalho" else "Geral"

            SuggestedTransactionEntity(
                title = title,
                amount = amount,
                type = type,
                paymentMethod = paymentMethod,
                bank = bank,
                category = category,
                date = System.currentTimeMillis(),
                source = if (senderOrApp.contains("SMS", ignoreCase = true)) "SMS" else "Notificação"
            )
        } catch (e: Exception) {
            Log.e("BankMessageParser", "Erro ao analisar mensagem de banco", e)
            null
        }
    }
}
