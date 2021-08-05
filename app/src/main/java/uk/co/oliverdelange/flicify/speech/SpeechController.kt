package uk.co.oliverdelange.flicify.speech

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.Engine.KEY_PARAM_VOLUME
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.util.*

class SpeechController(private val speech: TextToSpeech) {
    private val rxUtteranceProgressListener = RxUtteranceProgressListener()

    init {
        speech.setOnUtteranceProgressListener(rxUtteranceProgressListener)
    }

    fun speak(message: String): Observable<UtteranceEvent> =
        Single.fromCallable {
            UUID.randomUUID().toString()
        }.doOnSuccess {
            /** QUEUE_FLUSH used here as the fast turns exercise speaks at a high rate, and can sometimes
             * cause delays between the speech action coming from the engine and it being spoken causing a bad UX as it
             * just feels like the app is getting it wrong*/
            speech.speak(message, TextToSpeech.QUEUE_FLUSH, Bundle().apply { this.putFloat(KEY_PARAM_VOLUME, 1.0f) }, it)
        }.flatMapObservable { utteranceId ->
            rxUtteranceProgressListener.events.filter { it.utteranceId == utteranceId }
        }.takeUntil {
            when (it) {
                is UtteranceEvent.UtteranceDone -> true
                is UtteranceEvent.UtteranceError -> true
                else -> false
            }
        }
            .doOnComplete { Timber.d("Speech complete: $message") }
            .doOnSubscribe { Timber.d("Speaking $message") }

    fun getVoices(): List<Voice> {
        val engine = getEngine()
        val voices = speech.voices?.filter {
            val noNetworkEnglishVoices = !it.isNetworkConnectionRequired && it.locale.isO3Language == Locale.ENGLISH.isO3Language
            when (engine) {
                "com.google.android.tts" -> noNetworkEnglishVoices && !it.features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)
                else -> noNetworkEnglishVoices
            }
        }
        Timber.i("Available TTS engines: ${speech.engines}, available voices on selected engine ${speech.defaultEngine}: ${voices?.size} $voices")
        return voices ?: emptyList()

    }

    fun getEngine(): String = speech.defaultEngine
    fun isSpeaking() = speech.isSpeaking
}

private class RxUtteranceProgressListener : UtteranceProgressListener() {

    private val relay = PublishSubject.create<UtteranceEvent>()
    val events: Observable<UtteranceEvent> = relay.hide()

    override fun onError(utteranceId: String, errorCode: Int) =
        relay.onNext(UtteranceEvent.UtteranceError(utteranceId, errorCode))

    override fun onError(utteranceId: String) =
        relay.onNext(UtteranceEvent.UtteranceError(utteranceId))

    override fun onAudioAvailable(utteranceId: String, audio: ByteArray?) =
        relay.onNext(UtteranceEvent.AudioAvailable(utteranceId/*, audio*/))

    override fun onDone(utteranceId: String) =
        relay.onNext(UtteranceEvent.UtteranceDone(utteranceId))

    override fun onRangeStart(utteranceId: String, start: Int, end: Int, frame: Int) =
        relay.onNext(UtteranceEvent.RangeStart(utteranceId, start, end, frame))

    override fun onStart(utteranceId: String) =
        relay.onNext(UtteranceEvent.Start(utteranceId))

    override fun onStop(utteranceId: String, interrupted: Boolean) =
        relay.onNext(UtteranceEvent.Stop(utteranceId, interrupted))

    override fun onBeginSynthesis(utteranceId: String, sampleRateInHz: Int, audioFormat: Int, channelCount: Int) =
        relay.onNext(UtteranceEvent.BeginSynthesis(utteranceId, sampleRateInHz, audioFormat, channelCount))
}