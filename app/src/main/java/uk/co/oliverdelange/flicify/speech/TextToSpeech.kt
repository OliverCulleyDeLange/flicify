package uk.co.oliverdelange.flicify.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.disposables.Disposables
import io.reactivex.schedulers.Schedulers
import uk.co.oliverdelange.flicify.redux.Action
import java.util.*

sealed class SpeechEvents : Action {
    data class Speak(val speech: String) : SpeechEvents()
    data class Listen(val seconds: Int) : SpeechEvents()
}

sealed class SpeechResults : Action {
    sealed class Init : SpeechResults() {
        data class TTSInitError(val error: Throwable) : Init()
        data class TTSInitSuccess(val speechController: SpeechController) : Init()
        object SpeechEngineDeinitialised : Init()
    }
}

fun initSpeech(context: Context): Observable<SpeechResults.Init> = Speech(context).subscribeOn(Schedulers.io())

private class Speech(val context: Context) : Observable<SpeechResults.Init>() {
    override fun subscribeActual(observer: Observer<in SpeechResults.Init>) {
        var speech: TextToSpeech? = null

        speech = TextToSpeech(context) { status ->
            val _speech = speech
            if (_speech != null && status == TextToSpeech.SUCCESS) {
                when (_speech.setLanguage(Locale.ENGLISH)) {
                    TextToSpeech.LANG_MISSING_DATA, TextToSpeech.LANG_NOT_SUPPORTED -> {
                        observer.onNext(SpeechResults.Init.TTSInitError(IllegalStateException("The Language specified is not supported!")))
                    }
                    else -> {
                        observer.onNext(SpeechResults.Init.TTSInitSuccess(SpeechController(_speech)))
                    }
                }
            } else {
                observer.onNext(
                    SpeechResults.Init.TTSInitError(IllegalStateException("Error Initialising speech engine. speech = $_speech, status=$status"))
                )
            }
        }

        observer.onSubscribe(Disposables.fromAction {
            speech?.stop()
            speech?.shutdown()
            speech = null
            observer.onNext(SpeechResults.Init.SpeechEngineDeinitialised)
        })
    }
}

sealed class UtteranceEvent: Action {
    abstract val utteranceId: String

    data class UtteranceDone(override val utteranceId: String) : UtteranceEvent()
    data class UtteranceError(override val utteranceId: String, val errorCode: Int? = null) : UtteranceEvent()
    data class AudioAvailable(override val utteranceId: String/*, val audio: ByteArray?*/) : UtteranceEvent()
    data class RangeStart(override val utteranceId: String, val start: Int, val end: Int, val frame: Int) : UtteranceEvent()
    data class Start(override val utteranceId: String) : UtteranceEvent()
    data class Stop(override val utteranceId: String, val interrupted: Boolean) : UtteranceEvent()
    data class BeginSynthesis(override val utteranceId: String, val sampleRateInHz: Int, val audioFormat: Int, val channelCount: Int) :
        UtteranceEvent()
}