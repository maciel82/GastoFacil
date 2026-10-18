package com.example.gastofacil.data.local.util

import android.content.Context
import android.net.Uri
import com.example.gastofacil.data.local.CategoryEntity
import com.example.gastofacil.data.local.TransactionEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

data class BackupData(
    val transactions: List<TransactionEntity>,
    val categories: List<CategoryEntity>
)

object BackupManager {

    fun exportToJson(transactions: List<TransactionEntity>, categories: List<CategoryEntity>): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportDate", System.currentTimeMillis())

        val txArray = JSONArray()
        transactions.forEach { tx ->
            val obj = JSONObject().apply {
                put("id", tx.id)
                put("title", tx.title)
                put("storeLocation", tx.storeLocation ?: "")
                put("amount", tx.amount)
                put("date", tx.date)
                put("type", tx.type)
                put("paymentMethod", tx.paymentMethod)
                put("bank", tx.bank ?: "")
                put("category", tx.category)
                put("categoryId", tx.categoryId)
                put("isInstallment", tx.isInstallment)
                put("currentInstallment", tx.currentInstallment)
                put("totalInstallments", tx.totalInstallments)
                put("installmentGroupId", tx.installmentGroupId ?: "")
            }
            txArray.put(obj)
        }
        root.put("transactions", txArray)

        val catArray = JSONArray()
        categories.forEach { cat ->
            val obj = JSONObject().apply {
                put("id", cat.id)
                put("name", cat.name)
                put("isDefault", cat.isDefault)
            }
            catArray.put(obj)
        }
        root.put("categories", catArray)

        return root.toString(2)
    }

    fun parseJson(jsonString: String): BackupData {
        val root = JSONObject(jsonString)

        val transactions = mutableListOf<TransactionEntity>()
        if (root.has("transactions")) {
            val txArray = root.getJSONArray("transactions")
            for (i in 0 until txArray.length()) {
                val obj = txArray.getJSONObject(i)
                transactions.add(
                    TransactionEntity(
                        id = 0, // Zera para auto-gerar novos IDs no banco
                        title = obj.getString("title"),
                        storeLocation = obj.optString("storeLocation").ifBlank { null },
                        amount = obj.getDouble("amount"),
                        date = obj.getLong("date"),
                        type = obj.getString("type"),
                        paymentMethod = obj.getString("paymentMethod"),
                        bank = obj.optString("bank").ifBlank { null },
                        category = obj.optString("category", "Geral"),
                        categoryId = obj.optLong("categoryId", 0L),
                        isInstallment = obj.optBoolean("isInstallment", false),
                        currentInstallment = obj.optInt("currentInstallment", 1),
                        totalInstallments = obj.optInt("totalInstallments", 1),
                        installmentGroupId = obj.optString("installmentGroupId").ifBlank { null }
                    )
                )
            }
        }

        val categories = mutableListOf<CategoryEntity>()
        if (root.has("categories")) {
            val catArray = root.getJSONArray("categories")
            for (i in 0 until catArray.length()) {
                val obj = catArray.getJSONObject(i)
                categories.add(
                    CategoryEntity(
                        id = 0,
                        name = obj.getString("name"),
                        isDefault = obj.optBoolean("isDefault", false)
                    )
                )
            }
        }

        return BackupData(transactions, categories)
    }

    fun writeToUri(context: Context, uri: Uri, content: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(content)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun readFromUri(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readText()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
