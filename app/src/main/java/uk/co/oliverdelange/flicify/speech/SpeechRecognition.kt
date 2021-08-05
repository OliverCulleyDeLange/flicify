package uk.co.oliverdelange.flicify.speech

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposables
import timber.log.Timber
import uk.co.oliverdelange.flicify.redux.Action

/** Most speech recognition apis must be called from main thread */
fun initSpeechRecognition(context: Context): Observable<SpeechRecognitionResults> =
    SpeechRecognition(context).subscribeOn(AndroidSchedulers.mainThread())

private class SpeechRecognition(val context: Context) : Observable<SpeechRecognitionResults>() {
    override fun subscribeActual(observer: Observer<in SpeechRecognitionResults>) {
        var speechRecogniser: SpeechRecognizer? = null
        Timber.i("On SDK ${Build.VERSION.SDK_INT}")
        if (Build.VERSION.SDK_INT >= 31 &&
            SpeechRecognizer.isOnDeviceRecognitionAvailable(context).also{ Timber.w("On-device available=$it")}
        ) {
            Timber.i("Using on device speech recognition! thread ${Thread.currentThread()}")
            speechRecogniser = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        } else if (SpeechRecognizer.isRecognitionAvailable(context)) {
            Timber.i("Using off device speech recognition! thread ${Thread.currentThread()}")
            speechRecogniser = SpeechRecognizer.createSpeechRecognizer(context)
        }

        if (speechRecogniser != null) {
            speechRecogniser.setRecognitionListener(SpeechRecognitionListener(observer))
            observer.onNext(SpeechRecognitionResults.RecogniserAvailable(speechRecogniser))
        } else {
            observer.onNext(SpeechRecognitionResults.SpeechRecognitionUnavailable)
            observer.onComplete()
        }

        observer.onSubscribe(Disposables.fromAction {
            Timber.w("Tearing down speech recogniser on thread ${Thread.currentThread()}")
            speechRecogniser?.cancel()
            speechRecogniser?.destroy()
            observer.onNext(SpeechRecognitionResults.Destroyed)
        })
    }
}

sealed class SpeechRecognitionResults: Action {
    object SpeechRecognitionUnavailable : SpeechRecognitionResults()
    data class RecogniserAvailable(val speechRecognizer: SpeechRecognizer) : SpeechRecognitionResults()
    object Destroyed : SpeechRecognitionResults()
    object StartedListening : SpeechRecognitionResults()

    sealed class SpeechRecognitionListenerResults : SpeechRecognitionResults() {
        data class ReadyForSpeech(val bundle: Bundle?) : SpeechRecognitionListenerResults()
        object BeginningOfSpeech : SpeechRecognitionListenerResults()
        object EndOfSpeech : SpeechRecognitionListenerResults()

        data class RmsChanged(val f: Float) : SpeechRecognitionListenerResults()
        data class BufferReceived(val b: ByteArray?) : SpeechRecognitionListenerResults()
        data class Error(val errorInt: Int, val error: String) : SpeechRecognitionListenerResults()
        data class Results(val speech: List<String>?, val confidenceScores: List<Float>?) : SpeechRecognitionListenerResults()
        data class PartialResults(val speech: List<String>?) : SpeechRecognitionListenerResults()
        data class Event(val i: Int, val b: Bundle?) : SpeechRecognitionListenerResults()
    }
}

class SpeechRecognitionListener(val observer: Observer<in SpeechRecognitionResults.SpeechRecognitionListenerResults>) : RecognitionListener {
    override fun onReadyForSpeech(p0: Bundle?) = observer.onNext(SpeechRecognitionResults.SpeechRecognitionListenerResults.ReadyForSpeech(p0))
    override fun onBeginningOfSpeech() = observer.onNext(SpeechRecognitionResults.SpeechRecognitionListenerResults.BeginningOfSpeech)
    override fun onRmsChanged(p0: Float) {
        // We don't care when the audio level changes atm, it just pollutes the logs
//        observer.onNext(SpeechRecognitionResults.SpeechRecognitionListenerResults.RmsChanged(p0))
    }
    override fun onBufferReceived(p0: ByteArray?) = observer.onNext(SpeechRecognitionResults.SpeechRecognitionListenerResults.BufferReceived(p0))
    override fun onEndOfSpeech() = observer.onNext(SpeechRecognitionResults.SpeechRecognitionListenerResults.EndOfSpeech)
    override fun onError(p0: Int) {
        val error = when (p0) {
            SpeechRecognizer.ERROR_AUDIO -> "AUDIO"
            SpeechRecognizer.ERROR_CLIENT -> "CLIENT"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "INSUFFICIENT_PERMISSIONS"
            SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> "LANGUAGE_NOT_SUPPORTED"
            SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "LANGUAGE_UNAVAILABLE"
            SpeechRecognizer.ERROR_NETWORK -> "NETWORK"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "NETWORK_TIMEOUT"
            SpeechRecognizer.ERROR_NO_MATCH -> "NO_MATCH"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "RECOGNIZER_BUSY"
            SpeechRecognizer.ERROR_SERVER -> "SERVER"
            SpeechRecognizer.ERROR_SERVER_DISCONNECTED -> "SERVER_DISCONNECTED"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "SPEECH_TIMEOUT"
            SpeechRecognizer.ERROR_TOO_MANY_REQUESTS -> "TOO_MANY_REQUESTS"
            else -> "UNKNOWN"
        }
        observer.onNext(SpeechRecognitionResults.SpeechRecognitionListenerResults.Error(p0, error))
    }

    override fun onResults(results: Bundle?) {
        val spoken = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val confidenceScores = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)?.toList()
        observer.onNext(SpeechRecognitionResults.SpeechRecognitionListenerResults.Results(spoken, confidenceScores))
    }
    override fun onPartialResults(bundle: Bundle?) {
        val spoken = bundle?.getStringArray(SpeechRecognizer.RESULTS_RECOGNITION)?.toList()
        observer.onNext(SpeechRecognitionResults.SpeechRecognitionListenerResults.PartialResults(spoken))
    }
    override fun onEvent(p0: Int, p1: Bundle?) = observer.onNext(SpeechRecognitionResults.SpeechRecognitionListenerResults.Event(p0, p1))
}
