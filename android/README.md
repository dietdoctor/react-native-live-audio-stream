# Android Integration

## Overview
This directory contains the Android implementation of the VoiceStream React Native module. It provides real-time voice streaming functionality that converts microphone input to base64 encoded strings.

## Features
- Real-time audio recording from device microphone
- PCM 16-bit audio format support
- Configurable sample rate, buffer size, and channels
- Base64 encoding of audio data
- Event-driven architecture for streaming audio chunks
- Automatic permission handling
- **Auto-linking support** - No manual linking required!

## Auto-linking Support ✅

This module supports React Native auto-linking (RN ≥ 0.60). No manual linking steps are required!

The module will be automatically linked when you:
1. Install the package: `npm install react-native-voice-stream`
2. Run `cd ios && pod install` (for iOS)
3. Rebuild your app

## Integration Steps

### 1. Request Audio Permission

Add audio recording permission request in your app:

```javascript
import { PermissionsAndroid } from 'react-native';

const requestAudioPermission = async () => {
  try {
    const granted = await PermissionsAndroid.request(
      PermissionsAndroid.PERMISSIONS.RECORD_AUDIO,
      {
        title: 'Audio Recording Permission',
        message: 'This app needs access to your microphone to record audio.',
        buttonNeutral: 'Ask Me Later',
        buttonNegative: 'Cancel',
        buttonPositive: 'OK',
      },
    );
    return granted === PermissionsAndroid.RESULTS.GRANTED;
  } catch (err) {
    console.warn(err);
    return false;
  }
};
```

### 2. Usage Example

```javascript
import VoiceStreamer from 'react-native-voice-stream';

// Initialize with options
await VoiceStreamer.init({
  sampleRate: 44100,
  bufferSize: 2048,
  channels: 1
});

// Listen for audio data
const subscription = VoiceStreamer.listen('data', (base64Data) => {
  console.log('Received audio chunk:', base64Data);
  // Process the base64 encoded audio data
});

// Start recording
await VoiceStreamer.start();

// Stop recording
await VoiceStreamer.stop();

// Don't forget to remove the listener
subscription.remove();
```

## Configuration Options

- `sampleRate`: Audio sample rate in Hz (default: 44100)
- `bufferSize`: Buffer size in bytes (default: 2048)
- `channels`: Number of audio channels - 1 for mono, 2 for stereo (default: 1)

## Technical Details

### Audio Format
- **Format**: PCM 16-bit signed integer
- **Encoding**: Little-endian
- **Channels**: Configurable (1 = mono, 2 = stereo)
- **Sample Rate**: Configurable (default 44100 Hz)

### Threading
- Audio recording runs on a dedicated background thread
- Base64 encoding and React Native events are dispatched on the main thread
- Automatic cleanup on module destruction

### Permissions
The module automatically checks for `RECORD_AUDIO` permission before starting recording. Make sure to request this permission in your app before calling `start()`.

## Error Handling

The module provides detailed error messages for common issues:
- `PERMISSION_ERROR`: Audio recording permission not granted
- `INIT_ERROR`: Failed to initialize the module
- `START_ERROR`: Failed to start audio recording
- `STOP_ERROR`: Failed to stop audio recording
- `AUDIO_RECORD_ERROR`: Failed to initialize AudioRecord

## Minimum Requirements

- **Android API Level**: 16 (Android 4.1)
- **Target SDK**: 28+
- **Permissions**: `RECORD_AUDIO` 