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
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VoiceStreamModule extends ReactContextBaseJavaModule {
    private static final String TAG = "VoiceStreamModule";
    private static final String MODULE_NAME = "VoiceStream";
    
    private ReactApplicationContext reactContext;
    private AudioRecord audioRecord;
    private ExecutorService recordingExecutor;
    private Handler mainHandler;
    private boolean isRecording = false;
    
    // Audio configuration
    private int sampleRate = 44100;
    private int bufferSize = 2048;
    private int channels = 1;
    private int encoding = AudioFormat.ENCODING_PCM_16BIT;
    
    // Audio recording variables
    private int audioSource = MediaRecorder.AudioSource.MIC;
    private int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    private int minBufferSize;

    public VoiceStreamModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.recordingExecutor = Executors.newSingleThreadExecutor();
    }

    @Override
    public String getName() {
        return MODULE_NAME;
    }

    @ReactMethod
    public void init(ReadableMap options, Promise promise) {
        try {
            Log.d(TAG, "Initializing VoiceStream");
            
            // Parse options
            if (options.hasKey("sampleRate")) {
                this.sampleRate = options.getInt("sampleRate");
            }
            if (options.hasKey("bufferSize")) {
                this.bufferSize = options.getInt("bufferSize");
            }
            if (options.hasKey("channels")) {
                this.channels = options.getInt("channels");
                this.channelConfig = channels == 1 ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO;
            }
            
            // Calculate minimum buffer size
            this.minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, encoding);
            
            // Ensure buffer size is at least the minimum required
            if (this.bufferSize < this.minBufferSize) {
                this.bufferSize = this.minBufferSize;
            }
            
            Log.d(TAG, String.format("Audio config - SampleRate: %d, BufferSize: %d, Channels: %d, MinBufferSize: %d", 
                    sampleRate, bufferSize, channels, minBufferSize));
            
            promise.resolve(null);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing VoiceStream", e);
            promise.reject("INIT_ERROR", "Failed to initialize VoiceStream: " + e.getMessage());
        }
    }

    @ReactMethod
    public void start(Promise promise) {
        try {
            Log.d(TAG, "Starting voice stream");
            
            // Check permissions
            if (!hasAudioPermission()) {
                promise.reject("PERMISSION_ERROR", "Audio recording permission not granted");
                return;
            }
            
            if (isRecording) {
                Log.w(TAG, "Already recording");
                promise.resolve(null);
                return;
            }
            
            // Initialize AudioRecord
            audioRecord = new AudioRecord(
                    audioSource,
                    sampleRate,
                    channelConfig,
                    encoding,
                    bufferSize
            );
            
            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                promise.reject("AUDIO_RECORD_ERROR", "Failed to initialize AudioRecord");
                return;
            }
            
            isRecording = true;
            audioRecord.startRecording();
            
            // Start recording in background thread
            recordingExecutor.execute(this::recordAudio);
            
            Log.d(TAG, "Voice stream started successfully");
            promise.resolve(null);
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting voice stream", e);
            promise.reject("START_ERROR", "Failed to start voice stream: " + e.getMessage());
        }
    }

    @ReactMethod
    public void stop(Promise promise) {
        try {
            Log.d(TAG, "Stopping voice stream");
            
            if (!isRecording) {
                Log.w(TAG, "Not currently recording");
                promise.resolve(null);
                return;
            }
            
            isRecording = false;
            
            if (audioRecord != null) {
                audioRecord.stop();
                audioRecord.release();
                audioRecord = null;
            }
            
            Log.d(TAG, "Voice stream stopped successfully");
            promise.resolve(null);
            
        } catch (Exception e) {
            Log.e(TAG, "Error stopping voice stream", e);
            promise.reject("STOP_ERROR", "Failed to stop voice stream: " + e.getMessage());
        }
    }

    private void recordAudio() {
        Log.d(TAG, "Starting audio recording thread");
        
        // Calculate buffer size for reading (using smaller chunks for more frequent updates)
        int readBufferSize = Math.min(bufferSize, 4096);
        byte[] audioBuffer = new byte[readBufferSize];
        
        while (isRecording && audioRecord != null) {
            try {
                int bytesRead = audioRecord.read(audioBuffer, 0, readBufferSize);
                
                if (bytesRead > 0) {
                    // Convert to base64
                    byte[] audioData = new byte[bytesRead];
                    System.arraycopy(audioBuffer, 0, audioData, 0, bytesRead);
                    String base64Data = Base64.encodeToString(audioData, Base64.NO_WRAP);
                    
                    // Send data to JavaScript on main thread
                    mainHandler.post(() -> sendAudioData(base64Data));
                } else if (bytesRead == AudioRecord.ERROR_INVALID_OPERATION) {
                    Log.e(TAG, "Invalid operation error in audio recording");
                    break;
                } else if (bytesRead == AudioRecord.ERROR_BAD_VALUE) {
                    Log.e(TAG, "Bad value error in audio recording");
                    break;
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error in audio recording loop", e);
                break;
            }
        }
        
        Log.d(TAG, "Audio recording thread stopped");
    }

    private void sendAudioData(String base64Data) {
        try {
            reactContext
                    .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit("data", base64Data);
        } catch (Exception e) {
            Log.e(TAG, "Error sending audio data to JavaScript", e);
        }
    }

    private boolean hasAudioPermission() {
        return ActivityCompat.checkSelfPermission(reactContext, Manifest.permission.RECORD_AUDIO) 
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void invalidate() {
        if (isRecording) {
            isRecording = false;
            if (audioRecord != null) {
                audioRecord.stop();
                audioRecord.release();
                audioRecord = null;
            }
        }
        if (recordingExecutor != null && !recordingExecutor.isShutdown()) {
            recordingExecutor.shutdown();
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