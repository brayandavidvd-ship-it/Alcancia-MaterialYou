package com.example.util

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Helper composable para preparar y lanzar el reconocimiento de voz del sistema
 * mediante RecognizerIntent sin requerir permisos sensibles en tiempo de ejecución.
 */
@Composable
fun rememberVoiceDictationLauncher(
    prompt: String,
    onResult: (String) -> Unit,
    onUnavailable: () -> Unit = {}
): () -> Unit {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val transcript = matches?.firstOrNull()?.trim().orEmpty()
            if (transcript.isNotEmpty()) {
                onResult(transcript)
            }
        }
    }

    return remember(launcher, prompt) {
        {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-MX")
                putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
            }
            try {
                launcher.launch(intent)
            } catch (_: ActivityNotFoundException) {
                onUnavailable()
            } catch (_: Exception) {
                onUnavailable()
            }
        }
    }
}
