package com.dietdoctor.voicestream;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public class VoiceStreamModule extends ReactContextBaseJavaModule {
    private static final String TAG = "VoiceStreamModule";
    private static final String MODULE_NAME = "VoiceStream";
    private static final String DATA_EVENT = "data";
    
    private final AtomicBoolean isRecording = new AtomicBoolean(false);
    private final AtomicBoolean isInitialized = new AtomicBoolean(false);
    private final ReentrantLock recordingLock = new ReentrantLock();
    
    private final ReactApplicationContext reactContext;
    private final Handler mainHandler;
    
    private AudioRecord audioRecord;
    private ExecutorService recordingExecutor;
    private Future<?> recordingTask;
    
    private int sampleRate = 44100;
    private int channels = 1;
    private int bitsPerSample = 16;
    private int audioSource = MediaRecorder.AudioSource.MIC;
    private int bufferSize = 2048;
    
    private int channelConfig;
    private int audioFormat;
    private int minBufferSize;

    public VoiceStreamModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public String getName() {
        return MODULE_NAME;
    }

    @ReactMethod
    public void init(ReadableMap options, Promise promise) {
        recordingLock.lock();
        try {
            if (isRecording.get()) {
                promise.reject("INIT_ERROR", "Cannot initialize while recording is active");
                return;
            }

            Log.d(TAG, "Initializing VoiceStream with options: " + options.toString());
            
            if (!parseOptions(options)) {
                promise.reject("INIT_ERROR", "Invalid audio configuration options");
                return;
            }
            
            channelConfig = (channels == 1) ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO;
            audioFormat = (bitsPerSample == 16) ? AudioFormat.ENCODING_PCM_16BIT : AudioFormat.ENCODING_PCM_8BIT;
            
            minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
            if (minBufferSize == AudioRecord.ERROR_BAD_VALUE || minBufferSize == AudioRecord.ERROR) {
                promise.reject("INIT_ERROR", "Invalid audio configuration for this device");
                return;
            }
            
            if (bufferSize < minBufferSize) {
                bufferSize = minBufferSize;
                Log.w(TAG, "Buffer size increased to minimum required: " + bufferSize);
            }
            
            if (recordingExecutor != null && !recordingExecutor.isShutdown()) {
                recordingExecutor.shutdown();
            }
            recordingExecutor = Executors.newSingleThreadExecutor(r -> {
                Thread thread = new Thread(r, "VoiceStream-Recording");
                thread.setPriority(Thread.MAX_PRIORITY);
                return thread;
            });
            
            isInitialized.set(true);
            
            Log.d(TAG, String.format("VoiceStream initialized - SampleRate: %d, Channels: %d, " +
                    "BitsPerSample: %d, AudioSource: %d, BufferSize: %d, MinBufferSize: %d", 
                    sampleRate, channels, bitsPerSample, audioSource, bufferSize, minBufferSize));
            
            promise.resolve(null);
            
        } catch (Exception e) {
            Log.e(TAG, "Error initializing VoiceStream", e);
            promise.reject("INIT_ERROR", "Failed to initialize: " + e.getMessage());
        } finally {
            recordingLock.unlock();
        }
    }

    @ReactMethod
    public void start(Promise promise) {
        recordingLock.lock();
        try {
            if (!isInitialized.get()) {
                promise.reject("START_ERROR", "VoiceStream not initialized. Call init() first.");
                return;
            }
            
            if (isRecording.get()) {
                Log.w(TAG, "Already recording");
                promise.resolve(null);
                return;
            }
            
            if (!hasAudioPermission()) {
                promise.reject("PERMISSION_ERROR", "Audio recording permission not granted");
                return;
            }
            
            Log.d(TAG, "Starting voice stream");
            
            try {
                audioRecord = new AudioRecord(
                        audioSource,
                        sampleRate,
                        channelConfig,
                        audioFormat,
                        Math.max(bufferSize, minBufferSize * 2)
                );
                
                if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                    audioRecord.release();
                    audioRecord = null;
                    promise.reject("AUDIO_RECORD_ERROR", "Failed to initialize AudioRecord");
                    return;
                }
                
            } catch (Exception e) {
                if (audioRecord != null) {
                    audioRecord.release();
                    audioRecord = null;
                }
                promise.reject("AUDIO_RECORD_ERROR", "Failed to create AudioRecord: " + e.getMessage());
                return;
            }
            
            try {
                audioRecord.startRecording();
                isRecording.set(true);
                
                recordingTask = recordingExecutor.submit(this::recordAudioLoop);
                
                Log.d(TAG, "Voice stream started successfully");
                promise.resolve(null);
                
            } catch (Exception e) {
                isRecording.set(false);
                if (audioRecord != null) {
                    try {
                        audioRecord.stop();
                        audioRecord.release();
                    } catch (Exception ignored) {}
                    audioRecord = null;
                }
                promise.reject("START_ERROR", "Failed to start recording: " + e.getMessage());
            }
            
        } finally {
            recordingLock.unlock();
        }
    }

    @ReactMethod
    public void stop(Promise promise) {
        recordingLock.lock();
        try {
            if (!isRecording.get()) {
                Log.w(TAG, "Not currently recording");
                promise.resolve(null);
                return;
            }
            
            Log.d(TAG, "Stopping voice stream");
            
            isRecording.set(false);
            
            if (audioRecord != null) {
                try {
                    audioRecord.stop();
                } catch (Exception e) {
                    Log.w(TAG, "Error stopping AudioRecord: " + e.getMessage());
                }
                
                try {
                    audioRecord.release();
                } catch (Exception e) {
                    Log.w(TAG, "Error releasing AudioRecord: " + e.getMessage());
                }
                audioRecord = null;
            }
            
            if (recordingTask != null && !recordingTask.isDone()) {
                try {
                    recordingTask.get(500, TimeUnit.MILLISECONDS);
                } catch (Exception e) {
                    Log.w(TAG, "Recording task didn't finish gracefully: " + e.getMessage());
                    recordingTask.cancel(true);
                }
                recordingTask = null;
            }
            
            Log.d(TAG, "Voice stream stopped successfully");
            promise.resolve(null);
            
        } catch (Exception e) {
            Log.e(TAG, "Error stopping voice stream", e);
            promise.reject("STOP_ERROR", "Failed to stop voice stream: " + e.getMessage());
        } finally {
            recordingLock.unlock();
        }
    }

    private void recordAudioLoop() {
        Log.d(TAG, "Starting audio recording loop");
        
        int readBufferSize = Math.min(bufferSize / 4, 4096);
        byte[] audioBuffer = new byte[readBufferSize];
        
        while (isRecording.get() && audioRecord != null) {
            try {
                int bytesRead = audioRecord.read(audioBuffer, 0, readBufferSize);
                
                if (!isRecording.get()) {
                    break;
                }
                
                if (bytesRead > 0) {
                    byte[] audioData = new byte[bytesRead];
                    System.arraycopy(audioBuffer, 0, audioData, 0, bytesRead);
                    
                    if (isRecording.get()) {
                        String base64Data = Base64.encodeToString(audioData, Base64.NO_WRAP);
                        sendAudioDataSafely(base64Data);
                    }
                    
                } else if (bytesRead == AudioRecord.ERROR_INVALID_OPERATION) {
                    Log.e(TAG, "Invalid operation error in audio recording");
                    break;
                } else if (bytesRead == AudioRecord.ERROR_BAD_VALUE) {
                    Log.e(TAG, "Bad value error in audio recording");
                    break;
                } else if (bytesRead == AudioRecord.ERROR) {
                    Log.e(TAG, "Generic error in audio recording");
                    break;
                }
                
            } catch (Exception e) {
                if (isRecording.get()) {
                    Log.e(TAG, "Error in audio recording loop", e);
                }
                break;
            }
        }
        
        Log.d(TAG, "Audio recording loop finished");
    }

    private void sendAudioDataSafely(String base64Data) {
        if (!isRecording.get()) {
            return;
        }
        
        mainHandler.post(() -> {
            if (!isRecording.get()) {
                return;
            }
            
            try {
                reactContext
                        .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                        .emit(DATA_EVENT, base64Data);
            } catch (Exception e) {
                Log.e(TAG, "Error sending audio data to JavaScript", e);
            }
        });
    }

    private boolean parseOptions(ReadableMap options) {
        try {
            if (options.hasKey("sampleRate")) {
                int rate = options.getInt("sampleRate");
                if (rate < 8000 || rate > 48000) return false;
                this.sampleRate = rate;
            }
            
            if (options.hasKey("channels")) {
                int ch = options.getInt("channels");
                if (ch != 1 && ch != 2) return false;
                this.channels = ch;
            }
            
            if (options.hasKey("bitsPerSample")) {
                int bits = options.getInt("bitsPerSample");
                if (bits != 8 && bits != 16) return false;
                this.bitsPerSample = bits;
            }
            
            if (options.hasKey("audioSource")) {
                int source = options.getInt("audioSource");
                if (isValidAudioSource(source)) {
                    this.audioSource = source;
                }
            }
            
            if (options.hasKey("bufferSize")) {
                int size = options.getInt("bufferSize");
                if (size >= 1024) {
                    this.bufferSize = size;
                }
            }
            
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error parsing options", e);
            return false;
        }
    }

    private boolean isValidAudioSource(int source) {
        return source == MediaRecorder.AudioSource.DEFAULT ||
               source == MediaRecorder.AudioSource.MIC ||
               source == MediaRecorder.AudioSource.VOICE_RECOGNITION ||
               source == MediaRecorder.AudioSource.VOICE_COMMUNICATION ||
               source == MediaRecorder.AudioSource.CAMCORDER ||
               source == MediaRecorder.AudioSource.VOICE_DOWNLINK ||
               source == MediaRecorder.AudioSource.VOICE_UPLINK;
    }

    private boolean hasAudioPermission() {
        return ActivityCompat.checkSelfPermission(reactContext, Manifest.permission.RECORD_AUDIO) 
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void invalidate() {
        Log.d(TAG, "Module invalidating");
        
        recordingLock.lock();
        try {
            isRecording.set(false);
            isInitialized.set(false);
            
            if (audioRecord != null) {
                try {
                    audioRecord.stop();
                    audioRecord.release();
                } catch (Exception e) {
                    Log.w(TAG, "Error cleaning up AudioRecord: " + e.getMessage());
                }
                audioRecord = null;
            }
            
            if (recordingExecutor != null && !recordingExecutor.isShutdown()) {
                recordingExecutor.shutdown();
                try {
                    if (!recordingExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                        recordingExecutor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    recordingExecutor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
            
        } finally {
            recordingLock.unlock();
        }
    }

    @ReactMethod
    public void addListener(String eventName) {
        // Required for NativeEventEmitter
    }

    @ReactMethod
    public void removeListeners(Integer count) {
        // Required for NativeEventEmitter
    }
} 