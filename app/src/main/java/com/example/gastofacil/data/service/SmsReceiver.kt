package com.example.gastofacil.data.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.gastofacil.data.local.AppDatabase
import com.example.gastofacil.data.util.BankMessageParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
                for (sms in messages) {
                    val sender = sms.displayOriginatingAddress ?: ""
                    val body = sms.messageBody ?: ""

                    Log.d("SmsReceiver", "SMS recebido de $sender: $body")

                    val suggestion = BankMessageParser.parseMessage("SMS: $sender", body)
                    if (suggestion != null) {
                        val pendingResult = goAsync()
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                AppDatabase.getDatabase(context)
                                    .suggestedTransactionDao()
                                    .insertSuggestion(suggestion)
                                Log.d("SmsReceiver", "Sugestão via SMS salva com sucesso: ${suggestion.title}")
                            } catch (e: Exception) {
                                Log.e("SmsReceiver", "Erro ao salvar sugestão via SMS no BD", e)
                            } finally {
                                pendingResult.finish()
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SmsReceiver", "Erro geral no SmsReceiver", e)
        }
    }
}
