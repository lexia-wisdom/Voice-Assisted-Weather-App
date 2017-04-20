package kapil.voiceassistedweatherapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import java.util.ArrayList;
import java.util.Locale;

import javax.inject.Inject;

import kapil.voiceassistedweatherapp.weather.OnWeatherDataReceivedListener;
import kapil.voiceassistedweatherapp.weather.WeatherDataProvider;
import kapil.voiceassistedweatherapp.weather.models.weather.WeatherData;

/**
 * Created by Kapil on 29/01/17.
 */

public class WeatherPresenter implements WeatherContract.Presenter, RecognitionListener, OnWeatherDataReceivedListener {
    private static final String SPEECH_TAG = "Speech Recognition";

    private Context context;
    private WeatherContract.View view;

    @Inject SpeechRecognizer speechRecognizer;
    private Intent speechIntent;

    @Inject WeatherDataProvider weatherDataProvider;

    private String latestRequestedString;

    @Inject
    public WeatherPresenter(Context context) {
        this.context = context;

        setUpSpeechRecognizer();
        initializeWeatherService();

        latestRequestedString = "";
    }

    @Inject void setUpSpeechRecognizer() {
        if (speechRecognizer != null) {
            speechRecognizer.setRecognitionListener(this);

            speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            speechIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        }
    }

    @Inject void initializeWeatherService() {
        if (weatherDataProvider != null) {
            weatherDataProvider.setOnWeatherDataReceivedListener(this);
        }
    }

    @Override
    public void triggerSpeechRecognizer() {
        if (speechRecognizer != null) {
            speechRecognizer.startListening(speechIntent);
        }
    }

    @Override
    public void retryDataFetch() {
        if (weatherDataProvider != null) {
            view.showLoader(true);
            weatherDataProvider.requestWeatherData(latestRequestedString);
        }
    }

    @Override
    public void onReadyForSpeech(Bundle params) {
        Log.i(SPEECH_TAG, "onReady");
        if (context != null) {
            view.setVoiceString(context.getString(R.string.voice_listening));
        }
        view.setVoiceListeningAnimationEnabled(true);
    }

    @Override
    public void onBeginningOfSpeech() {
        Log.i(SPEECH_TAG, "onBegin");
    }

    @Override
    public void onRmsChanged(float rmsdB) {
        Log.i(SPEECH_TAG, "onRmsChanged: " + rmsdB);
    }

    @Override
    public void onBufferReceived(byte[] buffer) {
        Log.i(SPEECH_TAG, "onBufferReceived");
    }

    @Override
    public void onEndOfSpeech() {
        Log.i(SPEECH_TAG, "onEnd");
        view.setVoiceListeningAnimationEnabled(false);
    }

    @Override
    public void onError(int error) {
        Log.i(SPEECH_TAG, "showToastErrorMessage: " + error);
        if (context != null) {
            view.setVoiceString(context.getString(R.string.voice_button_promt));
        }
        view.setVoiceListeningAnimationEnabled(false);
    }

    @Override
    public void onResults(Bundle results) {
        ArrayList resultList = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        String bestResult = (String) resultList.get(0);
        Log.i(SPEECH_TAG, "onResult: " + bestResult);
        view.setVoiceString(bestResult);
        latestRequestedString = bestResult;

        if (weatherDataProvider != null) {
            view.showLoader(true);
            weatherDataProvider.requestWeatherData(bestResult);
        }
    }

    @Override
    public void onPartialResults(Bundle partialResults) {
        ArrayList resultList = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        String bestResult = (String) resultList.get(0);
        Log.i(SPEECH_TAG, "onPartialResult: " + bestResult);
        view.setVoiceString(String.format(bestResult + "%s", "..."));
    }

    @Override
    public void onEvent(int eventType, Bundle params) {
        Log.i(SPEECH_TAG, "onEvent");
    }

    @Override
    public void onWeatherDataReceived(WeatherData weatherData) {
        view.showLoader(false);
        view.showNoInternetSnackbar(false);
        view.showWeatherData(weatherData);
        view.showWeatherDataViewContainer(true);
    }

    @Override
    public void onFailure(@WeatherResponseFailureType int failureType) {
        view.showLoader(false);
        int resId = 0;
        switch (failureType) {
            case NO_INTERNET:
                resId = R.string.no_internet;
                view.showNoInternetSnackbar(true);
                break;
            case GPS_UNAVAILABLE:
                resId = R.string.gps_unavailable;
                break;
            case WITAI_NULL_RESPONSE:
                resId = R.string.null_wit_ai_response;
                break;
            case WEATHER_INTENT_NOT_FOUND:
                resId = R.string.weather_intent_not_found;
                break;
            case PLACE_UNRECOGNIZED:
                resId = R.string.place_unrecognized;
                break;
        }
        if (resId != R.string.no_internet) {
            view.showToastErrorMessage(resId);
            view.showWeatherDataViewContainer(false);
        }
    }

    public void setView(WeatherContract.View view) {
        this.view = view;
    }

    void destroy() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }
}
