import { NativeModules, NativeEventEmitter, Platform, PermissionsAndroid } from "react-native";
const {
  NativeVoiceStream
} = NativeModules;
const EventEmitter = new NativeEventEmitter(NativeVoiceStream);
const eventKey = "data";
const VoiceStream = {
  /**
   * Configures the audio recording parameters and prepares the voice streaming session
   * @param options Configuration for sample rate, buffer size, and other audio settings
   */
  init: options => {
    return NativeVoiceStream.init(options);
  },
  /**
   * Begins capturing audio input and streaming base64-encoded chunks
   */
  start: () => {
    return NativeVoiceStream.start();
  },
  /**
   * Terminates the active recording session and releases audio resources
   */
  stop: () => {
    return NativeVoiceStream.stop();
  },
  /**
   * Registers a callback to receive real-time base64 audio data chunks
   * @param event Must be 'data'
   * @param callback Function called with each audio chunk
   * @returns Subscription that can be removed
   */
  listen: (event, callback) => {
    if (event !== eventKey) {
      throw new Error("Invalid event");
    }
    EventEmitter.removeAllListeners(eventKey);
    return EventEmitter.addListener(eventKey, callback);
  }
};

/**
 * Verifies whether microphone access is currently granted for this app
 * @returns True if permission is granted, false otherwise
 */
export const checkMicrophonePermission = async () => {
  if (Platform.OS === "ios") {
    return NativeVoiceStream.checkMicrophonePermission();
  } else if (Platform.OS === "android") {
    try {
      const granted = await PermissionsAndroid.check(PermissionsAndroid.PERMISSIONS.RECORD_AUDIO);
      return granted;
    } catch (error) {
      return false;
    }
  }
  return false;
};

/**
 * Prompts the user to grant microphone access if not already authorized (Android only)
 * @returns True if permission granted or already exists, false if denied
 */
export const requestMicrophonePermission = async () => {
  if (Platform.OS === "ios") {
    return NativeVoiceStream.requestMicrophonePermission();
  } else if (Platform.OS === "android") {
    try {
      const granted = await PermissionsAndroid.request(PermissionsAndroid.PERMISSIONS.RECORD_AUDIO);
      return granted === PermissionsAndroid.RESULTS.GRANTED;
    } catch (error) {
      return false;
    }
  }
  return false;
};
export default VoiceStream;
//# sourceMappingURL=index.js.map