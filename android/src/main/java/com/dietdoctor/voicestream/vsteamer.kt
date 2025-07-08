package com.dietdoctor.voicestream

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import androidx.core.app.ActivityCompat
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class VoiceStreamModule(private val reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
    
    companion object {
        private const val TAG = "VoiceStreamModule"
        private const val MODULE_NAME = "VoiceStream"
        private const val DATA_EVENT = "data"
        
        private const val DEFAULT_SAMPLE_RATE = 44100
        private const val DEFAULT_CHANNELS = 1
        private const val DEFAULT_BITS_PER_SAMPLE = 16
        private const val DEFAULT_AUDIO_SOURCE = MediaRecorder.AudioSource.MIC
        private const val DEFAULT_BUFFER_SIZE = 2048
    }

    private val isRecording = AtomicBoolean(false)
    private val isInitialized = AtomicBoolean(false)
    private val recordingLock = ReentrantLock()
    
    private val recordingScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var recordingJob: Job? = null
    
    private val mainHandler = Handler(Looper.getMainLooper())
    
    private var audioRecord: AudioRecord? = null
    
    private var config = AudioConfig()
    
    data class AudioConfig(
        var sampleRate: Int = DEFAULT_SAMPLE_RATE,
        var channels: Int = DEFAULT_CHANNELS,
        var bitsPerSample: Int = DEFAULT_BITS_PER_SAMPLE,
        var audioSource: Int = DEFAULT_AUDIO_SOURCE,
        var bufferSize: Int = DEFAULT_BUFFER_SIZE,
        var channelConfig: Int = AudioFormat.CHANNEL_IN_MONO,
        var audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT,
        var minBufferSize: Int = 0
    )

    override fun getName(): String = MODULE_NAME

    @ReactMethod
    fun init(options: ReadableMap, promise: Promise) {
        recordingLock.withLock {
            try {
                if (isRecording.get()) {
                    promise.reject("INIT_ERROR", "Cannot initialize while recording is active")
                    return
                }

                Log.d(TAG, "Initializing VoiceStream with options: $options")
                
                val newConfig = parseOptions(options) ?: run {
                    promise.reject("INIT_ERROR", "Invalid audio configuration options")
                    return
                }
                
                newConfig.channelConfig = if (newConfig.channels == 1) {
                    AudioFormat.CHANNEL_IN_MONO
                } else {
                    AudioFormat.CHANNEL_IN_STEREO
                }
                
                newConfig.audioFormat = if (newConfig.bitsPerSample == 16) {
                    AudioFormat.ENCODING_PCM_16BIT
                } else {
                    AudioFormat.ENCODING_PCM_8BIT
                }
                
                newConfig.minBufferSize = AudioRecord.getMinBufferSize(
                    newConfig.sampleRate,
                    newConfig.channelConfig,
                    newConfig.audioFormat
                )
                
                when {
                    newConfig.minBufferSize == AudioRecord.ERROR_BAD_VALUE ||
                    newConfig.minBufferSize == AudioRecord.ERROR -> {
                        promise.reject("INIT_ERROR", "Invalid audio configuration for this device")
                        return
                    }
                    newConfig.bufferSize < newConfig.minBufferSize -> {
                        newConfig.bufferSize = newConfig.minBufferSize
                        Log.w(TAG, "Buffer size increased to minimum required: ${newConfig.bufferSize}")
                    }
                }
                
                config = newConfig
                isInitialized.set(true)
                
                Log.d(TAG, "VoiceStream initialized - ${configToString()}")
                promise.resolve(null)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing VoiceStream", e)
                promise.reject("INIT_ERROR", "Failed to initialize: ${e.message}")
            }
        }
    }

    @ReactMethod
    fun start(promise: Promise) {
        recordingLock.withLock {
            try {
                when {
                    !isInitialized.get() -> {
                        promise.reject("START_ERROR", "VoiceStream not initialized. Call init() first.")
                        return
                    }
                    isRecording.get() -> {
                        Log.w(TAG, "Already recording")
                        promise.resolve(null)
                        return
                    }
                    !hasAudioPermission() -> {
                        promise.reject("PERMISSION_ERROR", "Audio recording permission not granted")
                        return
                    }
                }
                
                Log.d(TAG, "Starting voice stream")
                
                val audioRecord = createAudioRecord() ?: run {
                    promise.reject("AUDIO_RECORD_ERROR", "Failed to initialize AudioRecord")
                    return
                }
                
                this.audioRecord = audioRecord
                
                try {
                    audioRecord.startRecording()
                    isRecording.set(true)
                    
                    recordingJob = recordingScope.launch {
                        recordAudioLoop()
                    }
                    
                    Log.d(TAG, "Voice stream started successfully")
                    promise.resolve(null)
                    
                } catch (e: Exception) {
                    cleanupAudioRecord()
                    isRecording.set(false)
                    promise.reject("START_ERROR", "Failed to start recording: ${e.message}")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in start method", e)
                promise.reject("START_ERROR", "Unexpected error: ${e.message}")
            }
        }
    }

    @ReactMethod
    fun stop(promise: Promise) {
        recordingLock.withLock {
            try {
                if (!isRecording.get()) {
                    Log.w(TAG, "Not currently recording")
                    promise.resolve(null)
                    return
                }
                
                Log.d(TAG, "Stopping voice stream")
                
                isRecording.set(false)
                
                audioRecord?.let { record ->
                    try {
                        record.stop()
                    } catch (e: Exception) {
                        Log.w(TAG, "Error stopping AudioRecord: ${e.message}")
                    }
                }
                
                recordingJob?.let { job ->
                    runBlocking {
                        try {
                            withTimeout(500) {
                                job.cancelAndJoin()
                            }
                        } catch (e: TimeoutCancellationException) {
                            Log.w(TAG, "Recording job didn't finish gracefully, force cancelled")
                        }
                    }
                    recordingJob = null
                }
                
                cleanupAudioRecord()
                
                Log.d(TAG, "Voice stream stopped successfully")
                promise.resolve(null)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping voice stream", e)
                promise.reject("STOP_ERROR", "Failed to stop voice stream: ${e.message}")
            }
        }
    }

    private suspend fun recordAudioLoop() {
        Log.d(TAG, "Starting audio recording loop")
        
        val readBufferSize = minOf(config.bufferSize / 4, 4096)
        val audioBuffer = ByteArray(readBufferSize)
        
        try {
            while (isRecording.get() && audioRecord != null && !currentCoroutineContext().isActive.not()) {
                val record = audioRecord ?: break
                
                val bytesRead = withContext(Dispatchers.IO) {
                    record.read(audioBuffer, 0, readBufferSize)
                }
                
                if (!isRecording.get() || !currentCoroutineContext().isActive) {
                    break
                }
                
                when {
                    bytesRead > 0 -> {
                        val audioData = audioBuffer.copyOfRange(0, bytesRead)
                        
                        if (isRecording.get() && currentCoroutineContext().isActive) {
                            val base64Data = Base64.encodeToString(audioData, Base64.NO_WRAP)
                            sendAudioDataSafely(base64Data)
                        }
                    }
                    bytesRead == AudioRecord.ERROR_INVALID_OPERATION -> {
                        Log.e(TAG, "Invalid operation error in audio recording")
                        break
                    }
                    bytesRead == AudioRecord.ERROR_BAD_VALUE -> {
                        Log.e(TAG, "Bad value error in audio recording")
                        break
                    }
                    bytesRead == AudioRecord.ERROR -> {
                        Log.e(TAG, "Generic error in audio recording")
                        break
                    }
                }
                
                // tiny delay to prevent excessive CPU usage
                delay(1)
            }
        } catch (e: CancellationException) {
            Log.d(TAG, "Recording loop cancelled")
        } catch (e: Exception) {
            if (isRecording.get()) {
                Log.e(TAG, "Error in audio recording loop", e)
            }
        }
        
        Log.d(TAG, "Audio recording loop finished")
    }

    private fun sendAudioDataSafely(base64Data: String) {
        if (!isRecording.get()) return
        
        mainHandler.post {
            if (!isRecording.get()) return@post
            
            try {
                reactContext
                    .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                    .emit(DATA_EVENT, base64Data)
            } catch (e: Exception) {
                Log.e(TAG, "Error sending audio data to JavaScript", e)
            }
        }
    }

    private fun createAudioRecord(): AudioRecord? {
        return try {
            val record = AudioRecord(
                config.audioSource,
                config.sampleRate,
                config.channelConfig,
                config.audioFormat,
                maxOf(config.bufferSize, config.minBufferSize * 2)
            )
            
            if (record.state != AudioRecord.STATE_INITIALIZED) {
                record.release()
                null
            } else {
                record
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create AudioRecord", e)
            null
        }
    }

    private fun cleanupAudioRecord() {
        audioRecord?.let { record ->
            try {
                record.release()
            } catch (e: Exception) {
                Log.w(TAG, "Error releasing AudioRecord: ${e.message}")
            }
        }
        audioRecord = null
    }

    private fun parseOptions(options: ReadableMap): AudioConfig? {
        return try {
            val newConfig = config.copy()
            
            if (options.hasKey("sampleRate")) {
                val rate = options.getInt("sampleRate")
                if (rate !in 8000..48000) return null
                newConfig.sampleRate = rate
            }
            
            if (options.hasKey("channels")) {
                val channels = options.getInt("channels")
                if (channels !in 1..2) return null
                newConfig.channels = channels
            }
            
            if (options.hasKey("bitsPerSample")) {
                val bits = options.getInt("bitsPerSample")
                if (bits !in listOf(8, 16)) return null
                newConfig.bitsPerSample = bits
            }
            
            if (options.hasKey("audioSource")) {
                val source = options.getInt("audioSource")
                if (!isValidAudioSource(source)) return null
                newConfig.audioSource = source
            }
            
            if (options.hasKey("bufferSize")) {
                val size = options.getInt("bufferSize")
                if (size < 1024) return null
                newConfig.bufferSize = size
            }
            
            newConfig
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing options", e)
            null
        }
    }

    private fun isValidAudioSource(source: Int): Boolean {
        return source in listOf(
            MediaRecorder.AudioSource.DEFAULT,
            MediaRecorder.AudioSource.MIC,
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            MediaRecorder.AudioSource.CAMCORDER,
            MediaRecorder.AudioSource.VOICE_DOWNLINK,
            MediaRecorder.AudioSource.VOICE_UPLINK
        )
    }

    private fun hasAudioPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            reactContext,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun configToString(): String {
        return "SampleRate: ${config.sampleRate}, Channels: ${config.channels}, " +
               "BitsPerSample: ${config.bitsPerSample}, AudioSource: ${config.audioSource}, " +
               "BufferSize: ${config.bufferSize}, MinBufferSize: ${config.minBufferSize}"
    }

    override fun invalidate() {
        Log.d(TAG, "Module invalidating")
        
        recordingLock.withLock {
            isRecording.set(false)
            isInitialized.set(false)
            
            recordingScope.cancel()
            cleanupAudioRecord()
        }
    }

    @ReactMethod
    fun addListener(eventName: String) {
        // Required for NativeEventEmitter
    }

    @ReactMethod
    fun removeListeners(count: Int) {
        // Required for NativeEventEmitter
    }
}