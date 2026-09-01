package br.com.studyou.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import br.com.studyou.domain.notification.UploadResultNotifier
import kotlin.random.Random

private const val CHANNEL_ID = "file_analysis"

/** Posts a system notification when a file finishes being analyzed, successfully or not. */
class AndroidUploadResultNotifier(private val context: Context) : UploadResultNotifier {

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Processamento de arquivos",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Avisa quando um arquivo enviado termina de ser analisado."
            }
            context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    override fun notifyAnalysisCompleted(fileName: String) {
        post(
            title = "Arquivo processado",
            text = "\"$fileName\" terminou de ser analisado. Os jogos já estão disponíveis."
        )
    }

    override fun notifyAnalysisFailed(fileName: String, reason: String) {
        post(
            title = "Falha ao processar arquivo",
            text = "\"$fileName\": $reason"
        )
    }

    private fun post(title: String, text: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        // NotificationManagerCompat.notify silently no-ops when POST_NOTIFICATIONS (API 33+)
        // hasn't been granted, so there is no permission check to do here.
        NotificationManagerCompat.from(context).notify(Random.nextInt(), notification)
    }
}
