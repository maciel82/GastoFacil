package com.example.gastofacil.data.service

import android.content.ComponentName
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.gastofacil.data.local.AppDatabase
import com.example.gastofacil.data.util.BankMessageParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BankNotificationService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("BankNotifService", "Service Listener Conectado com sucesso ao Android!")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.w("BankNotifService", "Service Listener Desconectado pelo sistema. Solicitando rebind...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                requestRebind(ComponentName(this, BankNotificationService::class.java))
            } catch (e: Exception) {
                Log.e("BankNotifService", "Erro ao solicitar rebind", e)
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        try {
            if (sbn == null) return
            val packageName = sbn.packageName ?: ""
            
            // Filtra pacotes de aplicativos bancários conhecidos (Nubank, PicPay, RecargaPay, Bradesco, Itaú, Santander, Caixa, Inter, C6, etc.)
            val isBankApp = packageName.contains("nu.production", true) ||
                    packageName.contains("picpay", true) ||
                    packageName.contains("recargapay", true) ||
                    packageName.contains("banco", true) ||
                    packageName.contains("bradesco", true) ||
                    packageName.contains("itau", true) ||
                    packageName.contains("santander", true) ||
                    packageName.contains("caixa", true) ||
                    packageName.contains("inter", true) ||
                    packageName.contains("c6bank", true) ||
                    packageName.contains("pagbank", true) ||
                    packageName.contains("neon", true)

            if (isBankApp) {
                val extras = sbn.notification?.extras ?: return
                val title = extras.getCharSequence("android.title")?.toString() ?: ""
                val text = extras.getCharSequence("android.text")?.toString() ?: ""
                val bigText = extras.getCharSequence("android.bigText")?.toString() ?: ""
                val fullMessage = if (bigText.isNotBlank()) "$title $bigText" else "$title $text"

                Log.d("BankNotifService", "Notificação capturada do app: $packageName - $fullMessage")

                val suggestion = BankMessageParser.parseMessage(packageName, fullMessage)
                if (suggestion != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            AppDatabase.getDatabase(applicationContext)
                                .suggestedTransactionDao()
                                .insertSuggestion(suggestion)
                            Log.d("BankNotifService", "Sugestão salva com sucesso: ${suggestion.title}")
                        } catch (e: Exception) {
                            Log.e("BankNotifService", "Erro ao inserir no banco de dados", e)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("BankNotifService", "Erro geral ao capturar notificação bancária", e)
        }
    }
}
