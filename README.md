# react-native-voice-stream
A React Native library for real-time audio recording and streaming as base64-encoded chunks to JavaScript in real-time. Perfect for voice messaging, live transcription, or audio-based real-time applications.

## Installation

```sh
npm install react-native-voice-stream
```

### iOS

Run `pod install` in your iOS directory.

## Compatibility

- **React Native**: `>=0.68.0`
- **Platform**: iOS only (Android support coming soon)
- **Architecture**: Supports both Old and New Architecture (TurboModules)

## Usage

```js
import VoiceStream, { VoiceStreamEmitter } from 'react-native-voice-stream';

// Initialize with options
VoiceStream.init({ 
  sampleRate: 44100, 
  channels: 1,
  bufferSize: 2048 
});

// Listen for real-time base64 audio data
const subscription = VoiceStreamEmitter?.addListener('data', (base64Audio) => {
  console.log('Received audio chunk:', base64Audio);
});

// Start recording
VoiceStream.start();

// Stop recording
VoiceStream.stop();

// Clean up
subscription?.remove();
```

## License

MIT