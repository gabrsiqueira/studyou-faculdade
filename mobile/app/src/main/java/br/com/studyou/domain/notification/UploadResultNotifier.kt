package br.com.studyou.domain.notification

/** Notifies the user, outside the app's own UI, when a file finishes processing. */
interface UploadResultNotifier {
    fun notifyAnalysisCompleted(fileName: String)
    fun notifyAnalysisFailed(fileName: String, reason: String)
}
