# react-native-voice-stream
A React Native library for real-time audio recording and streaming as base64-encoded chunks to JavaScript in real-time. Perfect for voice messaging, live transcription, or audio-based real-time applications.

## Installation

```sh
npm install react-native-voice-stream
```

### Auto-linking (React Native ≥ 0.60)

No manual linking required! The module will be automatically linked.

**For iOS**: Run `cd ios && pod install`  
**For Android**: No additional steps needed

### Manual linking (React Native < 0.60)

Please refer to the [React Native documentation](https://reactnative.dev/docs/linking-libraries-ios) for manual linking instructions.

## Compatibility

- **React Native**: `>=0.68.0`
- **Platforms**: iOS and Android
- **Architecture**: Supports both Old and New Architecture (TurboModules)
- **Auto-linking**: ✅ Fully supported (React Native ≥ 0.60)

## Usage

```js
import VoiceStream from 'react-native-voice-stream';
import { PermissionsAndroid, Platform } from 'react-native';

// Request microphone permission (Android)
const requestAudioPermission = async () => {
  if (Platform.OS === 'android') {
    const granted = await PermissionsAndroid.request(
      PermissionsAndroid.PERMISSIONS.RECORD_AUDIO
    );
    return granted === PermissionsAndroid.RESULTS.GRANTED;
  }
  return true; // iOS handles permissions automatically
};

// Initialize with options
await VoiceStream.init({ 
  sampleRate: 44100, 
  channels: 1,
  bufferSize: 2048 
});

// Listen for real-time base64 audio data
const subscription = VoiceStream.listen('data', (base64Audio) => {
  console.log('Received audio chunk:', base64Audio);
});

// Request permission and start recording
const hasPermission = await requestAudioPermission();
if (hasPermission) {
  await VoiceStream.start();
}

// Stop recording
await VoiceStream.stop();

// Clean up
subscription?.remove();
```

## License

MIT