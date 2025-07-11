// @ts-ignore
import { NativeModules, NativeEventEmitter, Platform, PermissionsAndroid } from 'react-native';
const {
  VoiceStream: NativeVoiceStream
} = NativeModules;
const EventEmitter = new NativeEventEmitter(NativeVoiceStream);
const eventKey = 'data';
const VoiceStream = {
  init: options => {
    return NativeVoiceStream.init(options);
  },
  start: () => {
    return NativeVoiceStream.start();
  },
  stop: () => {
    return NativeVoiceStream.stop();
  },
  listen: (event, callback) => {
    if (event !== eventKey) {
      throw new Error('Invalid event');
    }
    EventEmitter.removeAllListeners(eventKey);
    return EventEmitter.addListener(eventKey, callback);
  }
};
export const checkMicrophonePermission = async () => {
  if (Platform.OS === 'ios') {
    return NativeVoiceStream.checkMicrophonePermission();
  } else if (Platform.OS === 'android') {
    try {
      const granted = await PermissionsAndroid.check(PermissionsAndroid.PERMISSIONS.RECORD_AUDIO);
      return granted;
    } catch (error) {
      return false;
    }
  }
  return false;
};
export const requestMicrophonePermission = async () => {
  if (Platform.OS === 'ios') {
    return NativeVoiceStream.requestMicrophonePermission();
  } else if (Platform.OS === 'android') {
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